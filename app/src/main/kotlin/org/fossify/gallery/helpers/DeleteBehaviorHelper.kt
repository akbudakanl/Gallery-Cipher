package org.fossify.gallery.helpers

import org.fossify.commons.helpers.FAVORITES
import org.fossify.gallery.R
import org.fossify.gallery.extensions.isVirtualAlbumPath

enum class DeleteOriginType {
    VIRTUAL_ALBUM,
    FAVORITES,
    OTHER
}

/**
 * Resolves delete confirmation defaults and skip-dialog behavior for virtual albums and favorites.
 */
object DeleteBehaviorHelper {
    const val TEMP_KEEP_ORIGINAL_UNSET = -1
    const val TEMP_KEEP_ORIGINAL_FALSE = 0
    const val TEMP_KEEP_ORIGINAL_TRUE = 1

    fun getOriginType(directoryPath: String): DeleteOriginType = when {
        directoryPath.isVirtualAlbumPath() -> DeleteOriginType.VIRTUAL_ALBUM
        directoryPath == FAVORITES -> DeleteOriginType.FAVORITES
        else -> DeleteOriginType.OTHER
    }

    fun getKeepOriginalOptionTextRes(directoryPath: String): Int? = when (getOriginType(directoryPath)) {
        DeleteOriginType.VIRTUAL_ALBUM -> R.string.keep_original_remove_from_virtual_album
        DeleteOriginType.FAVORITES -> R.string.keep_original_remove_from_favorites
        DeleteOriginType.OTHER -> null
    }

    fun getKeepOriginalDefaultChecked(
        directoryPath: String,
        deleteOriginalFromFavoritesAndVirtualAlbums: Boolean
    ): Boolean {
        if (getOriginType(directoryPath) == DeleteOriginType.OTHER) {
            return false
        }
        return !deleteOriginalFromFavoritesAndVirtualAlbums
    }

    fun shouldKeepOriginalOnSkippedDelete(
        directoryPath: String,
        skipDeleteConfirmation: Boolean,
        tempSkipDeleteConfirmation: Boolean,
        tempKeepOriginalFile: Int,
        deleteOriginalFromFavoritesAndVirtualAlbums: Boolean
    ): Boolean {
        if (getOriginType(directoryPath) == DeleteOriginType.OTHER) {
            return false
        }
        return if (skipDeleteConfirmation) {
            !deleteOriginalFromFavoritesAndVirtualAlbums
        } else if (tempSkipDeleteConfirmation && tempKeepOriginalFile != TEMP_KEEP_ORIGINAL_UNSET) {
            tempKeepOriginalFile == TEMP_KEEP_ORIGINAL_TRUE
        } else {
            !deleteOriginalFromFavoritesAndVirtualAlbums
        }
    }
}
