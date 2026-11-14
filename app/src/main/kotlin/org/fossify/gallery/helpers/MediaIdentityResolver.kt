package org.fossify.gallery.helpers

import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.util.ArrayDeque

/**
 * Finds a file again after it was renamed or moved.
 *
 * The resolution goes through layers from the cheapest and most certain to the broadest:
 *
 *  0. Batch folder-relocation cache (when another file from the same moved folder was already found).
 *  1. The cached path itself (if it exists on disk).
 *  2. The MediaStore `_ID`, which is preserved by renames and moves on the same volume.
 *  3. A targeted MediaStore `SIZE` query (or batch `SIZE IN (...)` in a session) confirmed by the
 *     content fingerprint.
 *  4. A bounded, size-prefiltered filesystem fallback for hidden (`.nomedia`), vault (`.fenc`), or
 *     not-yet-indexed folders — scanning sibling/ancestor directories and standard media roots
 *     with a strict `stat()` cap so performance stays bounded.
 */
object MediaIdentityResolver {
    /** How deep we search below fallback roots when scanning for candidates. */
    private const val MAX_SCAN_DEPTH = 4

    /** Maximum number of files inspected (`stat()` only) during a single fallback scan. */
    private const val MAX_FALLBACK_FILES_VISITED = 5000

    /** Maximum number of size-matched candidates retained per size bucket. */
    private const val MAX_CANDIDATES_PER_SIZE = 40

    /** Chunk size for SQLite `IN (...)` queries against MediaStore. */
    private const val MEDIA_STORE_BATCH_SIZE = 200

    private val SKIPPED_DIR_NAMES = hashSetOf(
        ".thumbnails",
        "recycle_bin"
    )

    /** Result of a successful resolution. */
    data class Resolution(
        val path: String,
        val identity: MediaIdentity,
        /** True when the match was confirmed by the content fingerprint. */
        val confirmed: Boolean,
        val mediaStoreId: Long = 0L
    )

    data class MediaStoreCandidate(
        val mediaStoreId: Long,
        val path: String,
        val displayName: String
    )

    /**
     * Per-repair-pass cache that turns O(N) folder moves into O(1) lookups and batches
     * MediaStore queries and fallback scans across all missing favorites and virtual album items.
     */
    class ResolutionSession(
        val allowFilesystemFallback: Boolean = true
    ) {
        private val relocatedFolders = HashMap<String, String>()
        private val mediaStoreIdMap = HashMap<Long, String>()
        private val queriedMediaStoreIds = HashSet<Long>()
        private val mediaStoreSizeMap = HashMap<Long, MutableList<MediaStoreCandidate>>()
        private val queriedSizes = HashSet<Long>()
        private var fallbackSizeIndex: HashMap<Long, MutableList<File>>? = null
        private val targetSizes = HashSet<Long>()

        fun registerMissingSize(size: Long) {
            if (size > 0L) {
                targetSizes.add(size)
            }
        }

        fun recordRelocation(hintPaths: List<String>, resolvedPath: String) {
            val resolvedFile = File(resolvedPath)
            val newParent = resolvedFile.parent ?: return
            for (hint in hintPaths) {
                if (hint.isEmpty()) continue
                val oldParent = File(hint).parent ?: continue
                if (oldParent.isEmpty() || oldParent.equals(newParent, ignoreCase = true)) continue
                if (!File(oldParent).isDirectory) {
                    relocatedFolders[oldParent.lowercase()] = newParent

                    // If an ancestor folder with the same name moved (e.g. /Pictures/Trip/Day1 ->
                    // /DCIM/Trip/Day1), map that ancestor too so sibling subfolders resolve in O(1).
                    var oldAnc = File(oldParent).parentFile
                    var newAnc = File(newParent).parentFile
                    var levels = 0
                    while (levels < 2 && oldAnc != null && newAnc != null) {
                        if (oldAnc.name.isEmpty() || !oldAnc.name.equals(newAnc.name, ignoreCase = true)) {
                            break
                        }
                        if (!oldAnc.isDirectory && newAnc.isDirectory) {
                            relocatedFolders.putIfAbsent(oldAnc.absolutePath.lowercase(), newAnc.absolutePath)
                        }
                        oldAnc = oldAnc.parentFile
                        newAnc = newAnc.parentFile
                        levels++
                    }
                }
            }
        }

        fun resolveFromRelocatedFolder(hintPaths: List<String>, identity: MediaIdentity): Resolution? {
            if (relocatedFolders.isEmpty()) return null
            for (hint in hintPaths) {
                if (hint.isEmpty()) continue
                val hintFile = File(hint)
                val oldParent = hintFile.parent ?: continue
                val fileName = hintFile.name
                if (fileName.isEmpty()) continue

                val directTarget = relocatedFolders[oldParent.lowercase()]
                if (directTarget != null) {
                    val candidate = File(directTarget, fileName)
                    if (matchesFile(candidate, identity)) {
                        return Resolution(candidate.absolutePath, identity, confirmed = identity.hasFingerprint)
                    }
                }

                // Check mapped ancestors for nested subfolders (e.g. /OldRoot/Sub/img.jpg -> /NewRoot/Sub/img.jpg)
                val oldParentLower = oldParent.lowercase()
                for ((oldRootLower, newRoot) in relocatedFolders) {
                    val prefix = "$oldRootLower${File.separator}"
                    if (oldParentLower.startsWith(prefix)) {
                        val relSub = oldParent.substring(oldRootLower.length)
                        val candidate = File("$newRoot$relSub", fileName)
                        if (matchesFile(candidate, identity)) {
                            return Resolution(candidate.absolutePath, identity, confirmed = identity.hasFingerprint)
                        }
                    }
                }
            }
            return null
        }

        /**
         * Preloads MediaStore `_ID` and `SIZE` matches for all missing [identities] using batched
         * `IN (...)` queries instead of issuing one query per file.
         */
        fun preloadMediaStore(context: Context, identities: List<MediaIdentity>) {
            if (identities.isEmpty()) return

            val missingIds = ArrayList<Long>()
            val missingSizes = ArrayList<Long>()
            for (id in identities) {
                if (id.mediaStoreId > 0L && queriedMediaStoreIds.add(id.mediaStoreId)) {
                    missingIds.add(id.mediaStoreId)
                }
                if (id.size > 0L) {
                    targetSizes.add(id.size)
                    if (queriedSizes.add(id.size)) {
                        missingSizes.add(id.size)
                    }
                }
            }

            if (missingIds.isNotEmpty()) {
                queryMediaStoreByIds(context, missingIds, mediaStoreIdMap)
            }
            if (missingSizes.isNotEmpty()) {
                queryMediaStoreBySizes(context, missingSizes, mediaStoreSizeMap)
            }
        }

        fun getCachedMediaStorePath(mediaStoreId: Long): String? {
            return if (queriedMediaStoreIds.contains(mediaStoreId)) mediaStoreIdMap[mediaStoreId] else null
        }

        fun isMediaStoreIdQueried(mediaStoreId: Long): Boolean = queriedMediaStoreIds.contains(mediaStoreId)

        fun getCachedSizeCandidates(size: Long): List<MediaStoreCandidate>? {
            return if (queriedSizes.contains(size)) mediaStoreSizeMap[size].orEmpty() else null
        }

        fun getFallbackCandidates(
            identity: MediaIdentity,
            roots: List<File>
        ): List<File> {
            if (identity.size > 0L) {
                targetSizes.add(identity.size)
            }
            if (fallbackSizeIndex == null) {
                fallbackSizeIndex = buildSizeIndex(roots, targetSizes)
            }
            val index = fallbackSizeIndex ?: return emptyList()
            return if (identity.size > 0L) {
                index[identity.size].orEmpty()
            } else {
                index.values.flatten().take(MAX_CANDIDATES_PER_SIZE)
            }
        }
    }

    private data class ResolvedCandidate(
        val path: String,
        val mediaStoreId: Long,
        val lastModified: Long,
        val name: String
    )

    private data class CandidateSelection(
        val candidate: ResolvedCandidate,
        val highConfidenceRelocation: Boolean
    )

    private data class CandidateScore(
        val exactMediaStoreId: Int,
        val trailingFolderMatches: Int,
        val lastModifiedMatch: Int,
        val folderNameSimilarity: Int,
        val commonPrefixSegments: Int,
        val exactFileNameMatch: Int,
        val pathLength: Int,
        val normalizedPath: String
    ) : Comparable<CandidateScore> {
        override fun compareTo(other: CandidateScore): Int {
            if (exactMediaStoreId != other.exactMediaStoreId) return other.exactMediaStoreId.compareTo(exactMediaStoreId)
            if (trailingFolderMatches != other.trailingFolderMatches) return other.trailingFolderMatches.compareTo(trailingFolderMatches)
            if (lastModifiedMatch != other.lastModifiedMatch) return other.lastModifiedMatch.compareTo(lastModifiedMatch)
            if (folderNameSimilarity != other.folderNameSimilarity) return other.folderNameSimilarity.compareTo(folderNameSimilarity)
            if (commonPrefixSegments != other.commonPrefixSegments) return other.commonPrefixSegments.compareTo(commonPrefixSegments)
            if (exactFileNameMatch != other.exactFileNameMatch) return other.exactFileNameMatch.compareTo(exactFileNameMatch)
            if (pathLength != other.pathLength) return pathLength.compareTo(other.pathLength)
            return normalizedPath.compareTo(other.normalizedPath)
        }
    }

    /**
     * Resolves the file behind [identity].
     *
     * @param hintPaths paths we already know about, tried first without any scanning.
     * @param searchRoots folders that are scanned for candidates when MediaStore does not resolve.
     * @param session optional shared session that caches relocated folders and MediaStore batches.
     * @param excludedPaths lowercased/raw paths already claimed in the same target container so they
     *   are not overwritten or mistakenly bound to another item's moved reference.
     * @param allowAmbiguous when false, defers items that have multiple matching candidates on disk so
     *   unambiguous siblings in a batch pass can reveal the relocated parent folder first. When true,
     *   ranks verified duplicates by folder/metadata affinity so a valid file is still loaded.
     */
    fun resolve(
        context: Context,
        identity: MediaIdentity,
        hintPaths: List<String> = emptyList(),
        searchRoots: List<String> = emptyList(),
        session: ResolutionSession? = null,
        excludedPaths: Set<String> = emptySet(),
        allowAmbiguous: Boolean = true
    ): Resolution? {
        if (!identity.isUsable) {
            return null
        }

        val excludedLower = if (excludedPaths.isEmpty()) emptySet() else excludedPaths.mapTo(HashSet()) { it.lowercase() }

        // Layer 0a: the cached path still holds.
        for (path in hintPaths) {
            if (path.isNotEmpty() && path.lowercase() !in excludedLower) {
                val file = File(path)
                if (file.isFile) {
                    return Resolution(path, identity, confirmed = false, mediaStoreId = identity.mediaStoreId)
                }
            }
        }

        // Layer 0b: another item from the same moved physical folder already revealed its new home.
        session?.resolveFromRelocatedFolder(hintPaths, identity)?.let { res ->
            if (res.path.lowercase() !in excludedLower) {
                return res
            }
        }

        // Layer 1: the MediaStore id is authoritative (verified against size/fingerprint).
        if (identity.mediaStoreId > 0L) {
            val msPath = if (session != null && session.isMediaStoreIdQueried(identity.mediaStoreId)) {
                session.getCachedMediaStorePath(identity.mediaStoreId)
            } else {
                findByMediaStoreId(context, identity.mediaStoreId)
            }
            if (msPath != null && msPath.lowercase() !in excludedLower) {
                val file = File(msPath)
                if (file.isFile && (identity.size <= 0L || file.length() == identity.size)) {
                    val fpMatches = !identity.hasFingerprint || MediaIdentity.computeFingerprint(file) == identity.fingerprint
                    if (fpMatches) {
                        session?.recordRelocation(hintPaths, msPath)
                        return Resolution(
                            path = msPath,
                            identity = identity,
                            confirmed = identity.hasFingerprint,
                            mediaStoreId = identity.mediaStoreId
                        )
                    }
                }
            }
        }

        // Layer 1.5: query MediaStore by exact byte size, prioritizing matching filename, then
        // confirm with the content fingerprint. Finds moved public files anywhere on device in 1 query.
        if (identity.size > 0L) {
            val sizeCandidates = if (session != null) {
                session.getCachedSizeCandidates(identity.size) ?: queryMediaStoreBySingleSize(context, identity.size)
            } else {
                queryMediaStoreBySingleSize(context, identity.size)
            }
            resolveFromMediaStoreCandidates(
                candidates = sizeCandidates,
                identity = identity,
                hintPaths = hintPaths,
                excludedLower = excludedLower,
                allowAmbiguous = allowAmbiguous
            )?.let { (res, highConfidenceRelocation) ->
                if (highConfidenceRelocation) {
                    session?.recordRelocation(hintPaths, res.path)
                }
                return res
            }
        }

        // Layer 2: bounded filesystem fallback (handles hidden/.nomedia, vault .fenc, or unindexed files).
        if (session != null && !session.allowFilesystemFallback) {
            return null
        }

        val extraRoots = buildAndroidFallbackRoots(context, hintPaths)
        return resolveInFilesystem(
            identity = identity,
            hintPaths = hintPaths,
            searchRoots = searchRoots,
            session = session,
            extraRoots = extraRoots,
            excludedPaths = excludedPaths,
            allowAmbiguous = allowAmbiguous
        )
    }

    /**
     * Resolves [identity] on the local filesystem without querying Android MediaStore.
     * Expands missing [searchRoots] to existing ancestor/sibling directories and [extraRoots],
     * pre-filtering strictly by `file.length() == identity.size` before computing fingerprints.
     */
    fun resolveInFilesystem(
        identity: MediaIdentity,
        hintPaths: List<String> = emptyList(),
        searchRoots: List<String> = emptyList(),
        session: ResolutionSession? = null,
        extraRoots: List<String> = emptyList(),
        excludedPaths: Set<String> = emptySet(),
        allowAmbiguous: Boolean = true
    ): Resolution? {
        if (!identity.isUsable) {
            return null
        }

        val excludedLower = if (excludedPaths.isEmpty()) emptySet() else excludedPaths.mapTo(HashSet()) { it.lowercase() }

        for (path in hintPaths) {
            if (path.isNotEmpty() && path.lowercase() !in excludedLower && File(path).isFile) {
                return Resolution(path, identity, confirmed = false, mediaStoreId = identity.mediaStoreId)
            }
        }

        session?.resolveFromRelocatedFolder(hintPaths, identity)?.let { res ->
            if (res.path.lowercase() !in excludedLower) {
                return res
            }
        }

        val roots = expandSearchRoots(hintPaths, searchRoots, extraRoots)
        if (roots.isEmpty()) {
            return null
        }

        val candidates: List<File> = if (session != null) {
            session.getFallbackCandidates(identity, roots)
        } else {
            val targetSizes = if (identity.size > 0L) setOf(identity.size) else emptySet()
            val index = buildSizeIndex(roots, targetSizes)
            if (identity.size > 0L) {
                index[identity.size].orEmpty()
            } else {
                index.values.flatten().take(MAX_CANDIDATES_PER_SIZE)
            }
        }

        if (candidates.isEmpty()) {
            return null
        }

        val preferredName = hintPaths.firstOrNull { it.isNotEmpty() }?.let { File(it).name } ?: ""
        val ordered = if (preferredName.isNotEmpty()) {
            candidates.sortedByDescending { it.name.equals(preferredName, ignoreCase = true) }
        } else {
            candidates
        }

        val confirmedMatches = ArrayList<ResolvedCandidate>(2)
        val unconfirmedMatches = ArrayList<ResolvedCandidate>(2)

        for (file in ordered) {
            if (!file.isFile) continue
            if (identity.size > 0L && file.length() != identity.size) continue

            if (identity.hasFingerprint) {
                if (MediaIdentity.computeFingerprint(file) == identity.fingerprint) {
                    confirmedMatches.add(
                        ResolvedCandidate(
                            path = file.absolutePath,
                            mediaStoreId = identity.mediaStoreId,
                            lastModified = file.lastModified(),
                            name = file.name
                        )
                    )
                }
            } else if (matchesCheapSignature(file, identity)) {
                unconfirmedMatches.add(
                    ResolvedCandidate(
                        path = file.absolutePath,
                        mediaStoreId = identity.mediaStoreId,
                        lastModified = file.lastModified(),
                        name = file.name
                    )
                )
            }
        }

        val resolved = resolveMatchLists(
            confirmedMatches = confirmedMatches,
            unconfirmedMatches = unconfirmedMatches,
            identity = identity,
            hintPaths = hintPaths,
            excludedLower = excludedLower,
            allowAmbiguous = allowAmbiguous
        ) ?: return null

        val (res, highConfidenceRelocation) = resolved
        if (highConfidenceRelocation) {
            session?.recordRelocation(hintPaths, res.path)
        }
        return res
    }

    private fun resolveFromMediaStoreCandidates(
        candidates: List<MediaStoreCandidate>,
        identity: MediaIdentity,
        hintPaths: List<String>,
        excludedLower: Set<String>,
        allowAmbiguous: Boolean
    ): Pair<Resolution, Boolean>? {
        if (candidates.isEmpty()) return null

        val preferredName = hintPaths.firstOrNull { it.isNotEmpty() }?.let { File(it).name } ?: ""
        val ordered = if (preferredName.isNotEmpty()) {
            candidates.sortedByDescending { it.displayName.equals(preferredName, ignoreCase = true) }
        } else {
            candidates
        }

        val confirmedMatches = ArrayList<ResolvedCandidate>(2)
        val unconfirmedMatches = ArrayList<ResolvedCandidate>(2)

        for (candidate in ordered) {
            val file = File(candidate.path)
            if (!file.isFile || file.length() != identity.size) continue

            val entry = ResolvedCandidate(
                path = candidate.path,
                mediaStoreId = candidate.mediaStoreId,
                lastModified = file.lastModified(),
                name = file.name
            )
            if (identity.hasFingerprint) {
                if (MediaIdentity.computeFingerprint(file) == identity.fingerprint) {
                    confirmedMatches.add(entry)
                }
            } else if (matchesCheapSignature(file, identity)) {
                unconfirmedMatches.add(entry)
            }
        }

        return resolveMatchLists(
            confirmedMatches = confirmedMatches,
            unconfirmedMatches = unconfirmedMatches,
            identity = identity,
            hintPaths = hintPaths,
            excludedLower = excludedLower,
            allowAmbiguous = allowAmbiguous
        )
    }

    private fun resolveMatchLists(
        confirmedMatches: List<ResolvedCandidate>,
        unconfirmedMatches: List<ResolvedCandidate>,
        identity: MediaIdentity,
        hintPaths: List<String>,
        excludedLower: Set<String>,
        allowAmbiguous: Boolean
    ): Pair<Resolution, Boolean>? {
        val activeConfirmed = confirmedMatches.filter { it.path.lowercase() !in excludedLower }
        if (activeConfirmed.isNotEmpty()) {
            val selected = selectBestCandidate(
                candidates = activeConfirmed,
                identity = identity,
                hintPaths = hintPaths,
                allowAmbiguous = allowAmbiguous
            ) ?: return null
            val match = selected.candidate
            return Resolution(
                path = match.path,
                identity = identity,
                confirmed = true,
                mediaStoreId = match.mediaStoreId
            ) to selected.highConfidenceRelocation
        }

        if (confirmedMatches.isEmpty()) {
            val activeUnconfirmed = unconfirmedMatches.filter { it.path.lowercase() !in excludedLower }
            if (activeUnconfirmed.isNotEmpty()) {
                val canDisambiguateUnconfirmed = activeUnconfirmed.size == 1 ||
                    (identity.size > 0L && identity.lastModified > 0L)
                if (!canDisambiguateUnconfirmed) {
                    return null
                }
                val selected = selectBestCandidate(
                    candidates = activeUnconfirmed,
                    identity = identity,
                    hintPaths = hintPaths,
                    allowAmbiguous = allowAmbiguous
                ) ?: return null
                val match = selected.candidate
                return Resolution(
                    path = match.path,
                    identity = identity,
                    confirmed = false,
                    mediaStoreId = match.mediaStoreId
                ) to selected.highConfidenceRelocation
            }
        }

        return null
    }

    private fun selectBestCandidate(
        candidates: List<ResolvedCandidate>,
        identity: MediaIdentity,
        hintPaths: List<String>,
        allowAmbiguous: Boolean
    ): CandidateSelection? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) {
            return CandidateSelection(candidates[0], highConfidenceRelocation = true)
        }

        if (identity.mediaStoreId > 0L) {
            val idMatches = candidates.filter { it.mediaStoreId == identity.mediaStoreId }
            if (idMatches.size == 1) {
                return CandidateSelection(idMatches[0], highConfidenceRelocation = true)
            }
        }

        if (!allowAmbiguous) {
            return null
        }

        var bestCandidate = candidates[0]
        var bestScore = scoreCandidate(bestCandidate, identity, hintPaths)
        for (i in 1 until candidates.size) {
            val candidate = candidates[i]
            val score = scoreCandidate(candidate, identity, hintPaths)
            if (score < bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        val highConfidenceRelocation = bestScore.exactMediaStoreId > 0 ||
            bestScore.trailingFolderMatches > 0 ||
            (bestScore.folderNameSimilarity > 0 && bestScore.commonPrefixSegments > 0)
        return CandidateSelection(bestCandidate, highConfidenceRelocation = highConfidenceRelocation)
    }

    private fun scoreCandidate(
        candidate: ResolvedCandidate,
        identity: MediaIdentity,
        hintPaths: List<String>
    ): CandidateScore {
        val exactMediaStoreId = if (identity.mediaStoreId > 0L && candidate.mediaStoreId == identity.mediaStoreId) 1 else 0
        val lastModifiedMatch = if (identity.lastModified > 0L && candidate.lastModified == identity.lastModified) 1 else 0

        val candFile = File(candidate.path)
        val candParentSegs = splitPathSegments(candFile.parent ?: "")
        val candParentName = candParentSegs.lastOrNull().orEmpty()

        var bestTrailing = 0
        var bestNameSim = 0
        var bestPrefix = 0
        var bestExactFileName = 0

        for (hint in hintPaths) {
            if (hint.isEmpty()) continue
            val hintFile = File(hint)
            val hintParentSegs = splitPathSegments(hintFile.parent ?: "")
            val hintParentName = hintParentSegs.lastOrNull().orEmpty()

            val trailing = countTrailingMatchingSegments(hintParentSegs, candParentSegs)
            val nameSim = computeFolderNameSimilarity(hintParentName, candParentName)
            val prefix = countCommonPrefixSegments(hintParentSegs, candParentSegs)
            val exactName = if (hintFile.name.isNotEmpty() && candidate.name.equals(hintFile.name, ignoreCase = true)) 1 else 0

            if (trailing > bestTrailing ||
                (trailing == bestTrailing && nameSim > bestNameSim) ||
                (trailing == bestTrailing && nameSim == bestNameSim && prefix > bestPrefix) ||
                (trailing == bestTrailing && nameSim == bestNameSim && prefix == bestPrefix && exactName > bestExactFileName)
            ) {
                bestTrailing = trailing
                bestNameSim = nameSim
                bestPrefix = prefix
                bestExactFileName = exactName
            }
        }

        val normalized = candidate.path.replace('\\', '/').lowercase()
        return CandidateScore(
            exactMediaStoreId = exactMediaStoreId,
            trailingFolderMatches = bestTrailing,
            lastModifiedMatch = lastModifiedMatch,
            folderNameSimilarity = bestNameSim,
            commonPrefixSegments = bestPrefix,
            exactFileNameMatch = bestExactFileName,
            pathLength = normalized.length,
            normalizedPath = normalized
        )
    }

    private fun splitPathSegments(path: String): List<String> {
        if (path.isEmpty()) return emptyList()
        return path.replace('\\', '/')
            .trimEnd('/')
            .split('/')
            .filter { it.isNotEmpty() }
    }

    private fun countTrailingMatchingSegments(a: List<String>, b: List<String>): Int {
        var count = 0
        var ia = a.size - 1
        var ib = b.size - 1
        while (ia >= 0 && ib >= 0 && a[ia].equals(b[ib], ignoreCase = true)) {
            count++
            ia--
            ib--
        }
        return count
    }

    private fun countCommonPrefixSegments(a: List<String>, b: List<String>): Int {
        val limit = minOf(a.size, b.size)
        var count = 0
        while (count < limit && a[count].equals(b[count], ignoreCase = true)) {
            count++
        }
        return count
    }

    private fun computeFolderNameSimilarity(aRaw: String, bRaw: String): Int {
        if (aRaw.isEmpty() || bRaw.isEmpty()) return 0
        val a = aRaw.lowercase()
        val b = bRaw.lowercase()
        if (a == b) return 100

        val minLen = minOf(a.length, b.length)
        val maxLen = maxOf(a.length, b.length)
        if (a.startsWith(b) || b.startsWith(a)) {
            return 70 + (minLen * 20) / maxLen
        }
        if (minLen >= 3 && (a.contains(b) || b.contains(a))) {
            return 50 + (minLen * 20) / maxLen
        }

        var commonPrefix = 0
        while (commonPrefix < minLen && a[commonPrefix] == b[commonPrefix]) {
            commonPrefix++
        }
        if (commonPrefix >= 3) {
            return 20 + (commonPrefix * 30) / maxLen
        }

        val tokenRegex = Regex("[^a-z0-9]+")
        val tokensA = a.split(tokenRegex).filter { it.isNotEmpty() }.toHashSet()
        val tokensB = b.split(tokenRegex).filter { it.isNotEmpty() }.toHashSet()
        if (tokensA.isNotEmpty() && tokensB.isNotEmpty()) {
            val shared = tokensA.intersect(tokensB).size
            if (shared > 0) {
                return 15 + (shared * 30) / maxOf(tokensA.size, tokensB.size)
            }
        }

        return 0
    }

    private fun matchesFile(file: File, identity: MediaIdentity): Boolean {
        if (!file.isFile) return false
        if (identity.size > 0L && file.length() != identity.size) return false
        return if (identity.hasFingerprint) {
            MediaIdentity.computeFingerprint(file) == identity.fingerprint
        } else {
            matchesCheapSignature(file, identity)
        }
    }

    private fun findByMediaStoreId(context: Context, mediaStoreId: Long): String? {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val selection = "${MediaStore.MediaColumns._ID} = ?"
        val args = arrayOf(mediaStoreId.toString())

        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, projection, selection, args, null)
            if (cursor != null && cursor.moveToFirst()) {
                val path = cursor.getString(0)
                if (!path.isNullOrEmpty() && File(path).isFile) path else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun queryMediaStoreByIds(
        context: Context,
        ids: List<Long>,
        out: MutableMap<Long, String>
    ) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA)
        for (chunk in ids.chunked(MEDIA_STORE_BATCH_SIZE)) {
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = "${MediaStore.MediaColumns._ID} IN ($placeholders)"
            val args = chunk.map { it.toString() }.toTypedArray()
            try {
                context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val path = cursor.getString(1)
                        if (!path.isNullOrEmpty() && File(path).isFile) {
                            out[id] = path
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun queryMediaStoreBySingleSize(context: Context, size: Long): List<MediaStoreCandidate> {
        val map = HashMap<Long, MutableList<MediaStoreCandidate>>()
        queryMediaStoreBySizes(context, listOf(size), map)
        return map[size].orEmpty()
    }

    private fun queryMediaStoreBySizes(
        context: Context,
        sizes: List<Long>,
        out: MutableMap<Long, MutableList<MediaStoreCandidate>>
    ) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE
        )
        for (chunk in sizes.chunked(MEDIA_STORE_BATCH_SIZE)) {
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = "${MediaStore.MediaColumns.SIZE} IN ($placeholders)"
            val args = chunk.map { it.toString() }.toTypedArray()
            try {
                context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val path = cursor.getString(1) ?: continue
                        val name = cursor.getString(2) ?: File(path).name
                        val size = cursor.getLong(3)
                        if (path.isNotEmpty() && size > 0L) {
                            out.getOrPut(size) { ArrayList(2) }.add(MediaStoreCandidate(id, path, name))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun buildAndroidFallbackRoots(context: Context, hintPaths: List<String>): List<String> {
        val roots = LinkedHashSet<String>()
        val isVaultHint = hintPaths.any { CryptoManager.isVaultMediaPath(context, it) || CryptoManager.isEncryptedFile(it) }
        if (isVaultHint) {
            CryptoManager.findVaultRoot(context)?.takeIf { it.isDirectory }?.let {
                roots.add(it.absolutePath)
            }
        }
        try {
            val externalRoot = Environment.getExternalStorageDirectory()
            if (externalRoot != null && externalRoot.isDirectory) {
                listOf(
                    Environment.DIRECTORY_DCIM,
                    Environment.DIRECTORY_PICTURES,
                    Environment.DIRECTORY_MOVIES,
                    Environment.DIRECTORY_DOWNLOADS,
                    Environment.DIRECTORY_DOCUMENTS
                ).forEach { dirName ->
                    val sub = File(externalRoot, dirName)
                    if (sub.isDirectory) {
                        roots.add(sub.absolutePath)
                    }
                }
                roots.add(CryptoManager.getCipherRootDirectory().absolutePath)
            }
        } catch (_: Exception) {
        }
        return roots.toList()
    }

    private fun expandSearchRoots(
        hintPaths: List<String>,
        searchRoots: List<String>,
        extraRoots: List<String>
    ): List<File> {
        val result = ArrayList<File>()
        val seen = HashSet<String>()

        fun addRoot(dir: File?) {
            if (dir == null || !dir.isDirectory) return
            val path = dir.absolutePath
            if (isStorageBoundary(path)) return
            if (seen.add(path.lowercase())) {
                result.add(dir)
            }
        }

        // 1. Direct searchRoots or their nearest existing ancestor (up to 2 levels up when a folder was renamed/moved).
        for (raw in searchRoots) {
            if (raw.isEmpty()) continue
            var dir: File? = File(raw)
            if (dir != null && dir.isDirectory) {
                addRoot(dir)
                continue
            }
            var steps = 0
            while (dir != null && steps < 2) {
                dir = dir.parentFile
                steps++
                if (dir != null && dir.isDirectory) {
                    addRoot(dir)
                    break
                }
            }
        }

        // 2. Hint path parents or nearest existing ancestor.
        for (raw in hintPaths) {
            if (raw.isEmpty()) continue
            var dir: File? = File(raw).parentFile
            var steps = 0
            while (dir != null && steps < 3) {
                if (dir.isDirectory) {
                    addRoot(dir)
                    break
                }
                dir = dir.parentFile
                steps++
            }
        }

        // 3. Broad extra fallback roots (e.g. Vault root, DCIM, Pictures, Documents).
        for (raw in extraRoots) {
            if (raw.isNotEmpty()) {
                addRoot(File(raw))
            }
        }

        return result
    }

    private fun isStorageBoundary(path: String): Boolean {
        val trimmed = path.trimEnd('/', '\\')
        return trimmed.isEmpty() ||
            trimmed == "/storage" ||
            trimmed == "/storage/emulated" ||
            trimmed == "/mnt"
    }

    private fun buildSizeIndex(
        roots: List<File>,
        targetSizes: Set<Long>
    ): HashMap<Long, MutableList<File>> {
        val index = HashMap<Long, MutableList<File>>()
        val visitedDirs = HashSet<String>()
        var filesVisited = 0

        val queue = ArrayDeque<Pair<File, Int>>()
        for (root in roots) {
            if (root.isDirectory && visitedDirs.add(root.absolutePath.lowercase())) {
                queue.add(root to 0)
            }
        }

        while (queue.isNotEmpty() && filesVisited < MAX_FALLBACK_FILES_VISITED) {
            val (folder, depth) = queue.removeFirst()
            if (depth > MAX_SCAN_DEPTH) continue

            val children = folder.listFiles() ?: continue
            for (child in children) {
                if (filesVisited >= MAX_FALLBACK_FILES_VISITED) break

                if (child.isDirectory) {
                    val lowerName = child.name.lowercase()
                    val isAndroidSystemDir = lowerName == "android" &&
                        (File(child, "data").isDirectory || File(child, "obb").isDirectory)
                    if (lowerName in SKIPPED_DIR_NAMES || isAndroidSystemDir) continue
                    if (depth + 1 <= MAX_SCAN_DEPTH && visitedDirs.add(child.absolutePath.lowercase())) {
                        queue.add(child to (depth + 1))
                    }
                    continue
                }

                if (!child.isFile) continue
                filesVisited++

                val len = child.length()
                if (len <= 0L) continue
                if (targetSizes.isNotEmpty() && len !in targetSizes) continue

                val bucket = index.getOrPut(len) { ArrayList(2) }
                if (bucket.size < MAX_CANDIDATES_PER_SIZE) {
                    bucket.add(child)
                }
            }
        }

        return index
    }

    /**
     * Cheap pre-filter: the size has to match exactly and the modification time has to match.
     * Only used when no fingerprint is available.
     */
    private fun matchesCheapSignature(file: File, identity: MediaIdentity): Boolean {
        if (identity.size > 0L && file.length() != identity.size) {
            return false
        }

        if (identity.lastModified > 0L && file.lastModified() != identity.lastModified) {
            return false
        }

        return true
    }

    /**
     * Looks for a file that matches the given identity in the given folder.
     */
    fun findInFolder(identity: MediaIdentity, folder: File): String? {
        if (!folder.isDirectory) {
            return null
        }

        val children = folder.listFiles() ?: return null
        for (child in children) {
            if (!child.isFile) {
                continue
            }

            if (identity.size > 0L && child.length() != identity.size) {
                continue
            }

            if (!identity.hasFingerprint) {
                if (matchesCheapSignature(child, identity)) {
                    return child.absolutePath
                }
                continue
            }

            if (MediaIdentity.computeFingerprint(child) == identity.fingerprint) {
                return child.absolutePath
            }
        }

        return null
    }
}
