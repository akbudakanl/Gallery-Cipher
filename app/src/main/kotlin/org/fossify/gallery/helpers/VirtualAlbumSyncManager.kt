package org.fossify.gallery.helpers

import android.content.Context
import android.os.Environment
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.helpers.NOMEDIA
import org.fossify.gallery.extensions.virtualAlbumsDB
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem
import java.io.File
import java.security.MessageDigest

data class VirtualAlbumBackup(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("name") val name: String? = null,
    @SerializedName(value = "createdTs", alternate = ["created_ts"]) val createdTs: Long? = null,
    @SerializedName(value = "isHidden", alternate = ["is_hidden"]) val isHidden: Boolean = false,
    @SerializedName("items") val items: List<VirtualAlbumItemBackup>? = null,
    @SerializedName(value = "parentPhysicalPath", alternate = ["parent_physical_path", "sourcePath", "source_path"]) val parentPhysicalPath: String? = null,
    @SerializedName(value = "mediaPaths", alternate = ["media_paths", "paths", "media", "files"]) val mediaPaths: List<String>? = null,
    @SerializedName(value = "encryptedPayload", alternate = ["encrypted_payload", "encrypted_data"]) val encryptedPayload: String? = null
)

data class VirtualAlbumItemBackup(
    @SerializedName(value = "fullPath", alternate = ["full_path", "path"]) val fullPath: String? = null,
    @SerializedName(value = "addedTs", alternate = ["added_ts", "addedTS"]) val addedTs: Long? = null,
    @SerializedName(value = "mediaStoreId", alternate = ["media_store_id"]) val mediaStoreId: Long? = null,
    @SerializedName(value = "fileSize", alternate = ["file_size"]) val fileSize: Long? = null,
    @SerializedName(value = "lastModified", alternate = ["last_modified"]) val lastModified: Long? = null,
    @SerializedName(value = "fingerprint") val fingerprint: String? = null
)

object VirtualAlbumSyncManager {
    private const val ALBUMS_DIR_NAME = "Fossify Virtual Albums"
    private const val VAULT_JSON_SUBFOLDER = "JSON"

    @VisibleForTesting
    var customStorageDirForTest: File? = null

    @VisibleForTesting
    var customVaultDirForTest: File? = null

    @Volatile
    private var lastSyncTs = 0L

    fun getVirtualAlbumsStorageDir(): File {
        customStorageDirForTest?.let { return it }
        val documentsDir = CryptoManager.getDocumentsDirectory()
        val cipherDir = File(documentsDir, CIPHER_ROOT_DIR_NAME)
        return File(cipherDir, CIPHER_VIRTUAL_ALBUMS_DIR_NAME)
    }

    fun getVaultStorageDir(context: Context): File {
        customVaultDirForTest?.let { return it }
        // If the vault is gone, return the would-be path WITHOUT creating anything on disk.
        val vaultRoot = CryptoManager.findVaultRoot(context)
            ?: return File(CryptoManager.getDefaultVaultRoot(), VAULT_JSON_SUBFOLDER)
        val jsonDir = File(vaultRoot, VAULT_JSON_SUBFOLDER)
        if (!jsonDir.exists()) {
            jsonDir.mkdirs()
        }
        val noMedia = File(jsonDir, NOMEDIA)
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }
        return jsonDir
    }

    fun ensurePublicStorageDirExists(): File {
        val dir = getVirtualAlbumsStorageDir()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getPublicAlbumFileName(albumName: String, albumId: Long? = null): String {
        val sanitized = albumName.replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_").trim().trimStart('.')
        val baseName = sanitized.ifEmpty { "album_${albumId ?: System.currentTimeMillis()}" }
        return "$baseName.json"
    }

    fun getHiddenAlbumFileName(albumName: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest("FVA_STEALTH_$albumName".toByteArray(Charsets.UTF_8))
        val hex = hashBytes.take(8).joinToString("") { "%02x".format(it) }
        return "fva_$hex.json"
    }

    fun getAlbumFile(context: Context, album: VirtualAlbum): File {
        return if (album.isHidden) {
            File(getVaultStorageDir(context), getHiddenAlbumFileName(album.name))
        } else {
            File(getVirtualAlbumsStorageDir(), getPublicAlbumFileName(album.name, album.id))
        }
    }

    fun saveAlbumToFile(context: Context, albumId: Long) {
        try {
            val album = context.virtualAlbumsDB.getAlbumById(albumId) ?: return
            val items = context.virtualAlbumsDB.getItems(albumId)

            // Include items whose files exist or whose parent directories exist
            val validItems = items.filter { File(it.fullPath).exists() || File(it.fullPath).parentFile?.exists() != true }
            val backupItems = validItems.map {
                VirtualAlbumItemBackup(
                    fullPath = it.fullPath,
                    addedTs = it.addedTS,
                    mediaStoreId = it.mediaStoreId,
                    fileSize = it.fileSize,
                    lastModified = it.lastModified,
                    fingerprint = it.fingerprint
                )
            }

            val gson = GsonBuilder().setPrettyPrinting().create()

            if (album.isHidden) {
                // Stealth Vault Virtual Album:
                // 1. Remove any old plaintext file in public Documents
                val publicDir = getVirtualAlbumsStorageDir()
                if (publicDir.exists()) {
                    val oldPublicFile = File(publicDir, getPublicAlbumFileName(album.name, album.id))
                    if (oldPublicFile.exists()) {
                        CryptoManager.secureWipeAndDeleteFile(oldPublicFile, context)
                    }
                }

                // 2. Encrypt the inner JSON payload containing album name and all media paths
                val innerBackup = VirtualAlbumBackup(
                    version = 1,
                    name = album.name,
                    createdTs = album.createdTs,
                    isHidden = true,
                    parentPhysicalPath = album.parentPhysicalPath,
                    items = backupItems
                )
                val innerJson = gson.toJson(innerBackup)
                val encryptedPayload = CryptoManager.encryptStringForVault(context, innerJson)

                // 3. Write outer wrapper to vaultRoot/JSON/fva_<hash>.json
                val outerBackup = VirtualAlbumBackup(
                    version = 2,
                    isHidden = true,
                    encryptedPayload = encryptedPayload
                )
                val targetDir = getVaultStorageDir(context)
                val targetFile = File(targetDir, getHiddenAlbumFileName(album.name))
                val tempFile = File(targetDir, "${targetFile.name}.${System.currentTimeMillis()}.tmp")

                tempFile.writeText(gson.toJson(outerBackup), Charsets.UTF_8)
                if (tempFile.exists()) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    tempFile.renameTo(targetFile)
                }
            } else {
                // Normal (Non-hidden) Virtual Album:
                val targetDir = ensurePublicStorageDirExists()
                val targetFile = File(targetDir, getPublicAlbumFileName(album.name, album.id))
                val tempFile = File(targetDir, "${targetFile.name}.${System.currentTimeMillis()}.tmp")

                val backup = VirtualAlbumBackup(
                    version = 1,
                    name = album.name,
                    createdTs = album.createdTs,
                    isHidden = false,
                    parentPhysicalPath = album.parentPhysicalPath,
                    items = backupItems
                )

                tempFile.writeText(gson.toJson(backup), Charsets.UTF_8)
                if (tempFile.exists()) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    tempFile.renameTo(targetFile)
                }
            }
        } catch (_: Exception) {
            // Failure to write backup should not crash the app
        }
    }

    fun deleteAlbumFile(context: Context, album: VirtualAlbum) {
        try {
            val file = getAlbumFile(context, album)
            if (file.exists()) {
                CryptoManager.secureWipeAndDeleteFile(file, context)
            }
            // Check other location just in case
            val otherFile = if (album.isHidden) {
                File(getVirtualAlbumsStorageDir(), getPublicAlbumFileName(album.name, album.id))
            } else {
                File(getVaultStorageDir(context), getHiddenAlbumFileName(album.name))
            }
            if (otherFile.exists()) {
                CryptoManager.secureWipeAndDeleteFile(otherFile, context)
            }
        } catch (_: Exception) {
        }
    }

    fun onAlbumRenamed(context: Context, albumId: Long, oldAlbum: VirtualAlbum) {
        try {
            deleteAlbumFile(context, oldAlbum)
            saveAlbumToFile(context, albumId)
        } catch (_: Exception) {
        }
    }

    /**
     * Handles items whose file could not be found at the cached path.
     *
     * Nothing is deleted here. A missing file may simply have been renamed or moved, and the
     * identity of the item allows finding it again later. Removing the row would destroy the only
     * information needed to relocate it.
     */
    fun pruneMissingItems(context: Context, albumId: Long, missingPaths: Collection<String>) {
        if (missingPaths.isEmpty()) return
        try {
            // Only prune if the file's parent directory actually exists on disk (confirming drive is accessible)
            val actuallyMissing = missingPaths.filter { path ->
                val f = File(path)
                !f.exists() && f.parentFile?.exists() == true
            }
            if (actuallyMissing.isEmpty()) return

            // The item stays in the album and in the backup. It is only marked as unresolved so the
            // UI can tell the user that the file could not be found.
            val dao = context.virtualAlbumsDB
            actuallyMissing.forEach { path ->
                dao.markItemUnresolved(albumId, path)
            }
            saveAlbumToFile(context, albumId)
        } catch (_: Exception) {
        }
    }

    fun syncVirtualAlbums(context: Context, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncTs < 5000L) {
            return
        }
        lastSyncTs = now

        val dao = context.virtualAlbumsDB
        val dbAlbums = try {
            dao.getAllAlbums()
        } catch (_: Exception) {
            emptyList()
        }

        val documentsDir = CryptoManager.getDocumentsDirectory()
        val legacyDir = File(documentsDir, "Fossify Virtual Albums")
        val publicDir = getVirtualAlbumsStorageDir()

        // 0. Automatically migrate legacy saves from Documents/Fossify Virtual Albums
        try {
            if (legacyDir.exists() && legacyDir.isDirectory) {
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                legacyDir.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }?.forEach { f ->
                    val targetFile = File(publicDir, f.name)
                    if (!targetFile.exists()) {
                        f.copyTo(targetFile, overwrite = true)
                    }
                    try { f.delete() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        val gson = Gson()
        val processedAlbumNames = HashSet<String>()

        // 1. Restore public non-hidden albums from both new and legacy folders
        val candidateDirs = listOfNotNull(publicDir, legacyDir).filter { it.exists() && it.isDirectory }
        for (sourceDir in candidateDirs) {
            val jsonFiles = sourceDir.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) } ?: emptyArray()
            for (file in jsonFiles) {
                try {
                    val content = file.readText(Charsets.UTF_8).trim()
                    if (content.isEmpty()) continue

                    val backup = try {
                        gson.fromJson(content, VirtualAlbumBackup::class.java)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    // If a hidden album file was previously left in public folder, migrate to vault JSON
                    if (backup.isHidden && backup.encryptedPayload != null) {
                        try {
                            val vaultJsonDir = getVaultStorageDir(context)
                            val targetVaultFile = File(vaultJsonDir, file.name)
                            if (!targetVaultFile.exists()) {
                                file.copyTo(targetVaultFile, overwrite = true)
                            }
                            try { file.delete() } catch (_: Exception) {}
                        } catch (_: Exception) {}
                        continue
                    }

                    val albumName = backup.name?.trim()?.ifEmpty { null } ?: file.nameWithoutExtension.trim()
                    if (albumName.isEmpty()) continue
                    if (processedAlbumNames.contains(albumName.lowercase())) continue
                    processedAlbumNames.add(albumName.lowercase())

                    val isHidden = backup.isHidden
                    val createdTs = backup.createdTs ?: file.lastModified()

                    val parentPhysicalPath = backup.parentPhysicalPath?.trim()?.ifEmpty { null }
                        ?: backup.items?.firstOrNull()?.fullPath?.getParentPath()
                        ?: backup.mediaPaths?.firstOrNull()?.getParentPath()
                        ?: ""

                    var album = dao.getAlbumByName(albumName)
                    if (album == null) {
                        val newId = dao.insertAlbum(
                            VirtualAlbum(
                                id = null,
                                name = albumName,
                                createdTs = createdTs,
                                thumbnail = "",
                                isHidden = isHidden,
                                parentPhysicalPath = parentPhysicalPath
                            )
                        )
                        if (newId > 0) {
                            album = dao.getAlbumById(newId)
                        }
                    } else if (album.parentPhysicalPath.isEmpty() && parentPhysicalPath.isNotEmpty()) {
                        album.id?.let { dao.updateAlbumParentPhysicalPath(it, parentPhysicalPath) }
                        album.parentPhysicalPath = parentPhysicalPath
                    }

                    if (album?.id == null) continue
                    val albumId = album.id!!

                    val existingItems = dao.getItems(albumId)
                    val existingPaths = existingItems.map { it.fullPath.lowercase() }.toHashSet()

                    val rawItems = backup.items
                        ?: backup.mediaPaths?.map { VirtualAlbumItemBackup(fullPath = it, addedTs = createdTs) }
                        ?: emptyList()

                    val itemsToInsert = ArrayList<VirtualAlbumItem>()
                    var hadMissingFiles = false

                    for (entry in rawItems) {
                        try {
                            val path = entry.fullPath ?: continue
                            if (path.isBlank()) continue
                            val targetFile = File(path)
                            // If file definitely doesn't exist and parent exists, skip it
                            if (!targetFile.exists() && targetFile.parentFile?.exists() == true) {
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
                                        addedTS = if ((entry.addedTs ?: 0L) > 0) entry.addedTs!! else System.currentTimeMillis(),
                                        mediaStoreId = entry.mediaStoreId ?: 0L,
                                        fileSize = entry.fileSize ?: 0L,
                                        lastModified = entry.lastModified ?: 0L,
                                        fingerprint = entry.fingerprint ?: ""
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

                    val missingDbPaths = existingItems.filter {
                        val f = File(it.fullPath)
                        !f.exists() && f.parentFile?.exists() == true
                    }.map { it.fullPath }
                    if (missingDbPaths.isNotEmpty()) {
                        // The item is kept, only marked as unresolved. It may have been renamed or
                        // moved, and the identity is what allows finding it again.
                        missingDbPaths.forEach { dao.markItemUnresolved(albumId, it) }
                        hadMissingFiles = true
                    }

                    if (hadMissingFiles || itemsToInsert.isNotEmpty()) {
                        saveAlbumToFile(context, albumId)
                    }
                } catch (_: Exception) {
                }
            }
        }

        // 2. Restore stealth hidden virtual albums from vaultRoot/JSON/
        val vaultRoot = CryptoManager.findVaultRoot(context)
        if (vaultRoot != null && vaultRoot.exists()) {
            val vaultJsonDir = File(vaultRoot, VAULT_JSON_SUBFOLDER)
            if (vaultJsonDir.exists() && vaultJsonDir.isDirectory) {
                val vaultFiles = vaultJsonDir.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) } ?: emptyArray()
                for (file in vaultFiles) {
                    try {
                        val content = file.readText(Charsets.UTF_8).trim()
                        if (content.isEmpty()) continue

                        val outerBackup = try {
                            gson.fromJson(content, VirtualAlbumBackup::class.java)
                        } catch (_: Exception) {
                            null
                        } ?: continue

                        val innerBackup: VirtualAlbumBackup = if (outerBackup.encryptedPayload != null) {
                            if (!CryptoManager.isVaultUnlocked) {
                                // Vault locked, leave file to be restored upon unlock
                                continue
                            }
                            val decrypted = CryptoManager.decryptStringFromVault(outerBackup.encryptedPayload) ?: continue
                            try {
                                gson.fromJson(decrypted, VirtualAlbumBackup::class.java)
                            } catch (_: Exception) {
                                null
                            } ?: continue
                        } else {
                            outerBackup
                        }

                        val albumName = innerBackup.name?.trim()?.ifEmpty { null } ?: continue
                        val createdTs = innerBackup.createdTs ?: file.lastModified()

                        val parentPhysicalPath = innerBackup.parentPhysicalPath?.trim()?.ifEmpty { null }
                            ?: innerBackup.items?.firstOrNull()?.fullPath?.getParentPath()
                            ?: innerBackup.mediaPaths?.firstOrNull()?.getParentPath()
                            ?: ""

                        var album = dao.getAlbumByName(albumName)
                        if (album == null) {
                            val newId = dao.insertAlbum(
                                VirtualAlbum(
                                    id = null,
                                    name = albumName,
                                    createdTs = createdTs,
                                    thumbnail = "",
                                    isHidden = true,
                                    parentPhysicalPath = parentPhysicalPath
                                )
                            )
                            if (newId > 0) {
                                album = dao.getAlbumById(newId)
                            }
                        } else if (album.parentPhysicalPath.isEmpty() && parentPhysicalPath.isNotEmpty()) {
                            album.id?.let { dao.updateAlbumParentPhysicalPath(it, parentPhysicalPath) }
                            album.parentPhysicalPath = parentPhysicalPath
                        }

                        if (album?.id == null) continue
                        val albumId = album.id!!

                        val existingItems = dao.getItems(albumId)
                        val existingPaths = existingItems.map { it.fullPath.lowercase() }.toHashSet()

                        val rawItems = innerBackup.items
                            ?: innerBackup.mediaPaths?.map { VirtualAlbumItemBackup(fullPath = it, addedTs = createdTs) }
                            ?: emptyList()

                        val itemsToInsert = ArrayList<VirtualAlbumItem>()
                        var hadMissingFiles = false

                        for (entry in rawItems) {
                            try {
                                val path = entry.fullPath ?: continue
                                if (path.isBlank()) continue
                                val targetFile = File(path)
                                if (!targetFile.exists() && targetFile.parentFile?.exists() == true) {
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
                                            addedTS = if ((entry.addedTs ?: 0L) > 0) entry.addedTs!! else System.currentTimeMillis(),
                                            mediaStoreId = entry.mediaStoreId ?: 0L,
                                            fileSize = entry.fileSize ?: 0L,
                                            lastModified = entry.lastModified ?: 0L,
                                            fingerprint = entry.fingerprint ?: ""
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

                        val missingDbPaths = existingItems.filter {
                            val f = File(it.fullPath)
                            !f.exists() && f.parentFile?.exists() == true
                        }.map { it.fullPath }
                        if (missingDbPaths.isNotEmpty()) {
                            // Kept, only marked as unresolved, so a renamed file can be found again.
                            missingDbPaths.forEach { dao.markItemUnresolved(albumId, it) }
                            hadMissingFiles = true
                        }

                        if (hadMissingFiles || itemsToInsert.isNotEmpty()) {
                            saveAlbumToFile(context, albumId)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }

        // 3. Export any existing albums in DB that don't have a file on disk yet
        val updatedDbAlbums = try {
            dao.getAllAlbums()
        } catch (_: Exception) {
            emptyList()
        }
        for (album in updatedDbAlbums) {
            val albumId = album.id ?: continue
            val albumFile = getAlbumFile(context, album)
            if (!albumFile.exists()) {
                if (!album.isHidden || CryptoManager.isVaultUnlocked) {
                    saveAlbumToFile(context, albumId)
                }
            }
        }
    }
}
