package org.fossify.gallery.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.fossify.gallery.helpers.MediaIdentity

/** The item points at a file that was found. */
const val ITEM_STATE_OK = "OK"

/** The file of the item could not be found at the cached path. */
const val ITEM_STATE_UNRESOLVED = "UNRESOLVED"

/**
 * An item of a virtual album is a reference to a file, not a copy of it.
 *
 * The path is only a cache of where the file currently lives. The identity columns describe the
 * file itself and survive renames and moves, which is what keeps the item valid.
 */
@Entity(
    tableName = "virtual_album_items",
    foreignKeys = [
        ForeignKey(
            entity = VirtualAlbum::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["album_id", "full_path"], unique = true),
        Index(value = ["full_path"])
    ]
)
data class VirtualAlbumItem(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "album_id") var albumId: Long,
    @ColumnInfo(name = "full_path") var fullPath: String,
    @ColumnInfo(name = "filename") var filename: String,
    @ColumnInfo(name = "parent_path") var parentPath: String,
    @ColumnInfo(name = "added_ts") var addedTS: Long,
    @ColumnInfo(name = "media_store_id") var mediaStoreId: Long = 0L,
    @ColumnInfo(name = "file_size") var fileSize: Long = 0L,
    @ColumnInfo(name = "last_modified") var lastModified: Long = 0L,
    @ColumnInfo(name = "fingerprint") var fingerprint: String = "",
    /** OK while the file was found, UNRESOLVED when it could not be located. */
    @ColumnInfo(name = "state") var state: String = ITEM_STATE_OK
) {
    fun toIdentity(): MediaIdentity {
        return MediaIdentity(
            mediaStoreId = mediaStoreId,
            size = fileSize,
            lastModified = lastModified,
            fingerprint = fingerprint
        )
    }
}
