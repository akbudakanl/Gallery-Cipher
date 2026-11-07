package org.fossify.gallery.extensions

import android.content.Context
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.FileDirItem
import org.fossify.gallery.R
import org.fossify.gallery.databases.GalleryDatabase
import org.fossify.gallery.helpers.*
import org.fossify.gallery.interfaces.VirtualAlbumsDao
import org.fossify.gallery.models.Directory
import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem
import java.io.File

/*
 * Virtual albums: user created albums that only hold references (paths) to files which live in their
 * real folders. Adding a file to an album never copies or moves it, so albums do not use extra disk space.
 */

fun String.isVirtualAlbumPath() = startsWith(VIRTUAL_ALBUM_PREFIX) && removePrefix(VIRTUAL_ALBUM_PREFIX).toLongOrNull() != null

fun String.getVirtualAlbumId(): Long? = if (startsWith(VIRTUAL_ALBUM_PREFIX)) removePrefix(VIRTUAL_ALBUM_PREFIX).toLongOrNull() else null

fun getVirtualAlbumPath(albumId: Long) = "$VIRTUAL_ALBUM_PREFIX$albumId"

fun Directory.isVirtualAlbum() = path.isVirtualAlbumPath()

val Context.virtualAlbumsDB: VirtualAlbumsDao
    get() = GalleryDatabase.getInstance(applicationContext).VirtualAlbumsDao()

/**
 * Returns true only if hidden items are currently allowed to be viewed AND (if password protection is enabled)
 * the cryptographic vault has been unlocked in memory.
 */
fun Context.canAccessHiddenVirtualAlbums(): Boolean {
    return config.shouldShowHidden && (!config.isHiddenPasswordProtectionOn || CryptoManager.isVaultUnlocked)
}

fun Context.isHiddenVirtualAlbumPath(path: String): Boolean {
    val albumId = path.getVirtualAlbumId() ?: return false
    return try {
        virtualAlbumsDB.getAlbumById(albumId)?.isHidden == true
    } catch (_: Exception) {
        false
    }
}

fun Context.isPathHiddenOrEncrypted(path: String): Boolean {
    if (path.isEmpty()) return false
    if (path.isVirtualAlbumPath()) {
        return isHiddenVirtualAlbumPath(path)
    }
    if (CryptoManager.isEncryptedFile(path)) {
        return true
    }
    if (path.contains("/.")) {
        return true
    }
    val parent = path.getParentPath()
    if (parent.isNotEmpty() && CryptoManager.isEncryptedFolderPath(this, parent)) {
        return true
    }
    return false
}

fun Context.getResolvedVirtualAlbumName(album: VirtualAlbum): String? {
    if (!album.isHidden) {
        return album.name
    }
    if (!canAccessHiddenVirtualAlbums()) {
        return null
    }
    return CryptoManager.decryptStringFromVault(album.name)
}

/** Existing, not deleted files of the given virtual album path, newest additions first. Must run on a background thread. */
fun Context.getVirtualAlbumPaths(albumPath: String): ArrayList<String> {
    val albumId = albumPath.getVirtualAlbumId() ?: return ArrayList()
    return try {
        val album = virtualAlbumsDB.getAlbumById(albumId) ?: return ArrayList()
        if (album.isHidden && !canAccessHiddenVirtualAlbums()) {
            return ArrayList()
        }
        val items = virtualAlbumsDB.getItems(albumId)
        if (items.isEmpty()) return ArrayList()

        val deletedPaths = virtualAlbumsDB.getDeletedPathsForAlbum(albumId).toHashSet()
        val canAccessHidden = canAccessHiddenVirtualAlbums()
        val validPaths = ArrayList<String>()

        for (item in items) {
            val path = item.fullPath
            if (deletedPaths.contains(path) || path.startsWith(recycleBinPath)) continue
            val isHiddenOrEnc = CryptoManager.isEncryptedFile(path) ||
                CryptoManager.isEncryptedFolderPath(this, path.getParentPath()) ||
                path.contains("/.")
            if (isHiddenOrEnc && !canAccessHidden) continue
            if (File(path).exists()) {
                validPaths.add(path)
            }
        }
        validPaths
    } catch (e: Exception) {
        ArrayList()
    }
}

/**
 * Resolves the `Medium` list for a virtual album without requiring encrypted `.fenc` or hidden files
 * to be stored in the `media` SQLite table.
 */
fun Context.getVirtualAlbumMedia(albumId: Long): ArrayList<Medium> {
    return try {
        val album = virtualAlbumsDB.getAlbumById(albumId) ?: return ArrayList()
        val canAccessHidden = canAccessHiddenVirtualAlbums()
        if (album.isHidden && !canAccessHidden) {
            return ArrayList()
        }

        val items = virtualAlbumsDB.getItems(albumId)
        if (items.isEmpty()) return ArrayList()

        val cachedMediaByPath = virtualAlbumsDB.getValidMedia(albumId).associateBy { it.path }
        val deletedPaths = virtualAlbumsDB.getDeletedPathsForAlbum(albumId).toHashSet()
        val favoritePaths = try {
            favoritesDB.getValidFavoritePaths().toHashSet()
        } catch (_: Exception) {
            emptySet()
        }

        val result = ArrayList<Medium>()
        for (item in items) {
            val path = item.fullPath
            if (deletedPaths.contains(path) || path.startsWith(recycleBinPath)) continue

            val file = File(path)
            if (CryptoManager.isEncryptedFile(path)) {
                if (!canAccessHidden || !CryptoManager.isVaultUnlocked || !file.exists()) continue
                val meta = CryptoManager.readMetadata(file) ?: continue
                result.add(
                    Medium(
                        id = null,
                        name = meta.originalName,
                        path = path,
                        parentPath = file.parent ?: item.parentPath,
                        modified = meta.lastModified,
                        taken = meta.dateTaken,
                        size = meta.originalSize,
                        type = meta.mediaType,
                        videoDuration = meta.videoDuration,
                        isFavorite = favoritePaths.contains(path),
                        deletedTS = 0L,
                        mediaStoreId = 0L
                    )
                )
                continue
            }

            val isHiddenItem = path.contains("/.") || CryptoManager.isEncryptedFolderPath(this, path.getParentPath())
            if (isHiddenItem && !canAccessHidden) continue

            val cached = cachedMediaByPath[path]
            if (cached != null) {
                result.add(cached)
            } else if (file.exists() && file.isFile) {
                val isImage = path.isImageFast()
                val isVideo = if (isImage) false else path.isVideoFast()
                val isGif = if (isImage || isVideo) false else path.isGif()
                val isRaw = if (isImage || isVideo || isGif) false else path.isRawFast()
                val isSvg = if (isImage || isVideo || isGif || isRaw) false else path.isSvg()
                if (!isImage && !isVideo && !isGif && !isRaw && !isSvg) continue

                val type = when {
                    isGif -> TYPE_GIFS
                    isRaw -> TYPE_RAWS
                    isVideo -> TYPE_VIDEOS
                    isSvg -> TYPE_SVGS
                    else -> TYPE_IMAGES
                }
                val lastMod = file.lastModified()
                val duration = if (isVideo) (getDuration(path) ?: 0) else 0
                result.add(
                    Medium(
                        id = null,
                        name = file.name,
                        path = path,
                        parentPath = file.parent ?: item.parentPath,
                        modified = lastMod,
                        taken = lastMod,
                        size = file.length(),
                        type = type,
                        videoDuration = duration,
                        isFavorite = favoritePaths.contains(path),
                        deletedTS = 0L,
                        mediaStoreId = 0L
                    )
                )
            }
        }
        result
    } catch (e: Exception) {
        ArrayList()
    }
}

fun Context.getVirtualAlbumName(albumPath: String): String? {
    val albumId = albumPath.getVirtualAlbumId() ?: return null
    return try {
        val album = virtualAlbumsDB.getAlbumById(albumId) ?: return null
        getResolvedVirtualAlbumName(album)
    } catch (e: Exception) {
        null
    }
}

fun Context.getVirtualAlbums(): List<VirtualAlbum> {
    return try {
        virtualAlbumsDB.getAllAlbums()
    } catch (e: Exception) {
        emptyList()
    }
}

fun Context.virtualAlbumNameExists(name: String, ignoreAlbumId: Long? = null): Boolean {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return false
    return try {
        val allAlbums = virtualAlbumsDB.getAllAlbums()
        allAlbums.any { album ->
            if (album.id == ignoreAlbumId) return@any false
            val resolved = if (album.isHidden) {
                CryptoManager.decryptStringFromVault(album.name) ?: album.name
            } else {
                album.name
            }
            resolved.equals(trimmed, ignoreCase = true)
        }
    } catch (e: Exception) {
        false
    }
}

/** @return id of the new album, or -1 if the name is blank or already used */
fun Context.createVirtualAlbum(name: String, isHidden: Boolean = false): Long {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || virtualAlbumNameExists(trimmed)) {
        return -1L
    }

    val storedName = if (isHidden) {
        CryptoManager.encryptStringForVault(this, trimmed)
    } else {
        trimmed
    }

    return try {
        virtualAlbumsDB.insertAlbum(
            VirtualAlbum(
                id = null,
                name = storedName,
                createdTs = System.currentTimeMillis(),
                thumbnail = "",
                isHidden = isHidden
            )
        )
    } catch (e: Exception) {
        -1L
    }
}

fun Context.renameVirtualAlbum(albumId: Long, newName: String): Boolean {
    val trimmed = newName.trim()
    if (trimmed.isEmpty() || virtualAlbumNameExists(trimmed, albumId)) {
        return false
    }

    return try {
        val album = virtualAlbumsDB.getAlbumById(albumId) ?: return false
        val storedName = if (album.isHidden) {
            CryptoManager.encryptStringForVault(this, trimmed)
        } else {
            trimmed
        }
        virtualAlbumsDB.renameAlbum(albumId, storedName)
        true
    } catch (e: Exception) {
        false
    }
}

/** Deletes only the album and its references, the files stay untouched. */
fun Context.deleteVirtualAlbum(albumId: Long) {
    try {
        virtualAlbumsDB.deleteAlbumById(albumId)
        directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
    } catch (ignored: Exception) {
    }
}

/**
 * Adds references to the given media to the album. Nothing is copied on disk.
 * Hidden or encrypted items are NEVER inserted into `mediaDB`, and if any added item is hidden/encrypted
 * (or `forceMarkAlbumHidden` is true), the virtual album itself is marked hidden with an encrypted name.
 * @return how many items were not already in the album
 */
fun Context.addMediaToVirtualAlbum(albumId: Long, media: Collection<Medium>, forceMarkAlbumHidden: Boolean = false): Int {
    val validMedia = media.filter { !it.path.startsWith(recycleBinPath) }.distinctBy { it.path }
    if (validMedia.isEmpty()) {
        return 0
    }

    return try {
        val existingPaths = virtualAlbumsDB.getItems(albumId).map { it.fullPath.lowercase() }.toHashSet()
        val alreadyInAlbum = validMedia.count { existingPaths.contains(it.path.lowercase()) }
        var hasHiddenItem = forceMarkAlbumHidden

        validMedia.forEach { medium ->
            val itemHidden = isPathHiddenOrEncrypted(medium.path)
            if (itemHidden) {
                hasHiddenItem = true
                // Ensure no plaintext metadata of encrypted/hidden files exists in mediaDB
                if (CryptoManager.isEncryptedFile(medium.path)) {
                    mediaDB.deleteMediumPath(medium.path)
                }
            } else {
                // Only cache non-hidden, non-encrypted public files in mediaDB
                mediaDB.insert(medium.copy(id = null, deletedTS = 0L))
            }
        }

        if (hasHiddenItem) {
            val album = virtualAlbumsDB.getAlbumById(albumId)
            if (album != null && (!album.isHidden || !CryptoManager.isEncryptedVaultString(album.name))) {
                val plainName = CryptoManager.decryptStringFromVault(album.name) ?: album.name
                val encName = CryptoManager.encryptStringForVault(this, plainName)
                virtualAlbumsDB.updateAlbumVisibilityAndName(albumId, true, encName)
                directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
            }
        }

        val now = System.currentTimeMillis()
        val items = validMedia.map {
            // Always store the on-disk filename (e.g. .fv_xxx.fenc), never decrypted originalName
            VirtualAlbumItem(null, albumId, it.path, it.path.getFilenameFromPath(), it.path.getParentPath(), now)
        }
        virtualAlbumsDB.insertItems(items)
        validMedia.size - alreadyInAlbum
    } catch (e: Exception) {
        0
    }
}

/**
 * Adds references to the given `FileDirItem`s to the virtual album without copying anything on disk
 * and without writing hidden/encrypted item metadata to `mediaDB`.
 * @return how many items were not already in the album
 */
fun Context.addFileDirItemsToVirtualAlbum(
    albumId: Long,
    fileDirItems: Collection<FileDirItem>,
    forceMarkAlbumHidden: Boolean = false
): Int {
    val validItems = fileDirItems.filter { !it.isDirectory && !it.path.startsWith(recycleBinPath) }.distinctBy { it.path }
    if (validItems.isEmpty()) {
        return 0
    }

    return try {
        val existingPaths = virtualAlbumsDB.getItems(albumId).map { it.fullPath.lowercase() }.toHashSet()
        val alreadyInAlbum = validItems.count { existingPaths.contains(it.path.lowercase()) }
        var hasHiddenItem = forceMarkAlbumHidden

        validItems.forEach { item ->
            val path = item.path
            val itemHidden = isPathHiddenOrEncrypted(path)
            if (itemHidden) {
                hasHiddenItem = true
                if (CryptoManager.isEncryptedFile(path)) {
                    mediaDB.deleteMediumPath(path)
                }
            }
        }

        if (hasHiddenItem) {
            val album = virtualAlbumsDB.getAlbumById(albumId)
            if (album != null && (!album.isHidden || !CryptoManager.isEncryptedVaultString(album.name))) {
                val plainName = CryptoManager.decryptStringFromVault(album.name) ?: album.name
                val encName = CryptoManager.encryptStringForVault(this, plainName)
                virtualAlbumsDB.updateAlbumVisibilityAndName(albumId, true, encName)
                directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
            }
        }

        val now = System.currentTimeMillis()
        val items = validItems.map {
            VirtualAlbumItem(null, albumId, it.path, it.path.getFilenameFromPath(), it.path.getParentPath(), now)
        }
        virtualAlbumsDB.insertItems(items)
        validItems.size - alreadyInAlbum
    } catch (e: Exception) {
        0
    }
}

fun Context.removePathsFromVirtualAlbum(albumId: Long, paths: Collection<String>) {
    try {
        paths.forEach { virtualAlbumsDB.removeItemFromAlbum(albumId, it) }
    } catch (ignored: Exception) {
    }
}

fun Context.updateVirtualAlbumItemPath(oldPath: String, newPath: String) {
    try {
        virtualAlbumsDB.updateItemPath(oldPath, newPath, newPath.getFilenameFromPath(), newPath.getParentPath())
    } catch (ignored: Exception) {
    }
}

fun Context.deleteVirtualAlbumItemFromAllAlbums(path: String) {
    try {
        virtualAlbumsDB.removeItemFromAllAlbums(path)
    } catch (ignored: Exception) {
    }
}

/**
 * Builds the main screen entries of all virtual albums, so they can be shown immediately.
 * Hidden virtual albums are strictly omitted unless `canAccessHiddenVirtualAlbums()` is true.
 * Empty albums are included too, otherwise a freshly created album could not be found. Must run on a background thread.
 */
fun Context.getVirtualAlbumDirectories(): ArrayList<Directory> {
    val directories = ArrayList<Directory>()
    val albums = getVirtualAlbums()
    if (albums.isEmpty()) {
        return directories
    }

    val albumCovers = config.parseAlbumCovers()
    val includedFolders = config.includedFolders
    val canAccessHidden = canAccessHiddenVirtualAlbums()
    val getProperFileSize = config.directorySorting and SORT_BY_SIZE != 0
    val hiddenString = getString(R.string.hidden)
    val mediaFetcher = MediaFetcher(this)

    for (album in albums) {
        val albumId = album.id ?: continue
        if (config.temporarilyShowHidden) {
            if (!album.isHidden || !canAccessHidden) {
                continue
            }
        } else if (album.isHidden) {
            continue
        }
        val resolvedName = getResolvedVirtualAlbumName(album) ?: continue
        val path = getVirtualAlbumPath(albumId)
        val media = getVirtualAlbumMedia(albumId)

        val directory = if (media.isEmpty()) {
            Directory(null, path, "", resolvedName, 0, album.createdTs, album.createdTs, 0L, LOCATION_INTERNAL, 0, "")
        } else {
            mediaFetcher.sortMedia(media, config.getFolderSorting(path))
            createDirectoryFromMedia(
                path = path,
                curMedia = media,
                albumCovers = albumCovers,
                hiddenString = hiddenString,
                includedFolders = includedFolders,
                getProperFileSize = getProperFileSize,
                noMediaFolders = ArrayList()
            )
        }

        directory.name = resolvedName
        directories.add(directory)
    }

    return directories
}
