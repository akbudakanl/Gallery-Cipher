package org.fossify.gallery

import org.fossify.gallery.helpers.FolderIdentity
import org.fossify.gallery.helpers.MediaIdentity
import org.fossify.gallery.helpers.MediaIdentityResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests for filesystem identity resolution when physical folders or files are moved or renamed
 * outside of the app.
 */
class MediaIdentityResolverTest {
    private fun createTempDir(name: String): File {
        val dir = File(
            System.getProperty("java.io.tmpdir"),
            "fossify_resolver_${name}_${System.nanoTime()}"
        )
        dir.mkdirs()
        return dir
    }

    private fun writeFile(file: File, size: Int, seed: Byte): File {
        file.parentFile?.mkdirs()
        val content = ByteArray(size) { ((it * 31 + seed) and 0xFF).toByte() }
        file.writeBytes(content)
        return file
    }

    @Test
    fun testFindsFileWhenParentFolderIsRenamed() {
        val root = createTempDir("folder_rename")
        try {
            val oldFolder = File(root, "Pictures/Vacation")
            val photo = writeFile(File(oldFolder, "IMG_001.jpg"), 4096, 7)
            val identity = MediaIdentity.of(photo)

            val newFolder = File(root, "Pictures/Vacation_2026")
            assertTrue(oldFolder.renameTo(newFolder))

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(photo.absolutePath),
                searchRoots = listOf(oldFolder.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(File(newFolder, "IMG_001.jpg").absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testFindsFileWhenFolderMovesToSiblingMediaTree() {
        val root = createTempDir("folder_move")
        try {
            val oldFolder = File(root, "Pictures/Trips")
            val photo = writeFile(File(oldFolder, "beach.jpg"), 8192, 13)
            val identity = MediaIdentity.of(photo)

            val dcim = File(root, "DCIM").apply { mkdirs() }
            val newFolder = File(dcim, "Trips_Archived")
            assertTrue(oldFolder.renameTo(newFolder))

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(photo.absolutePath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(dcim.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(File(newFolder, "beach.jpg").absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testSessionRelocatedFolderResolvesRemainingItemsWithoutRescanning() {
        val root = createTempDir("batch_relocation")
        try {
            val oldFolder = File(root, "Pictures/AlbumA")
            val files = (1..25).map { idx ->
                writeFile(File(oldFolder, "photo_$idx.jpg"), 2048 + idx * 17, idx.toByte())
            }
            val records = files.map { it.absolutePath to MediaIdentity.of(it) }

            val newFolder = File(root, "DCIM/MovedAlbumA").apply { parentFile?.mkdirs() }
            assertTrue(oldFolder.renameTo(newFolder))

            val session = MediaIdentityResolver.ResolutionSession()
            records.forEach { (_, id) -> session.registerMissingSize(id.size) }

            for ((oldPath, identity) in records) {
                val res = MediaIdentityResolver.resolveInFilesystem(
                    identity = identity,
                    hintPaths = listOf(oldPath),
                    searchRoots = listOf(oldFolder.absolutePath),
                    session = session,
                    extraRoots = listOf(root.absolutePath)
                )
                assertNotNull("Expected to resolve $oldPath", res)
                assertEquals(
                    File(newFolder, File(oldPath).name).absolutePath,
                    res!!.path
                )
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testDoesNotPullFromForeignFolderWhenSourceFolderEmptiedButStillExists() {
        val root = createTempDir("emptied_folder")
        try {
            val fixedMtime = 1_700_000_000_000L
            val folderA = File(root, "Pictures/A")
            val fileA = writeFile(File(folderA, "dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val identity = MediaIdentity.of(fileA).withFolder(FolderIdentity.captureForFile(fileA))
            val oldPath = fileA.absolutePath

            // User deletes file inside Pictures/A, leaving Pictures/A as an empty folder on disk.
            assertTrue(fileA.delete())
            assertTrue(folderA.isDirectory)

            // An identical byte-for-byte copy sits in Pictures/B.
            writeFile(File(root, "Pictures/B/dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(folderA.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNull(resolution)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testDoesNotPullFromForeignFolderWhenSourceFolderCompletelyDeleted() {
        val root = createTempDir("deleted_folder")
        try {
            val fixedMtime = 1_700_000_050_000L
            val folderA = File(root, "Pictures/A")
            val fileA = writeFile(File(folderA, "dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            writeFile(File(folderA, "neighbor.jpg"), 5000, 17)
            val identity = MediaIdentity.of(fileA).withFolder(FolderIdentity.captureForFile(fileA))
            val oldPath = fileA.absolutePath

            // Completely delete Pictures/A from disk.
            assertTrue(folderA.deleteRecursively())

            // Pictures/B has an identical copy of dosya.jpg, but neither neighbor anchors nor name match.
            writeFile(File(root, "Pictures/B/dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(folderA.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNull(resolution)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testFindsFileInUnrelatedRenamedFolderViaNeighborAnchorsEvenWithForeignDuplicate() {
        val root = createTempDir("neighbor_anchor_rename")
        try {
            val fixedMtime = 1_700_000_080_000L
            val folderA = File(root, "Pictures/A")
            val fileA = writeFile(File(folderA, "dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            writeFile(File(folderA, "anchor1.jpg"), 5120, 77)
            writeFile(File(folderA, "anchor2.jpg"), 6144, 88)
            val identity = MediaIdentity.of(fileA).withFolder(FolderIdentity.captureForFile(fileA))
            val oldPath = fileA.absolutePath

            // Rename & move Pictures/A to a completely unrelated name under DCIM.
            val movedFolder = File(root, "DCIM/TamamenFarkliAd").apply { parentFile?.mkdirs() }
            assertTrue(folderA.renameTo(movedFolder))
            File(movedFolder, "dosya.jpg").setLastModified(fixedMtime)

            // Also keep a byte-identical copy in Pictures/B with identical filename & mtime.
            writeFile(File(root, "Pictures/B/dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(folderA.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(File(movedFolder, "dosya.jpg").absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testFindsSingleFileRenamedToUnrelatedFolderWhenDirectoryInodeMatches() {
        val root = createTempDir("inode_solo_rename")
        val previousReader = FolderIdentity.customInodeReader
        try {
            val fixedMtime = 1_700_000_090_000L
            val folderA = File(root, "Pictures/A")
            val fileA = writeFile(File(folderA, "dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val movedFolder = File(root, "DCIM/HicAlakasizAd")
            val foreignFolder = File(root, "Pictures/B")

            FolderIdentity.customInodeReader = { dir ->
                when (dir.absolutePath) {
                    folderA.absolutePath, movedFolder.absolutePath -> 555777L
                    foreignFolder.absolutePath -> 999111L
                    else -> 0L
                }
            }

            val identity = MediaIdentity.of(fileA).withFolder(FolderIdentity.captureForFile(fileA))
            val oldPath = fileA.absolutePath

            movedFolder.parentFile?.mkdirs()
            assertTrue(folderA.renameTo(movedFolder))
            File(movedFolder, "dosya.jpg").setLastModified(fixedMtime)

            writeFile(File(foreignFolder, "dosya.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(folderA.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(File(movedFolder, "dosya.jpg").absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            FolderIdentity.customInodeReader = previousReader
            root.deleteRecursively()
        }
    }

    @Test
    fun testLoadsOneVerifiedFileWhenMovedCopiesBothShareFolderContinuity() {
        val root = createTempDir("moved_dup_tie")
        try {
            val fixedMtime = 1_700_000_095_000L
            val oldFolder = File(root, "Pictures/Holiday")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val identity = MediaIdentity.of(original).withFolder(FolderIdentity.captureForFile(original))
            val oldPath = original.absolutePath
            assertTrue(original.delete())
            assertTrue(oldFolder.delete())

            // Two relocated copies both keeping the moved folder name "Holiday".
            val copy1 = writeFile(File(root, "DCIM/Holiday/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val copy2 = writeFile(File(root, "Documents/Holiday/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNotNull(resolution)
            assertTrue(resolution!!.confirmed)
            assertTrue(resolution.path == copy1.absolutePath || resolution.path == copy2.absolutePath)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testPrefersMovedFolderKeepingNameWhenDuplicateContentExistsElsewhere() {
        val root = createTempDir("moved_folder_dup")
        try {
            val fixedMtime = 1_700_000_100_000L
            val oldFolder = File(root, "Pictures/Vacation")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val identity = MediaIdentity.of(original)
            val oldPath = original.absolutePath

            // Move the Vacation folder under DCIM while keeping the folder name.
            val movedFolder = File(root, "DCIM/Vacation").apply { parentFile?.mkdirs() }
            assertTrue(oldFolder.renameTo(movedFolder))

            // Even with a byte-identical copy inside Pictures/Archive with matching mtime,
            // DCIM/Vacation wins because the folder name itself matches the relocated source.
            writeFile(File(root, "Pictures/Archive/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(File(movedFolder, "dup.jpg").absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testPrefersRenamedSimilarFolderWhenDuplicateContentExistsInAnotherTree() {
        val root = createTempDir("renamed_similar_dup")
        try {
            val fixedMtime = 1_700_000_200_000L
            val oldFolder = File(root, "Pictures/Vacation")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val identity = MediaIdentity.of(original)
            val oldPath = original.absolutePath

            val renamedFolder = File(root, "Pictures/Vacation_2026")
            assertTrue(oldFolder.renameTo(renamedFolder))

            writeFile(File(root, "Pictures/Backup/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(File(renamedFolder, "dup.jpg").absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testPrefersMatchingLastModifiedOverOtherDuplicateCopy() {
        val root = createTempDir("mtime_dup")
        try {
            val originalMtime = 1_700_000_300_000L
            val oldFolder = File(root, "Pictures/Summer")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42).apply {
                setLastModified(originalMtime)
            }
            val identity = MediaIdentity.of(original).withFolder(FolderIdentity.captureForFile(original))
            val oldPath = original.absolutePath

            val renamedFolder = File(root, "Pictures/Summer_Renamed")
            assertTrue(oldFolder.renameTo(renamedFolder))
            val movedFile = File(renamedFolder, "dup.jpg").apply { setLastModified(originalMtime) }

            writeFile(File(root, "Pictures/Summer_OlderCopy/dup.jpg"), 4096, 42).apply {
                setLastModified(originalMtime - 60_000L)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(movedFile.absolutePath, resolution!!.path)
            assertTrue(resolution.confirmed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testRespectsExcludedPathsWhenResolvingDuplicates() {
        val root = createTempDir("excluded_dup")
        try {
            val fixedMtime = 1_700_000_400_000L
            val oldFolder = File(root, "Pictures/Old")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val identity = MediaIdentity.of(original)
            val oldPath = original.absolutePath
            assertTrue(original.delete())
            assertTrue(oldFolder.delete())

            val existing = writeFile(File(root, "Pictures/Old_Existing/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val relocated = writeFile(File(root, "DCIM/Old/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath),
                excludedPaths = setOf(existing.absolutePath)
            )

            assertNotNull(resolution)
            assertEquals(relocated.absolutePath, resolution!!.path)

            // If all matching copies on disk are already claimed in the same container, return null
            // rather than overwriting the existing entry.
            val blocked = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath),
                excludedPaths = setOf(existing.absolutePath, relocated.absolutePath)
            )
            assertNull(blocked)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testTwoPassSessionUsesSiblingRelocationToResolveDuplicateFile() {
        val root = createTempDir("two_pass_session")
        try {
            val fixedMtime = 1_700_000_500_000L
            val oldFolder = File(root, "Pictures/Album")
            val dupFile = writeFile(File(oldFolder, "dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val uniqueFile = writeFile(File(oldFolder, "unique.jpg"), 6144, 99)

            val dupIdentity = MediaIdentity.of(dupFile)
            val uniqueIdentity = MediaIdentity.of(uniqueFile)
            val oldDupPath = dupFile.absolutePath
            val oldUniquePath = uniqueFile.absolutePath

            // Move the album across trees while keeping its folder name (no folder anchors on legacy row).
            val movedFolder = File(root, "DCIM/Album").apply { parentFile?.mkdirs() }
            assertTrue(oldFolder.renameTo(movedFolder))
            File(movedFolder, "dup.jpg").setLastModified(fixedMtime)

            // Also place an identical copy inside another Album folder with exact same name & mtime.
            writeFile(File(root, "Pictures/Backup/Album/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }

            val session = MediaIdentityResolver.ResolutionSession().apply {
                registerMissingSize(dupIdentity.size)
                registerMissingSize(uniqueIdentity.size)
            }

            // Pass 1 (allowAmbiguous = false): dup.jpg comes first and is deferred; unique.jpg
            // resolves and records Pictures/Album -> DCIM/UnrelatedNewName in the session.
            val pass1Dup = MediaIdentityResolver.resolveInFilesystem(
                identity = dupIdentity,
                hintPaths = listOf(oldDupPath),
                searchRoots = listOf(oldFolder.absolutePath),
                session = session,
                extraRoots = listOf(root.absolutePath),
                allowAmbiguous = false
            )
            assertNull(pass1Dup)

            val pass1Unique = MediaIdentityResolver.resolveInFilesystem(
                identity = uniqueIdentity,
                hintPaths = listOf(oldUniquePath),
                searchRoots = listOf(oldFolder.absolutePath),
                session = session,
                extraRoots = listOf(root.absolutePath),
                allowAmbiguous = false
            )
            assertNotNull(pass1Unique)
            assertEquals(File(movedFolder, "unique.jpg").absolutePath, pass1Unique!!.path)

            // Pass 2 (allowAmbiguous = true): dup.jpg now hits Layer 0b (relocated folder cache)
            // and picks DCIM/Album/dup.jpg instead of Pictures/Backup/Album/dup.jpg.
            val pass2Dup = MediaIdentityResolver.resolveInFilesystem(
                identity = dupIdentity,
                hintPaths = listOf(oldDupPath),
                searchRoots = listOf(oldFolder.absolutePath),
                session = session,
                extraRoots = listOf(root.absolutePath),
                allowAmbiguous = true
            )
            assertNotNull(pass2Dup)
            assertEquals(File(movedFolder, "dup.jpg").absolutePath, pass2Dup!!.path)
        } finally {
            root.deleteRecursively()
        }
    }
}
