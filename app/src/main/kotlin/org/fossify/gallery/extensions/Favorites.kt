package org.fossify.gallery.extensions

import android.content.Context
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getParentPath
import org.fossify.gallery.helpers.MediaIdentity
import org.fossify.gallery.helpers.MediaIdentityResolver
import org.fossify.gallery.helpers.ReferenceBackupManager
import org.fossify.gallery.models.Favorite
import org.fossify.gallery.models.Medium
import java.io.File

/**
 * Favorites are references to files. The path is only a cache of the current location, the
 * identity columns describe the file itself and survive renames and moves.
 */

/** Builds a favorite including the identity of the file it points to. */
fun Context.getFavoriteFromPath(path: String, mediaStoreId: Long = 0L): Favorite {
    val file = File(path)
    val identity = MediaIdentity.of(file, mediaStoreId)
    return Favorite(
        id = null,
        fullPath = path,
        filename = path.getFilenameFromPath(),
        parentPath = path.getParentPath(),
        mediaStoreId = identity.mediaStoreId,
        fileSize = identity.size,
        lastModified = identity.lastModified,
        fingerprint = identity.fingerprint
    )
}

/** Builds a favorite for a known medium, reusing the MediaStore id it already carries. */
fun Context.getFavoriteFromMedium(medium: Medium): Favorite {
    val identity = MediaIdentity.of(this, medium)
    return Favorite(
        id = null,
        fullPath = medium.path,
        filename = medium.name,
        parentPath = medium.parentPath.ifEmpty { medium.path.getParentPath() },
        mediaStoreId = identity.mediaStoreId,
        fileSize = identity.size,
        lastModified = identity.lastModified,
        fingerprint = identity.fingerprint
    )
}

/**
 * Looks for the file behind a favorite again.
 *
 * The cached path is tried first. When the file is gone from there, the identity is used to find
 * it at its new location. Returns null when the file cannot be found with certainty.
 */
fun Context.resolveFavorite(favorite: Favorite): Favorite? {
    val cached = File(favorite.fullPath)
    if (cached.isFile) {
        return favorite
    }

    val identity = favorite.toIdentity()
    if (!identity.isUsable) {
        return null
    }

    val resolution = MediaIdentityResolver.resolve(
        context = this,
        identity = identity,
        hintPaths = listOf(favorite.fullPath),
        searchRoots = listOf(favorite.parentPath)
    ) ?: return null

    val updated = favorite.copy(
        fullPath = resolution.path,
        filename = resolution.path.getFilenameFromPath(),
        parentPath = resolution.path.getParentPath()
    )

    favoritesDB.updateFavorite(
        updated.filename,
        updated.fullPath,
        updated.parentPath,
        favorite.fullPath
    )

    return updated
}

/**
 * Goes through all favorites and points the ones whose file was renamed or moved to their new
 * location. Returns how many favorites were repaired.
 */
fun Context.repairFavorites(): Int {
    val favorites = try {
        favoritesDB.getAll()
    } catch (_: Exception) {
        return 0
    }

    var repaired = 0
    for (favorite in favorites) {
        if (File(favorite.fullPath).isFile) {
            continue
        }

        val resolved = resolveFavorite(favorite)
        if (resolved != null && resolved.fullPath != favorite.fullPath) {
            repaired++
        }
    }

    return repaired
}

/**
 * Removes the favorites whose file no longer exists.
 *
 * A file that was deleted outside of the app leaves a favorite behind that can never be shown
 * again. The reference is removed so the backup does not keep it alive.
 *
 * @return how many favorites were removed
 */
fun Context.removeFavoritesOfDeletedFiles(): Int {
    val favorites = try {
        favoritesDB.getAll()
    } catch (_: Exception) {
        return 0
    }

    var removed = 0
    for (favorite in favorites) {
        if (File(favorite.fullPath).isFile) {
            continue
        }

        // The file may still be found at a new location, in that case it is repaired instead.
        if (resolveFavorite(favorite) != null) {
            continue
        }

        try {
            favoritesDB.deleteFavoritePath(favorite.fullPath)
            removed++
        } catch (_: Exception) {
        }
    }

    return removed
}

/**
 * Rewrites all favorites below [oldRoot] so they point below [newRoot]. Called when a physical
 * folder is renamed or moved.
 */
fun Context.updateFavoritePathsForFolderRename(oldRoot: String, newRoot: String) {
    if (oldRoot.isEmpty() || newRoot.isEmpty() || oldRoot == newRoot) {
        return
    }

    try {
        favoritesDB.updateFavoritesByFolderPrefix(oldRoot, newRoot)
        favoritesDB.updateFavoriteParentsByFolderPrefix(oldRoot, newRoot)
        favoritesDB.updateFavoriteParentsAtFolder(oldRoot, newRoot)
    } catch (_: Exception) {
    }
}

/**
 * Removes [paths] from favorites, updates the media cache flag, and syncs the favorites backup.
 */
fun Context.removeFavoritePaths(paths: Collection<String>) {
    if (paths.isEmpty()) {
        return
    }

    try {
        paths.forEach { path ->
            favoritesDB.deleteFavoritePath(path)
            mediaDB.updateFavorite(path, false)
        }
        ReferenceBackupManager.saveFavorites(this)
    } catch (_: Exception) {
    }
}
