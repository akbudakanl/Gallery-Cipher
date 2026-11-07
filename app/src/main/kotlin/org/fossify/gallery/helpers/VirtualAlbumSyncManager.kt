package org.fossify.gallery.helpers

import android.content.Context
import android.os.Environment
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.fossify.commons.extensions.getParentPath
import org.fossify.gallery.extensions.virtualAlbumsDB
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem
import java.io.File

data class VirtualAlbumBackup(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("name") val name: String? = null,
    @SerializedName(value = "createdTs", alternate = ["created_ts"]) val createdTs: Long? = null,
    @SerializedName(value = "isHidden", alternate = ["is_hidden"]) val isHidden: Boolean = false,
    @SerializedName("items") val items: List<VirtualAlbumItemBackup>? = null,
    @SerializedName(value = "mediaPaths", alternate = ["media_paths"]) val mediaPaths: List<String>? = null
)

data class VirtualAlbumItemBackup(
    @SerializedName(value = "fullPath", alternate = ["full_path", "path"]) val fullPath: String? = null,
    @SerializedName(value = "addedTs", alternate = ["added_ts", "addedTS"]) val addedTs: Long? = null
)

object VirtualAlbumSyncManager {
    private const val ALBUMS_DIR_NAME = "Fossify Virtual Albums"

    @VisibleForTesting
    var customStorageDirForTest: File? = null

    @Volatile
    private var lastSyncTs = 0L

    fun getVirtualAlbumsStorageDir(): File {
        customStorageDirForTest?.let { return it }
        val documentsDir = try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        } catch (_: Throwable) {
            null
        } ?: try {
            File(Environment.getExternalStorageDirectory(), "Documents")
        } catch (_: Throwable) {
            File("/storage/emulated/0/Documents")
        }
        return File(documentsDir, ALBUMS_DIR_NAME)
    }

    fun doesStorageDirExist(): Boolean {
        val dir = getVirtualAlbumsStorageDir()
        return dir.exists() && dir.isDirectory
    }

    fun ensureStorageDirExists(): File {
        val dir = getVirtualAlbumsStorageDir()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAlbumFileName(album: VirtualAlbum): String {
        val sanitized = album.name.replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_").trim().trimStart('.')
        val baseName = sanitized.ifEmpty { "album_${album.id ?: album.createdTs}" }
        return if (album.isHidden) {
            "hidden_album_${album.id ?: album.createdTs}.json"
        } else {
            "$baseName.json"
        }
    }

    fun getAlbumFile(album: VirtualAlbum): File {
        return File(getVirtualAlbumsStorageDir(), getAlbumFileName(album))
    }

    fun saveAlbumToFile(context: Context, albumId: Long) {
        try {
            val album = context.virtualAlbumsDB.getAlbumById(albumId) ?: return
            val items = context.virtualAlbumsDB.getItems(albumId)

            // Include only items whose physical files actually exist on disk
            // This guarantees missing files never corrupt or linger in backups
            val validItems = items.filter { File(it.fullPath).exists() }

            val backupItems = validItems.map {
                VirtualAlbumItemBackup(
                    fullPath = it.fullPath,
                    addedTs = it.addedTS
                )
            }

            val backup = VirtualAlbumBackup(
                version = 1,
                name = album.name,
                createdTs = album.createdTs,
                isHidden = album.isHidden,
                items = backupItems
            )

            val targetDir = ensureStorageDirExists()
            val targetFile = getAlbumFile(album)
            val tempFile = File(targetDir, "${targetFile.name}.${System.currentTimeMillis()}.tmp")

            val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
            tempFile.writeText(json, Charsets.UTF_8)

            if (tempFile.exists()) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
            }
        } catch (_: Exception) {
            // Failure to write backup should not crash the app
        }
    }

    fun deleteAlbumFile(album: VirtualAlbum) {
        try {
            val file = getAlbumFile(album)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    fun onAlbumRenamed(context: Context, albumId: Long, oldAlbum: VirtualAlbum) {
        try {
            val oldFile = getAlbumFile(oldAlbum)
            if (oldFile.exists()) {
                oldFile.delete()
            }
            saveAlbumToFile(context, albumId)
        } catch (_: Exception) {
        }
    }

    fun pruneMissingItems(context: Context, albumId: Long, missingPaths: Collection<String>) {
        if (missingPaths.isEmpty()) return
        try {
            val dao = context.virtualAlbumsDB
            missingPaths.forEach { dao.removeItemFromAlbum(albumId, it) }
            saveAlbumToFile(context, albumId)
        } catch (_: Exception) {
        }
    }

    fun syncVirtualAlbums(context: Context, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncTs < 3000L) {
            return
        }
        lastSyncTs = now

        val dao = context.virtualAlbumsDB
        val dbAlbums = try {
            dao.getAllAlbums()
        } catch (_: Exception) {
            emptyList()
        }

        val dir = getVirtualAlbumsStorageDir()
        if (!dir.exists() || !dir.isDirectory) {
            // If the user already had virtual albums in their DB before this update,
            // automatically export them to Documents/Fossify Virtual Albums so they are preserved
            if (dbAlbums.isNotEmpty()) {
                dbAlbums.forEach { album ->
                    album.id?.let { saveAlbumToFile(context, it) }
                }
            }
            return
        }

        val jsonFiles = dir.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) } ?: emptyArray()
        val gson = Gson()

        for (file in jsonFiles) {
            try {
                val content = file.readText(Charsets.UTF_8).trim()
                if (content.isEmpty()) continue

                val backup = try {
                    gson.fromJson(content, VirtualAlbumBackup::class.java)
                } catch (_: Exception) {
                    // Corrupted JSON in one file must not affect other albums
                    null
                } ?: continue

                val albumName = backup.name?.trim()?.ifEmpty { null } ?: file.nameWithoutExtension.trim()
                if (albumName.isEmpty()) continue

                val isHidden = backup.isHidden
                val createdTs = backup.createdTs ?: file.lastModified()

                var album = dao.getAlbumByName(albumName)
                if (album == null) {
                    val newId = dao.insertAlbum(
                        VirtualAlbum(
                            id = null,
                            name = albumName,
                            createdTs = createdTs,
                            thumbnail = "",
                            isHidden = isHidden
                        )
                    )
                    if (newId > 0) {
                        album = dao.getAlbumById(newId)
                    }
                }

                if (album?.id == null) continue
                val albumId = album.id!!

                val existingItems = dao.getItems(albumId)
                val existingPaths = existingItems.map { it.fullPath.lowercase() }.toHashSet()

                val rawItems = backup.items?.mapNotNull { it.fullPath?.let { path -> path to (it.addedTs ?: createdTs) } }
                    ?: backup.mediaPaths?.map { it to createdTs }
                    ?: emptyList()

                val itemsToInsert = ArrayList<VirtualAlbumItem>()
                var hadMissingFiles = false

                for ((path, addedTs) in rawItems) {
                    try {
                        if (path.isBlank()) continue
                        val targetFile = File(path)
                        // If file was deleted from its physical folder (e.g. Resimler),
                        // omit it safely without corrupting the album
                        if (!targetFile.exists()) {
                            hadMissingFiles = true
                            continue
                        }

                        if (!existingPaths.contains(path.lowercase())) {
                            itemsToInsert.add(
                                VirtualAlbumItem(
                                    id = null,
                                    albumId = albumId,
                                    fullPath = path,
                                    filename = targetFile.name,
                                    parentPath = targetFile.parent ?: path.getParentPath(),
                                    addedTS = if (addedTs > 0) addedTs else System.currentTimeMillis()
                                )
                            )
                            existingPaths.add(path.lowercase())
                        }
                    } catch (_: Exception) {
                        hadMissingFiles = true
                    }
                }

                if (itemsToInsert.isNotEmpty()) {
                    dao.insertItems(itemsToInsert)
                }

                // Check existing items in DB: remove any that physically no longer exist
                val missingDbPaths = existingItems.filter { !File(it.fullPath).exists() }.map { it.fullPath }
                if (missingDbPaths.isNotEmpty()) {
                    missingDbPaths.forEach { dao.removeItemFromAlbum(albumId, it) }
                    hadMissingFiles = true
                }

                // If any missing files were pruned or new items added, update the file
                if (hadMissingFiles || itemsToInsert.isNotEmpty()) {
                    saveAlbumToFile(context, albumId)
                }
            } catch (_: Exception) {
                // Ignore errors for individual files to prevent cascading issues
            }
        }

        // Also ensure any existing albums in DB that don't have a JSON file yet get exported
        val updatedDbAlbums = try {
            dao.getAllAlbums()
        } catch (_: Exception) {
            emptyList()
        }
        for (album in updatedDbAlbums) {
            val albumId = album.id ?: continue
            val albumFile = getAlbumFile(album)
            if (!albumFile.exists()) {
                saveAlbumToFile(context, albumId)
            }
        }
    }
}
