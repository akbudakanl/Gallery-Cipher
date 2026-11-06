package org.fossify.gallery.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @ColumnInfo(name = "added_ts") var addedTS: Long
)
