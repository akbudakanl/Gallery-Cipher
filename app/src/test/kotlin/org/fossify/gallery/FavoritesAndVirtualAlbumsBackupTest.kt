package org.fossify.gallery

import com.google.gson.Gson
import org.fossify.gallery.helpers.FavoritesAndVirtualAlbumsBackupManager
import org.fossify.gallery.helpers.ReferenceBackupManager
import org.fossify.gallery.helpers.VirtualAlbumBackup
import org.fossify.gallery.models.Favorite
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FavoritesAndVirtualAlbumsBackupTest {

    private fun readZipEntries(bytes: ByteArray): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return entries
    }

    @Test
    fun testExportIncludesFavoritesAndVirtualAlbumsFoldersInZip() {
        val favorites = listOf(
            Favorite(
                id = 1,
                fullPath = "/storage/emulated/0/DCIM/Camera/IMG_001.jpg",
                filename = "IMG_001.jpg",
                parentPath = "/storage/emulated/0/DCIM/Camera",
                mediaStoreId = 101L,
                fileSize = 2048L,
                lastModified = 1700000000000L,
                fingerprint = "fp_1"
            )
        )
        val publicAlbum = VirtualAlbum(
            id = 10L,
            name = "Vacation",
            createdTs = 1700000100000L,
            thumbnail = "",
            isHidden = false,
            parentPhysicalPath = "/storage/emulated/0/Pictures"
        )
        val hiddenAlbum = VirtualAlbum(
            id = 11L,
            name = "SecretVault",
            createdTs = 1700000200000L,
            thumbnail = "",
            isHidden = true,
            parentPhysicalPath = ""
        )
        val publicItems = listOf(
            VirtualAlbumItem(
                id = 1L,
                albumId = 10L,
                fullPath = "/storage/emulated/0/Pictures/beach.jpg",
                filename = "beach.jpg",
                parentPath = "/storage/emulated/0/Pictures",
                addedTS = 1700000105000L,
                mediaStoreId = 202L,
                fileSize = 4096L,
                lastModified = 1700000105000L,
                fingerprint = "fp_beach"
            )
        )

        val out = ByteArrayOutputStream()
        val exported = FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(
            outputStream = out,
            favorites = favorites,
            virtualAlbums = listOf(publicAlbum to publicItems, hiddenAlbum to emptyList())
        )

        assertTrue(exported)
        val zipBytes = out.toByteArray()
        val entries = readZipEntries(zipBytes)

        assertEquals(2, entries.size)
        assertTrue(entries.containsKey("Favorites/favorites.json"))
        assertTrue(entries.containsKey("Virtual albums/Vacation.json"))
        assertFalse(entries.keys.any { it.contains("SecretVault") })

        val favJson = Gson().fromJson(
            entries["Favorites/favorites.json"],
            ReferenceBackupManager.FavoritesBackup::class.java
        )
        assertEquals(1, favJson.favorites?.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/IMG_001.jpg", favJson.favorites?.get(0)?.fullPath)
        assertEquals("fp_1", favJson.favorites?.get(0)?.fingerprint)

        val albumJson = Gson().fromJson(entries["Virtual albums/Vacation.json"], VirtualAlbumBackup::class.java)
        assertEquals("Vacation", albumJson.name)
        assertEquals(1, albumJson.items?.size)
        assertEquals("/storage/emulated/0/Pictures/beach.jpg", albumJson.items?.get(0)?.fullPath)
        assertEquals("fp_beach", albumJson.items?.get(0)?.fingerprint)
    }

    @Test
    fun testExportOnlyFavoritesOmitsVirtualAlbumsFolder() {
        val favorites = listOf(
            Favorite(
                id = 1,
                fullPath = "/storage/emulated/0/DCIM/IMG.jpg",
                filename = "IMG.jpg",
                parentPath = "/storage/emulated/0/DCIM"
            )
        )

        val out = ByteArrayOutputStream()
        val exported = FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(out, favorites, emptyList())
        assertTrue(exported)

        val entries = readZipEntries(out.toByteArray())
        assertEquals(1, entries.size)
        assertTrue(entries.containsKey("Favorites/favorites.json"))
        assertFalse(entries.keys.any { it.startsWith("Virtual albums/") })
    }

    @Test
    fun testExportOnlyVirtualAlbumsOmitsFavoritesFolder() {
        val album = VirtualAlbum(id = 1L, name = "Work", createdTs = 1700000000000L)
        val out = ByteArrayOutputStream()
        val exported = FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(
            out,
            emptyList(),
            listOf(album to emptyList())
        )
        assertTrue(exported)

        val entries = readZipEntries(out.toByteArray())
        assertEquals(1, entries.size)
        assertTrue(entries.containsKey("Virtual albums/Work.json"))
        assertFalse(entries.keys.any { it.startsWith("Favorites/") })
    }

    @Test
    fun testExportEmptyReturnsFalse() {
        val out = ByteArrayOutputStream()
        val exported = FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(out, emptyList(), emptyList())
        assertFalse(exported)
        assertEquals(0, out.size())
    }

    @Test
    fun testParseZipPackageRoundTrip() {
        val favorites = listOf(
            Favorite(
                id = 1,
                fullPath = "/storage/emulated/0/DCIM/Camera/a.jpg",
                filename = "a.jpg",
                parentPath = "/storage/emulated/0/DCIM/Camera",
                mediaStoreId = 55L,
                fileSize = 1024L,
                lastModified = 1700000000000L,
                fingerprint = "fp_a"
            )
        )
        val album1 = VirtualAlbum(id = 1L, name = "Trips", createdTs = 1700000001000L)
        val album2 = VirtualAlbum(id = 2L, name = "Trips(1)", createdTs = 1700000002000L)
        val item1 = VirtualAlbumItem(
            id = 1L,
            albumId = 1L,
            fullPath = "/storage/emulated/0/Pictures/t1.jpg",
            filename = "t1.jpg",
            parentPath = "/storage/emulated/0/Pictures",
            addedTS = 1700000001000L
        )

        val out = ByteArrayOutputStream()
        FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(
            out,
            favorites,
            listOf(album1 to listOf(item1), album2 to emptyList())
        )

        val parsed = FavoritesAndVirtualAlbumsBackupManager.parseImportPackage(out.toByteArray())
        assertEquals(1, parsed.favorites.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/a.jpg", parsed.favorites[0].fullPath)
        assertEquals("fp_a", parsed.favorites[0].fingerprint)

        assertEquals(2, parsed.virtualAlbums.size)
        val names = parsed.virtualAlbums.map { it.name }.toSet()
        assertTrue(names.contains("Trips"))
        assertTrue(names.contains("Trips(1)"))
        val trips = parsed.virtualAlbums.first { it.name == "Trips" }
        assertEquals(1, trips.items.size)
        assertEquals("/storage/emulated/0/Pictures/t1.jpg", trips.items[0].fullPath)
    }

    @Test
    fun testVirtualAlbumConflictRenamingAndSubsequentExport() {
        val existing = hashSetOf("travel")

        val firstImport = FavoritesAndVirtualAlbumsBackupManager.resolveUniqueVirtualAlbumName("Travel", existing)
        assertEquals("Travel(1)", firstImport)
        existing.add(firstImport.lowercase())

        val secondImport = FavoritesAndVirtualAlbumsBackupManager.resolveUniqueVirtualAlbumName("Travel", existing)
        assertEquals("Travel(2)", secondImport)
        existing.add(secondImport.lowercase())

        val thirdImport = FavoritesAndVirtualAlbumsBackupManager.resolveUniqueVirtualAlbumName("travel", existing)
        assertEquals("travel(3)", thirdImport)

        val albums = listOf(
            VirtualAlbum(id = 1L, name = "Travel", createdTs = 1L) to emptyList<VirtualAlbumItem>(),
            VirtualAlbum(id = 2L, name = firstImport, createdTs = 2L) to emptyList(),
            VirtualAlbum(id = 3L, name = secondImport, createdTs = 3L) to emptyList()
        )
        val out = ByteArrayOutputStream()
        assertTrue(FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(out, emptyList(), albums))

        val entries = readZipEntries(out.toByteArray())
        assertEquals(3, entries.size)
        assertTrue(entries.containsKey("Virtual albums/Travel.json"))
        assertTrue(entries.containsKey("Virtual albums/Travel(1).json"))
        assertTrue(entries.containsKey("Virtual albums/Travel(2).json"))
    }

    @Test
    fun testSanitizedFilenameCollisionInZipIsDeduplicated() {
        val album1 = VirtualAlbum(id = 1L, name = "A/B", createdTs = 1L)
        val album2 = VirtualAlbum(id = 2L, name = "A:B", createdTs = 2L)

        val out = ByteArrayOutputStream()
        assertTrue(
            FavoritesAndVirtualAlbumsBackupManager.writeExportPackage(
                out,
                emptyList(),
                listOf(album1 to emptyList(), album2 to emptyList())
            )
        )

        val entries = readZipEntries(out.toByteArray())
        assertEquals(2, entries.size)
        assertTrue(entries.containsKey("Virtual albums/A_B.json"))
        assertTrue(entries.containsKey("Virtual albums/A_B(1).json"))
    }

    @Test
    fun testLegacyPlainTextFavoritesStillParse() {
        val legacyTxt = """
            /storage/emulated/0/DCIM/Camera/1.jpg
            /storage/emulated/0/DCIM/Camera/2.jpg
        """.trimIndent()

        val parsed = FavoritesAndVirtualAlbumsBackupManager.parseImportPackage(legacyTxt.toByteArray(Charsets.UTF_8))
        assertEquals(2, parsed.favorites.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/1.jpg", parsed.favorites[0].fullPath)
        assertEquals("/storage/emulated/0/DCIM/Camera/2.jpg", parsed.favorites[1].fullPath)
        assertTrue(parsed.virtualAlbums.isEmpty())
    }

    @Test
    fun testZipRejectsTraversalEntriesAndAcceptsVirtualAlbumSavesFolder() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("../Favorites/favorites.json"))
            zos.write("""{"version":1,"favorites":[{"fullPath":"/bad.jpg"}]}""".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("Virtual Album Saves/LegacyName.json"))
            zos.write("""{"version":1,"name":"LegacyName","items":[]}""".toByteArray())
            zos.closeEntry()
        }

        val parsed = FavoritesAndVirtualAlbumsBackupManager.parseImportPackage(out.toByteArray())
        assertTrue(parsed.favorites.isEmpty())
        assertEquals(1, parsed.virtualAlbums.size)
        assertEquals("LegacyName", parsed.virtualAlbums[0].name)
    }

    @Test
    fun testCorruptedBytesDoNotCrashParser() {
        val corruptedZip = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01)
        val parsed = FavoritesAndVirtualAlbumsBackupManager.parseImportPackage(corruptedZip)
        assertTrue(parsed.favorites.isEmpty())
        assertTrue(parsed.virtualAlbums.isEmpty())
    }
}
