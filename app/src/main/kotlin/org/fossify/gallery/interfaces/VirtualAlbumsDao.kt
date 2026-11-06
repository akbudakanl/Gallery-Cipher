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
    @Insert
    fun insertAlbum(album: VirtualAlbum): Long

    @Query("SELECT * FROM virtual_albums ORDER BY created_ts ASC")
    fun getAlbums(): List<VirtualAlbum>

    @Query("SELECT * FROM virtual_albums WHERE id = :id")
    fun getAlbum(id: Long): VirtualAlbum?

    @Query("SELECT * FROM virtual_albums WHERE name = :name COLLATE NOCASE LIMIT 1")
    fun getAlbumByName(name: String): VirtualAlbum?

    @Query("UPDATE virtual_albums SET name = :name WHERE id = :id")
    fun renameAlbum(id: Long, name: String)

    // album items are removed by the ON DELETE CASCADE foreign key
    @Query("DELETE FROM virtual_albums WHERE id = :id")
    fun deleteAlbum(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItems(items: List<VirtualAlbumItem>)

    @Query("DELETE FROM virtual_album_items WHERE album_id = :albumId AND full_path = :path COLLATE NOCASE")
    fun deleteItem(albumId: Long, path: String)

    @Query("DELETE FROM virtual_album_items WHERE full_path = :path COLLATE NOCASE")
    fun deleteItemFromAllAlbums(path: String)

    @Query("SELECT COUNT(*) FROM virtual_album_items WHERE album_id = :albumId AND full_path = :path COLLATE NOCASE")
    fun isInAlbum(albumId: Long, path: String): Int

    @Query("UPDATE OR REPLACE virtual_album_items SET filename = :newFilename, full_path = :newFullPath, parent_path = :newParentPath WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateItemPath(newFilename: String, newFullPath: String, newParentPath: String, oldPath: String)

    /**
     * Paths of the album items that still exist in the media cache and are not in the recycle bin.
     * Moving a file to the recycle bin changes its media path, so the item disappears from the album
     * and shows up again if the file gets restored.
     */
    @Query(
        "SELECT virtual_album_items.full_path FROM virtual_album_items " +
            "INNER JOIN media ON virtual_album_items.full_path = media.full_path " +
            "WHERE virtual_album_items.album_id = :albumId AND media.deleted_ts = 0 " +
            "ORDER BY virtual_album_items.added_ts DESC"
    )
    fun getValidPaths(albumId: Long): List<String>

    @Query(
        "SELECT media.filename, media.full_path, media.parent_path, media.last_modified, media.date_taken, media.size, " +
            "media.type, media.video_duration, media.is_favorite, media.deleted_ts, media.media_store_id FROM media " +
            "INNER JOIN virtual_album_items ON virtual_album_items.full_path = media.full_path " +
            "WHERE virtual_album_items.album_id = :albumId AND media.deleted_ts = 0"
    )
    fun getValidMedia(albumId: Long): List<Medium>

    @Query("SELECT COUNT(*) FROM virtual_album_items WHERE album_id = :albumId")
    fun getItemCount(albumId: Long): Int

    @Query("SELECT COUNT(*) FROM media WHERE full_path = :path COLLATE NOCASE")
    fun isMediaCached(path: String): Int
}
