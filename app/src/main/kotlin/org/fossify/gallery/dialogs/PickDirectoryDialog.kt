package org.fossify.gallery.dialogs

import android.graphics.Color
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
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.handleHiddenFolderPasswordProtection
import org.fossify.commons.extensions.handleLockedFolderOpening
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.isGone
import org.fossify.commons.extensions.isInDownloadDir
import org.fossify.commons.extensions.isRestrictedWithSAFSdk30
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.VIEW_TYPE_GRID
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.commons.views.MySearchMenu
import org.fossify.gallery.R
import org.fossify.gallery.adapters.DirectoryAdapter
import org.fossify.gallery.databinding.DialogDirectoryPickerBinding
import org.fossify.gallery.extensions.*
import org.fossify.gallery.models.Directory

class PickDirectoryDialog(
    val activity: BaseSimpleActivity,
    val sourcePath: String,
    showOtherFolderButton: Boolean,
    val showFavoritesBin: Boolean,
    val isPickingCopyMoveDestination: Boolean,
    val isPickingFolderForWidget: Boolean,
    val isCopyOperation: Boolean = false,
    val isHiddenCopySource: Boolean = false,
    val callback: (path: String) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var shownDirectories = ArrayList<Directory>()
    private var allDirectories = ArrayList<Directory>()
    private var openedSubfolders = arrayListOf("")
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
            builder.setNeutralButton(R.string.other_folder) { dialogInterface, i -> showOtherFolder() }
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

    private fun fetchDirectories(forceShowHiddenAndExcluded: Boolean) {
        val includeVirtual = isPickingCopyMoveDestination && isCopyOperation
        activity.getCachedDirectories(
            forceShowHidden = forceShowHiddenAndExcluded,
            forceShowExcluded = forceShowHiddenAndExcluded,
            includeVirtualAlbums = includeVirtual
        ) {
            if (it.isNotEmpty()) {
                it.forEach {
                    it.subfoldersMediaCount = it.mediaCnt
                }

                activity.runOnUiThread {
                    allDirectories.clear()
                    val dirs = if (isPickingCopyMoveDestination && isCopyOperation) {
                        it
                    } else {
                        activity.addTempFolderIfNeeded(it)
                    }
                    gotDirectories(dirs)
                }
            }
        }
    }

    private fun showOtherFolder() {
        activity.hideKeyboard(searchEditText)
        if (isPickingCopyMoveDestination && isCopyOperation) {
            val effectiveSource = activity.resolveEffectiveSourcePhysicalPath(sourcePath)
            val items = arrayListOf(
                RadioItem(1, activity.getString(R.string.create_virtual_album)),
                RadioItem(2, activity.getString(R.string.other_folder))
            )
            RadioGroupDialog(activity, items) { selected ->
                if (selected == 1) {
                    dialog?.dismiss()
                    CreateVirtualAlbumDialog(
                        activity = activity,
                        isHidden = isHiddenCopySource,
                        parentPhysicalPath = effectiveSource
                    ) { albumId ->
                        callback(getVirtualAlbumPath(albumId))
                    }
                } else {
                    openPhysicalFolderPicker(effectiveSource)
                }
            }
            return
        }

        openPhysicalFolderPicker("")
    }

    private fun openPhysicalFolderPicker(excludedPath: String) {
        FilePickerDialog(
            activity,
            activity.getDefaultCopyDestinationPath(showHidden, sourcePath),
            !isPickingCopyMoveDestination && !isPickingFolderForWidget,
            showHidden,
            true,
            true
        ) {
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
        if (allDirectories.isEmpty()) {
            allDirectories = newDirs.clone() as ArrayList<Directory>
        }

        val effectiveSource = if (isPickingCopyMoveDestination && isCopyOperation) {
            activity.resolveEffectiveSourcePhysicalPath(sourcePath)
        } else {
            sourcePath
        }

        val distinctDirs = newDirs.filter { dir ->
            if (!showFavoritesBin && (dir.isRecycleBin() || dir.areFavorites())) {
                return@filter false
            }

            if (isPickingCopyMoveDestination && isCopyOperation) {
                if (dir.isVirtualAlbum()) {
                    val albumId = dir.path.getVirtualAlbumId() ?: return@filter false
                    val albumOwner = activity.getVirtualAlbumOwnerPhysicalPath(albumId)
                    if (albumOwner.isNotEmpty()) {
                        return@filter albumOwner.equals(effectiveSource, ignoreCase = true) && dir.path != sourcePath
                    }
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
        if (dirs.hashCode() == shownDirectories.hashCode()) {
            return
        }

        shownDirectories = dirs
        val adapter = DirectoryAdapter(activity, dirs.clone() as ArrayList<Directory>, null, binding.directoriesGrid, true) {
            val clickedDir = it as Directory
            val path = clickedDir.path
            if (clickedDir.isVirtualAlbum()) {
                if (!isCopyOperation) {
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
                if (isPickingCopyMoveDestination && path.trimEnd('/') == sourcePath) {
                    activity.toast(org.fossify.commons.R.string.source_and_destination_same)
                    return@DirectoryAdapter
                } else if (isPickingCopyMoveDestination && activity.isRestrictedWithSAFSdk30(path) && !activity.isInDownloadDir(path)) {
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
