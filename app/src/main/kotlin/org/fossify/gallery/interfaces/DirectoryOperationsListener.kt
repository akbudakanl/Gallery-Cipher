package org.fossify.gallery.interfaces

import org.fossify.gallery.models.Directory
import java.io.File

interface DirectoryOperationsListener {
    fun refreshItems()

    fun refreshPaths(paths: Collection<String>) {
        refreshItems()
    }

    fun deleteFolders(folders: ArrayList<File>)

    fun recheckPinnedFolders()

    fun updateDirectories(directories: ArrayList<Directory>)
}
