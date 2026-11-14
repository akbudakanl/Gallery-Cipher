package org.fossify.gallery

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
    fun testLoadsOneVerifiedFileWhenAmbiguousDuplicatesTieCompletely() {
        val root = createTempDir("duplicate_tie")
        try {
            val oldFolder = File(root, "Pictures/Old")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42)
            val fixedMtime = 1_700_000_000_000L
            original.setLastModified(fixedMtime)
            val identity = MediaIdentity.of(original)
            val oldPath = original.absolutePath
            assertTrue(original.delete())
            assertTrue(oldFolder.delete())

            val copy1 = writeFile(File(root, "Pictures/Copy1/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val copy2 = writeFile(File(root, "Pictures/Copy2/dup.jpg"), 4096, 42).apply {
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
            val identity = MediaIdentity.of(original)
            val oldPath = original.absolutePath

            val renamedFolder = File(root, "Pictures/CompletelyDifferent")
            assertTrue(oldFolder.renameTo(renamedFolder))
            val movedFile = File(renamedFolder, "dup.jpg").apply { setLastModified(originalMtime) }

            writeFile(File(root, "Pictures/OlderCopy/dup.jpg"), 4096, 42).apply {
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

            val existing = writeFile(File(root, "Pictures/Existing/dup.jpg"), 4096, 42).apply {
                setLastModified(fixedMtime)
            }
            val relocated = writeFile(File(root, "DCIM/Relocated/dup.jpg"), 4096, 42).apply {
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

            // Rename and move the album across trees to an unrelated folder name.
            val movedFolder = File(root, "DCIM/UnrelatedNewName").apply { parentFile?.mkdirs() }
            assertTrue(oldFolder.renameTo(movedFolder))
            File(movedFolder, "dup.jpg").setLastModified(fixedMtime)

            // Also place an identical copy inside Pictures/Archive with exact same name & mtime.
            writeFile(File(root, "Pictures/Archive/dup.jpg"), 4096, 42).apply {
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
            // and picks DCIM/UnrelatedNewName/dup.jpg instead of Pictures/Archive/dup.jpg.
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
