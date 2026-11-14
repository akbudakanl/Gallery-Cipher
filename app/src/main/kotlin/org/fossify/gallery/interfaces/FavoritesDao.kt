package org.fossify.gallery.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.gallery.models.Favorite

@Dao
interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favorite: Favorite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favorites: List<Favorite>)

    @Query("SELECT * FROM favorites")
    fun getAll(): List<Favorite>

    @Query("SELECT * FROM favorites WHERE full_path = :path COLLATE NOCASE LIMIT 1")
    fun getFavorite(path: String): Favorite?

    /**
     * The paths of all favorites that were not deleted.
     *
     * The media table only holds the non-hidden files, so a LEFT JOIN is used: a favorite pointing
     * at a file in a hidden folder must stay in the list instead of disappearing from it. Whether
     * the file is really there is checked on disk afterwards.
     */
    @Query(
        """
        SELECT favorites.full_path FROM favorites
         LEFT JOIN media ON favorites.full_path = media.full_path
         WHERE media.deleted_ts IS NULL OR media.deleted_ts = 0
        """
    )
    fun getValidFavoritePaths(): List<String>

    @Query("SELECT id FROM favorites WHERE full_path = :path COLLATE NOCASE")
    fun isFavorite(path: String): Boolean

    @Query("UPDATE OR REPLACE favorites SET filename = :newFilename, full_path = :newFullPath, parent_path = :newParentPath WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateFavorite(newFilename: String, newFullPath: String, newParentPath: String, oldPath: String)

    @Query("DELETE FROM favorites WHERE full_path = :path COLLATE NOCASE")
    fun deleteFavoritePath(path: String)

    @Query("DELETE FROM favorites WHERE full_path LIKE :pathPrefix || '%' COLLATE NOCASE")
    fun deleteFavoritesByPrefix(pathPrefix: String)

    @Query("DELETE FROM favorites")
    fun clearFavorites()

    /**
     * Rewrites every favorite below [oldRoot] so it points below [newRoot]. Used when a physical
     * folder is renamed or moved, so the favorites survive the operation.
     */
    @Query(
        """
        UPDATE OR REPLACE favorites
         SET full_path = :newRoot || substr(full_path, length(:oldRoot) + 1),
             parent_path = :newRoot || substr(parent_path, length(:oldRoot) + 1)
         WHERE instr(lower(full_path), lower(:oldRoot) || '/') = 1
        """
    )
    fun updateFavoritesByFolderPrefix(oldRoot: String, newRoot: String)

    /**
     * The favorites whose parent folder is the renamed folder itself.
     */
    @Query(
        """
        UPDATE OR REPLACE favorites
         SET parent_path = :newRoot
         WHERE lower(parent_path) = lower(:oldRoot)
        """
    )
    fun updateFavoriteParentsAtFolder(oldRoot: String, newRoot: String)

    /** Same as [updateFavoritesByFolderPrefix] but only for the parent folder of the entries. */
    @Query(
        """
        UPDATE OR REPLACE favorites
         SET parent_path = :newRoot || substr(parent_path, length(:oldRoot) + 1)
         WHERE instr(lower(parent_path), lower(:oldRoot) || '/') = 1
        """
    )
    fun updateFavoriteParentsByFolderPrefix(oldRoot: String, newRoot: String)

    /** Stores the identity of a favorite so it can be found again after a rename or move. */
    @Query(
        """
        UPDATE OR REPLACE favorites
         SET media_store_id = CASE WHEN :mediaStoreId > 0 THEN :mediaStoreId ELSE media_store_id END,
             file_size = :fileSize,
             last_modified = :lastModified,
             fingerprint = :fingerprint,
             folder_inode = CASE WHEN :folderInode > 0 THEN :folderInode ELSE folder_inode END,
             folder_signature = CASE WHEN length(:folderSignature) > 0 THEN :folderSignature ELSE folder_signature END,
             captured_at_sec = CASE WHEN :capturedAtSec > 0 THEN :capturedAtSec ELSE captured_at_sec END
         WHERE full_path = :path COLLATE NOCASE
        """
    )
    fun updateFavoriteIdentity(
        path: String,
        mediaStoreId: Long,
        fileSize: Long,
        lastModified: Long,
        fingerprint: String,
        folderInode: Long = 0L,
        folderSignature: String = "",
        capturedAtSec: Long = 0L
    )
}
