package org.fossify.gallery.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A favorite is a reference to a file, not a copy of it.
 *
 * The path is only a cache of where the file currently lives. The identity columns describe the
 * file itself and survive renames and moves, which is what keeps the favorite valid.
 */
@Entity(tableName = "favorites", indices = [Index(value = ["full_path"], unique = true)])
data class Favorite(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    @ColumnInfo(name = "full_path") var fullPath: String,
    @ColumnInfo(name = "filename") var filename: String,
    @ColumnInfo(name = "parent_path") var parentPath: String,
    @ColumnInfo(name = "media_store_id") var mediaStoreId: Long = 0L,
    @ColumnInfo(name = "file_size") var fileSize: Long = 0L,
    @ColumnInfo(name = "last_modified") var lastModified: Long = 0L,
    @ColumnInfo(name = "fingerprint") var fingerprint: String = "",
    @ColumnInfo(name = "folder_inode") var folderInode: Long = 0L,
    @ColumnInfo(name = "folder_signature") var folderSignature: String = "",
    @ColumnInfo(name = "captured_at_sec") var capturedAtSec: Long = 0L
) {
    fun toIdentity(): org.fossify.gallery.helpers.MediaIdentity {
        return org.fossify.gallery.helpers.MediaIdentity(
            mediaStoreId = mediaStoreId,
            size = fileSize,
            lastModified = lastModified,
            fingerprint = fingerprint,
            folderInode = folderInode,
            folderSignature = folderSignature,
            capturedAtSec = capturedAtSec
        )
    }
}
