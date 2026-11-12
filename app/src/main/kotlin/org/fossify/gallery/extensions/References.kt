package org.fossify.gallery.extensions

import android.content.Context
import org.fossify.gallery.helpers.MediaIdentityResolver
import org.fossify.gallery.helpers.ReferenceBackupManager
import org.fossify.gallery.helpers.VirtualAlbumSyncManager
import java.io.File

/**
 * Repairs the references to files that were renamed or moved outside of the app.
 *
 * The app can only follow a rename it performed itself. When the file was renamed by a file
 * manager, by another app or by a desktop tool, the cached path becomes stale. The identity of the
 * reference is what makes it possible to find the file again.
 *
 * Nothing is deleted here. A file that cannot be found stays in the album as unresolved, so the
 * user still sees it and can repair it manually.
 */
fun Context.repairReferencesAfterExternalChange(): Int {
    var repaired = 0

    try {
        repaired += repairFavorites()
    } catch (_: Exception) {
    }

    // A favorite whose file was deleted outside of the app is removed, so it does not come back
    // when the backups are restored.
    try {
        removeFavoritesOfDeletedFiles()
    } catch (_: Exception) {
    }

    try {
        val albums = virtualAlbumsDB.getAllAlbums()
        for (album in albums) {
            val albumId = album.id ?: continue
            try {
                repaired += repairVirtualAlbumItems(albumId)
            } catch (_: Exception) {
            }
        }
    } catch (_: Exception) {
    }

    // The backups are rewritten in any case, so a file that was deleted outside of the app does
    // not come back the next time the app data is cleared.
    try {
        saveReferenceBackups()
    } catch (_: Exception) {
    }

    return repaired
}

/**
 * Looks for a file that is referenced by a favorite or a virtual album item but that cannot be
 * found at the cached path.
 *
 * The MediaStore is asked first, because it knows the current location of every indexed file.
 * Only when that fails the folders around the reference are scanned.
 */
fun Context.findReferenceLocation(mediaStoreId: Long, fingerprint: String, size: Long, hintParent: String): String? {
    val identity = org.fossify.gallery.helpers.MediaIdentity(
        mediaStoreId = mediaStoreId,
        size = size,
        lastModified = 0L,
        fingerprint = fingerprint
    )

    val resolution = MediaIdentityResolver.resolve(
        context = this,
        identity = identity,
        hintPaths = emptyList(),
        searchRoots = listOf(hintParent)
    )

    return resolution?.path
}

/**
 * Writes the backups of the favorites and the virtual albums to the Cipher folder.
 *
 * The backups live outside of the app sandbox, so they survive a wipe of the app data.
 */
fun Context.saveReferenceBackups() {
    try {
        ReferenceBackupManager.saveFavorites(this)
    } catch (_: Exception) {
    }

    try {
        virtualAlbumsDB.getAllAlbums().forEach { album ->
            album.id?.let { VirtualAlbumSyncManager.saveAlbumToFile(this, it) }
        }
    } catch (_: Exception) {
    }
}

/**
 * Restores the favorites from the backup in the Cipher folder. Used when the app starts without
 * any data, for example after the user cleared the app data.
 *
 * @return how many favorites were restored
 */
fun Context.restoreFavoritesFromBackup(): Int {
    return try {
        ReferenceBackupManager.restoreFavorites(this)
    } catch (_: Exception) {
        0
    }
}

/**
 * True when the file behind the reference is still there. Used to decide if a backup entry may be
 * restored.
 */
fun Context.doesReferenceStillExist(path: String): Boolean {
    return File(path).isFile
}
