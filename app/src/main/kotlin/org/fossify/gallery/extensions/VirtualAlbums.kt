package org.fossify.gallery.extensions

import android.content.Context
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.recycleBinPath
import org.fossify.commons.helpers.SORT_BY_SIZE
import org.fossify.gallery.R
import org.fossify.gallery.databases.GalleryDatabase
import org.fossify.gallery.helpers.LOCATION_INTERNAL
import org.fossify.gallery.helpers.MediaFetcher
import org.fossify.gallery.helpers.VIRTUAL_ALBUM_PREFIX
import org.fossify.gallery.interfaces.VirtualAlbumsDao
import org.fossify.gallery.models.Directory
import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem

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

/** Existing, not deleted files of the given virtual album path, newest additions first. Must run on a background thread. */
fun Context.getVirtualAlbumPaths(albumPath: String): ArrayList<String> {
    val albumId = albumPath.getVirtualAlbumId() ?: return ArrayList()
    return try {
        ArrayList(virtualAlbumsDB.getValidPaths(albumId))
    } catch (e: Exception) {
        ArrayList()
    }
}

fun Context.getVirtualAlbumName(albumPath: String): String? {
    val albumId = albumPath.getVirtualAlbumId() ?: return null
    return try {
        virtualAlbumsDB.getAlbum(albumId)?.name
    } catch (e: Exception) {
        null
    }
}

fun Context.getVirtualAlbums(): List<VirtualAlbum> {
    return try {
        virtualAlbumsDB.getAlbums()
    } catch (e: Exception) {
        emptyList()
    }
}

fun Context.virtualAlbumNameExists(name: String, ignoreAlbumId: Long? = null): Boolean {
    return try {
        val album = virtualAlbumsDB.getAlbumByName(name.trim())
        album != null && album.id != ignoreAlbumId
    } catch (e: Exception) {
        false
    }
}

/** @return id of the new album, or -1 if the name is blank or already used */
fun Context.createVirtualAlbum(name: String): Long {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || virtualAlbumNameExists(trimmed)) {
        return -1L
    }

    return try {
        virtualAlbumsDB.insertAlbum(VirtualAlbum(null, trimmed, System.currentTimeMillis()))
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
        virtualAlbumsDB.renameAlbum(albumId, trimmed)
        true
    } catch (e: Exception) {
        false
    }
}

/** Deletes only the album and its references, the files stay untouched. */
fun Context.deleteVirtualAlbum(albumId: Long) {
    try {
        virtualAlbumsDB.deleteAlbum(albumId)
        directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
    } catch (ignored: Exception) {
    }
}

/**
 * Adds references to the given media to the album. Nothing is copied. Media not yet present in the media cache
 * are inserted into it, so the album can resolve them.
 * @return how many items were not already in the album
 */
fun Context.addMediaToVirtualAlbum(albumId: Long, media: Collection<Medium>): Int {
    val validMedia = media.filter { !it.path.startsWith(recycleBinPath) }.distinctBy { it.path }
    if (validMedia.isEmpty()) {
        return 0
    }

    return try {
        val alreadyInAlbum = validMedia.count { virtualAlbumsDB.isInAlbum(albumId, it.path) > 0 }
        validMedia.forEach {
            if (virtualAlbumsDB.isMediaCached(it.path) == 0) {
                mediaDB.insert(it.copy(id = null, deletedTS = 0L))
            }
        }

        val now = System.currentTimeMillis()
        val items = validMedia.map {
            VirtualAlbumItem(null, albumId, it.path, it.path.getFilenameFromPath(), it.path.getParentPath(), now)
        }
        virtualAlbumsDB.insertItems(items)
        validMedia.size - alreadyInAlbum
    } catch (e: Exception) {
        0
    }
}

fun Context.removePathsFromVirtualAlbum(albumId: Long, paths: Collection<String>) {
    try {
        paths.forEach { virtualAlbumsDB.deleteItem(albumId, it) }
    } catch (ignored: Exception) {
    }
}

fun Context.updateVirtualAlbumItemPath(oldPath: String, newPath: String) {
    try {
        virtualAlbumsDB.updateItemPath(newPath.getFilenameFromPath(), newPath, newPath.getParentPath(), oldPath)
    } catch (ignored: Exception) {
    }
}

fun Context.deleteVirtualAlbumItemFromAllAlbums(path: String) {
    try {
        virtualAlbumsDB.deleteItemFromAllAlbums(path)
    } catch (ignored: Exception) {
    }
}

/**
 * Builds the main screen entries of all virtual albums from the media cache, so they can be shown immediately.
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
    val showHidden = config.shouldShowHidden
    val getProperFileSize = config.directorySorting and SORT_BY_SIZE != 0
    val hiddenString = getString(R.string.hidden)
    val mediaFetcher = MediaFetcher(this)

    for (album in albums) {
        val albumId = album.id ?: continue
        val path = getVirtualAlbumPath(albumId)
        var media = try {
            ArrayList(virtualAlbumsDB.getValidMedia(albumId))
        } catch (e: Exception) {
            ArrayList()
        }

        if (!showHidden) {
            media = media.filter { !it.path.contains("/.") } as ArrayList<Medium>
        }

        val directory = if (media.isEmpty()) {
            Directory(null, path, "", album.name, 0, album.createdTS, album.createdTS, 0L, LOCATION_INTERNAL, 0, "")
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

        directory.name = album.name
        directories.add(directory)
    }

    return directories
}
