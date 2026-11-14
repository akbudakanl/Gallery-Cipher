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
    fun testRefusesAmbiguousDuplicateContentMatch() {
        val root = createTempDir("duplicate")
        try {
            val oldFolder = File(root, "Pictures/Old")
            val original = writeFile(File(oldFolder, "dup.jpg"), 4096, 42)
            val identity = MediaIdentity.of(original)
            val oldPath = original.absolutePath
            assertTrue(original.delete())
            assertTrue(oldFolder.delete())

            // Two identical files at different locations with no MediaStore ID to disambiguate.
            writeFile(File(root, "Pictures/Copy1/dup.jpg"), 4096, 42)
            writeFile(File(root, "Pictures/Copy2/dup.jpg"), 4096, 42)

            val resolution = MediaIdentityResolver.resolveInFilesystem(
                identity = identity,
                hintPaths = listOf(oldPath),
                searchRoots = listOf(oldFolder.absolutePath),
                extraRoots = listOf(root.absolutePath)
            )

            assertNull(resolution)
        } finally {
            root.deleteRecursively()
        }
    }
}
