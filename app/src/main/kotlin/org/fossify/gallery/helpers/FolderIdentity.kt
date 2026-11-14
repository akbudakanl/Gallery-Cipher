package org.fossify.gallery.helpers

import android.system.Os
import androidx.annotation.VisibleForTesting
import java.io.File

/**
 * Zero-pollution identity for a physical parent folder, captured without writing any marker files
 * into the user's shared folders.
 *
 * Combines the directory's filesystem inode (when exposed by the underlying OS/filesystem) with
 * compact size+fingerprint anchors of neighboring files in that folder. Together these allow the
 * resolver to recognize a renamed or moved folder cleanly, while rejecting unrelated folders when
 * the original folder or all of its files have been deleted.
 */
data class FolderIdentity(
    val dirInode: Long = 0L,
    val anchorSignatures: List<String> = emptyList()
) {
    val serializedSignature: String
        get() = anchorSignatures.joinToString(",")

    val hasEvidence: Boolean
        get() = dirInode > 0L || anchorSignatures.isNotEmpty()

    data class MatchResult(
        val inodeMatches: Boolean,
        val matchedAnchorCount: Int
    ) {
        val hasStrongMatch: Boolean
            get() = inodeMatches || matchedAnchorCount > 0
    }

    /**
     * Reusable per-batch directory cache so adding multiple files from the same folder (or
     * checking multiple candidates in the same folder) hashes sibling anchors at most once.
     */
    class CaptureSession {
        private data class CachedEntry(
            val pathLower: String,
            val signature: String
        )

        private data class CachedDir(
            val inode: Long,
            val entries: List<CachedEntry>
        )

        private val cache = HashMap<String, CachedDir>()

        fun captureForFile(targetFile: File): FolderIdentity {
            val parent = targetFile.parentFile ?: return FolderIdentity()
            if (!parent.isDirectory) return FolderIdentity()

            val cached = cache.getOrPut(parent.absolutePath.lowercase()) {
                val inode = readDirectoryInode(parent)
                val children = parent.listFiles()
                    ?.asSequence()
                    ?.filter { it.isFile && !it.name.startsWith(".") && it.length() > 0L }
                    ?.sortedBy { it.name.lowercase() }
                    ?.take(MAX_ANCHORS + 1)
                    ?.mapNotNull { child ->
                        val fp = MediaIdentity.computeFingerprint(child)
                        if (fp.isEmpty()) null else CachedEntry(child.absolutePath.lowercase(), "${child.length()}:$fp")
                    }
                    ?.toList()
                    ?: emptyList()
                CachedDir(inode, children)
            }

            val targetLower = targetFile.absolutePath.lowercase()
            val anchors = cached.entries
                .asSequence()
                .filter { it.pathLower != targetLower }
                .take(MAX_ANCHORS)
                .map { it.signature }
                .toList()
            return FolderIdentity(dirInode = cached.inode, anchorSignatures = anchors)
        }
    }

    companion object {
        private const val MAX_ANCHORS = 5
        private const val MAX_SCAN_CHILDREN = 2000

        @VisibleForTesting
        var customInodeReader: ((File) -> Long)? = null

        fun readDirectoryInode(dir: File): Long {
            if (!dir.isDirectory) return 0L
            customInodeReader?.let { return it(dir) }
            return try {
                val stat = Os.stat(dir.absolutePath)
                if (stat.st_ino > 0L) stat.st_ino else 0L
            } catch (_: Throwable) {
                0L
            }
        }

        fun captureForFile(targetFile: File): FolderIdentity {
            val parent = targetFile.parentFile ?: return FolderIdentity()
            if (!parent.isDirectory) return FolderIdentity()

            val inode = readDirectoryInode(parent)
            val targetLower = targetFile.absolutePath.lowercase()
            val anchors = parent.listFiles()
                ?.asSequence()
                ?.filter {
                    it.isFile &&
                        !it.name.startsWith(".") &&
                        it.length() > 0L &&
                        it.absolutePath.lowercase() != targetLower
                }
                ?.sortedBy { it.name.lowercase() }
                ?.take(MAX_ANCHORS)
                ?.mapNotNull { sibling ->
                    val fp = MediaIdentity.computeFingerprint(sibling)
                    if (fp.isEmpty()) null else "${sibling.length()}:$fp"
                }
                ?.toList()
                ?: emptyList()

            return FolderIdentity(dirInode = inode, anchorSignatures = anchors)
        }

        fun parseAnchors(serialized: String): List<Pair<Long, String>> {
            if (serialized.isEmpty()) return emptyList()
            val result = ArrayList<Pair<Long, String>>(MAX_ANCHORS)
            for (token in serialized.split(',')) {
                val idx = token.indexOf(':')
                if (idx <= 0 || idx >= token.length - 1) continue
                val size = token.substring(0, idx).toLongOrNull() ?: continue
                val fp = token.substring(idx + 1)
                if (size > 0L && fp.isNotEmpty()) {
                    result.add(size to fp)
                }
            }
            return result
        }

        /**
         * Checks whether [candidateParent] shares the stored directory [expectedInode] or contains
         * sibling files matching [serializedAnchors], excluding [excludeFile] itself.
         */
        fun matchesCandidateParent(
            candidateParent: File?,
            excludeFile: File,
            expectedInode: Long,
            serializedAnchors: String
        ): MatchResult {
            if (candidateParent == null || !candidateParent.isDirectory) {
                return MatchResult(inodeMatches = false, matchedAnchorCount = 0)
            }

            val inodeMatches = expectedInode > 0L && readDirectoryInode(candidateParent) == expectedInode
            val anchors = parseAnchors(serializedAnchors)
            if (anchors.isEmpty()) {
                return MatchResult(inodeMatches = inodeMatches, matchedAnchorCount = 0)
            }

            val neededBySize = HashMap<Long, MutableSet<String>>(anchors.size)
            for ((size, fp) in anchors) {
                neededBySize.getOrPut(size) { HashSet(1) }.add(fp)
            }

            val children = candidateParent.listFiles() ?: return MatchResult(inodeMatches, 0)
            val excludeLower = excludeFile.absolutePath.lowercase()
            val matchedFingerprints = HashSet<String>(anchors.size)
            var checked = 0

            for (child in children) {
                if (checked >= MAX_SCAN_CHILDREN || matchedFingerprints.size >= anchors.size) {
                    break
                }
                if (!child.isFile || child.name.startsWith(".")) continue
                if (child.absolutePath.lowercase() == excludeLower) continue
                checked++

                val expectedFps = neededBySize[child.length()] ?: continue
                val fp = MediaIdentity.computeFingerprint(child)
                if (fp.isNotEmpty() && expectedFps.contains(fp)) {
                    matchedFingerprints.add("${child.length()}:$fp")
                }
            }

            return MatchResult(
                inodeMatches = inodeMatches,
                matchedAnchorCount = matchedFingerprints.size
            )
        }
    }
}
