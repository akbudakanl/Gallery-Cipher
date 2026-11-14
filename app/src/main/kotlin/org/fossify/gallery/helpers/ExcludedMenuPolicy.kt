package org.fossify.gallery.helpers

/**
 * Pure policy helpers for excluded folder detection, menu visibility, and `.nomedia` preservation
 * across normal gallery and encrypted vault scopes.
 */
object ExcludedMenuPolicy {
    data class Visibility(
        val showTemporarilyShowExcluded: Boolean,
        val showStopShowingExcluded: Boolean
    )

    fun resolve(hasExcludedInCurrentScope: Boolean, isCurrentlyShowingExcluded: Boolean): Visibility {
        if (!hasExcludedInCurrentScope) {
            return Visibility(showTemporarilyShowExcluded = false, showStopShowingExcluded = false)
        }
        return Visibility(
            showTemporarilyShowExcluded = !isCurrentlyShowingExcluded,
            showStopShowingExcluded = isCurrentlyShowingExcluded
        )
    }

    fun shouldAddNoMediaOnEncrypt(
        folderName: String,
        decryptedName: String?,
        hasNoMediaFile: Boolean,
        isPathOrParentHidden: Boolean = false
    ): Boolean {
        return folderName.startsWith('.') ||
            (decryptedName?.startsWith('.') == true) ||
            hasNoMediaFile ||
            isPathOrParentHidden
    }

    fun isEncryptedAlbumExcluded(
        albumPath: String,
        folderNameOnDisk: String,
        decryptedName: String?,
        hasNoMediaFile: Boolean,
        isVaultRecycleBin: Boolean,
        isDefaultAlbum: Boolean,
        excludedFolders: Set<String>
    ): Boolean {
        if (isVaultRecycleBin || isDefaultAlbum) {
            return false
        }
        if (hasNoMediaFile) {
            return true
        }
        val effectiveName = decryptedName ?: folderNameOnDisk.takeUnless { it.startsWith(ENCRYPTED_FOLDER_PREFIX) }
        if (effectiveName != null && effectiveName.startsWith('.')) {
            return true
        }
        val cleanPath = albumPath.trimEnd('/')
        return excludedFolders.any { excluded ->
            val cleanExcluded = excluded.trimEnd('/')
            cleanExcluded.isNotEmpty() &&
                (cleanPath.equals(cleanExcluded, ignoreCase = true) ||
                    "$cleanPath/".startsWith("$cleanExcluded/", ignoreCase = true))
        }
    }
}
