package org.fossify.gallery.extensions

import android.content.Context
import org.fossify.gallery.helpers.MediaIdentity
import org.fossify.gallery.helpers.MediaIdentityResolver
import org.fossify.gallery.helpers.ReferenceBackupManager
import org.fossify.gallery.helpers.VirtualAlbumSyncManager
import org.fossify.gallery.models.ITEM_STATE_UNRESOLVED
import java.io.File

private const val MIN_REPAIR_INTERVAL_MS = 5_000L
private const val MIN_FALLBACK_SCAN_INTERVAL_MS = 30_000L

@Volatile
private var lastRepairPassTs = 0L

@Volatile
private var lastFallbackScanTs = 0L

private val repairLock = Any()

/**
 * Repairs the references to files that were renamed or moved outside of the app.
 *
 * The app can only follow a rename it performed itself. When the file was renamed by a file
 * manager, by another app or by a desktop tool, the cached path becomes stale. The identity of the
 * reference is what makes it possible to find the file again.
 *
 * Nothing is deleted here: unresolvable favorites and virtual album items stay preserved so a
 * temporarily moved or unmounted folder can be recovered later. Backups are only rewritten when
 * something actually changed.
 */
fun Context.repairReferencesAfterExternalChange(force: Boolean = false): Int {
    synchronized(repairLock) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRepairPassTs < MIN_REPAIR_INTERVAL_MS) {
            return 0
        }
        lastRepairPassTs = now

        val allowFallback = force || (now - lastFallbackScanTs >= MIN_FALLBACK_SCAN_INTERVAL_MS)

        val missingFavorites = try {
            favoritesDB.getAll().filter { !File(it.fullPath).isFile }
        } catch (_: Exception) {
            emptyList()
        }

        val albums = try {
            virtualAlbumsDB.getAllAlbums()
        } catch (_: Exception) {
            emptyList()
        }

        val allItems = try {
            virtualAlbumsDB.getAllItems()
        } catch (_: Exception) {
            emptyList()
        }

        val missingItems = allItems.filter { !File(it.fullPath).isFile }
        val reappearedAlbumIds = allItems
            .filter { it.state == ITEM_STATE_UNRESOLVED && File(it.fullPath).isFile }
            .mapTo(HashSet()) { it.albumId }

        if (missingFavorites.isEmpty() && missingItems.isEmpty() && reappearedAlbumIds.isEmpty()) {
            return 0
        }

        val missingIdentities = ArrayList<MediaIdentity>(missingFavorites.size + missingItems.size)
        missingFavorites.forEach { missingIdentities.add(it.toIdentity()) }
        missingItems.forEach { missingIdentities.add(it.toIdentity()) }

        val session = MediaIdentityResolver.ResolutionSession(allowFilesystemFallback = allowFallback)
        if (missingIdentities.isNotEmpty()) {
            session.preloadMediaStore(this, missingIdentities)
        }

        var favoritesRepaired = 0
        if (missingFavorites.isNotEmpty()) {
            try {
                favoritesRepaired = repairFavorites(session = session)
            } catch (_: Exception) {
            }
        }

        val candidateAlbumIds = LinkedHashSet<Long>()
        missingItems.forEach { candidateAlbumIds.add(it.albumId) }
        candidateAlbumIds.addAll(reappearedAlbumIds)

        var albumsRepaired = 0
        for (album in albums) {
            val albumId = album.id ?: continue
            if (albumId !in candidateAlbumIds) continue
            try {
                val repairedInAlbum = repairVirtualAlbumItems(albumId, session = session, saveBackup = false)
                if (repairedInAlbum > 0) {
                    albumsRepaired += repairedInAlbum
                    VirtualAlbumSyncManager.saveAlbumToFile(this, albumId)
                }
            } catch (_: Exception) {
            }
        }

        if (allowFallback && missingIdentities.isNotEmpty()) {
            lastFallbackScanTs = System.currentTimeMillis()
        }

        if (favoritesRepaired > 0) {
            try {
                ReferenceBackupManager.saveFavorites(this)
            } catch (_: Exception) {
            }
        }

        return favoritesRepaired + albumsRepaired
    }
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
