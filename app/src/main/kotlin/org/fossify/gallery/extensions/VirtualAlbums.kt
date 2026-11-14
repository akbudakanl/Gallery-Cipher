package org.fossify.gallery.extensions

import android.content.Context
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.FileDirItem
import org.fossify.gallery.R
import org.fossify.gallery.databases.GalleryDatabase
import org.fossify.gallery.helpers.*
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.MediaIdentity
import org.fossify.gallery.helpers.MediaIdentityResolver
import org.fossify.gallery.helpers.ReferenceBackupManager
import org.fossify.gallery.helpers.VirtualAlbumSyncManager
import org.fossify.gallery.interfaces.VirtualAlbumsDao
import org.fossify.gallery.models.Directory
import org.fossify.gallery.models.ITEM_STATE_UNRESOLVED
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
    return CryptoManager.decryptStringFromVault(album.name) ?: album.name
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
        val newlyMissingPaths = ArrayList<String>()
        var repairedAny = false

        val missingItems = items.filter { item ->
            val path = item.fullPath
            !deletedPaths.contains(path) &&
                !path.startsWith(recycleBinPath) &&
                !(CryptoManager.isVaultMediaPath(this, path) && !canAccessHidden) &&
                !File(path).exists()
        }
        val resolvedMissingMap = resolveVirtualAlbumItemsTwoPass(items, missingItems, session = null)

        for (item in items) {
            val path = item.fullPath
            if (deletedPaths.contains(path) || path.startsWith(recycleBinPath)) continue
            // Only vault content is protected inside a virtual album. Files coming from a hidden
            // folder are explicitly referenced by the user, so they stay visible.
            val isVaultContent = CryptoManager.isVaultMediaPath(this, path)
            if (isVaultContent && !canAccessHidden) continue
            if (File(path).exists()) {
                if (item.state == ITEM_STATE_UNRESOLVED) {
                    try {
                        virtualAlbumsDB.markItemResolved(albumId, path)
                        repairedAny = true
                    } catch (_: Exception) {
                    }
                }
                validPaths.add(path)
            } else {
                // The file may have been renamed or moved outside of the app. Its identity allows
                // finding it again at the new location.
                val resolved = resolvedMissingMap[path]
                if (resolved != null) {
                    validPaths.add(resolved)
                    repairedAny = true
                } else if (item.state != ITEM_STATE_UNRESOLVED) {
                    // The item stays in the album and is only marked as unresolved, so it can be
                    // repaired later. It is never removed behind the user's back.
                    newlyMissingPaths.add(path)
                }
            }
        }

        if (newlyMissingPaths.isNotEmpty() || repairedAny) {
            ensureBackgroundThread {
                if (newlyMissingPaths.isNotEmpty()) {
                    VirtualAlbumSyncManager.pruneMissingItems(this, albumId, newlyMissingPaths)
                } else {
                    VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
                }
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
fun Context.getVirtualAlbumMedia(albumId: Long, getVideoDurations: Boolean = false): ArrayList<Medium> {
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
        val newlyMissingPaths = ArrayList<String>()
        var repairedAny = false

        val missingItems = items.filter { item ->
            val path = item.fullPath
            !deletedPaths.contains(path) && !path.startsWith(recycleBinPath) && !File(path).exists()
        }
        val resolvedMissingMap = resolveVirtualAlbumItemsTwoPass(items, missingItems, session = null)

        for (item in items) {
            var path = item.fullPath
            if (deletedPaths.contains(path) || path.startsWith(recycleBinPath)) continue

            var file = File(path)
            if (!file.exists()) {
                // The file may have been renamed or moved outside of the app.
                val resolved = resolvedMissingMap[path]
                if (resolved == null) {
                    if (item.state != ITEM_STATE_UNRESOLVED) {
                        newlyMissingPaths.add(path)
                    }
                    continue
                }
                path = resolved
                file = File(path)
                repairedAny = true
            } else if (item.state == ITEM_STATE_UNRESOLVED) {
                try {
                    virtualAlbumsDB.markItemResolved(albumId, path)
                    repairedAny = true
                } catch (_: Exception) {
                }
            }

            if (CryptoManager.isEncryptedFile(path)) {
                if (!canAccessHidden || !CryptoManager.isVaultUnlocked) continue
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

            // Only vault content is protected inside a virtual album. Files coming from a hidden
            // folder are explicitly referenced by the user, so they stay visible.
            val isVaultContent = CryptoManager.isVaultMediaPath(this, path)
            if (isVaultContent && !canAccessHidden) continue

            val cached = cachedMediaByPath[path]
            if (cached != null) {
                result.add(cached)
            } else if (file.isFile) {
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
                val duration = if (isVideo && getVideoDurations) (getDuration(path) ?: 0) else 0
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

        if (newlyMissingPaths.isNotEmpty() || repairedAny) {
            ensureBackgroundThread {
                if (newlyMissingPaths.isNotEmpty()) {
                    VirtualAlbumSyncManager.pruneMissingItems(this, albumId, newlyMissingPaths)
                } else {
                    VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
                }
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

/**
 * The folder the items of an album mostly came from.
 *
 * This is only a hint, for example to preselect the folder of the copy dialog. It does not
 * restrict the album: an album may hold items from any number of physical folders, exactly like
 * the favorites do.
 */
fun Context.getVirtualAlbumOwnerPhysicalPath(albumId: Long): String {
    val album = virtualAlbumsDB.getAlbumById(albumId) ?: return ""
    if (album.parentPhysicalPath.isNotEmpty()) {
        return album.parentPhysicalPath
    }
    val items = virtualAlbumsDB.getItems(albumId)
    val firstItemParent = items.firstOrNull()?.parentPath ?: ""
    if (firstItemParent.isNotEmpty()) {
        album.parentPhysicalPath = firstItemParent
        ensureBackgroundThread {
            try {
                virtualAlbumsDB.updateAlbumParentPhysicalPath(albumId, firstItemParent)
            } catch (_: Exception) {}
        }
        return firstItemParent
    }
    return ""
}

fun Context.resolveEffectiveSourcePhysicalPath(sourcePath: String): String {
    if (sourcePath.isVirtualAlbumPath()) {
        val albumId = sourcePath.getVirtualAlbumId() ?: return ""
        return getVirtualAlbumOwnerPhysicalPath(albumId)
    }
    return sourcePath
}

/** @return id of the new album, or -1 if the name is blank or already used */
fun Context.createVirtualAlbum(name: String, isHidden: Boolean = false, parentPhysicalPath: String = ""): Long {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || virtualAlbumNameExists(trimmed)) {
        return -1L
    }

    val storedName = if (isHidden) {
        CryptoManager.encryptStringForVault(this, trimmed)
    } else {
        trimmed
    }

    val albumId = try {
        virtualAlbumsDB.insertAlbum(
            VirtualAlbum(
                id = null,
                name = storedName,
                createdTs = System.currentTimeMillis(),
                thumbnail = "",
                isHidden = isHidden,
                parentPhysicalPath = parentPhysicalPath
            )
        )
    } catch (e: Exception) {
        -1L
    }

    if (albumId > 0) {
        VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
    }
    return albumId
}

fun Context.renameVirtualAlbum(albumId: Long, newName: String): Boolean {
    val trimmed = newName.trim()
    if (trimmed.isEmpty() || virtualAlbumNameExists(trimmed, albumId)) {
        return false
    }

    return try {
        val oldAlbum = virtualAlbumsDB.getAlbumById(albumId) ?: return false
        val storedName = if (oldAlbum.isHidden) {
            CryptoManager.encryptStringForVault(this, trimmed)
        } else {
            trimmed
        }
        virtualAlbumsDB.renameAlbum(albumId, storedName)
        VirtualAlbumSyncManager.onAlbumRenamed(this, albumId, oldAlbum)
        true
    } catch (e: Exception) {
        false
    }
}

/** Deletes only the album and its references, the files stay untouched. */
fun Context.deleteVirtualAlbum(albumId: Long) {
    try {
        val album = virtualAlbumsDB.getAlbumById(albumId)
        if (album != null) {
            VirtualAlbumSyncManager.deleteAlbumFile(this, album)
        }
        virtualAlbumsDB.deleteAlbumById(albumId)
        directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
    } catch (ignored: Exception) {
    }
}

/**
 * Adds references to the given media to the album. Nothing is copied on disk.
 *
 * An album may hold items from any number of physical folders, exactly like the favorites. The
 * folder the items came from is only remembered as a hint for the copy dialog.
 *
 * Vault content is NEVER inserted into `mediaDB`. If any added item is vault content (or
 * `forceMarkAlbumHidden` is true), the album itself is marked hidden with an encrypted name.
 * Files coming from a hidden folder are not vault content: they are explicitly referenced by the
 * user and stay visible in the album.
 *
 * @return how many items were not already in the album
 */
fun Context.addMediaToVirtualAlbum(albumId: Long, media: Collection<Medium>, forceMarkAlbumHidden: Boolean = false): Int {
    val validMedia = media.filter { !it.path.startsWith(recycleBinPath) }.distinctBy { it.path }
    if (validMedia.isEmpty()) {
        return 0
    }

    return try {
        val album = virtualAlbumsDB.getAlbumById(albumId)

        // Remember where the items came from, but never use it to reject items.
        if (album != null && album.parentPhysicalPath.isEmpty()) {
            val firstParent = validMedia.firstOrNull()?.parentPath ?: ""
            if (firstParent.isNotEmpty()) {
                album.parentPhysicalPath = firstParent
                virtualAlbumsDB.updateAlbumParentPhysicalPath(albumId, firstParent)
            }
        }

        val allowedMedia = validMedia
        val existingPaths = virtualAlbumsDB.getItems(albumId).map { it.fullPath.lowercase() }.toHashSet()
        val alreadyInAlbum = allowedMedia.count { existingPaths.contains(it.path.lowercase()) }
        var hasVaultItem = forceMarkAlbumHidden

        allowedMedia.forEach { medium ->
            if (CryptoManager.isVaultMediaPath(this, medium.path)) {
                hasVaultItem = true
                // Ensure no plaintext metadata of encrypted files exists in mediaDB
                if (CryptoManager.isEncryptedFile(medium.path)) {
                    mediaDB.deleteMediumPath(medium.path)
                }
            } else {
                // Only cache public files in mediaDB
                mediaDB.insert(medium.copy(id = null, deletedTS = 0L))
            }
        }

        if (hasVaultItem) {
            val currentAlbum = virtualAlbumsDB.getAlbumById(albumId)
            if (currentAlbum != null && (!currentAlbum.isHidden || !CryptoManager.isEncryptedVaultString(currentAlbum.name))) {
                val plainName = CryptoManager.decryptStringFromVault(currentAlbum.name) ?: currentAlbum.name
                val encName = CryptoManager.encryptStringForVault(this, plainName)
                virtualAlbumsDB.updateAlbumVisibilityAndName(albumId, true, encName)
                directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
            }
        }

        val now = System.currentTimeMillis()
        val items = allowedMedia.map { medium ->
            // Always store the on-disk filename (e.g. .fv_xxx.fenc), never decrypted originalName
            val identity = MediaIdentity.of(this, medium)
            VirtualAlbumItem(
                id = null,
                albumId = albumId,
                fullPath = medium.path,
                filename = medium.path.getFilenameFromPath(),
                parentPath = medium.path.getParentPath(),
                addedTS = now,
                mediaStoreId = identity.mediaStoreId,
                fileSize = identity.size,
                lastModified = identity.lastModified,
                fingerprint = identity.fingerprint
            )
        }
        virtualAlbumsDB.insertItems(items)
        VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
        allowedMedia.size - alreadyInAlbum
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
        val album = virtualAlbumsDB.getAlbumById(albumId)

        // Remember where the items came from, but never use it to reject items.
        if (album != null && album.parentPhysicalPath.isEmpty()) {
            val firstParent = validItems.firstOrNull()?.path?.getParentPath() ?: ""
            if (firstParent.isNotEmpty()) {
                album.parentPhysicalPath = firstParent
                virtualAlbumsDB.updateAlbumParentPhysicalPath(albumId, firstParent)
            }
        }

        val allowedItems = validItems
        val existingPaths = virtualAlbumsDB.getItems(albumId).map { it.fullPath.lowercase() }.toHashSet()
        val alreadyInAlbum = allowedItems.count { existingPaths.contains(it.path.lowercase()) }
        var hasVaultItem = forceMarkAlbumHidden

        allowedItems.forEach { item ->
            val path = item.path
            if (CryptoManager.isVaultMediaPath(this, path)) {
                hasVaultItem = true
                if (CryptoManager.isEncryptedFile(path)) {
                    mediaDB.deleteMediumPath(path)
                }
            }
        }

        if (hasVaultItem) {
            val currentAlbum = virtualAlbumsDB.getAlbumById(albumId)
            if (currentAlbum != null && (!currentAlbum.isHidden || !CryptoManager.isEncryptedVaultString(currentAlbum.name))) {
                val plainName = CryptoManager.decryptStringFromVault(currentAlbum.name) ?: currentAlbum.name
                val encName = CryptoManager.encryptStringForVault(this, plainName)
                virtualAlbumsDB.updateAlbumVisibilityAndName(albumId, true, encName)
                directoryDB.deleteDirPath(getVirtualAlbumPath(albumId))
            }
        }

        val now = System.currentTimeMillis()
        val items = allowedItems.map { item ->
            val identity = MediaIdentity.of(this, File(item.path), item.mediaStoreId)
            VirtualAlbumItem(
                id = null,
                albumId = albumId,
                fullPath = item.path,
                filename = item.path.getFilenameFromPath(),
                parentPath = item.path.getParentPath(),
                addedTS = now,
                mediaStoreId = identity.mediaStoreId,
                fileSize = identity.size,
                lastModified = identity.lastModified,
                fingerprint = identity.fingerprint
            )
        }
        virtualAlbumsDB.insertItems(items)
        VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
        allowedItems.size - alreadyInAlbum
    } catch (e: Exception) {
        0
    }
}

fun Context.removePathsFromVirtualAlbum(albumId: Long, paths: Collection<String>) {
    try {
        paths.forEach { virtualAlbumsDB.removeItemFromAlbum(albumId, it) }
        VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
    } catch (ignored: Exception) {
    }
}

fun Context.updateVirtualAlbumItemPath(oldPath: String, newPath: String) {
    try {
        val albumIds = virtualAlbumsDB.getAlbumIdsWithItem(oldPath)
        virtualAlbumsDB.updateItemPath(oldPath, newPath, newPath.getFilenameFromPath(), newPath.getParentPath())
        // The file moved, so its identity has to be captured again at the new location.
        updateReferenceIdentitiesForPath(newPath)
        albumIds.forEach { VirtualAlbumSyncManager.saveAlbumToFile(this, it) }
    } catch (ignored: Exception) {
    }
}

/**
 * Rewrites every reference below [oldRoot] so it points below [newRoot].
 *
 * Called when a physical folder is renamed or moved. Without this the favorites and the virtual
 * album items would keep pointing at the old location and appear empty.
 */
fun Context.updateReferencesForFolderRename(oldRoot: String, newRoot: String) {
    if (oldRoot.isEmpty() || newRoot.isEmpty() || oldRoot.equals(newRoot, ignoreCase = true)) {
        return
    }

    try {
        virtualAlbumsDB.updateItemsByFolderPrefix(oldRoot, newRoot)
        virtualAlbumsDB.updateItemParentsByFolderPrefix(oldRoot, newRoot)
        virtualAlbumsDB.updateItemParentsAtFolder(oldRoot, newRoot)
        virtualAlbumsDB.updateAlbumParentPhysicalPathByPrefix(oldRoot, newRoot)
        virtualAlbumsDB.updateAlbumParentPhysicalPathAtFolder(oldRoot, newRoot)
        updateFavoritePathsForFolderRename(oldRoot, newRoot)
    } catch (ignored: Exception) {
    }

    // The albums whose items just moved need their backup files rewritten.
    try {
        virtualAlbumsDB.getAllAlbums().forEach { album ->
            album.id?.let { VirtualAlbumSyncManager.saveAlbumToFile(this, it) }
        }
        ReferenceBackupManager.saveFavorites(this)
    } catch (ignored: Exception) {
    }
}

/**
 * Looks for the file of an album item at its current location.
 *
 * When the file was renamed or moved outside of the app, the cached path is stale. The identity of
 * the item is used to find it again, and the album is updated so the new path is used from now on.
 *
 * @return the current path of the file, or null when it could not be found
 */
fun Context.resolveVirtualAlbumItemPath(
    item: VirtualAlbumItem,
    session: MediaIdentityResolver.ResolutionSession? = null,
    saveBackup: Boolean = true,
    excludedPaths: Set<String> = emptySet(),
    allowAmbiguous: Boolean = true
): String? {
    val identity = item.toIdentity()
    if (!identity.isUsable) {
        return null
    }

    val resolution = MediaIdentityResolver.resolve(
        context = this,
        identity = identity,
        hintPaths = listOf(item.fullPath),
        searchRoots = listOf(item.parentPath),
        session = session,
        excludedPaths = excludedPaths,
        allowAmbiguous = allowAmbiguous
    ) ?: return null

    if (resolution.path == item.fullPath) {
        if (item.state == ITEM_STATE_UNRESOLVED) {
            try {
                virtualAlbumsDB.markItemResolved(item.albumId, item.fullPath)
            } catch (_: Exception) {
            }
        }
        return item.fullPath
    }

    try {
        virtualAlbumsDB.updateItemPath(
            item.fullPath,
            resolution.path,
            resolution.path.getFilenameFromPath(),
            resolution.path.getParentPath()
        )
        virtualAlbumsDB.markItemResolved(item.albumId, resolution.path)
        updateReferenceIdentitiesForPath(resolution.path, resolution.mediaStoreId)
        if (saveBackup) {
            VirtualAlbumSyncManager.saveAlbumToFile(this, item.albumId)
        }
    } catch (_: Exception) {
    }

    return resolution.path
}

/**
 * Resolves missing virtual album items in two passes inside one [MediaIdentityResolver.ResolutionSession]:
 * first resolving unambiguous items (so sibling folder relocations are discovered before duplicates),
 * then resolving remaining items using full candidate scoring and scoped path exclusion.
 */
private fun Context.resolveVirtualAlbumItemsTwoPass(
    allItems: List<VirtualAlbumItem>,
    missingItems: List<VirtualAlbumItem>,
    session: MediaIdentityResolver.ResolutionSession?
): Map<String, String> {
    if (missingItems.isEmpty()) return emptyMap()

    val claimedPaths = HashSet<String>(allItems.size)
    for (item in allItems) {
        if (File(item.fullPath).isFile) {
            claimedPaths.add(item.fullPath.lowercase())
        }
    }

    val activeSession = session ?: MediaIdentityResolver.ResolutionSession().apply {
        preloadMediaStore(this@resolveVirtualAlbumItemsTwoPass, missingItems.map { it.toIdentity() })
    }

    val resolvedByOldPath = HashMap<String, String>(missingItems.size)
    val deferred = ArrayList<VirtualAlbumItem>()
    val firstPassAllowAmbiguous = missingItems.size <= 1

    for (item in missingItems) {
        val resolved = resolveVirtualAlbumItemPath(
            item = item,
            session = activeSession,
            saveBackup = false,
            excludedPaths = claimedPaths,
            allowAmbiguous = firstPassAllowAmbiguous
        )
        if (resolved != null) {
            resolvedByOldPath[item.fullPath] = resolved
            claimedPaths.add(resolved.lowercase())
        } else if (!firstPassAllowAmbiguous) {
            deferred.add(item)
        }
    }

    for (item in deferred) {
        val resolved = resolveVirtualAlbumItemPath(
            item = item,
            session = activeSession,
            saveBackup = false,
            excludedPaths = claimedPaths,
            allowAmbiguous = true
        )
        if (resolved != null) {
            resolvedByOldPath[item.fullPath] = resolved
            claimedPaths.add(resolved.lowercase())
        }
    }

    return resolvedByOldPath
}

/**
 * Looks for the items of a virtual album whose file could not be found at the cached path and tries
 * to find them at their new location.
 *
 * @return how many items were repaired
 */
fun Context.repairVirtualAlbumItems(
    albumId: Long,
    session: MediaIdentityResolver.ResolutionSession? = null,
    saveBackup: Boolean = true
): Int {
    val items = try {
        virtualAlbumsDB.getItems(albumId)
    } catch (_: Exception) {
        return 0
    }

    val missing = ArrayList<VirtualAlbumItem>()
    var repaired = 0

    for (item in items) {
        if (File(item.fullPath).isFile) {
            if (item.state == ITEM_STATE_UNRESOLVED) {
                try {
                    virtualAlbumsDB.markItemResolved(albumId, item.fullPath)
                    repaired++
                } catch (_: Exception) {
                }
            }
        } else {
            missing.add(item)
        }
    }

    if (missing.isNotEmpty()) {
        val resolvedMap = resolveVirtualAlbumItemsTwoPass(items, missing, session)
        for ((oldPath, newPath) in resolvedMap) {
            if (newPath != oldPath) {
                repaired++
            }
        }
    }

    if (repaired > 0 && saveBackup) {
        VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
    }

    return repaired
}

fun Context.deleteVirtualAlbumItemFromAllAlbums(path: String) {
    try {
        val albumIds = virtualAlbumsDB.getAlbumIdsWithItem(path)
        virtualAlbumsDB.removeItemFromAllAlbums(path)
        albumIds.forEach { VirtualAlbumSyncManager.saveAlbumToFile(this, it) }
    } catch (ignored: Exception) {
    }
}

/**
 * Builds the main screen entries of all virtual albums, so they can be shown immediately.
 * Hidden virtual albums are strictly omitted unless `canAccessHiddenVirtualAlbums()` is true.
 * Empty albums are included too, otherwise a freshly created album could not be found. Must run on a background thread.
 */
fun Context.getVirtualAlbumDirectories(isSecretVaultView: Boolean = false): ArrayList<Directory> {
    VirtualAlbumSyncManager.syncVirtualAlbums(this)
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
        if (isSecretVaultView) {
            if (!album.isHidden || !canAccessHidden) {
                continue
            }
        } else if (album.isHidden && !canAccessHidden) {
            continue
        }
        val resolvedName = getResolvedVirtualAlbumName(album) ?: continue
        val path = getVirtualAlbumPath(albumId)
        val media = getVirtualAlbumMedia(albumId, getVideoDurations = false)

        val directory = if (media.isEmpty()) {
            Directory(null, path, "", resolvedName, 0, album.createdTs, album.createdTs, 0L, LOCATION_INTERNAL, 0, "")
        } else {
            mediaFetcher.sortMedia(media, config.getFolderSorting(path), enableRandom = false)
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
