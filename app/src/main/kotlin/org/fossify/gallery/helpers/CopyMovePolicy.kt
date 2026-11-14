package org.fossify.gallery.helpers

import org.fossify.commons.helpers.FAVORITES
import org.fossify.gallery.extensions.isVirtualAlbumPath

enum class PickDirectoryMode {
    DEFAULT,
    UNENCRYPTED_COPY,
    UNENCRYPTED_MOVE,
    ENCRYPTED_COPY,
    ENCRYPTED_MOVE,
    DECRYPT_MOVE
}

/**
 * Centralizes visibility and destination filtering rules for Copy to, Move to, Encrypt, and Decrypt.
 */
object CopyMovePolicy {
    fun isVirtualOrFavoritesPath(directoryPath: String): Boolean {
        return directoryPath == FAVORITES || directoryPath.isVirtualAlbumPath()
    }

    fun isRealAlbumSource(
        directoryPath: String,
        isInRecycleBin: Boolean = false,
        isVaultRecycleBin: Boolean = false
    ): Boolean {
        return !isVirtualOrFavoritesPath(directoryPath) &&
            directoryPath != RECYCLE_BIN &&
            !isInRecycleBin &&
            !isVaultRecycleBin
    }

    fun canMoveFrom(
        directoryPath: String,
        isInRecycleBin: Boolean = false,
        isVaultRecycleBin: Boolean = false
    ): Boolean {
        return isRealAlbumSource(
            directoryPath = directoryPath,
            isInRecycleBin = isInRecycleBin,
            isVaultRecycleBin = isVaultRecycleBin
        )
    }

    fun canEncryptFrom(
        directoryPath: String,
        hasEncryptedSelection: Boolean,
        isInRecycleBin: Boolean = false,
        isVaultRecycleBin: Boolean = false
    ): Boolean {
        return !hasEncryptedSelection &&
            isRealAlbumSource(
                directoryPath = directoryPath,
                isInRecycleBin = isInRecycleBin,
                isVaultRecycleBin = isVaultRecycleBin
            )
    }

    fun canDecryptFrom(
        directoryPath: String,
        hasHiddenOrEncryptedSelection: Boolean,
        isInRecycleBin: Boolean = false,
        isVaultRecycleBin: Boolean = false
    ): Boolean {
        return hasHiddenOrEncryptedSelection &&
            isRealAlbumSource(
                directoryPath = directoryPath,
                isInRecycleBin = isInRecycleBin,
                isVaultRecycleBin = isVaultRecycleBin
            )
    }

    fun resolveCopyMoveMode(isCopyOperation: Boolean, isEncryptedSource: Boolean): PickDirectoryMode {
        return when {
            isEncryptedSource && isCopyOperation -> PickDirectoryMode.ENCRYPTED_COPY
            isEncryptedSource && !isCopyOperation -> PickDirectoryMode.ENCRYPTED_MOVE
            !isEncryptedSource && isCopyOperation -> PickDirectoryMode.UNENCRYPTED_COPY
            else -> PickDirectoryMode.UNENCRYPTED_MOVE
        }
    }

    fun isCopyMode(mode: PickDirectoryMode): Boolean {
        return mode == PickDirectoryMode.UNENCRYPTED_COPY || mode == PickDirectoryMode.ENCRYPTED_COPY
    }

    fun isEncryptedTargetMode(mode: PickDirectoryMode): Boolean {
        return mode == PickDirectoryMode.ENCRYPTED_COPY || mode == PickDirectoryMode.ENCRYPTED_MOVE
    }

    fun isAllowedDestination(
        mode: PickDirectoryMode,
        isVirtualAlbum: Boolean,
        isEncryptedAlbum: Boolean,
        isRecycleBin: Boolean,
        isFavorites: Boolean
    ): Boolean {
        if (isRecycleBin || isFavorites) {
            return false
        }

        return when (mode) {
            PickDirectoryMode.DEFAULT -> true
            PickDirectoryMode.UNENCRYPTED_COPY -> !isEncryptedAlbum
            PickDirectoryMode.UNENCRYPTED_MOVE -> !isEncryptedAlbum && !isVirtualAlbum
            PickDirectoryMode.ENCRYPTED_COPY -> isEncryptedAlbum
            PickDirectoryMode.ENCRYPTED_MOVE -> isEncryptedAlbum && !isVirtualAlbum
            PickDirectoryMode.DECRYPT_MOVE -> !isEncryptedAlbum && !isVirtualAlbum
        }
    }
}
