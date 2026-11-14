package org.fossify.gallery

import org.fossify.gallery.helpers.FolderIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests for zero-pollution physical folder identity capture and matching.
 */
class FolderIdentityTest {
    private fun createTempDir(name: String): File {
        val dir = File(
            System.getProperty("java.io.tmpdir"),
            "fossify_folder_id_${name}_${System.nanoTime()}"
        )
        dir.mkdirs()
        return dir
    }

    private fun writeFile(file: File, size: Int, seed: Byte): File {
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(size) { ((it * 31 + seed) and 0xFF).toByte() })
        return file
    }

    @Test
    fun testCapturesSiblingAnchorsExcludingTargetAndHiddenMarkers() {
        val root = createTempDir("capture")
        try {
            val folder = File(root, "Pictures/Album")
            val target = writeFile(File(folder, "target.jpg"), 4096, 1)
            writeFile(File(folder, "sibling1.jpg"), 4100, 2)
            writeFile(File(folder, "sibling2.jpg"), 4200, 3)
            writeFile(File(folder, ".nomedia"), 10, 9)

            val captured = FolderIdentity.captureForFile(target)
            assertEquals(2, captured.anchorSignatures.size)
            assertTrue(captured.anchorSignatures.all { !it.startsWith("4096:") })
            assertTrue(captured.hasEvidence)

            val sameDirResult = FolderIdentity.matchesCandidateParent(
                candidateParent = folder,
                excludeFile = target,
                expectedInode = captured.dirInode,
                serializedAnchors = captured.serializedSignature
            )
            assertEquals(2, sameDirResult.matchedAnchorCount)
            assertTrue(sameDirResult.hasStrongMatch)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testCaptureSessionCachesFolderAndExcludesEachTargetItem() {
        val root = createTempDir("session")
        try {
            val folder = File(root, "Pictures/Batch")
            val f1 = writeFile(File(folder, "img_1.jpg"), 4001, 11)
            val f2 = writeFile(File(folder, "img_2.jpg"), 4002, 12)

            val session = FolderIdentity.CaptureSession()
            val id1 = session.captureForFile(f1)
            val id2 = session.captureForFile(f2)

            assertEquals(1, id1.anchorSignatures.size)
            assertTrue(id1.anchorSignatures[0].startsWith("4002:"))
            assertEquals(1, id2.anchorSignatures.size)
            assertTrue(id2.anchorSignatures[0].startsWith("4001:"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testMatchesRenamedFolderViaInodeWhenCustomReaderProvidesInode() {
        val root = createTempDir("inode")
        val previousReader = FolderIdentity.customInodeReader
        try {
            val oldFolder = File(root, "Pictures/A")
            val target = writeFile(File(oldFolder, "solo.jpg"), 4096, 5)
            val newFolder = File(root, "DCIM/TamamenFarkli").apply { parentFile?.mkdirs() }
            val otherFolder = File(root, "Pictures/B")
            otherFolder.mkdirs()

            FolderIdentity.customInodeReader = { dir ->
                when (dir.absolutePath) {
                    oldFolder.absolutePath, newFolder.absolutePath -> 987654L
                    otherFolder.absolutePath -> 111222L
                    else -> 0L
                }
            }

            val captured = FolderIdentity.captureForFile(target)
            assertEquals(987654L, captured.dirInode)
            assertTrue(oldFolder.renameTo(newFolder))

            val movedTarget = File(newFolder, "solo.jpg")
            val otherCopy = writeFile(File(otherFolder, "solo.jpg"), 4096, 5)

            val matchedMoved = FolderIdentity.matchesCandidateParent(
                candidateParent = newFolder,
                excludeFile = movedTarget,
                expectedInode = captured.dirInode,
                serializedAnchors = captured.serializedSignature
            )
            assertTrue(matchedMoved.inodeMatches)
            assertTrue(matchedMoved.hasStrongMatch)

            val matchedOther = FolderIdentity.matchesCandidateParent(
                candidateParent = otherFolder,
                excludeFile = otherCopy,
                expectedInode = captured.dirInode,
                serializedAnchors = captured.serializedSignature
            )
            assertFalse(matchedOther.inodeMatches)
            assertFalse(matchedOther.hasStrongMatch)
        } finally {
            FolderIdentity.customInodeReader = previousReader
            root.deleteRecursively()
        }
    }
}
