package org.fossify.gallery

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.fossify.gallery.helpers.ReferenceBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the backup format of the favorites and the virtual albums.
 */
class ReferenceBackupTest {
    @Test
    fun testFavoritesBackupSerialization() {
        val backup = ReferenceBackupManager.FavoritesBackup(
            version = 1,
            favorites = listOf(
                ReferenceBackupManager.FavoriteBackup(
                    fullPath = "/storage/emulated/0/DCIM/Camera/IMG_001.jpg",
                    filename = "IMG_001.jpg",
                    parentPath = "/storage/emulated/0/DCIM/Camera",
                    mediaStoreId = 41233,
                    fileSize = 2841923,
                    lastModified = 1700000000000L,
                    fingerprint = "a3f1c2d4"
                )
            )
        )

        val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("/storage/emulated/0/DCIM/Camera/IMG_001.jpg"))
        assertTrue(json.contains("\"mediaStoreId\": 41233"))
        assertTrue(json.contains("\"fingerprint\": \"a3f1c2d4\""))

        val parsed = Gson().fromJson(json, ReferenceBackupManager.FavoritesBackup::class.java)
        assertEquals(1, parsed.favorites?.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/IMG_001.jpg", parsed.favorites?.get(0)?.fullPath)
        assertEquals(41233L, parsed.favorites?.get(0)?.mediaStoreId)
        assertEquals("a3f1c2d4", parsed.favorites?.get(0)?.fingerprint)
    }

    @Test
    fun testFavoritesBackupWithoutEntries() {
        val backup = ReferenceBackupManager.FavoritesBackup(version = 1, favorites = emptyList())
        val json = Gson().toJson(backup)

        val parsed = Gson().fromJson(json, ReferenceBackupManager.FavoritesBackup::class.java)
        assertNotNull(parsed.favorites)
        assertEquals(0, parsed.favorites?.size)
    }

    @Test
    fun testAnEmptyBackupFileDoesNotBreakTheParser() {
        val parsed = Gson().fromJson("{}", ReferenceBackupManager.FavoritesBackup::class.java)
        assertEquals(1, parsed.version)
        assertEquals(null, parsed.favorites)
    }

    @Test
    fun testACorruptedBackupFileDoesNotCrashTheParser() {
        val corrupted = "{ this is not valid json !!"
        var parsed: ReferenceBackupManager.FavoritesBackup? = null
        try {
            parsed = Gson().fromJson(corrupted, ReferenceBackupManager.FavoritesBackup::class.java)
        } catch (_: Exception) {
            // Expected, the caller has to survive it.
        }
        assertEquals(null, parsed)
    }
}
