package org.fossify.gallery.dialogs

import android.graphics.Color
import android.os.Environment
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.models.RadioItem
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beInvisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getDefaultCopyDestinationPath
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.handleLockedFolderOpening
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.internalStoragePath
import org.fossify.commons.extensions.isGone
import org.fossify.commons.extensions.isInDownloadDir
import org.fossify.commons.extensions.isMediaFile
import org.fossify.commons.extensions.isRestrictedWithSAFSdk30
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.VIEW_TYPE_GRID
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.commons.views.MySearchMenu
import org.fossify.gallery.R
import org.fossify.gallery.adapters.DirectoryAdapter
import org.fossify.gallery.databinding.DialogDirectoryPickerBinding
import org.fossify.gallery.extensions.*
import org.fossify.gallery.helpers.CopyMovePolicy
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.LOCATION_INTERNAL
import org.fossify.gallery.helpers.PickDirectoryMode
import org.fossify.gallery.helpers.TYPE_IMAGES
import org.fossify.gallery.helpers.TYPE_VIDEOS
import org.fossify.gallery.models.Directory
import java.io.File

class PickDirectoryDialog(
    val activity: BaseSimpleActivity,
    val sourcePath: String,
    showOtherFolderButton: Boolean,
    val showFavoritesBin: Boolean,
    val isPickingCopyMoveDestination: Boolean,
    val isPickingFolderForWidget: Boolean,
    val isCopyOperation: Boolean = false,
    val isHiddenCopySource: Boolean = false,
    val mode: PickDirectoryMode = PickDirectoryMode.DEFAULT,
    val callback: (path: String) -> Unit
) {
    private val effectiveMode = if (mode != PickDirectoryMode.DEFAULT) {
        mode
    } else if (isPickingCopyMoveDestination) {
        CopyMovePolicy.resolveCopyMoveMode(isCopyOperation, isHiddenCopySource)
    } else {
        PickDirectoryMode.DEFAULT
    }

    private var dialog: AlertDialog? = null
    private var shownDirectories = ArrayList<Directory>()
    private var allDirectories = ArrayList<Directory>()
    private var openedSubfolders = arrayListOf("")
    private var hasLoadedDirectories = false
    private var binding = DialogDirectoryPickerBinding.inflate(activity.layoutInflater)
    private var isGridViewType = activity.config.viewTypeFolders == VIEW_TYPE_GRID
    private var showHidden = activity.config.shouldShowHidden
    private var currentPathPrefix = ""
    private val config = activity.config
    private val searchView = binding.folderSearchView
    private val searchEditText = searchView.binding.topToolbarSearch
    private val searchBarContainer = searchView.binding.searchBarContainer

    init {
        (binding.directoriesGrid.layoutManager as MyGridLayoutManager).apply {
            orientation = if (activity.config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) activity.config.dirColumnCnt else 1
        }

        binding.directoriesFastscroller.updateColors(activity.getProperPrimaryColor())

        configureSearchView()

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)

        if (showOtherFolderButton) {
            val neutralTextRes = if (effectiveMode == PickDirectoryMode.ENCRYPTED_MOVE) {
                R.string.new_hidden_album
            } else {
                R.string.other_folder
            }
            builder.setNeutralButton(neutralTextRes) { dialogInterface, i -> showOtherFolder() }
        }

        builder.apply {
            activity.setupDialogStuff(binding.root, this, org.fossify.commons.R.string.select_destination) { alertDialog ->
                dialog = alertDialog
                binding.directoriesShowHidden.beGone()

                alertDialog.onBackPressedDispatcher.addCallback(alertDialog) {
                    if (searchView.isSearchOpen) {
                        searchView.closeSearch()
                    } else if (activity.config.groupDirectSubfolders) {
                        if (currentPathPrefix.isEmpty()) {
                            isEnabled = false
                            alertDialog.onBackPressedDispatcher.onBackPressed()
                        } else {
                            openedSubfolders.removeAt(openedSubfolders.lastIndex)
                            currentPathPrefix = openedSubfolders.last()
                            gotDirectories(allDirectories)
                        }
                    } else {
                        isEnabled = false
                        alertDialog.onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }

        fetchDirectories(false)
    }

    private fun configureSearchView() = with(searchView) {
        updateHintText(context.getString(org.fossify.commons.R.string.search_folders))
        searchEditText.imeOptions = EditorInfo.IME_ACTION_DONE

        toggleHideOnScroll(!config.scrollHorizontally)
        setupMenu()
        setSearchViewListeners()
        updateSearchViewUi()
    }

    private fun MySearchMenu.updateSearchViewUi() {
        requireToolbar().beInvisible()
        updateColors()
        setBackgroundColor(Color.TRANSPARENT)
        searchBarContainer.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun MySearchMenu.setSearchViewListeners() {
        onSearchOpenListener = {
            updateSearchViewLeftIcon(org.fossify.commons.R.drawable.ic_cross_vector)
        }

        onSearchClosedListener = {
            searchEditText.clearFocus()
            activity.hideKeyboard(searchEditText)
            updateSearchViewLeftIcon(org.fossify.commons.R.drawable.ic_search_vector)
        }

        onSearchTextChangedListener = { text ->
            filterFolderListBySearchQuery(text)
        }
    }

    private fun updateSearchViewLeftIcon(iconResId: Int) = with(searchView.binding.topToolbarSearchIcon) {
        post {
            setImageResource(iconResId)
        }
    }

    private fun filterFolderListBySearchQuery(query: String) {
        val adapter = binding.directoriesGrid.adapter as? DirectoryAdapter
        var dirsToShow = allDirectories
        if (query.isNotEmpty()) {
            dirsToShow = dirsToShow.filter { it.name.contains(query, true) }.toMutableList() as ArrayList
        }
        dirsToShow = activity.getSortedDirectories(dirsToShow)
        checkPlaceholderVisibility(dirsToShow)

        val filteredFolderListUpdated = adapter?.dirs != dirsToShow
        if (filteredFolderListUpdated) {
            adapter?.updateDirs(dirsToShow)

            binding.directoriesGrid.apply {
                post {
                    scrollToPosition(0)
                }
            }
        }
    }

    private fun checkPlaceholderVisibility(dirs: ArrayList<Directory>) = with(binding) {
        directoriesEmptyPlaceholder.beVisibleIf(dirs.isEmpty())

        if (folderSearchView.isSearchOpen) {
            directoriesEmptyPlaceholder.text = root.context.getString(org.fossify.commons.R.string.no_items_found)
        }

        directoriesFastscroller.beVisibleIf(directoriesEmptyPlaceholder.isGone())
    }

    private fun isDirectoryAllowedForMode(dir: Directory): Boolean {
        if (!showFavoritesBin && (dir.isRecycleBin() || dir.areFavorites())) {
            return false
        }
        if (CryptoManager.isVaultRecycleBinPath(activity, dir.path)) {
            return false
        }
        if (effectiveMode == PickDirectoryMode.DEFAULT) {
            return true
        }

        val isVirtual = dir.isVirtualAlbum()
        val isEncrypted = if (isVirtual) {
            activity.isHiddenVirtualAlbumPath(dir.path)
        } else {
            CryptoManager.isEncryptedFolderPath(activity, dir.path) || activity.isEncryptedVaultAlbum(dir.path)
        }
        return CopyMovePolicy.isAllowedDestination(
            mode = effectiveMode,
            isVirtualAlbum = isVirtual,
            isEncryptedAlbum = isEncrypted,
            isRecycleBin = dir.isRecycleBin() || CryptoManager.isVaultRecycleBinPath(activity, dir.path),
            isFavorites = dir.areFavorites()
        )
    }

    private fun fetchDirectories(forceShowHiddenAndExcluded: Boolean) {
        val isEncryptedMode = CopyMovePolicy.isEncryptedTargetMode(effectiveMode)
        val includeVirtual = when (effectiveMode) {
            PickDirectoryMode.UNENCRYPTED_COPY, PickDirectoryMode.ENCRYPTED_COPY -> true
            PickDirectoryMode.DEFAULT -> isPickingCopyMoveDestination && isCopyOperation
            else -> false
        }
        activity.getCachedDirectories(
            forceShowHidden = forceShowHiddenAndExcluded || isEncryptedMode,
            forceShowExcluded = forceShowHiddenAndExcluded,
            includeVirtualAlbums = includeVirtual,
            isSecretVaultView = isEncryptedMode
        ) {
            val allDirs = ArrayList(it)
            if (isEncryptedMode) {
                if (CryptoManager.isVaultUnlocked) {
                    val encryptedAlbums = CryptoManager.getVaultDestinationAlbums(activity)
                    encryptedAlbums.forEach { album ->
                        if (!CryptoManager.isVaultRecycleBin(album.folder) &&
                            !allDirs.any { d -> d.path.equals(album.fullPath, true) }
                        ) {
                            val firstTmb = album.folder.listFiles()?.firstOrNull { f ->
                                f.isFile && CryptoManager.isEncryptedFile(f.name)
                            }?.absolutePath ?: ""
                            allDirs.add(
                                Directory(
                                    id = null,
                                    path = album.fullPath,
                                    tmb = firstTmb,
                                    name = album.name,
                                    mediaCnt = album.itemCount,
                                    modified = album.folder.lastModified(),
                                    taken = album.folder.lastModified(),
                                    size = 0L,
                                    location = LOCATION_INTERNAL,
                                    types = TYPE_IMAGES or TYPE_VIDEOS,
                                    sortValue = ""
                                )
                            )
                        }
                    }
                }
            } else {
                val canShowHidden = activity.config.shouldShowHidden &&
                    (!activity.config.isHiddenPasswordProtectionOn || CryptoManager.isVaultUnlocked)
                if (canShowHidden) {
                    if (effectiveMode == PickDirectoryMode.DEFAULT) {
                        val encryptedAlbums = CryptoManager.getHiddenPhysicalAlbums(activity)
                        encryptedAlbums.forEach { album ->
                            if (!CryptoManager.isVaultRecycleBin(album.folder) &&
                                !allDirs.any { d -> d.path.equals(album.fullPath, true) }
                            ) {
                                allDirs.add(
                                    Directory(
                                        id = null,
                                        path = album.fullPath,
                                        tmb = "",
                                        name = album.name,
                                        mediaCnt = album.itemCount,
                                        modified = album.folder.lastModified(),
                                        taken = album.folder.lastModified(),
                                        size = 0L,
                                        location = LOCATION_INTERNAL,
                                        types = TYPE_IMAGES or TYPE_VIDEOS,
                                        sortValue = ""
                                    )
                                )
                            }
                        }
                    }

                    // Add unencrypted hidden folders (e.g. .nomedia folders)
                    val noMediaFolders = activity.getNoMediaFoldersSync()
                    noMediaFolders.forEach { folderPath ->
                        if (!allDirs.any { d -> d.path.equals(folderPath, true) } &&
                            !CryptoManager.isEncryptedFolderPath(activity, folderPath) &&
                            !activity.isEncryptedVaultAlbum(folderPath)
                        ) {
                            val folder = File(folderPath)
                            if (folder.exists() && folder.isDirectory) {
                                val count = folder.listFiles()?.count { f -> f.isFile && f.path.isMediaFile() } ?: 0
                                allDirs.add(
                                    Directory(
                                        id = null,
                                        path = folderPath,
                                        tmb = "",
                                        name = folderPath.getFilenameFromPath(),
                                        mediaCnt = count,
                                        modified = folder.lastModified(),
                                        taken = folder.lastModified(),
                                        size = 0L,
                                        location = LOCATION_INTERNAL,
                                        types = TYPE_IMAGES or TYPE_VIDEOS,
                                        sortValue = ""
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Exclude recycle bins and any destinations disallowed for the current mode
            allDirs.removeAll { d -> !isDirectoryAllowedForMode(d) }

            allDirs.forEach { d ->
                d.subfoldersMediaCount = d.mediaCnt
            }

            allDirectories.clear()
            val dirs = if (CopyMovePolicy.isCopyMode(effectiveMode) || isEncryptedMode) {
                allDirs
            } else {
                val withTemp = activity.addTempFolderIfNeeded(allDirs)
                if (effectiveMode == PickDirectoryMode.DEFAULT) {
                    withTemp
                } else {
                    ArrayList(withTemp.filter { isDirectoryAllowedForMode(it) })
                }
            }
            gotDirectories(dirs)
        }
    }

    private fun showOtherFolder() {
        activity.hideKeyboard(searchEditText)
        when (effectiveMode) {
            PickDirectoryMode.ENCRYPTED_COPY -> {
                ensureBackgroundThread {
                    val effectiveSource = activity.resolveEffectiveSourcePhysicalPath(sourcePath)
                    activity.runOnUiThread {
                        val items = arrayListOf(
                            RadioItem(1, activity.getString(R.string.create_virtual_album)),
                            RadioItem(2, activity.getString(R.string.create_new_hidden_album))
                        )
                        RadioGroupDialog(activity, items) { selected ->
                            if (selected == 1) {
                                dialog?.dismiss()
                                CreateVirtualAlbumDialog(
                                    activity = activity,
                                    isHidden = true,
                                    parentPhysicalPath = effectiveSource
                                ) { albumId ->
                                    callback(getVirtualAlbumPath(albumId))
                                }
                            } else {
                                dialog?.dismiss()
                                CreateEncryptedAlbumDialog(activity) { targetFolder, _ ->
                                    callback(targetFolder.absolutePath)
                                }
                            }
                        }
                    }
                }
            }

            PickDirectoryMode.ENCRYPTED_MOVE -> {
                dialog?.dismiss()
                CreateEncryptedAlbumDialog(activity) { targetFolder, _ ->
                    callback(targetFolder.absolutePath)
                }
            }

            PickDirectoryMode.UNENCRYPTED_COPY -> {
                ensureBackgroundThread {
                    val effectiveSource = activity.resolveEffectiveSourcePhysicalPath(sourcePath)
                    activity.runOnUiThread {
                        val items = arrayListOf(
                            RadioItem(1, activity.getString(R.string.create_virtual_album)),
                            RadioItem(2, activity.getString(R.string.other_folder))
                        )
                        RadioGroupDialog(activity, items) { selected ->
                            if (selected == 1) {
                                dialog?.dismiss()
                                CreateVirtualAlbumDialog(
                                    activity = activity,
                                    isHidden = false,
                                    parentPhysicalPath = effectiveSource
                                ) { albumId ->
                                    callback(getVirtualAlbumPath(albumId))
                                }
                            } else {
                                openPhysicalFolderPicker(effectiveSource)
                            }
                        }
                    }
                }
            }

            PickDirectoryMode.UNENCRYPTED_MOVE -> {
                openPhysicalFolderPicker(sourcePath.trimEnd('/'))
            }

            PickDirectoryMode.DECRYPT_MOVE, PickDirectoryMode.DEFAULT -> {
                openPhysicalFolderPicker("")
            }
        }
    }

    private fun openPhysicalFolderPicker(excludedPath: String) {
        val pickerShowHidden = if (effectiveMode == PickDirectoryMode.DEFAULT) showHidden else activity.config.showHiddenMedia
        val rawDefaultPath = activity.getDefaultCopyDestinationPath(pickerShowHidden, sourcePath)
        val defaultPath = if (effectiveMode != PickDirectoryMode.DEFAULT &&
            (CryptoManager.isEncryptedFolderPath(activity, rawDefaultPath) || activity.isEncryptedVaultAlbum(rawDefaultPath))
        ) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.absolutePath
                ?: activity.internalStoragePath
        } else {
            rawDefaultPath
        }
        FilePickerDialog(
            activity,
            defaultPath,
            !isPickingCopyMoveDestination && !isPickingFolderForWidget,
            pickerShowHidden,
            true,
            true
        ) {
            if (effectiveMode != PickDirectoryMode.DEFAULT &&
                (CryptoManager.isEncryptedFolderPath(activity, it) || activity.isEncryptedVaultAlbum(it))
            ) {
                activity.toast(R.string.cannot_target_encrypted_album)
                return@FilePickerDialog
            }
            if (excludedPath.isNotEmpty() && it.trimEnd('/').equals(excludedPath.trimEnd('/'), ignoreCase = true)) {
                activity.toast(org.fossify.commons.R.string.source_and_destination_same)
                return@FilePickerDialog
            }
            config.lastCopyPath = it
            activity.handleLockedFolderOpening(it) { success ->
                if (success) {
                    callback(it)
                }
            }
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        ensureBackgroundThread {
            if (allDirectories.isEmpty()) {
                allDirectories = newDirs.clone() as ArrayList<Directory>
            }

            val isCopyMode = CopyMovePolicy.isCopyMode(effectiveMode) || (effectiveMode == PickDirectoryMode.DEFAULT && isCopyOperation)
            val effectiveSource = if (isPickingCopyMoveDestination && isCopyMode) {
                activity.resolveEffectiveSourcePhysicalPath(sourcePath)
            } else {
                sourcePath
            }

            val distinctDirs = newDirs.filter { dir ->
                if (!isDirectoryAllowedForMode(dir)) {
                    return@filter false
                }

                if (isPickingCopyMoveDestination && isCopyMode) {
                    if (dir.isVirtualAlbum()) {
                        // A virtual album may hold items from any number of folders, so it is
                        // offered as a destination regardless of where the source files live.
                        return@filter dir.path != sourcePath
                    } else if (sourcePath.isVirtualAlbumPath()) {
                        if (effectiveSource.isNotEmpty() && dir.path.trimEnd('/').equals(effectiveSource.trimEnd('/'), ignoreCase = true)) {
                            return@filter false
                        }
                    }
                }

                true
            }.distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<Directory>
            val sortedDirs = activity.getSortedDirectories(distinctDirs)
            val dirs = activity.getDirsToShow(sortedDirs, allDirectories, currentPathPrefix).clone() as ArrayList<Directory>
            if (hasLoadedDirectories && dirs.hashCode() == shownDirectories.hashCode()) {
                return@ensureBackgroundThread
            }

            hasLoadedDirectories = true
            shownDirectories = dirs
            activity.runOnUiThread {
                checkPlaceholderVisibility(dirs)
                val adapter = DirectoryAdapter(activity, dirs.clone() as ArrayList<Directory>, null, binding.directoriesGrid, true) {
                    val clickedDir = it as Directory
                    val path = clickedDir.path
                    if (!isDirectoryAllowedForMode(clickedDir)) {
                        activity.toast(R.string.cannot_target_encrypted_album)
                        return@DirectoryAdapter
                    }
                    if (clickedDir.isVirtualAlbum()) {
                        if (!isCopyMode) {
                            activity.toast(R.string.virtual_album_cannot_be_modified)
                            return@DirectoryAdapter
                        }
                        if (isPickingCopyMoveDestination && path.trimEnd('/') == sourcePath.trimEnd('/')) {
                            activity.toast(org.fossify.commons.R.string.source_and_destination_same)
                            return@DirectoryAdapter
                        }
                        callback(path)
                        dialog?.dismiss()
                        return@DirectoryAdapter
                    }

                    if (clickedDir.subfoldersCount == 1 || !activity.config.groupDirectSubfolders) {
                        if (isPickingCopyMoveDestination && sourcePath.isNotEmpty() && path.trimEnd('/') == sourcePath.trimEnd('/')) {
                            activity.toast(org.fossify.commons.R.string.source_and_destination_same)
                            return@DirectoryAdapter
                        } else if (isPickingCopyMoveDestination && !CryptoManager.isEncryptedFolderPath(activity, path) && activity.isRestrictedWithSAFSdk30(path) && !activity.isInDownloadDir(path)) {
                            activity.toast(org.fossify.commons.R.string.system_folder_copy_restriction, Toast.LENGTH_LONG)
                            return@DirectoryAdapter
                        } else {
                            activity.handleLockedFolderOpening(path) { success ->
                                if (success) {
                                    callback(path)
                                }
                            }
                            dialog?.dismiss()
                        }
                    } else {
                        currentPathPrefix = path
                        openedSubfolders.add(path)
                        gotDirectories(allDirectories)
                    }
                }

                val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
                binding.apply {
                    directoriesGrid.adapter = adapter
                    directoriesFastscroller.setScrollVertically(!scrollHorizontally)
                }
            }
        }
    }
}
