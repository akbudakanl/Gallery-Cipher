package org.fossify.gallery.helpers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getParentPath
import org.fossify.gallery.extensions.favoritesDB
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.extensions.virtualAlbumsDB
import java.io.File

/**
 * Keeps a copy of the favorites and the virtual albums in the Cipher folder on shared storage.
 *
 * The app database lives in the internal storage of the app, so it is gone when the user clears
 * the app data. The backups live next to it in the `Fossify Gallery Cipher` folder, outside of the
 * app sandbox, and are restored when the app starts without any data.
 *
 * The backup is only written while the app may access the shared storage. Without that permission
 * the folder cannot be read or written, and nothing is attempted.
 */
object ReferenceBackupManager {
    internal const val FAVORITES_DIR_NAME = "Favorites"
    internal const val FAVORITES_FILE_NAME = "favorites.json"

    /** The backup format version, bumped when the structure changes. */
    private const val BACKUP_VERSION = 1

    fun buildFavoritesBackup(favorites: List<org.fossify.gallery.models.Favorite>): FavoritesBackup {
        return FavoritesBackup(
            version = BACKUP_VERSION,
            favorites = favorites.map { favorite ->
                FavoriteBackup(
                    fullPath = favorite.fullPath,
                    filename = favorite.filename,
                    parentPath = favorite.parentPath,
                    mediaStoreId = favorite.mediaStoreId,
                    fileSize = favorite.fileSize,
                    lastModified = favorite.lastModified,
                    fingerprint = favorite.fingerprint,
                    folderInode = favorite.folderInode,
                    folderSignature = favorite.folderSignature.ifEmpty { null },
                    capturedAtSec = favorite.capturedAtSec.takeIf { it > 0L }
                )
            }
        )
    }

    data class FavoritesBackup(
        @SerializedName("version") val version: Int = BACKUP_VERSION,
        @SerializedName("favorites") val favorites: List<FavoriteBackup>? = null
    )

    data class FavoriteBackup(
        @SerializedName("fullPath") val fullPath: String? = null,
        @SerializedName("filename") val filename: String? = null,
        @SerializedName("parentPath") val parentPath: String? = null,
        @SerializedName("mediaStoreId") val mediaStoreId: Long? = null,
        @SerializedName("fileSize") val fileSize: Long? = null,
        @SerializedName("lastModified") val lastModified: Long? = null,
        @SerializedName("fingerprint") val fingerprint: String? = null,
        @SerializedName("folderInode") val folderInode: Long? = null,
        @SerializedName("folderSignature") val folderSignature: String? = null,
        @SerializedName("capturedAtSec") val capturedAtSec: Long? = null
    )

    /**
     * The folder the backups are written to. It lives inside the Cipher folder so it stays
     * together with the vault and the virtual album saves.
     */
    fun getFavoritesBackupDir(): File {
        return File(CryptoManager.getCipherRootDirectory(), FAVORITES_DIR_NAME)
    }

    fun getFavoritesBackupFile(): File {
        return File(getFavoritesBackupDir(), FAVORITES_FILE_NAME)
    }

    /**
     * The backup of the older versions, it lived in the Cipher folder inside the Documents folder.
     */
    private fun getLegacyFavoritesBackupFile(): File {
        return File(
            CryptoManager.getDocumentsDirectory(),
            "$CIPHER_ROOT_DIR_NAME/$FAVORITES_DIR_NAME/$FAVORITES_FILE_NAME"
        )
    }

    /**
     * Moves the backup of the older versions into the new folder, so the favorites of the user
     * survive the update to the standalone container.
     */
    private fun migrateLegacyBackup(): File? {
        val legacyFile = getLegacyFavoritesBackupFile()
        if (!legacyFile.isFile) {
            return null
        }

        return try {
            CryptoManager.ensureCipherRootDirectory()
            val target = getFavoritesBackupFile()
            if (!target.exists()) {
                val dir = getFavoritesBackupDir()
                if (!dir.exists() && !dir.mkdirs()) {
                    return null
                }
                legacyFile.copyTo(target, overwrite = true)
            }
            legacyFile.delete()
            target.takeIf { it.isFile }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes the current favorites to the backup file. Does nothing when the shared storage cannot
     * be accessed.
     */
    fun saveFavorites(context: Context) {
        if (!canAccessSharedStorage(context)) {
            return
        }

        try {
            val favorites = context.favoritesDB.getAll()
            val backup = buildFavoritesBackup(favorites)

            CryptoManager.ensureCipherRootDirectory()

            val dir = getFavoritesBackupDir()
            if (!dir.exists() && !dir.mkdirs()) {
                return
            }

            val target = getFavoritesBackupFile()
            val temp = File(dir, "${target.name}.${System.currentTimeMillis()}.tmp")
            temp.writeText(GsonBuilder().setPrettyPrinting().create().toJson(backup), Charsets.UTF_8)
            if (temp.exists()) {
                if (target.exists()) {
                    target.delete()
                }
                temp.renameTo(target)
            }
        } catch (_: Exception) {
            // A failed backup must never crash the app.
        }
    }

    /**
     * Restores the favorites from the backup file. Only entries whose file still exists are
     * restored, so a stale backup cannot resurrect deleted files.
     *
     * @return how many favorites were restored
     */
    fun restoreFavorites(context: Context): Int {
        if (!canAccessSharedStorage(context)) {
            return 0
        }

        val file = getFavoritesBackupFile().takeIf { it.isFile }
            ?: migrateLegacyBackup()
            ?: return 0

        return try {
            val content = file.readText(Charsets.UTF_8).trim()
            if (content.isEmpty()) {
                return 0
            }

            val backup = Gson().fromJson(content, FavoritesBackup::class.java) ?: return 0
            val entries = backup.favorites ?: return 0

            var restored = 0
            for (entry in entries) {
                val path = entry.fullPath ?: continue
                if (!File(path).exists()) {
                    continue
                }

                try {
                    val existing = context.favoritesDB.getFavorite(path)
                    if (existing == null) {
                        val identity = MediaIdentity.ofWithFolder(
                            context,
                            File(path),
                            entry.mediaStoreId ?: 0L
                        )
                        context.favoritesDB.insert(
                            org.fossify.gallery.models.Favorite(
                                id = null,
                                fullPath = path,
                                filename = entry.filename ?: path.getFilenameFromPath(),
                                parentPath = entry.parentPath ?: path.getParentPath(),
                                mediaStoreId = identity.mediaStoreId,
                                fileSize = if (identity.size > 0) identity.size else entry.fileSize ?: 0L,
                                lastModified = if (identity.lastModified > 0) identity.lastModified else entry.lastModified ?: 0L,
                                fingerprint = if (identity.hasFingerprint) identity.fingerprint else entry.fingerprint ?: "",
                                folderInode = if (identity.folderInode > 0L) identity.folderInode else entry.folderInode ?: 0L,
                                folderSignature = identity.folderSignature.ifEmpty { entry.folderSignature ?: "" },
                                capturedAtSec = entry.capturedAtSec ?: identity.capturedAtSec
                            )
                        )
                        try {
                            context.mediaDB.updateFavorite(path, true)
                        } catch (_: Exception) {
                        }
                        restored++
                    } else {
                        // The favorite exists, but the backup may know its identity better.
                        if ((existing.fingerprint.isEmpty() && !entry.fingerprint.isNullOrEmpty()) ||
                            (existing.folderSignature.isEmpty() && !entry.folderSignature.isNullOrEmpty()) ||
                            (existing.folderInode == 0L && (entry.folderInode ?: 0L) > 0L)
                        ) {
                            context.favoritesDB.updateFavoriteIdentity(
                                path = path,
                                mediaStoreId = entry.mediaStoreId ?: existing.mediaStoreId,
                                fileSize = entry.fileSize ?: existing.fileSize,
                                lastModified = entry.lastModified ?: existing.lastModified,
                                fingerprint = entry.fingerprint?.ifEmpty { existing.fingerprint } ?: existing.fingerprint,
                                folderInode = entry.folderInode ?: existing.folderInode,
                                folderSignature = entry.folderSignature ?: existing.folderSignature,
                                capturedAtSec = entry.capturedAtSec ?: existing.capturedAtSec
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }

            restored
        } catch (_: Exception) {
            0
        }
    }

    /**
     * The shared storage can only be read and written while the app has the permission for it.
     */
    private fun canAccessSharedStorage(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
