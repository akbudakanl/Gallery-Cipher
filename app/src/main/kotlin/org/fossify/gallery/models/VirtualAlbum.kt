package org.fossify.gallery.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user created virtual album. It owns no files: its content is a list of path references
 * stored in [VirtualAlbumItem], so adding a file to any number of albums never duplicates it on disk.
 */
@Entity(tableName = "virtual_albums")
data class VirtualAlbum(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "created_ts") var createdTS: Long
)
