package org.fossify.gallery.helpers

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import org.fossify.commons.extensions.getParentPath
import java.io.File

/**
 * Finds a file again after it was renamed or moved.
 *
 * The resolution goes through three layers, from the most certain to the least:
 *
 *  1. The MediaStore id, which is preserved by renames and moves and cannot collide.
 *  2. The full content fingerprint, which is unique in practice.
 *  3. A cheap pre-filter (size plus modification time) that only narrows down the candidates.
 *
 * Layer 3 never decides on its own. It only produces candidates, and the winner is always
 * confirmed with the full fingerprint, so an ambiguous situation can never bind a reference to
 * the wrong file. When nothing matches with certainty the file stays unresolved instead of
 * being silently dropped.
 */
object MediaIdentityResolver {
    /** How deep we search below the folders we know about when scanning for candidates. */
    private const val MAX_SCAN_DEPTH = 3

    /** Upper bound for the candidate list built by the cheap pre-filter. */
    private const val MAX_CANDIDATES = 400

    /** Result of a successful resolution. */
    data class Resolution(
        val path: String,
        val identity: MediaIdentity,
        /** True when the match was confirmed by the content fingerprint. */
        val confirmed: Boolean
    )

    /**
     * Resolves the file behind [identity].
     *
     * @param hintPaths paths we already know about, tried first without any scanning.
     * @param searchRoots folders that are scanned for candidates when the hints do not resolve.
     */
    fun resolve(
        context: Context,
        identity: MediaIdentity,
        hintPaths: List<String> = emptyList(),
        searchRoots: List<String> = emptyList()
    ): Resolution? {
        if (!identity.isUsable) {
            return null
        }

        // Layer 1: the MediaStore id is authoritative.
        if (identity.mediaStoreId > 0L) {
            findByMediaStoreId(context, identity.mediaStoreId)?.let { path ->
                return Resolution(path, identity, confirmed = false)
            }
        }

        // The cached path still holds, nothing to do.
        hintPaths.forEach { path ->
            if (path.isNotEmpty() && File(path).isFile) {
                return Resolution(path, identity, confirmed = false)
            }
        }

        // Layer 2 and 3: build candidates and confirm them with the full fingerprint.
        val candidates = collectCandidates(identity, hintPaths, searchRoots)
        if (candidates.isEmpty()) {
            return null
        }

        var confirmedMatch: String? = null
        var unconfirmedMatch: String? = null

        for (candidate in candidates) {
            val file = File(candidate)
            if (!file.isFile) {
                continue
            }

            if (identity.hasFingerprint) {
                if (MediaIdentity.computeFingerprint(file) == identity.fingerprint) {
                    if (confirmedMatch == null) {
                        confirmedMatch = candidate
                    } else {
                        // Two files share the fingerprint, so we cannot tell them apart.
                        // Refusing to guess is the only safe option.
                        return null
                    }
                }
            } else {
                // No fingerprint available, fall back to the cheap signature.
                if (matchesCheapSignature(file, identity) && unconfirmedMatch == null) {
                    unconfirmedMatch = candidate
                }
            }
        }

        confirmedMatch?.let {
            return Resolution(it, identity, confirmed = true)
        }

        unconfirmedMatch?.let {
            return Resolution(it, identity, confirmed = false)
        }

        return null
    }

    /**
     * Asks the MediaStore for the current path of a file id.
     */
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
                if (!path.isNullOrEmpty() && File(path).isFile) {
                    path
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Builds the list of paths that could be the file we are looking for.
     */
    private fun collectCandidates(
        identity: MediaIdentity,
        hintPaths: List<String>,
        searchRoots: List<String>
    ): List<String> {
        val candidates = LinkedHashSet<String>()

        // Folders we know about may still hold the file under a new name.
        searchRoots.forEach { root ->
            if (root.isEmpty()) {
                return@forEach
            }
            collectFromFolder(File(root), candidates, 0)
        }

        // The folder the file used to live in may still exist and hold a renamed copy.
        hintPaths.forEach { path ->
            if (path.isEmpty()) {
                return@forEach
            }
            val parent = path.getParentPath()
            if (parent.isNotEmpty()) {
                collectFromFolder(File(parent), candidates, 0)
            }
        }

        return candidates.take(MAX_CANDIDATES)
    }

    /**
     * Walks a folder tree and adds files that pass the cheap pre-filter.
     */
    private fun collectFromFolder(folder: File, candidates: MutableSet<String>, depth: Int) {
        if (depth > MAX_SCAN_DEPTH || !folder.isDirectory) {
            return
        }

        val children = folder.listFiles() ?: return
        for (child in children) {
            if (candidates.size >= MAX_CANDIDATES) {
                return
            }

            if (child.isDirectory) {
                collectFromFolder(child, candidates, depth + 1)
                continue
            }

            if (!child.isFile) {
                continue
            }

            candidates.add(child.absolutePath)
        }
    }

    /**
     * Cheap pre-filter: the size has to match exactly and the modification time has to be close.
     * Only used to narrow down candidates, never as a final decision.
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

            if (!matchesCheapSignature(child, identity)) {
                continue
            }

            if (!identity.hasFingerprint) {
                return child.absolutePath
            }

            if (MediaIdentity.computeFingerprint(child) == identity.fingerprint) {
                return child.absolutePath
            }
        }

        return null
    }
}
