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
    val identity = MediaIdentity.ofWithFolder(this, file, mediaStoreId)
    return Favorite(
        id = null,
        fullPath = path,
        filename = path.getFilenameFromPath(),
        parentPath = path.getParentPath(),
        mediaStoreId = identity.mediaStoreId,
        fileSize = identity.size,
        lastModified = identity.lastModified,
        fingerprint = identity.fingerprint,
        folderInode = identity.folderInode,
        folderSignature = identity.folderSignature,
        capturedAtSec = identity.capturedAtSec
    )
}

/** Builds a favorite for a known medium, reusing the MediaStore id it already carries. */
fun Context.getFavoriteFromMedium(medium: Medium): Favorite {
    val base = MediaIdentity.of(this, medium)
    val file = File(medium.path)
    val identity = if (file.isFile) {
        base.withFolder(org.fossify.gallery.helpers.FolderIdentity.captureForFile(file))
            .copy(capturedAtSec = base.capturedAtSec.takeIf { it > 0L } ?: (System.currentTimeMillis() / 1000L))
    } else {
        base
    }
    return Favorite(
        id = null,
        fullPath = medium.path,
        filename = medium.name,
        parentPath = medium.parentPath.ifEmpty { medium.path.getParentPath() },
        mediaStoreId = identity.mediaStoreId,
        fileSize = identity.size,
        lastModified = identity.lastModified,
        fingerprint = identity.fingerprint,
        folderInode = identity.folderInode,
        folderSignature = identity.folderSignature,
        capturedAtSec = identity.capturedAtSec
    )
}

/**
 * Looks for the file behind a favorite again.
 *
 * The cached path is tried first. When the file is gone from there, the identity is used to find
 * it at its new location. Returns null when the file cannot be found with certainty.
 */
fun Context.resolveFavorite(
    favorite: Favorite,
    session: MediaIdentityResolver.ResolutionSession? = null,
    excludedPaths: Set<String> = emptySet(),
    allowAmbiguous: Boolean = true
): Favorite? {
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
        searchRoots = listOf(favorite.parentPath),
        session = session,
        excludedPaths = excludedPaths,
        allowAmbiguous = allowAmbiguous
    ) ?: return null

    val updated = favorite.copy(
        fullPath = resolution.path,
        filename = resolution.path.getFilenameFromPath(),
        parentPath = resolution.path.getParentPath(),
        mediaStoreId = if (resolution.mediaStoreId > 0L) resolution.mediaStoreId else favorite.mediaStoreId
    )

    favoritesDB.updateFavorite(
        updated.filename,
        updated.fullPath,
        updated.parentPath,
        favorite.fullPath
    )
    if (updated.mediaStoreId != favorite.mediaStoreId && updated.mediaStoreId > 0L) {
        updateReferenceIdentitiesForPath(updated.fullPath, updated.mediaStoreId)
    }
    try {
        mediaDB.updateFavorite(favorite.fullPath, false)
        mediaDB.updateFavorite(updated.fullPath, true)
    } catch (_: Exception) {
    }

    return updated
}

/**
 * Goes through all favorites and points the ones whose file was renamed or moved to their new
 * location. Returns how many favorites were repaired.
 */
fun Context.repairFavorites(
    session: MediaIdentityResolver.ResolutionSession? = null
): Int {
    val favorites = try {
        favoritesDB.getAll()
    } catch (_: Exception) {
        return 0
    }

    return resolveFavoritesTwoPass(favorites, session).repairedCount
}

/**
 * Removes the favorites whose file no longer exists on an accessible folder.
 *
 * Favorites whose parent folder does not exist (moved physical folder or unmounted storage) are
 * preserved so they can still be resolved later.
 *
 * @return how many favorites were removed
 */
fun Context.removeFavoritesOfDeletedFiles(): Int {
    val favorites = try {
        favoritesDB.getAll()
    } catch (_: Exception) {
        return 0
    }

    val unresolved = resolveFavoritesTwoPass(favorites, session = null).unresolved
    var removed = 0
    for (favorite in unresolved) {
        val file = File(favorite.fullPath)
        // Only remove if the original parent folder still exists on disk; if the folder itself
        // moved or storage is unmounted, keep the reference so it can be recovered.
        if (file.parentFile?.exists() != true) {
            continue
        }

        try {
            favoritesDB.deleteFavoritePath(favorite.fullPath)
            mediaDB.updateFavorite(favorite.fullPath, false)
            removed++
        } catch (_: Exception) {
        }
    }

    return removed
}

private data class FavoritesBatchResolution(
    val repairedCount: Int,
    val unresolved: List<Favorite>
)

private fun Context.resolveFavoritesTwoPass(
    favorites: List<Favorite>,
    session: MediaIdentityResolver.ResolutionSession?
): FavoritesBatchResolution {
    val claimedPaths = HashSet<String>(favorites.size)
    val missing = ArrayList<Favorite>()
    for (favorite in favorites) {
        if (File(favorite.fullPath).isFile) {
            claimedPaths.add(favorite.fullPath.lowercase())
        } else {
            missing.add(favorite)
        }
    }
    if (missing.isEmpty()) {
        return FavoritesBatchResolution(0, emptyList())
    }

    val activeSession = session ?: MediaIdentityResolver.ResolutionSession().apply {
        preloadMediaStore(this@resolveFavoritesTwoPass, missing.map { it.toIdentity() })
    }

    var repaired = 0
    val deferred = ArrayList<Favorite>()
    val unresolved = ArrayList<Favorite>()
    val firstPassAllowAmbiguous = missing.size <= 1

    fun attemptResolve(favorite: Favorite, allowAmbiguous: Boolean): Boolean {
        val resolved = resolveFavorite(
            favorite = favorite,
            session = activeSession,
            excludedPaths = claimedPaths,
            allowAmbiguous = allowAmbiguous
        ) ?: return false
        claimedPaths.add(resolved.fullPath.lowercase())
        if (resolved.fullPath != favorite.fullPath) {
            repaired++
        }
        return true
    }

    for (favorite in missing) {
        if (!attemptResolve(favorite, firstPassAllowAmbiguous)) {
            if (firstPassAllowAmbiguous) unresolved.add(favorite) else deferred.add(favorite)
        }
    }

    for (favorite in deferred) {
        if (!attemptResolve(favorite, allowAmbiguous = true)) {
            unresolved.add(favorite)
        }
    }

    return FavoritesBatchResolution(repaired, unresolved)
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
