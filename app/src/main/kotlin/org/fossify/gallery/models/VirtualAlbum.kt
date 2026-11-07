package org.fossify.gallery.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "virtual_albums",
    indices = [Index(value = ["name"], unique = true)]
)
data class VirtualAlbum(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "created_ts") var createdTs: Long = System.currentTimeMillis() / 1000L,
    @ColumnInfo(name = "thumbnail") var thumbnail: String = "",
    @ColumnInfo(name = "is_hidden") var isHidden: Boolean = false,
    @ColumnInfo(name = "parent_physical_path") var parentPhysicalPath: String = ""
)
