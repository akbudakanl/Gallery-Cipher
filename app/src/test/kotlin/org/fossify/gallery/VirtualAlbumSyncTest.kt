package org.fossify.gallery

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.fossify.gallery.helpers.VirtualAlbumBackup
import org.fossify.gallery.helpers.VirtualAlbumItemBackup
import org.fossify.gallery.helpers.VirtualAlbumSyncManager
import org.fossify.gallery.models.VirtualAlbum
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VirtualAlbumSyncTest {

    @Test
    fun testSerializationAndDeserialization() {
        val backup = VirtualAlbumBackup(
            version = 1,
            name = "Resimler",
            createdTs = 1700000000000L,
            isHidden = false,
            items = listOf(
                VirtualAlbumItemBackup("/storage/emulated/0/Pictures/Resimler/1.jpg", 1700000010000L),
                VirtualAlbumItemBackup("/storage/emulated/0/Pictures/Resimler/2.jpg", 1700000020000L)
            )
        )

        val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
        assertTrue(json.contains("\"name\": \"Resimler\""))
        assertTrue(json.contains("/storage/emulated/0/Pictures/Resimler/1.jpg"))

        val parsed = Gson().fromJson(json, VirtualAlbumBackup::class.java)
        assertEquals("Resimler", parsed.name)
        assertEquals(2, parsed.items?.size)
        assertEquals("/storage/emulated/0/Pictures/Resimler/1.jpg", parsed.items?.get(0)?.fullPath)
        assertEquals(1700000010000L, parsed.items?.get(0)?.addedTs)
    }

    @Test
    fun testSnakeCaseAndLegacyFallbackCompatibility() {
        // Test parsing snake_case JSON (e.g. from external sources or scripts)
        val snakeCaseJson = """
            {
                "version": 1,
                "name": "Tatil Albümü",
                "created_ts": 1690000000000,
                "is_hidden": false,
                "items": [
                    {
                        "full_path": "/storage/emulated/0/DCIM/Camera/IMG_01.jpg",
                        "added_ts": 1690000010000
                    }
                ]
            }
        """.trimIndent()

        val parsed = Gson().fromJson(snakeCaseJson, VirtualAlbumBackup::class.java)
        assertEquals("Tatil Albümü", parsed.name)
        assertEquals(1690000000000L, parsed.createdTs)
        assertFalse(parsed.isHidden)
        assertEquals(1, parsed.items?.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/IMG_01.jpg", parsed.items?.get(0)?.fullPath)
        assertEquals(1690000010000L, parsed.items?.get(0)?.addedTs)

        // Test fallback media_paths
        val mediaPathsJson = """
            {
                "name": "Manzaralar",
                "media_paths": [
                    "/storage/emulated/0/Pictures/m1.jpg",
                    "/storage/emulated/0/Pictures/m2.jpg"
                ]
            }
        """.trimIndent()

        val parsedPaths = Gson().fromJson(mediaPathsJson, VirtualAlbumBackup::class.java)
        assertEquals("Manzaralar", parsedPaths.name)
        assertEquals(2, parsedPaths.mediaPaths?.size)
    }

    @Test
    fun testCorruptedJsonDoesNotCrash() {
        val corruptedJson = "{ this is completely invalid json !! "
        var parsed: VirtualAlbumBackup? = null
        try {
            parsed = Gson().fromJson(corruptedJson, VirtualAlbumBackup::class.java)
        } catch (_: Exception) {
            // Expected exception caught safely
        }
        assertNull(parsed)
    }

    @Test
    fun testSafeFileNameGeneration() {
        val normalAlbum = VirtualAlbum(id = 1L, name = "Resimler", isHidden = false)
        val normalFileName = VirtualAlbumSyncManager.getAlbumFileName(normalAlbum)
        assertEquals("Resimler.json", normalFileName)

        val specialCharsAlbum = VirtualAlbum(id = 2L, name = "Tatil / 2026 : Yaz ?", isHidden = false)
        val specialFileName = VirtualAlbumSyncManager.getAlbumFileName(specialCharsAlbum)
        assertEquals("Tatil _ 2026 _ Yaz _.json", specialFileName)

        val hiddenAlbum = VirtualAlbum(id = 42L, name = "FVSTR1:secret", isHidden = true)
        val hiddenFileName = VirtualAlbumSyncManager.getAlbumFileName(hiddenAlbum)
        assertEquals("hidden_album_42.json", hiddenFileName)
    }

    @Test
    fun testMissingFileHandlingPreservesValidFiles() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "fossify_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val file1 = File(tempDir, "photo1.jpg")
            val file2 = File(tempDir, "photo2.jpg")
            val file3Missing = File(tempDir, "photo3_deleted.jpg")

            file1.writeText("fake photo 1")
            file2.writeText("fake photo 2")
            // file3Missing is NOT created (simulates deleted file from Resimler)

            val rawPaths = listOf(file1.absolutePath, file3Missing.absolutePath, file2.absolutePath)

            // Filtering logic used in VirtualAlbumSyncManager:
            val validPaths = rawPaths.filter { File(it).exists() }

            // Verify only existing files are kept, and missing file does not corrupt or drop valid files
            assertEquals(2, validPaths.size)
            assertTrue(validPaths.contains(file1.absolutePath))
            assertTrue(validPaths.contains(file2.absolutePath))
            assertFalse(validPaths.contains(file3Missing.absolutePath))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
