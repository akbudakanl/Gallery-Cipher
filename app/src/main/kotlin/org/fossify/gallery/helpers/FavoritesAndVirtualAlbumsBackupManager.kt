package org.fossify.gallery.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.recycleBinPath
import org.fossify.gallery.extensions.favoritesDB
import org.fossify.gallery.extensions.virtualAlbumsDB
import org.fossify.gallery.models.Favorite
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class FavoritesImportMode {
    MERGE,
    DELETE_EXISTING
}

data class ImportedVirtualAlbum(
    val name: String,
    val createdTs: Long,
    val parentPhysicalPath: String,
    val items: List<VirtualAlbumItemBackup>
)

data class ParsedImportPackage(
    val favorites: List<ReferenceBackupManager.FavoriteBackup>,
    val virtualAlbums: List<ImportedVirtualAlbum>
)

data class ImportResult(
    val importedFavoritesCount: Int,
    val processedFavoritesCount: Int,
    val importedVirtualAlbumsCount: Int,
    val importedVirtualAlbumItemsCount: Int
)

private const val ZIP_HEADER_MIN_BYTES = 4
private const val ZIP_MAGIC_BYTE_0 = 0x50
private const val ZIP_MAGIC_BYTE_1 = 0x4B
private const val ZIP_MAGIC_BYTE_2_LOCAL = 0x03
private const val ZIP_MAGIC_BYTE_2_EMPTY = 0x05
private const val ZIP_MAGIC_BYTE_2_SPANNED = 0x07
private const val BYTE_MASK = 0xFF
private const val JSON_EXTENSION = ".json"

/**
 * Exports and imports Favorites and Virtual Albums as a unified `.zip` package containing
 * `Favorites/favorites.json` and `Virtual albums/<album>.json` depending on which data is present.
 *
 * The export output is identical whether or not the user granted all-files access. On import,
 * internal backup files under `Fossify Gallery Cipher` are written only when shared-storage
 * access is granted; otherwise imported entries stay within the application's database.
 */
object FavoritesAndVirtualAlbumsBackupManager {
    const val EXPORT_FAVORITES_FOLDER_NAME = "Favorites"
    const val EXPORT_FAVORITES_FILE_NAME = "favorites.json"
    const val EXPORT_VIRTUAL_ALBUMS_FOLDER_NAME = "Virtual albums"

    fun canWriteInternalBackups(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasDataToExport(context: Context): Boolean {
        val hasFavorites = try {
            context.favoritesDB.getValidFavoritePaths().isNotEmpty()
        } catch (_: Exception) {
            false
        }
        if (hasFavorites) {
            return true
        }
        return try {
            context.virtualAlbumsDB.getAllAlbums().any { !it.isHidden }
        } catch (_: Exception) {
            false
        }
    }

    fun hasExistingFavorites(context: Context): Boolean {
        return try {
            context.favoritesDB.getAll().isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun exportToStream(context: Context, outputStream: OutputStream?): Boolean {
        if (outputStream == null) {
            return false
        }

        val validFavoritePaths = try {
            context.favoritesDB.getValidFavoritePaths().toHashSet()
        } catch (_: Exception) {
            emptySet()
        }
        val favorites = try {
            context.favoritesDB.getAll().filter { it.fullPath in validFavoritePaths }
        } catch (_: Exception) {
            emptyList()
        }

        val publicAlbums = try {
            context.virtualAlbumsDB.getAllAlbums().filter { !it.isHidden }
        } catch (_: Exception) {
            emptyList()
        }
        val albumPairs = publicAlbums.mapNotNull { album ->
            val albumId = album.id ?: return@mapNotNull null
            val deletedPaths = try {
                context.virtualAlbumsDB.getDeletedPathsForAlbum(albumId).toHashSet()
            } catch (_: Exception) {
                emptySet()
            }
            val items = try {
                context.virtualAlbumsDB.getItems(albumId).filter {
                    !deletedPaths.contains(it.fullPath) && !it.fullPath.startsWith(context.recycleBinPath)
                }
            } catch (_: Exception) {
                emptyList()
            }
            album to items
        }

        return writeExportPackage(outputStream, favorites, albumPairs)
    }

    fun writeExportPackage(
        outputStream: OutputStream,
        favorites: List<Favorite>,
        virtualAlbums: List<Pair<VirtualAlbum, List<VirtualAlbumItem>>>
    ): Boolean {
        val publicAlbums = virtualAlbums.filter { !it.first.isHidden }
        if (favorites.isEmpty() && publicAlbums.isEmpty()) {
            return false
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
            if (favorites.isNotEmpty()) {
                val favoritesBackup = ReferenceBackupManager.buildFavoritesBackup(favorites)
                val favoritesJson = gson.toJson(favoritesBackup)
                zos.putNextEntry(ZipEntry("$EXPORT_FAVORITES_FOLDER_NAME/$EXPORT_FAVORITES_FILE_NAME"))
                zos.write(favoritesJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            if (publicAlbums.isNotEmpty()) {
                val usedEntryNames = HashSet<String>()
                for ((album, items) in publicAlbums) {
                    val baseFileName = VirtualAlbumSyncManager.getPublicAlbumFileName(album.name, album.id)
                    val uniqueFileName = resolveUniqueZipFileName(baseFileName, usedEntryNames)
                    usedEntryNames.add(uniqueFileName.lowercase())

                    val backup = VirtualAlbumSyncManager.buildPublicAlbumBackup(album, items)
                    val json = gson.toJson(backup)
                    zos.putNextEntry(ZipEntry("$EXPORT_VIRTUAL_ALBUMS_FOLDER_NAME/$uniqueFileName"))
                    zos.write(json.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
        }
        return true
    }

    fun parseImportPackage(bytes: ByteArray): ParsedImportPackage {
        if (bytes.isEmpty()) {
            return ParsedImportPackage(emptyList(), emptyList())
        }

        val gson = Gson()
        return if (isZipArchive(bytes)) {
            parseZipPackage(bytes, gson)
        } else {
            parseLegacyOrPlainPackage(bytes.toString(Charsets.UTF_8), gson)
        }
    }

    fun resolveUniqueVirtualAlbumName(
        desiredName: String,
        existingNamesLowercase: Set<String>
    ): String {
        val trimmed = desiredName.trim()
        if (!existingNamesLowercase.contains(trimmed.lowercase())) {
            return trimmed
        }

        var index = 1
        while (true) {
            val candidate = "$trimmed($index)"
            if (!existingNamesLowercase.contains(candidate.lowercase())) {
                return candidate
            }
            index++
        }
    }

    fun resolveUniqueZipFileName(
        fileName: String,
        usedNamesLowercase: Set<String>
    ): String {
        if (!usedNamesLowercase.contains(fileName.lowercase())) {
            return fileName
        }

        val dotIndex = fileName.lastIndexOf('.')
        val stem = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val ext = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var index = 1
        while (true) {
            val candidate = "$stem($index)$ext"
            if (!usedNamesLowercase.contains(candidate.lowercase())) {
                return candidate
            }
            index++
        }
    }

    fun applyImport(
        context: Context,
        pkg: ParsedImportPackage,
        favoritesMode: FavoritesImportMode
    ): ImportResult {
        return FavoritesAndVirtualAlbumsImporter.applyImport(context, pkg, favoritesMode)
    }

    private fun parseZipPackage(bytes: ByteArray, gson: Gson): ParsedImportPackage {
        val favorites = ArrayList<ReferenceBackupManager.FavoriteBackup>()
        val virtualAlbums = ArrayList<ImportedVirtualAlbum>()

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val normalized = entry.name.replace('\\', '/').trimStart('/')
                        if (normalized.split('/').none { it == ".." }) {
                            val content = zis.readBytes().toString(Charsets.UTF_8).trim()
                            if (content.isNotEmpty()) {
                                parseZipEntryContent(normalized, content, gson, favorites, virtualAlbums)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        }

        return ParsedImportPackage(favorites, virtualAlbums)
    }

    private fun parseZipEntryContent(
        normalizedPath: String,
        content: String,
        gson: Gson,
        favoritesOut: MutableList<ReferenceBackupManager.FavoriteBackup>,
        virtualAlbumsOut: MutableList<ImportedVirtualAlbum>
    ) {
        if (!normalizedPath.endsWith(JSON_EXTENSION, ignoreCase = true)) {
            return
        }

        val isFavoritesEntry = normalizedPath.startsWith("$EXPORT_FAVORITES_FOLDER_NAME/", ignoreCase = true)
        val isVirtualAlbumEntry =
            normalizedPath.startsWith("$EXPORT_VIRTUAL_ALBUMS_FOLDER_NAME/", ignoreCase = true) ||
                normalizedPath.startsWith("$CIPHER_VIRTUAL_ALBUMS_DIR_NAME/", ignoreCase = true)

        if (isFavoritesEntry) {
            parseFavoritesJson(content, gson)?.let { favoritesOut.addAll(it) }
        } else if (isVirtualAlbumEntry) {
            val fallbackName = File(normalizedPath).nameWithoutExtension.trim()
            parseVirtualAlbumJson(content, fallbackName, gson)?.let { virtualAlbumsOut.add(it) }
        }
    }

    private fun parseLegacyOrPlainPackage(text: String, gson: Gson): ParsedImportPackage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ParsedImportPackage(emptyList(), emptyList())
        }

        if (trimmed.startsWith("{")) {
            parseFavoritesJson(trimmed, gson)?.let {
                return ParsedImportPackage(it, emptyList())
            }
            parseVirtualAlbumJson(trimmed, "", gson)?.let {
                return ParsedImportPackage(emptyList(), listOf(it))
            }
        }

        val legacyFavorites = trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { ReferenceBackupManager.FavoriteBackup(fullPath = it) }
            .toList()
        return ParsedImportPackage(legacyFavorites, emptyList())
    }

    private fun parseFavoritesJson(
        content: String,
        gson: Gson
    ): List<ReferenceBackupManager.FavoriteBackup>? {
        val backup = try {
            gson.fromJson(content, ReferenceBackupManager.FavoritesBackup::class.java)
        } catch (_: Exception) {
            null
        } ?: return null

        val list = backup.favorites ?: return null
        return list.filter { !it.fullPath.isNullOrBlank() }
    }

    private fun parseVirtualAlbumJson(
        content: String,
        fallbackName: String,
        gson: Gson
    ): ImportedVirtualAlbum? {
        val backup = try {
            gson.fromJson(content, VirtualAlbumBackup::class.java)
        } catch (_: Exception) {
            null
        } ?: return null

        if (backup.isHidden && backup.encryptedPayload != null) {
            return null
        }

        val albumName = backup.name?.trim()?.ifEmpty { null } ?: fallbackName.trim()
        if (albumName.isEmpty()) {
            return null
        }

        val createdTs = backup.createdTs ?: 0L
        val rawItems = backup.items
            ?: backup.mediaPaths?.map { VirtualAlbumItemBackup(fullPath = it, addedTs = createdTs) }
            ?: emptyList()

        val parentPhysicalPath = backup.parentPhysicalPath?.trim()?.ifEmpty { null }
            ?: rawItems.firstOrNull { !it.fullPath.isNullOrBlank() }?.fullPath?.getParentPath()
            ?: ""

        return ImportedVirtualAlbum(
            name = albumName,
            createdTs = createdTs,
            parentPhysicalPath = parentPhysicalPath,
            items = rawItems
        )
    }

    private fun isZipArchive(bytes: ByteArray): Boolean {
        if (bytes.size < ZIP_HEADER_MIN_BYTES) {
            return false
        }
        val b0 = bytes[0].toInt() and BYTE_MASK
        val b1 = bytes[1].toInt() and BYTE_MASK
        val b2 = bytes[2].toInt() and BYTE_MASK
        return b0 == ZIP_MAGIC_BYTE_0 && b1 == ZIP_MAGIC_BYTE_1 &&
            (b2 == ZIP_MAGIC_BYTE_2_LOCAL || b2 == ZIP_MAGIC_BYTE_2_EMPTY || b2 == ZIP_MAGIC_BYTE_2_SPANNED)
    }
}
