package org.fossify.gallery

import org.fossify.gallery.helpers.ExcludedMenuPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcludedMenuPolicyTest {

    @Test
    fun noExcludedFoldersInCurrentScopeHidesBothMenuItems() {
        val hiddenWhenOff = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = false,
            isCurrentlyShowingExcluded = false
        )
        assertFalse(hiddenWhenOff.showTemporarilyShowExcluded)
        assertFalse(hiddenWhenOff.showStopShowingExcluded)

        val hiddenWhenStaleOn = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = false,
            isCurrentlyShowingExcluded = true
        )
        assertFalse(hiddenWhenStaleOn.showTemporarilyShowExcluded)
        assertFalse(hiddenWhenStaleOn.showStopShowingExcluded)
    }

    @Test
    fun excludedFoldersInCurrentScopeToggleMenuItemsByScopeState() {
        val showOptionVisible = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = true,
            isCurrentlyShowingExcluded = false
        )
        assertTrue(showOptionVisible.showTemporarilyShowExcluded)
        assertFalse(showOptionVisible.showStopShowingExcluded)

        val stopOptionVisible = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = true,
            isCurrentlyShowingExcluded = true
        )
        assertFalse(stopOptionVisible.showTemporarilyShowExcluded)
        assertTrue(stopOptionVisible.showStopShowingExcluded)
    }

    @Test
    fun scopeIndependenceBetweenNormalGalleryAndSecretVault() {
        val hasNormalExcluded = true
        val hasVaultExcluded = false

        val normalMenu = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = hasNormalExcluded,
            isCurrentlyShowingExcluded = false
        )
        val vaultMenu = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = hasVaultExcluded,
            isCurrentlyShowingExcluded = false
        )
        assertTrue(normalMenu.showTemporarilyShowExcluded)
        assertFalse(normalMenu.showStopShowingExcluded)
        assertFalse(vaultMenu.showTemporarilyShowExcluded)
        assertFalse(vaultMenu.showStopShowingExcluded)

        val reversedNormalMenu = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = false,
            isCurrentlyShowingExcluded = false
        )
        val reversedVaultMenu = ExcludedMenuPolicy.resolve(
            hasExcludedInCurrentScope = true,
            isCurrentlyShowingExcluded = true
        )
        assertFalse(reversedNormalMenu.showTemporarilyShowExcluded)
        assertFalse(reversedNormalMenu.showStopShowingExcluded)
        assertFalse(reversedVaultMenu.showTemporarilyShowExcluded)
        assertTrue(reversedVaultMenu.showStopShowingExcluded)
    }

    @Test
    fun shouldAddNoMediaOnEncryptDetectsExcludedFoldersOnly() {
        assertTrue(
            ExcludedMenuPolicy.shouldAddNoMediaOnEncrypt(
                folderName = ".thumbnails",
                decryptedName = null,
                hasNoMediaFile = false
            )
        )
        assertTrue(
            ExcludedMenuPolicy.shouldAddNoMediaOnEncrypt(
                folderName = ".fvault_abc123",
                decryptedName = ".hiddenAlbum",
                hasNoMediaFile = false
            )
        )
        assertTrue(
            ExcludedMenuPolicy.shouldAddNoMediaOnEncrypt(
                folderName = "PrivateDocs",
                decryptedName = null,
                hasNoMediaFile = true
            )
        )
        assertTrue(
            ExcludedMenuPolicy.shouldAddNoMediaOnEncrypt(
                folderName = "Subfolder",
                decryptedName = null,
                hasNoMediaFile = false,
                isPathOrParentHidden = true
            )
        )
        assertFalse(
            ExcludedMenuPolicy.shouldAddNoMediaOnEncrypt(
                folderName = "Camera",
                decryptedName = "Camera",
                hasNoMediaFile = false,
                isPathOrParentHidden = false
            )
        )
    }

    @Test
    fun isEncryptedAlbumExcludedIdentifiesExcludedAndSystemAlbumsCorrectly() {
        val vaultRoot = "/storage/emulated/0/Fossify Gallery Cipher/Cipher_Secrets"
        val normalAlbum = "$vaultRoot/.fvault_1111"
        val noMediaAlbum = "$vaultRoot/.fvault_2222"
        val dotDecryptedAlbum = "$vaultRoot/.fvault_3333"
        val configExcludedAlbum = "$vaultRoot/.fvault_4444"
        val recycleBin = "$vaultRoot/.fvault_recycle"
        val defaultAlbum = "$vaultRoot/.fvault_default"

        assertFalse(
            ExcludedMenuPolicy.isEncryptedAlbumExcluded(
                albumPath = normalAlbum,
                folderNameOnDisk = ".fvault_1111",
                decryptedName = "Vacation",
                hasNoMediaFile = false,
                isVaultRecycleBin = false,
                isDefaultAlbum = false,
                excludedFolders = emptySet()
            )
        )

        assertTrue(
            ExcludedMenuPolicy.isEncryptedAlbumExcluded(
                albumPath = noMediaAlbum,
                folderNameOnDisk = ".fvault_2222",
                decryptedName = "Receipts",
                hasNoMediaFile = true,
                isVaultRecycleBin = false,
                isDefaultAlbum = false,
                excludedFolders = emptySet()
            )
        )

        assertTrue(
            ExcludedMenuPolicy.isEncryptedAlbumExcluded(
                albumPath = dotDecryptedAlbum,
                folderNameOnDisk = ".fvault_3333",
                decryptedName = ".thumbnails",
                hasNoMediaFile = false,
                isVaultRecycleBin = false,
                isDefaultAlbum = false,
                excludedFolders = emptySet()
            )
        )

        assertTrue(
            ExcludedMenuPolicy.isEncryptedAlbumExcluded(
                albumPath = "$configExcludedAlbum/",
                folderNameOnDisk = ".fvault_4444",
                decryptedName = "Work",
                hasNoMediaFile = false,
                isVaultRecycleBin = false,
                isDefaultAlbum = false,
                excludedFolders = setOf(configExcludedAlbum)
            )
        )

        assertFalse(
            ExcludedMenuPolicy.isEncryptedAlbumExcluded(
                albumPath = recycleBin,
                folderNameOnDisk = ".fvault_recycle",
                decryptedName = "Recycle Bin",
                hasNoMediaFile = true,
                isVaultRecycleBin = true,
                isDefaultAlbum = false,
                excludedFolders = setOf(recycleBin)
            )
        )

        assertFalse(
            ExcludedMenuPolicy.isEncryptedAlbumExcluded(
                albumPath = defaultAlbum,
                folderNameOnDisk = ".fvault_default",
                decryptedName = "Default Secret Album",
                hasNoMediaFile = true,
                isVaultRecycleBin = false,
                isDefaultAlbum = true,
                excludedFolders = setOf(defaultAlbum)
            )
        )
    }
}
