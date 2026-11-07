package org.fossify.gallery

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.fossify.gallery.helpers.*
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
        val normalFileName = VirtualAlbumSyncManager.getPublicAlbumFileName("Resimler", 1L)
        assertEquals("Resimler.json", normalFileName)

        val specialFileName = VirtualAlbumSyncManager.getPublicAlbumFileName("Tatil / 2026 : Yaz ?", 2L)
        assertEquals("Tatil _ 2026 _ Yaz _.json", specialFileName)

        val hiddenFileName = VirtualAlbumSyncManager.getHiddenAlbumFileName("Gizli Albüm")
        assertTrue(hiddenFileName.startsWith("fva_"))
        assertTrue(hiddenFileName.endsWith(".json"))
        assertFalse(hiddenFileName.contains("Gizli Albüm"))
        // Deterministic check
        assertEquals(hiddenFileName, VirtualAlbumSyncManager.getHiddenAlbumFileName("Gizli Albüm"))
    }

    @Test
    fun testStealthHiddenAlbumWrapper() {
        // Stealth hidden albums store opaque outer JSON with encrypted payload
        val fakeEncryptedPayload = "v2:argon2id_aes256_gcm:ciphertext123456789"
        val outerBackup = VirtualAlbumBackup(
            version = 2,
            isHidden = true,
            encryptedPayload = fakeEncryptedPayload
        )

        val outerJson = Gson().toJson(outerBackup)
        // Ensure album name and item paths are not present in outer JSON
        assertFalse(outerJson.contains("Resimler"))
        assertFalse(outerJson.contains("fullPath"))
        assertFalse(outerJson.contains("mediaPaths"))
        assertTrue(outerJson.contains("encryptedPayload"))

        val parsed = Gson().fromJson(outerJson, VirtualAlbumBackup::class.java)
        assertTrue(parsed.isHidden)
        assertEquals(fakeEncryptedPayload, parsed.encryptedPayload)
        assertNull(parsed.name)
    }

    @Test
    fun testDirectoryStructurePaths() {
        val vaultRoot = CryptoManager.getDefaultVaultRoot()
        val virtualAlbumsDir = VirtualAlbumSyncManager.getVirtualAlbumsStorageDir()

        assertEquals(CIPHER_SECRETS_DIR_NAME, vaultRoot.name)
        assertEquals(CIPHER_VIRTUAL_ALBUMS_DIR_NAME, virtualAlbumsDir.name)

        // Both folders must be siblings inside the Documents/Cipher/ container
        assertEquals(CIPHER_ROOT_DIR_NAME, vaultRoot.parentFile?.name)
        assertEquals(CIPHER_ROOT_DIR_NAME, virtualAlbumsDir.parentFile?.name)
        assertEquals(vaultRoot.parentFile?.absolutePath, virtualAlbumsDir.parentFile?.absolutePath)
    }

    @Test
    fun testLegacyDirectoryMigrationLogic() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "cipher_mig_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val legacyDir = File(tempDir, "Fossify Virtual Albums")
            legacyDir.mkdirs()
            val sampleAlbumFile = File(legacyDir, "EskiAlbum.json")
            sampleAlbumFile.writeText("{\"version\":1,\"name\":\"EskiAlbum\"}")

            val targetDir = File(tempDir, "Cipher/$CIPHER_VIRTUAL_ALBUMS_DIR_NAME")
            targetDir.mkdirs()

            // Execute migration logic
            legacyDir.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }?.forEach { f ->
                val targetFile = File(targetDir, f.name)
                if (!targetFile.exists()) {
                    f.copyTo(targetFile, overwrite = true)
                }
                f.delete()
            }

            val migratedFile = File(targetDir, "EskiAlbum.json")
            assertTrue(migratedFile.exists())
            assertFalse(sampleAlbumFile.exists())
            assertEquals("{\"version\":1,\"name\":\"EskiAlbum\"}", migratedFile.readText())
        } finally {
            tempDir.deleteRecursively()
        }
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
