package org.fossify.gallery.helpers

import android.content.Context
import org.fossify.commons.extensions.getDoesFilePathExist
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.recycleBinPath
import org.fossify.commons.helpers.FAVORITES
import org.fossify.gallery.extensions.directoryDB
import org.fossify.gallery.extensions.favoritesDB
import org.fossify.gallery.extensions.getResolvedVirtualAlbumName
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.extensions.virtualAlbumsDB
import org.fossify.gallery.models.Favorite
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem
import java.io.File

internal object FavoritesAndVirtualAlbumsImporter {

    private data class FavoriteImportOutcome(val inserted: Boolean, val processed: Boolean)

    fun applyImport(
        context: Context,
        pkg: ParsedImportPackage,
        favoritesMode: FavoritesImportMode
    ): ImportResult {
        val canWriteDisk = FavoritesAndVirtualAlbumsBackupManager.canWriteInternalBackups(context)
        val (importedFavoritesCount, processedFavoritesCount) =
            importFavoritesList(context, pkg.favorites, favoritesMode, canWriteDisk)
        val (importedAlbumsCount, importedItemsCount) =
            importVirtualAlbumsList(context, pkg.virtualAlbums, canWriteDisk)

        return ImportResult(
            importedFavoritesCount = importedFavoritesCount,
            processedFavoritesCount = processedFavoritesCount,
            importedVirtualAlbumsCount = importedAlbumsCount,
            importedVirtualAlbumItemsCount = importedItemsCount
        )
    }

    private fun importFavoritesList(
        context: Context,
        entries: List<ReferenceBackupManager.FavoriteBackup>,
        mode: FavoritesImportMode,
        canWriteDisk: Boolean
    ): Pair<Int, Int> {
        if (entries.isEmpty()) {
            return 0 to 0
        }

        if (mode == FavoritesImportMode.DELETE_EXISTING) {
            try {
                context.favoritesDB.clearFavorites()
                context.mediaDB.clearFavorites()
                context.directoryDB.deleteDirPath(FAVORITES)
            } catch (_: Exception) {
            }
        }

        var imported = 0
        var processed = 0
        for (entry in entries) {
            val outcome = importSingleFavorite(context, entry)
            if (outcome.processed) {
                processed++
            }
            if (outcome.inserted) {
                imported++
            }
        }

        if (canWriteDisk) {
            ReferenceBackupManager.saveFavorites(context)
        }
        return imported to processed
    }

    private fun importSingleFavorite(
        context: Context,
        entry: ReferenceBackupManager.FavoriteBackup
    ): FavoriteImportOutcome {
        val rawPath = entry.fullPath?.trim()?.ifEmpty { null }
            ?: return FavoriteImportOutcome(inserted = false, processed = false)
        val fallbackIdentity = MediaIdentity(
            mediaStoreId = entry.mediaStoreId ?: 0L,
            size = entry.fileSize ?: 0L,
            lastModified = entry.lastModified ?: 0L,
            fingerprint = entry.fingerprint ?: ""
        )
        val resolvedPath = resolveExistingPath(
            context = context,
            path = rawPath,
            identity = fallbackIdentity,
            parentPathHint = entry.parentPath ?: rawPath.getParentPath()
        ) ?: return FavoriteImportOutcome(inserted = false, processed = false)

        val isDisallowed = resolvedPath.startsWith(context.recycleBinPath) ||
            CryptoManager.isVaultMediaPath(context, resolvedPath)
        if (isDisallowed) {
            return FavoriteImportOutcome(inserted = false, processed = false)
        }

        return try {
            val existing = context.favoritesDB.getFavorite(resolvedPath)
            if (existing == null) {
                val identity = resolveIdentityForPath(resolvedPath, fallbackIdentity)
                context.favoritesDB.insert(
                    Favorite(
                        id = null,
                        fullPath = resolvedPath,
                        filename = resolvedPath.getFilenameFromPath(),
                        parentPath = resolvedPath.getParentPath(),
                        mediaStoreId = identity.mediaStoreId,
                        fileSize = identity.size,
                        lastModified = identity.lastModified,
                        fingerprint = identity.fingerprint
                    )
                )
                context.mediaDB.updateFavorite(resolvedPath, true)
                FavoriteImportOutcome(inserted = true, processed = true)
            } else {
                if (existing.fingerprint.isEmpty() && !entry.fingerprint.isNullOrEmpty()) {
                    context.favoritesDB.updateFavoriteIdentity(
                        resolvedPath,
                        entry.mediaStoreId ?: existing.mediaStoreId,
                        entry.fileSize ?: existing.fileSize,
                        entry.lastModified ?: existing.lastModified,
                        entry.fingerprint
                    )
                }
                context.mediaDB.updateFavorite(resolvedPath, true)
                FavoriteImportOutcome(inserted = false, processed = true)
            }
        } catch (_: Exception) {
            FavoriteImportOutcome(inserted = false, processed = false)
        }
    }

    private fun importVirtualAlbumsList(
        context: Context,
        albums: List<ImportedVirtualAlbum>,
        canWriteDisk: Boolean
    ): Pair<Int, Int> {
        if (albums.isEmpty()) {
            return 0 to 0
        }

        val existingNamesLowercase = HashSet<String>()
        try {
            context.virtualAlbumsDB.getAllAlbums().forEach { album ->
                existingNamesLowercase.add(album.name.lowercase())
                context.getResolvedVirtualAlbumName(album)?.let { resolved ->
                    existingNamesLowercase.add(resolved.lowercase())
                }
            }
        } catch (_: Exception) {
        }

        var importedAlbumsCount = 0
        var importedItemsCount = 0
        for (importedAlbum in albums) {
            val uniqueName = FavoritesAndVirtualAlbumsBackupManager.resolveUniqueVirtualAlbumName(
                importedAlbum.name,
                existingNamesLowercase
            )
            if (uniqueName.isEmpty()) {
                continue
            }
            existingNamesLowercase.add(uniqueName.lowercase())

            val itemsAdded = insertImportedVirtualAlbum(context, importedAlbum, uniqueName, canWriteDisk)
            if (itemsAdded >= 0) {
                importedAlbumsCount++
                importedItemsCount += itemsAdded
            }
        }

        return importedAlbumsCount to importedItemsCount
    }

    private fun insertImportedVirtualAlbum(
        context: Context,
        importedAlbum: ImportedVirtualAlbum,
        uniqueName: String,
        canWriteDisk: Boolean
    ): Int {
        val dao = context.virtualAlbumsDB
        val createdTs = if (importedAlbum.createdTs > 0L) {
            importedAlbum.createdTs
        } else {
            System.currentTimeMillis()
        }
        val newAlbumId = try {
            dao.insertAlbum(
                VirtualAlbum(
                    id = null,
                    name = uniqueName,
                    createdTs = createdTs,
                    thumbnail = "",
                    isHidden = false,
                    parentPhysicalPath = importedAlbum.parentPhysicalPath
                )
            )
        } catch (_: Exception) {
            -1L
        }
        if (newAlbumId <= 0L) {
            return -1
        }

        val seenPaths = HashSet<String>()
        val itemsToInsert = importedAlbum.items.mapNotNull { entry ->
            buildSingleVirtualAlbumItem(context, newAlbumId, entry, seenPaths)
        }
        if (itemsToInsert.isNotEmpty()) {
            try {
                dao.insertItems(itemsToInsert)
            } catch (_: Exception) {
            }
        }
        if (canWriteDisk) {
            VirtualAlbumSyncManager.saveAlbumToFile(context, newAlbumId)
        }
        return itemsToInsert.size
    }

    private fun buildSingleVirtualAlbumItem(
        context: Context,
        albumId: Long,
        entry: VirtualAlbumItemBackup,
        seenPaths: MutableSet<String>
    ): VirtualAlbumItem? {
        val rawPath = entry.fullPath?.trim()?.ifEmpty { null } ?: return null
        if (rawPath.startsWith(context.recycleBinPath) || CryptoManager.isVaultMediaPath(context, rawPath)) {
            return null
        }

        val fallbackIdentity = MediaIdentity(
            mediaStoreId = entry.mediaStoreId ?: 0L,
            size = entry.fileSize ?: 0L,
            lastModified = entry.lastModified ?: 0L,
            fingerprint = entry.fingerprint ?: ""
        )
        val resolvedPath = resolveExistingPath(
            context = context,
            path = rawPath,
            identity = fallbackIdentity,
            parentPathHint = rawPath.getParentPath()
        ) ?: if (File(rawPath).parentFile?.exists() != true) rawPath else null
            ?: return null

        val isDisallowed = resolvedPath.startsWith(context.recycleBinPath) ||
            CryptoManager.isVaultMediaPath(context, resolvedPath)
        if (isDisallowed || !seenPaths.add(resolvedPath.lowercase())) {
            return null
        }

        val identity = resolveIdentityForPath(resolvedPath, fallbackIdentity)
        return VirtualAlbumItem(
            id = null,
            albumId = albumId,
            fullPath = resolvedPath,
            filename = resolvedPath.getFilenameFromPath(),
            parentPath = resolvedPath.getParentPath(),
            addedTS = if ((entry.addedTs ?: 0L) > 0L) entry.addedTs!! else System.currentTimeMillis(),
            mediaStoreId = identity.mediaStoreId,
            fileSize = identity.size,
            lastModified = identity.lastModified,
            fingerprint = identity.fingerprint
        )
    }

    private fun resolveExistingPath(
        context: Context,
        path: String,
        identity: MediaIdentity,
        parentPathHint: String
    ): String? {
        if (File(path).isFile || context.getDoesFilePathExist(path)) {
            return path
        }
        if (!identity.isUsable) {
            return null
        }
        return MediaIdentityResolver.resolve(
            context = context,
            identity = identity,
            hintPaths = listOf(path),
            searchRoots = listOf(parentPathHint)
        )?.path
    }

    private fun resolveIdentityForPath(path: String, fallback: MediaIdentity): MediaIdentity {
        val file = File(path)
        if (!file.isFile) {
            return fallback
        }
        val computed = MediaIdentity.of(file, fallback.mediaStoreId)
        return MediaIdentity(
            mediaStoreId = if (computed.mediaStoreId != 0L) computed.mediaStoreId else fallback.mediaStoreId,
            size = if (computed.size > 0L) computed.size else fallback.size,
            lastModified = if (computed.lastModified > 0L) computed.lastModified else fallback.lastModified,
            fingerprint = if (computed.hasFingerprint) computed.fingerprint else fallback.fingerprint
        )
    }
}
