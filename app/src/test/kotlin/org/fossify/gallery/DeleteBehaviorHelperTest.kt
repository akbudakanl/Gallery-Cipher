package org.fossify.gallery

import org.fossify.commons.helpers.FAVORITES
import org.fossify.gallery.helpers.DeleteBehaviorHelper
import org.fossify.gallery.helpers.DeleteOriginType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteBehaviorHelperTest {

    private val virtualAlbumPath = "virtual_album_42"
    private val physicalFolderPath = "/storage/emulated/0/DCIM/Camera"

    @Test
    fun originTypeAndCheckboxTextMatchDirectory() {
        assertEquals(DeleteOriginType.VIRTUAL_ALBUM, DeleteBehaviorHelper.getOriginType(virtualAlbumPath))
        assertEquals(
            R.string.keep_original_remove_from_virtual_album,
            DeleteBehaviorHelper.getKeepOriginalOptionTextRes(virtualAlbumPath)
        )

        assertEquals(DeleteOriginType.FAVORITES, DeleteBehaviorHelper.getOriginType(FAVORITES))
        assertEquals(
            R.string.keep_original_remove_from_favorites,
            DeleteBehaviorHelper.getKeepOriginalOptionTextRes(FAVORITES)
        )

        assertEquals(DeleteOriginType.OTHER, DeleteBehaviorHelper.getOriginType(physicalFolderPath))
        assertNull(DeleteBehaviorHelper.getKeepOriginalOptionTextRes(physicalFolderPath))

        assertEquals(DeleteOriginType.OTHER, DeleteBehaviorHelper.getOriginType(""))
        assertNull(DeleteBehaviorHelper.getKeepOriginalOptionTextRes(""))
    }

    @Test
    fun keepOriginalDefaultCheckedFollowsSettingForVirtualAlbumsAndFavorites() {
        // Setting enabled -> Keep original checkbox starts unchecked
        assertFalse(DeleteBehaviorHelper.getKeepOriginalDefaultChecked(virtualAlbumPath, true))
        assertFalse(DeleteBehaviorHelper.getKeepOriginalDefaultChecked(FAVORITES, true))
        assertFalse(DeleteBehaviorHelper.getKeepOriginalDefaultChecked(physicalFolderPath, true))

        // Default setting (disabled = false) -> Keep original checkbox starts checked for virtual albums and favorites
        assertTrue(DeleteBehaviorHelper.getKeepOriginalDefaultChecked(virtualAlbumPath, false))
        assertTrue(DeleteBehaviorHelper.getKeepOriginalDefaultChecked(FAVORITES, false))
        assertFalse(DeleteBehaviorHelper.getKeepOriginalDefaultChecked(physicalFolderPath, false))
    }

    @Test
    fun alwaysSkipConfirmationFollowsSetting() {
        assertFalse(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = virtualAlbumPath,
                skipDeleteConfirmation = true,
                tempSkipDeleteConfirmation = false,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_UNSET,
                deleteOriginalFromFavoritesAndVirtualAlbums = true
            )
        )
        assertTrue(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = virtualAlbumPath,
                skipDeleteConfirmation = true,
                tempSkipDeleteConfirmation = false,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_UNSET,
                deleteOriginalFromFavoritesAndVirtualAlbums = false
            )
        )
        assertTrue(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = FAVORITES,
                skipDeleteConfirmation = true,
                tempSkipDeleteConfirmation = true,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_FALSE,
                deleteOriginalFromFavoritesAndVirtualAlbums = false
            )
        )
        assertFalse(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = physicalFolderPath,
                skipDeleteConfirmation = true,
                tempSkipDeleteConfirmation = false,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_TRUE,
                deleteOriginalFromFavoritesAndVirtualAlbums = false
            )
        )
    }

    @Test
    fun sessionRememberedChoiceIsUsedWhenRecordedAndFallsBackWhenUnset() {
        // Remembered "keep original = true" overrides enabled setting during the session
        assertTrue(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = virtualAlbumPath,
                skipDeleteConfirmation = false,
                tempSkipDeleteConfirmation = true,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_TRUE,
                deleteOriginalFromFavoritesAndVirtualAlbums = true
            )
        )
        // Remembered "keep original = false" overrides disabled setting during the session
        assertFalse(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = FAVORITES,
                skipDeleteConfirmation = false,
                tempSkipDeleteConfirmation = true,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_FALSE,
                deleteOriginalFromFavoritesAndVirtualAlbums = false
            )
        )
        // Session remember set in a physical folder (unset) falls back to the setting
        assertTrue(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = virtualAlbumPath,
                skipDeleteConfirmation = false,
                tempSkipDeleteConfirmation = true,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_UNSET,
                deleteOriginalFromFavoritesAndVirtualAlbums = false
            )
        )
        // Never applies to physical folders
        assertFalse(
            DeleteBehaviorHelper.shouldKeepOriginalOnSkippedDelete(
                directoryPath = physicalFolderPath,
                skipDeleteConfirmation = false,
                tempSkipDeleteConfirmation = true,
                tempKeepOriginalFile = DeleteBehaviorHelper.TEMP_KEEP_ORIGINAL_TRUE,
                deleteOriginalFromFavoritesAndVirtualAlbums = false
            )
        )
    }
}
