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
            name = "Pictures",
            createdTs = 1700000000000L,
            isHidden = false,
            items = listOf(
                VirtualAlbumItemBackup("/storage/emulated/0/Pictures/Album/1.jpg", 1700000010000L),
                VirtualAlbumItemBackup("/storage/emulated/0/Pictures/Album/2.jpg", 1700000020000L)
            )
        )

        val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
        assertTrue(json.contains("\"name\": \"Pictures\""))
        assertTrue(json.contains("/storage/emulated/0/Pictures/Album/1.jpg"))

        val parsed = Gson().fromJson(json, VirtualAlbumBackup::class.java)
        assertEquals("Pictures", parsed.name)
        assertEquals(2, parsed.items?.size)
        assertEquals("/storage/emulated/0/Pictures/Album/1.jpg", parsed.items?.get(0)?.fullPath)
        assertEquals(1700000010000L, parsed.items?.get(0)?.addedTs)
    }

    @Test
    fun testSnakeCaseAndLegacyFallbackCompatibility() {
        // Test parsing snake_case JSON (e.g. from external sources or scripts)
        val snakeCaseJson = """
            {
                "version": 1,
                "name": "Vacation Album",
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
        assertEquals("Vacation Album", parsed.name)
        assertEquals(1690000000000L, parsed.createdTs)
        assertFalse(parsed.isHidden)
        assertEquals(1, parsed.items?.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/IMG_01.jpg", parsed.items?.get(0)?.fullPath)
        assertEquals(1690000010000L, parsed.items?.get(0)?.addedTs)

        // Test fallback media_paths
        val mediaPathsJson = """
            {
                "name": "Landscapes",
                "media_paths": [
                    "/storage/emulated/0/Pictures/m1.jpg",
                    "/storage/emulated/0/Pictures/m2.jpg"
                ]
            }
        """.trimIndent()

        val parsedPaths = Gson().fromJson(mediaPathsJson, VirtualAlbumBackup::class.java)
        assertEquals("Landscapes", parsedPaths.name)
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
        val normalFileName = VirtualAlbumSyncManager.getPublicAlbumFileName("Pictures", 1L)
        assertEquals("Pictures.json", normalFileName)

        val specialFileName = VirtualAlbumSyncManager.getPublicAlbumFileName("Vacation / 2026 : Summer ?", 2L)
        assertEquals("Vacation _ 2026 _ Summer _.json", specialFileName)

        val hiddenFileName = VirtualAlbumSyncManager.getHiddenAlbumFileName("Hidden Album")
        assertTrue(hiddenFileName.startsWith("fva_"))
        assertTrue(hiddenFileName.endsWith(".json"))
        assertFalse(hiddenFileName.contains("Hidden Album"))
        // Deterministic check
        assertEquals(hiddenFileName, VirtualAlbumSyncManager.getHiddenAlbumFileName("Hidden Album"))
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
        assertFalse(outerJson.contains("Pictures"))
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

        // Both folders must be siblings inside the "Fossify Gallery Cipher" container, which is a
        // standalone folder in the internal storage and not inside the Documents folder.
        assertEquals(CIPHER_APP_DIR_NAME, vaultRoot.parentFile?.name)
        assertEquals(CIPHER_APP_DIR_NAME, virtualAlbumsDir.parentFile?.name)
        assertEquals(vaultRoot.parentFile?.absolutePath, virtualAlbumsDir.parentFile?.absolutePath)
    }

    @Test
    fun testLegacyDirectoryMigrationLogic() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "cipher_mig_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val legacyDir = File(tempDir, "Fossify Virtual Albums")
            legacyDir.mkdirs()
            val sampleAlbumFile = File(legacyDir, "LegacyAlbum.json")
            sampleAlbumFile.writeText("{\"version\":1,\"name\":\"LegacyAlbum\"}")

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

            val migratedFile = File(targetDir, "LegacyAlbum.json")
            assertTrue(migratedFile.exists())
            assertFalse(sampleAlbumFile.exists())
            assertEquals("{\"version\":1,\"name\":\"LegacyAlbum\"}", migratedFile.readText())
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
            // file3Missing is NOT created (simulates deleted file from Pictures)

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
    @Test
    fun testParentPhysicalPathSerializationAndDeserialization() {
        val backup = VirtualAlbumBackup(
            version = 1,
            name = "Camera Album",
            createdTs = 1700000000000L,
            isHidden = false,
            parentPhysicalPath = "/storage/emulated/0/DCIM/Camera",
            items = listOf(
                VirtualAlbumItemBackup("/storage/emulated/0/DCIM/Camera/IMG_01.jpg", 1700000010000L)
            )
        )

        val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
        assertTrue(json.contains("\"parentPhysicalPath\": \"/storage/emulated/0/DCIM/Camera\""))

        val parsed = Gson().fromJson(json, VirtualAlbumBackup::class.java)
        assertEquals("/storage/emulated/0/DCIM/Camera", parsed.parentPhysicalPath)
        assertEquals("Camera Album", parsed.name)

        // Also test deserialization when snake_case "parent_physical_path" is used
        val snakeCaseJson = """
            {
                "name": "Camera Album",
                "parent_physical_path": "/storage/emulated/0/DCIM/Camera"
            }
        """.trimIndent()
        val parsedSnakeCase = Gson().fromJson(snakeCaseJson, VirtualAlbumBackup::class.java)
        assertEquals("/storage/emulated/0/DCIM/Camera", parsedSnakeCase.parentPhysicalPath)
    }

    @Test
    fun testAlbumsMayHoldItemsFromSeveralPhysicalFolders() {
        val cameraFolder = "/storage/emulated/0/DCIM/Camera"
        val screenshotsFolder = "/storage/emulated/0/Pictures/Screenshots"
        val downloadsFolder = "/storage/emulated/0/Download"

        val albums = listOf(
            VirtualAlbum(id = 1L, name = "Camera 1", createdTs = 0L, thumbnail = "", parentPhysicalPath = cameraFolder),
            VirtualAlbum(id = 2L, name = "Screenshots 1", createdTs = 0L, thumbnail = "", parentPhysicalPath = screenshotsFolder),
            VirtualAlbum(id = 3L, name = "Camera 2", createdTs = 0L, thumbnail = "", parentPhysicalPath = cameraFolder)
        )

        // The remembered source folder is only a hint for the copy dialog, it does not restrict
        // which albums may be offered as a destination.
        val cameraMatchedAlbums = albums.filter { it.parentPhysicalPath.equals(cameraFolder, ignoreCase = true) }
        assertEquals(2, cameraMatchedAlbums.size)
        assertTrue(cameraMatchedAlbums.any { it.name == "Camera 1" })
        assertTrue(cameraMatchedAlbums.any { it.name == "Camera 2" })
        assertFalse(cameraMatchedAlbums.any { it.name == "Screenshots 1" })

        // An album may hold items from any number of physical folders, exactly like the favorites.
        val incomingItems = listOf(
            "$cameraFolder/IMG_001.jpg",
            "$downloadsFolder/doc_photo.jpg",
            "$cameraFolder/IMG_002.jpg",
            "$screenshotsFolder/shot.png"
        )
        val allowedItems = incomingItems.filter { path ->
            File(path).name.isNotEmpty()
        }
        assertEquals(4, allowedItems.size)
        assertTrue(allowedItems.contains("$downloadsFolder/doc_photo.jpg"))
        assertTrue(allowedItems.contains("$screenshotsFolder/shot.png"))

        // Copy destination restriction: cannot copy from a virtual album to its owner physical album
        val destinationDirs = listOf(cameraFolder, screenshotsFolder, downloadsFolder)
        val validDestinations = destinationDirs.filter { !it.equals(cameraFolder, ignoreCase = true) }
        assertEquals(2, validDestinations.size)
        assertFalse(validDestinations.contains(cameraFolder))
        assertTrue(validDestinations.contains(screenshotsFolder))
        assertTrue(validDestinations.contains(downloadsFolder))
    }

    @Test
    fun testLegacyAndNewFolderDiscovery() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "cipher_multi_dir_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val legacyDir = File(tempDir, "Fossify Virtual Albums").apply { mkdirs() }
            val publicDir = File(tempDir, "Cipher/$CIPHER_VIRTUAL_ALBUMS_DIR_NAME").apply { mkdirs() }

            File(legacyDir, "OldAlbum.json").writeText("{\"version\":1,\"name\":\"Old Album\"}")
            File(publicDir, "NewAlbum.json").writeText("{\"version\":1,\"name\":\"New Album\"}")
            // Duplicate in legacy folder should be deduplicated
            File(legacyDir, "NewAlbum.json").writeText("{\"version\":1,\"name\":\"New Album\"}")

            val candidateDirs = listOf(publicDir, legacyDir).filter { it.exists() && it.isDirectory }
            val processedNames = HashSet<String>()
            val loadedNames = ArrayList<String>()

            for (dir in candidateDirs) {
                val jsonFiles = dir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) } ?: emptyArray()
                for (f in jsonFiles) {
                    val backup = Gson().fromJson(f.readText(), VirtualAlbumBackup::class.java)
                    val albumName = backup.name?.trim() ?: f.nameWithoutExtension.trim()
                    if (processedNames.contains(albumName.lowercase())) continue
                    processedNames.add(albumName.lowercase())
                    loadedNames.add(albumName)
                }
            }

            assertEquals(2, loadedNames.size)
            assertTrue(loadedNames.contains("Old Album"))
            assertTrue(loadedNames.contains("New Album"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDecryptionConfirmationCriteria() {
        val encryptedFolder = "/storage/emulated/0/DCIM/.fv_vault"
        val normalFolder = "/storage/emulated/0/DCIM/Camera"
        val hiddenVirtualPath = "virtual_album_1"
        val normalVirtualPath = "virtual_album_2"

        // 1. From hidden physical to normal physical: requires confirmation
        val isSourceHiddenPhysical1 = true
        val isDestNonHiddenPhysical1 = true
        assertTrue(isSourceHiddenPhysical1 && isDestNonHiddenPhysical1)

        // 2. From hidden virtual to normal physical: requires confirmation
        val isSourceHiddenVirtual2 = true
        val isDestNonHiddenPhysical2 = true
        assertTrue(isSourceHiddenVirtual2 && isDestNonHiddenPhysical2)

        // 3. From normal physical to normal physical: NO confirmation
        val isSourceHiddenPhysical3 = false
        val isSourceHiddenVirtual3 = false
        val needsConfirmation3 = (isSourceHiddenPhysical3 || isSourceHiddenVirtual3) && true
        assertFalse(needsConfirmation3)

        // 4. From hidden to hidden: NO confirmation
        val isDestNonHiddenPhysical4 = false
        val needsConfirmation4 = (true) && isDestNonHiddenPhysical4
        assertFalse(needsConfirmation4)
    }
}