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

    @Query("DELETE FROM virtual_album_items WHERE full_path NOT IN (SELECT full_path FROM media WHERE deleted_ts = 0)")
    fun deleteOrphanedItems()
}
