package org.fossify.gallery.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.VirtualAlbum
import org.fossify.gallery.models.VirtualAlbumItem

@Dao
interface VirtualAlbumsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAlbum(album: VirtualAlbum): Long

    @Query("SELECT * FROM virtual_albums ORDER BY created_ts DESC")
    fun getAllAlbums(): List<VirtualAlbum>

    @Query("SELECT * FROM virtual_albums WHERE id = :id LIMIT 1")
    fun getAlbumById(id: Long): VirtualAlbum?

    @Query("SELECT * FROM virtual_albums WHERE name = :name COLLATE NOCASE LIMIT 1")
    fun getAlbumByName(name: String): VirtualAlbum?

    @Query("UPDATE virtual_albums SET name = :name WHERE id = :id")
    fun renameAlbum(id: Long, name: String)

    @Query("UPDATE virtual_albums SET is_hidden = :isHidden, name = :name WHERE id = :id")
    fun updateAlbumVisibilityAndName(id: Long, isHidden: Boolean, name: String)

    @Query("UPDATE virtual_albums SET thumbnail = :thumbnail WHERE id = :id")
    fun updateAlbumThumbnail(id: Long, thumbnail: String)

    @Query("UPDATE virtual_albums SET parent_physical_path = :parentPhysicalPath WHERE id = :id")
    fun updateAlbumParentPhysicalPath(id: Long, parentPhysicalPath: String)

    /**
     * Rewrites the remembered source folder of the albums below [oldRoot] so it points below
     * [newRoot]. Used when a physical folder is renamed or moved.
     */
    @Query(
        """
        UPDATE virtual_albums SET parent_physical_path = :newRoot
         WHERE instr(lower(parent_physical_path), lower(:oldRoot) || '/') = 1
        """
    )
    fun updateAlbumParentPhysicalPathByPrefix(oldRoot: String, newRoot: String)

    /**
     * Same as [updateAlbumParentPhysicalPathByPrefix] for the albums whose source folder is the
     * renamed folder itself.
     */
    @Query(
        """
        UPDATE virtual_albums SET parent_physical_path = :newRoot
         WHERE lower(parent_physical_path) = lower(:oldRoot)
        """
    )
    fun updateAlbumParentPhysicalPathAtFolder(oldRoot: String, newRoot: String)

    @Query("SELECT * FROM virtual_albums WHERE parent_physical_path = :parentPhysicalPath COLLATE NOCASE")
    fun getAlbumsByParentPhysicalPath(parentPhysicalPath: String): List<VirtualAlbum>

    @Query("DELETE FROM virtual_albums WHERE id = :id")
    fun deleteAlbumById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(item: VirtualAlbumItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItems(items: List<VirtualAlbumItem>): List<Long>

    @Query("DELETE FROM virtual_album_items WHERE album_id = :albumId AND full_path = :fullPath COLLATE NOCASE")
    fun removeItemFromAlbum(albumId: Long, fullPath: String)

    @Query("DELETE FROM virtual_album_items WHERE full_path = :fullPath COLLATE NOCASE")
    fun removeItemFromAllAlbums(fullPath: String)

    @Query("SELECT DISTINCT album_id FROM virtual_album_items WHERE full_path = :fullPath COLLATE NOCASE")
    fun getAlbumIdsWithItem(fullPath: String): List<Long>

    @Query("UPDATE OR REPLACE virtual_album_items SET full_path = :newPath, filename = :newFilename, parent_path = :newParentPath WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateItemPath(oldPath: String, newPath: String, newFilename: String, newParentPath: String)

    @Query("SELECT * FROM virtual_album_items WHERE album_id = :albumId ORDER BY added_ts DESC")
    fun getItems(albumId: Long): List<VirtualAlbumItem>

    @Query("SELECT * FROM virtual_album_items")
    fun getAllItems(): List<VirtualAlbumItem>

    @Query("SELECT virtual_album_items.full_path FROM virtual_album_items INNER JOIN media ON virtual_album_items.full_path = media.full_path WHERE virtual_album_items.album_id = :albumId AND media.deleted_ts != 0")
    fun getDeletedPathsForAlbum(albumId: Long): List<String>

    @Query("SELECT virtual_album_items.full_path FROM virtual_album_items INNER JOIN media ON virtual_album_items.full_path = media.full_path WHERE virtual_album_items.album_id = :albumId AND media.deleted_ts = 0 ORDER BY virtual_album_items.added_ts DESC")
    fun getValidPaths(albumId: Long): List<String>

    @Query("SELECT media.* FROM media INNER JOIN virtual_album_items ON media.full_path = virtual_album_items.full_path WHERE virtual_album_items.album_id = :albumId AND media.deleted_ts = 0 ORDER BY virtual_album_items.added_ts DESC")
    fun getValidMedia(albumId: Long): List<Medium>

    @Query("SELECT COUNT(*) FROM virtual_album_items INNER JOIN media ON virtual_album_items.full_path = media.full_path WHERE virtual_album_items.album_id = :albumId AND media.deleted_ts = 0")
    fun getValidItemCount(albumId: Long): Int

    /**
     * Rewrites every item below [oldRoot] so it points below [newRoot]. Used when a physical
     * folder is renamed or moved, so the album items survive the operation.
     *
     * The filename is left alone: it holds the name of the file, not a path.
     */
    @Query(
        """
        UPDATE OR REPLACE virtual_album_items
         SET full_path = :newRoot || substr(full_path, length(:oldRoot) + 1),
             parent_path = :newRoot || substr(parent_path, length(:oldRoot) + 1)
         WHERE instr(lower(full_path), lower(:oldRoot) || '/') = 1
        """
    )
    fun updateItemsByFolderPrefix(oldRoot: String, newRoot: String)

    /**
     * Same as [updateItemsByFolderPrefix] but only for the parent folder of the entries.
     *
     * The parent folder may be the renamed folder itself, so an exact match is handled as well.
     */
    @Query(
        """
        UPDATE OR REPLACE virtual_album_items
         SET parent_path = :newRoot
         WHERE lower(parent_path) = lower(:oldRoot)
        """
    )
    fun updateItemParentsAtFolder(oldRoot: String, newRoot: String)

    /** Same as [updateItemsByFolderPrefix] but only for the parent folder of the entries. */
    @Query(
        """
        UPDATE OR REPLACE virtual_album_items
         SET parent_path = :newRoot || substr(parent_path, length(:oldRoot) + 1)
         WHERE instr(lower(parent_path), lower(:oldRoot) || '/') = 1
        """
    )
    fun updateItemParentsByFolderPrefix(oldRoot: String, newRoot: String)

    /** Stores the identity of an item so it can be found again after a rename or move. */
    @Query(
        """
        UPDATE OR REPLACE virtual_album_items
         SET media_store_id = :mediaStoreId, file_size = :fileSize,
             last_modified = :lastModified, fingerprint = :fingerprint
         WHERE full_path = :path COLLATE NOCASE
        """
    )
    fun updateItemIdentity(
        path: String,
        mediaStoreId: Long,
        fileSize: Long,
        lastModified: Long,
        fingerprint: String
    )

    /** Returns the items whose file could not be found at the cached path. */
    @Query("SELECT * FROM virtual_album_items WHERE album_id = :albumId AND full_path = :fullPath COLLATE NOCASE")
    fun getItemsWithPath(albumId: Long, fullPath: String): List<VirtualAlbumItem>

    /**
     * Marks an item as unresolved. The row is kept, because it still holds the identity that is
     * needed to find the file again after a rename or a move.
     */
    @Query(
        """
        UPDATE virtual_album_items SET state = 'UNRESOLVED'
         WHERE album_id = :albumId AND full_path = :fullPath COLLATE NOCASE
        """
    )
    fun markItemUnresolved(albumId: Long, fullPath: String)

    /** Marks an item as resolved again after it was found at a new location. */
    @Query(
        """
        UPDATE virtual_album_items SET state = 'OK'
         WHERE album_id = :albumId AND full_path = :fullPath COLLATE NOCASE
        """
    )
    fun markItemResolved(albumId: Long, fullPath: String)

    /** The items of an album whose file could not be found. */
    @Query("SELECT * FROM virtual_album_items WHERE album_id = :albumId AND state = 'UNRESOLVED'")
    fun getUnresolvedItems(albumId: Long): List<VirtualAlbumItem>
}
