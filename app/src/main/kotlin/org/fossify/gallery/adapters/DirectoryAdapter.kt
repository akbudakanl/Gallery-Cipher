package org.fossify.gallery.adapters

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.text.TextUtils
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FolderLockingNoticeDialog
import org.fossify.commons.dialogs.PropertiesDialog
import org.fossify.commons.dialogs.RenameItemDialog
import org.fossify.commons.dialogs.RenameItemsDialog
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.containsNoMedia
import org.fossify.commons.extensions.convertToBitmap
import org.fossify.commons.extensions.doesThisOrParentHaveNoMedia
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getTimeFormat
import org.fossify.commons.extensions.handleDeletePasswordProtection
import org.fossify.commons.extensions.handleLockedFolderOpening
import org.fossify.commons.extensions.isAStorageRootFolder
import org.fossify.commons.extensions.isExternalStorageManager
import org.fossify.commons.extensions.isGif
import org.fossify.commons.extensions.isImageFast
import org.fossify.commons.extensions.isMediaFile
import org.fossify.commons.extensions.isRawFast
import org.fossify.commons.extensions.isSvg
import org.fossify.commons.extensions.isVideoFast
import org.fossify.commons.extensions.isVisible
import org.fossify.commons.extensions.rescanPaths
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.FAVORITES
import org.fossify.commons.helpers.NOMEDIA
import org.fossify.commons.helpers.PROTECTION_PATTERN
import org.fossify.commons.helpers.PROTECTION_PIN
import org.fossify.gallery.helpers.PROTECTION_PASSWORD
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.VIEW_TYPE_LIST
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isRPlus
import org.fossify.commons.interfaces.ItemMoveCallback
import org.fossify.commons.interfaces.ItemTouchHelperContract
import org.fossify.commons.interfaces.StartReorderDragListener
import org.fossify.commons.models.FileDirItem
import org.fossify.commons.views.MyRecyclerView
import org.fossify.gallery.R
import org.fossify.gallery.activities.MediaActivity
import org.fossify.gallery.databinding.DirectoryItemGridRoundedCornersBinding
import org.fossify.gallery.databinding.DirectoryItemGridSquareBinding
import org.fossify.gallery.databinding.DirectoryItemListBinding
import org.fossify.gallery.dialogs.ConfirmDeleteFolderDialog
import org.fossify.gallery.dialogs.CryptoProgressDialog
import org.fossify.gallery.dialogs.EncryptConfirmationDialog
import org.fossify.gallery.dialogs.ExcludeFolderDialog
import org.fossify.gallery.dialogs.PickMediumDialog
import org.fossify.gallery.dialogs.RenameEncryptedFolderDialog
import org.fossify.gallery.dialogs.RenameVirtualAlbumDialog
import org.fossify.gallery.dialogs.VaultSecurityDialog
import org.fossify.gallery.extensions.addNoMedia
import org.fossify.gallery.extensions.checkAppendingHidden
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.deleteVirtualAlbum
import org.fossify.gallery.extensions.directoryDB
import org.fossify.gallery.extensions.emptyAndDisableTheRecycleBin
import org.fossify.gallery.extensions.emptyTheRecycleBin
import org.fossify.gallery.extensions.favoritesDB
import org.fossify.gallery.extensions.fixDateTaken
import org.fossify.gallery.extensions.getShortcutImage
import org.fossify.gallery.extensions.getVirtualAlbumId
import org.fossify.gallery.extensions.hasVaultCreated
import org.fossify.gallery.extensions.isThisOrParentFolderHidden
import org.fossify.gallery.extensions.isVirtualAlbum
import org.fossify.gallery.extensions.isVirtualAlbumPath
import org.fossify.gallery.extensions.loadImage
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.extensions.promptCreateVault
import org.fossify.gallery.extensions.removeNoMedia
import org.fossify.gallery.extensions.showRecycleBinEmptyingDialog
import org.fossify.gallery.extensions.tryCopyMoveFilesTo
import org.fossify.gallery.extensions.isHiddenVirtualAlbumPath
import org.fossify.gallery.extensions.tryDecryptFolders
import org.fossify.gallery.extensions.repairReferencesAfterExternalChange
import org.fossify.gallery.extensions.updateFavoritePaths
import org.fossify.gallery.extensions.updateReferencesForFolderRename
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.DIRECTORY
import org.fossify.gallery.helpers.ensureNotificationPermissionThenRun
import org.fossify.gallery.helpers.FOLDER_MEDIA_CNT_BRACKETS
import org.fossify.gallery.helpers.FOLDER_MEDIA_CNT_LINE
import org.fossify.gallery.helpers.FOLDER_STYLE_ROUNDED_CORNERS
import org.fossify.gallery.helpers.FOLDER_STYLE_SQUARE
import org.fossify.gallery.helpers.LOCATION_INTERNAL
import org.fossify.gallery.helpers.LOCATION_SD
import org.fossify.gallery.helpers.PATH
import org.fossify.gallery.helpers.PinMenuPolicy
import org.fossify.gallery.helpers.RECYCLE_BIN
import org.fossify.gallery.helpers.ROUNDED_CORNERS_BIG
import org.fossify.gallery.helpers.ROUNDED_CORNERS_NONE
import org.fossify.gallery.helpers.ROUNDED_CORNERS_SMALL
import org.fossify.gallery.helpers.TYPE_GIFS
import org.fossify.gallery.helpers.TYPE_IMAGES
import org.fossify.gallery.helpers.TYPE_RAWS
import org.fossify.gallery.helpers.TYPE_SVGS
import org.fossify.gallery.helpers.TYPE_VIDEOS
import org.fossify.gallery.interfaces.DirectoryOperationsListener
import org.fossify.gallery.models.AlbumCover
import org.fossify.gallery.models.Directory
import java.io.File
import java.util.Collections

class DirectoryAdapter(
    activity: BaseSimpleActivity,
    var dirs: ArrayList<Directory>,
    val listener: DirectoryOperationsListener?,
    recyclerView: MyRecyclerView,
    val isPickIntent: Boolean,
    val swipeRefreshLayout: SwipeRefreshLayout? = null,
    itemClick: (Any) -> Unit
) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick), ItemTouchHelperContract,
    RecyclerViewFastScroller.OnPopupTextUpdate {

    private val config = activity.config
    private val isListViewType = config.viewTypeFolders == VIEW_TYPE_LIST
    private var pinnedFolders = config.pinnedFolders
    private var scrollHorizontally = config.scrollHorizontally
    private var animateGifs = config.animateGifs
    private var cropThumbnails = config.cropThumbnails
    private var groupDirectSubfolders = config.groupDirectSubfolders
    private var currentDirectoriesHash = dirs.hashCode()
    private var lockedFolderPaths = ArrayList<String>()
    private var isDragAndDropping = false
    private var startReorderDragListener: StartReorderDragListener? = null

    private var showMediaCount = config.showFolderMediaCount
    private var folderStyle = config.folderStyle
    private var limitFolderTitle = config.limitFolderTitle
    var directorySorting = config.directorySorting
    var dateFormat = config.dateFormat
    var timeFormat = activity.getTimeFormat()

    init {
        setupDragListener(true)
        fillLockedFolders()
    }

    override fun getActionMenuId() = R.menu.cab_directories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when {
            isListViewType -> DirectoryItemListBinding.inflate(layoutInflater, parent, false)
            folderStyle == FOLDER_STYLE_SQUARE -> DirectoryItemGridSquareBinding.inflate(layoutInflater, parent, false)
            else -> DirectoryItemGridRoundedCornersBinding.inflate(layoutInflater, parent, false)
        }

        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val dir = dirs.getOrNull(position) ?: return
        holder.bindView(dir, true, !isPickIntent) { itemView, adapterPosition ->
            setupView(itemView, dir, holder)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = dirs.size

    override fun prepareActionMode(menu: Menu) {
        val selectedPaths = getSelectedPaths()
        if (selectedPaths.isEmpty()) {
            return
        }

        val isOneItemSelected = isOneItemSelected()
        val hasVirtualAlbum = selectedPaths.any { it.isVirtualAlbumPath() }
        val hasFavorites = selectedPaths.contains(FAVORITES)
        val hasRecycleBin = selectedPaths.contains(RECYCLE_BIN)
        val hasVaultRecycleBin = selectedPaths.any { CryptoManager.isVaultRecycleBinPath(activity, it) }
        val hasEncryptedFolder = selectedPaths.any {
            CryptoManager.isEncryptedFolderPath(activity, it) && !CryptoManager.isVaultRecycleBinPath(activity, it)
        }
        val hasUnencryptedRealFolder = selectedPaths.any {
            !it.isVirtualAlbumPath() &&
                it != FAVORITES &&
                it != RECYCLE_BIN &&
                !CryptoManager.isVaultRecycleBinPath(activity, it) &&
                !CryptoManager.isEncryptedFolderPath(activity, it)
        }
        val isMixedEncryption = hasEncryptedFolder && hasUnencryptedRealFolder
        val canCopyMove = !hasVirtualAlbum && !hasFavorites && !hasRecycleBin && !hasVaultRecycleBin && !isMixedEncryption

        menu.apply {
            findItem(R.id.cab_move_to_top).isVisible = isDragAndDropping
            findItem(R.id.cab_move_to_bottom).isVisible = isDragAndDropping

            val hasNonRenamableEncrypted = selectedPaths.any {
                CryptoManager.isVaultRecycleBinPath(activity, it) || CryptoManager.isDefaultHiddenAlbumPath(it)
            }
            val hasMultipleWithEncrypted = !isOneItemSelected && selectedPaths.any { CryptoManager.isEncryptedFolderPath(activity, it) }
            findItem(R.id.cab_rename).isVisible =
                !hasFavorites && !hasRecycleBin && !hasNonRenamableEncrypted && !hasMultipleWithEncrypted
            findItem(R.id.cab_change_cover_image).isVisible = isOneItemSelected

            val lockablePaths = selectedPaths.filter {
                !CryptoManager.isEncryptedFolderPath(activity, it) &&
                    !CryptoManager.isVaultRecycleBinPath(activity, it) &&
                    !(it.isVirtualAlbumPath() && activity.isHiddenVirtualAlbumPath(it))
            }
            findItem(R.id.cab_lock).isVisible = lockablePaths.any { !config.isFolderProtected(it) }
            findItem(R.id.cab_unlock).isVisible = lockablePaths.any { config.isFolderProtected(it) }

            val selectedDir = getSelectedItems().firstOrNull()
            val isRecycleBin = isOneItemSelected && (selectedPaths.first() == RECYCLE_BIN || selectedDir?.isRecycleBin() == true)
            findItem(R.id.cab_empty_recycle_bin).isVisible = isRecycleBin
            findItem(R.id.cab_empty_disable_recycle_bin).isVisible = isOneItemSelected && selectedPaths.first() == RECYCLE_BIN

            findItem(R.id.cab_create_shortcut).isVisible = isOneItemSelected
            findItem(R.id.cab_copy_to).isVisible = canCopyMove
            findItem(R.id.cab_move_to).isVisible = canCopyMove

            checkHideBtnVisibility(this, selectedPaths)
            checkPinBtnVisibility(this, selectedPaths)

            // virtual albums are not real folders: they cannot be hidden, excluded, copied, moved or inspected on disk
            if (hasVirtualAlbum) {
                findItem(R.id.cab_rename).isVisible = isOneItemSelected
                findItem(R.id.cab_hide).isVisible = false
                findItem(R.id.cab_unhide).isVisible = false
                findItem(R.id.cab_exclude).isVisible = false
                findItem(R.id.cab_copy_to).isVisible = false
                findItem(R.id.cab_move_to).isVisible = false
                findItem(R.id.cab_create_shortcut).isVisible = false
                findItem(R.id.cab_properties).isVisible = false
            }

            if (hasFavorites) {
                findItem(R.id.cab_hide).isVisible = false
                findItem(R.id.cab_unhide).isVisible = false
                findItem(R.id.cab_copy_to).isVisible = false
                findItem(R.id.cab_move_to).isVisible = false
                findItem(R.id.cab_exclude).isVisible = false
                findItem(R.id.cab_properties).isVisible = false
            }

            if (hasVaultRecycleBin) {
                findItem(R.id.cab_hide).isVisible = false
                findItem(R.id.cab_unhide).isVisible = false
                findItem(R.id.cab_exclude).isVisible = false
                findItem(R.id.cab_lock).isVisible = false
                findItem(R.id.cab_unlock).isVisible = false
                findItem(R.id.cab_create_shortcut).isVisible = false
            }
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_move_to_top -> moveSelectedItemsToTop()
            R.id.cab_move_to_bottom -> moveSelectedItemsToBottom()
            R.id.cab_properties -> showProperties()
            R.id.cab_rename -> renameDir()
            R.id.cab_pin -> pinFolders(true)
            R.id.cab_unpin -> pinFolders(false)
            R.id.cab_change_order -> changeOrder()
            R.id.cab_empty_recycle_bin -> tryEmptyRecycleBin(true)
            R.id.cab_empty_disable_recycle_bin -> emptyAndDisableRecycleBin()
            R.id.cab_hide -> toggleFoldersVisibility(true)
            R.id.cab_unhide -> toggleFoldersVisibility(false)
            R.id.cab_exclude -> tryExcludeFolder()
            R.id.cab_lock -> tryLockFolder()
            R.id.cab_unlock -> unlockFolder()
            R.id.cab_copy_to -> copyFilesTo()
            R.id.cab_move_to -> moveFilesTo()
            R.id.cab_select_all -> selectAll()
            R.id.cab_create_shortcut -> tryCreateShortcut()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_select_photo -> tryChangeAlbumCover(false)
            R.id.cab_use_default -> tryChangeAlbumCover(true)
        }
    }

    override fun getSelectableItemCount() = dirs.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(position)?.path?.hashCode()

    override fun getItemKeyPosition(key: Int) = dirs.indexOfFirst { it.path.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {
        if (isDragAndDropping) {
            notifyDataSetChanged()

            val reorderedFoldersList = dirs.map { it.path }
            config.customFoldersOrder = TextUtils.join("|||", reorderedFoldersList)
            config.directorySorting = SORT_BY_CUSTOM
        }

        isDragAndDropping = false
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed) {
            Glide.with(activity).clear(bindItem(holder.itemView).dirThumbnail)
        }
    }

    private fun checkHideBtnVisibility(menu: Menu, selectedPaths: ArrayList<String>) {
        val hasVault = activity.hasVaultCreated()
        val hasForbidden = selectedPaths.any {
            it.isVirtualAlbumPath() || it == FAVORITES || CryptoManager.isVaultRecycleBinPath(activity, it)
        }
        val realUnencryptedPaths = selectedPaths.filter {
            !it.isVirtualAlbumPath() &&
                it != FAVORITES &&
                !CryptoManager.isVaultRecycleBinPath(activity, it) &&
                !CryptoManager.isEncryptedFolderPath(activity, it)
        }
        menu.findItem(R.id.cab_hide).isVisible =
            (!isRPlus() || isExternalStorageManager()) &&
            !hasForbidden &&
            realUnencryptedPaths.any { !it.doesThisOrParentHaveNoMedia(HashMap(), null) || hasVault }

        val decryptablePaths = selectedPaths.filter {
            !it.isVirtualAlbumPath() &&
                it != FAVORITES &&
                it != RECYCLE_BIN &&
                !CryptoManager.isVaultRecycleBinPath(activity, it)
        }
        val hasEncryptedFolder = decryptablePaths.any { CryptoManager.isEncryptedFolderPath(activity, it) }
        menu.findItem(R.id.cab_unhide).isVisible =
            (!isRPlus() || isExternalStorageManager()) &&
            !hasForbidden &&
            !selectedPaths.contains(RECYCLE_BIN) &&
            decryptablePaths.any {
                CryptoManager.isEncryptedFolderPath(activity, it) || (!hasVault && it.doesThisOrParentHaveNoMedia(HashMap(), null))
            }

        // encrypted albums are decrypted, while legacy .nomedia folders are just shown again
        menu.findItem(R.id.cab_unhide).title = if (hasEncryptedFolder) {
            activity.getString(R.string.decrypt_album)
        } else {
            activity.getString(org.fossify.commons.R.string.unhide)
        }
    }

    private fun checkPinBtnVisibility(menu: Menu, selectedPaths: ArrayList<String>) {
        val visibility = PinMenuPolicy.resolve(selectedPaths, config.pinnedFolders)
        menu.findItem(R.id.cab_pin).isVisible = visibility.showPin
        menu.findItem(R.id.cab_unpin).isVisible = visibility.showUnpin
    }

    private fun moveSelectedItemsToTop() {
        selectedKeys.toMutableList().reversed().forEach { key ->
            val position = dirs.indexOfFirst { it.path.hashCode() == key }
            val tempItem = dirs[position]
            dirs.removeAt(position)
            dirs.add(0, tempItem)
        }

        notifyDataSetChanged()
    }

    private fun moveSelectedItemsToBottom() {
        selectedKeys.forEach { key ->
            val position = dirs.indexOfFirst { it.path.hashCode() == key }
            val tempItem = dirs[position]
            dirs.removeAt(position)
            dirs.add(dirs.size, tempItem)
        }

        notifyDataSetChanged()
    }

    private fun showProperties() {
        if (selectedKeys.size <= 1) {
            val path = getFirstSelectedItemPath() ?: return
            if (path != FAVORITES && path != RECYCLE_BIN) {
                activity.handleLockedFolderOpening(path) { success ->
                    if (success) {
                        PropertiesDialog(activity, path, config.shouldShowHidden)
                    }
                }
            }
        } else {
            PropertiesDialog(activity, getSelectedPaths().filter {
                it != FAVORITES && it != RECYCLE_BIN && !config.isFolderProtected(it)
            }.toMutableList(), config.shouldShowHidden)
        }
    }

    private fun renameDir() {
        if (selectedKeys.size == 1) {
            val firstDir = getFirstSelectedItem() ?: return
            if (firstDir.isVirtualAlbum()) {
                val albumId = firstDir.path.getVirtualAlbumId() ?: return
                RenameVirtualAlbumDialog(activity, albumId, firstDir.name) {
                    listener?.refreshItems()
                    finishActMode()
                }
                return
            }

            val sourcePath = firstDir.path
            val dir = File(sourcePath)
            if (CryptoManager.isEncryptedFolder(dir)) {
                if (CryptoManager.isVaultRecycleBin(dir) || CryptoManager.isDefaultHiddenAlbum(dir)) {
                    return
                }
                RenameEncryptedFolderDialog(activity, dir, firstDir.name) { newName ->
                    firstDir.name = newName
                    updateDirs(dirs)
                    listener?.refreshItems()
                    finishActMode()
                }
                return
            }

            if (activity.isAStorageRootFolder(dir.absolutePath)) {
                activity.toast(org.fossify.commons.R.string.rename_folder_root)
                return
            }

            activity.handleLockedFolderOpening(sourcePath) { success ->
                if (success) {
                    RenameItemDialog(activity, dir.absolutePath) { newPath ->
                        activity.runOnUiThread {
                            firstDir.apply {
                                path = newPath
                                name = newPath.getFilenameFromPath()
                                tmb = File(newPath, tmb.getFilenameFromPath()).absolutePath
                            }
                            updateDirs(dirs)
                            ensureBackgroundThread {
                                try {
                                    activity.directoryDB.updateDirectoryAfterRename(firstDir.tmb, firstDir.name, firstDir.path, sourcePath)
                                    // The folder moved, so every reference below it has to follow.
                                    activity.updateReferencesForFolderRename(sourcePath, newPath)
                                    listener?.refreshItems()
                                } catch (e: Exception) {
                                    activity.showErrorToast(e)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val paths = getSelectedPaths().filter { !activity.isAStorageRootFolder(it) && !config.isFolderProtected(it) } as ArrayList<String>
            RenameItemsDialog(activity, paths) {
                ensureBackgroundThread {
                    activity.repairReferencesAfterExternalChange(force = true)
                    activity.runOnUiThread {
                        listener?.refreshItems()
                    }
                }
            }
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        val selectedPaths = getSelectedPaths()
        if (hide && selectedPaths.contains(RECYCLE_BIN)) {
            config.showRecycleBinAtFolders = false
            if (selectedPaths.size == 1) {
                listener?.refreshItems()
                finishActMode()
            }
        }

        if (hide) {
            val folderPaths = ArrayList(
                selectedPaths.filter {
                    it != RECYCLE_BIN &&
                        it != FAVORITES &&
                        !it.isVirtualAlbumPath() &&
                        !CryptoManager.isVaultRecycleBinPath(activity, it) &&
                        !CryptoManager.isEncryptedFolderPath(activity, it)
                }
            )
            if (folderPaths.isEmpty()) {
                return
            }

            // The album is encrypted, so the confirmation has to be shown before the vault password
            // is requested.
            val message = if (folderPaths.size == 1) {
                activity.getString(R.string.confirm_encrypt_album)
            } else {
                activity.getString(R.string.confirm_encrypt_albums)
            }

            EncryptConfirmationDialog(activity, message) {
                hideFolders(folderPaths)
            }
        } else {
            val decryptablePaths = selectedPaths.filter {
                it != FAVORITES &&
                    it != RECYCLE_BIN &&
                    !it.isVirtualAlbumPath() &&
                    !CryptoManager.isVaultRecycleBinPath(activity, it)
            }
            if (decryptablePaths.isEmpty()) {
                return
            }
            if (decryptablePaths.any { !CryptoManager.isEncryptedFolderPath(activity, it) && it.isThisOrParentFolderHidden() }) {
                ConfirmationDialog(activity, "", R.string.cant_unhide_folder, org.fossify.commons.R.string.ok, 0) {}
                return
            }

            val encryptedPaths = decryptablePaths.filter {
                CryptoManager.isEncryptedFolderPath(activity, it) && (decryptablePaths.size == 1 || !config.isFolderProtected(it))
            }
            if (encryptedPaths.isNotEmpty()) {
                handleLockedFolderOpeningForFolders(encryptedPaths) { unlockedPaths ->
                    if (unlockedPaths.isNotEmpty()) {
                        activity.tryDecryptFolders(unlockedPaths.toList()) {
                            listener?.refreshItems()
                            finishActMode()
                        }
                    }
                }
                return
            }

            decryptablePaths.filter { decryptablePaths.size == 1 || !config.isFolderProtected(it) }.forEach {
                val path = it
                activity.handleLockedFolderOpening(path) { success ->
                    if (success && path.containsNoMedia()) {
                        activity.removeNoMedia(path) {
                            if (config.shouldShowHidden) {
                                updateFolderNames()
                            } else {
                                activity.runOnUiThread {
                                    listener?.refreshItems()
                                    finishActMode()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hideFolders(paths: ArrayList<String>) {
        if (!activity.hasVaultCreated()) {
            activity.promptCreateVault(
                onCreated = {
                    hideFolders(paths)
                }
            )
            return
        }

        if (!CryptoManager.isVaultUnlocked) {
            val initialType = CryptoManager.getVaultProtectionType(activity)
                ?: if (config.hiddenProtectionType == PROTECTION_PASSWORD) {
                    PROTECTION_PASSWORD
                } else {
                    PROTECTION_PIN
                }
            VaultSecurityDialog(
                activity = activity,
                requiredHash = config.hiddenPasswordHash,
                showTabIndex = initialType,
                minimalist = true
            ) { _, type, success ->
                if (success) {
                    config.hiddenProtectionType = type
                    hideFolders(paths)
                }
            }
            return
        }

        val totalFilesCount = paths.sumOf { path ->
            File(path).listFiles()?.count { CryptoManager.isEncryptableMediaFile(it) } ?: 0
        }

        activity.ensureNotificationPermissionThenRun { notificationsAllowed ->
            val progressDialog = CryptoProgressDialog(
                activity = activity,
                max = totalFilesCount,
                titleId = R.string.encrypting_folder,
                enableNotification = notificationsAllowed
            )

            ensureBackgroundThread {
                var globalProcessed = 0
                var allSuccess = true
                for (path in paths) {
                    var folderLastCurrent = 0
                    val result = CryptoManager.encryptFolder(activity, path) { current, _ ->
                        val diff = current - folderLastCurrent
                        if (diff > 0) {
                            globalProcessed += diff
                            folderLastCurrent = current
                            activity.runOnUiThread {
                                progressDialog.updateProgress(globalProcessed, totalFilesCount)
                            }
                        }
                    }
                    if (result == null) {
                        allSuccess = false
                    }
                }
                activity.runOnUiThread {
                    try {
                        progressDialog.dismiss()
                    } catch (_: Exception) {}
                    if (allSuccess) {
                        activity.toast(R.string.folder_encrypted_successfully)
                    } else {
                        activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                    }
                    finishActMode()
                    listener?.refreshItems()
                }
            }
        }
    }

    private fun tryEmptyRecycleBin(askConfirmation: Boolean) {
        if (askConfirmation) {
            activity.showRecycleBinEmptyingDialog {
                emptyRecycleBin()
            }
        } else {
            emptyRecycleBin()
        }
    }

    private fun emptyRecycleBin() {
        val selectedDir = getSelectedItems().firstOrNull()
        if (selectedDir != null && CryptoManager.isVaultRecycleBinPath(activity, selectedDir.path)) {
            ensureBackgroundThread {
                CryptoManager.emptyVaultRecycleBin(activity)
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
            return
        }

        activity.handleLockedFolderOpening(RECYCLE_BIN) { success ->
            if (success) {
                activity.emptyTheRecycleBin {
                    listener?.refreshItems()
                }
            }
        }
    }

    private fun emptyAndDisableRecycleBin() {
        activity.handleLockedFolderOpening(RECYCLE_BIN) { success ->
            if (success) {
                activity.showRecycleBinEmptyingDialog {
                    activity.emptyAndDisableTheRecycleBin {
                        listener?.refreshItems()
                    }
                }
            }
        }
    }

    private fun updateFolderNames() {
        val includedFolders = config.includedFolders
        val hidden = activity.getString(R.string.hidden)
        dirs.forEach {
            it.name = activity.checkAppendingHidden(it.path, hidden, includedFolders, ArrayList())
        }
        listener?.updateDirectories(dirs.toMutableList() as ArrayList)
        activity.runOnUiThread {
            updateDirs(dirs)
        }
    }

    private fun hideFolder(path: String) {
        if (!activity.hasVaultCreated()) {
            activity.promptCreateVault(
                onCreated = {
                    hideFolder(path)
                }
            )
            return
        }

        if (!CryptoManager.isVaultUnlocked) {
            val initialType = CryptoManager.getVaultProtectionType(activity)
                ?: if (config.hiddenProtectionType == PROTECTION_PASSWORD) {
                    PROTECTION_PASSWORD
                } else {
                    PROTECTION_PIN
                }
            VaultSecurityDialog(
                activity = activity,
                requiredHash = config.hiddenPasswordHash,
                showTabIndex = initialType,
                minimalist = true
            ) { _, type, success ->
                if (success) {
                    config.hiddenProtectionType = type
                    hideFolder(path)
                }
            }
            return
        }

        val folder = File(path)
        val filesToEncrypt = folder.listFiles()?.filter { CryptoManager.isEncryptableMediaFile(it) } ?: emptyList()

        activity.ensureNotificationPermissionThenRun { notificationsAllowed ->
            val progressDialog = CryptoProgressDialog(
                activity = activity,
                max = filesToEncrypt.size,
                titleId = R.string.encrypting_folder,
                enableNotification = notificationsAllowed
            )

            ensureBackgroundThread {
                val result = CryptoManager.encryptFolder(activity, path) { current, total ->
                    activity.runOnUiThread {
                        progressDialog.updateProgress(current, total)
                    }
                }
                activity.runOnUiThread {
                    try {
                        progressDialog.dismiss()
                    } catch (_: Exception) {}
                    if (result != null) {
                        activity.toast(R.string.folder_encrypted_successfully)
                    } else {
                        activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                    }
                    finishActMode()
                    listener?.refreshItems()
                }
            }
        }
    }

    private fun tryExcludeFolder() {
        val selectedPaths = getSelectedPaths()
        val paths = selectedPaths.filter { it != PATH && it != RECYCLE_BIN && it != FAVORITES }.toSet()
        if (selectedPaths.contains(RECYCLE_BIN)) {
            config.showRecycleBinAtFolders = false
            if (selectedPaths.size == 1) {
                listener?.refreshItems()
                finishActMode()
            }
        }

        if (paths.size == 1) {
            ExcludeFolderDialog(activity, paths.toMutableList()) {
                listener?.refreshItems()
                finishActMode()
            }
        } else if (paths.size > 1) {
            config.addExcludedFolders(paths)
            listener?.refreshItems()
            finishActMode()
        }
    }

    private fun tryLockFolder() {
        if (config.wasFolderLockingNoticeShown) {
            lockFolder()
        } else {
            FolderLockingNoticeDialog(activity) {
                lockFolder()
            }
        }
    }

    private fun lockFolder() {
        SecurityDialog(activity, "", SHOW_ALL_TABS) { hash, type, success ->
            if (success) {
                getSelectedPaths().filter {
                    !config.isFolderProtected(it) &&
                        !CryptoManager.isEncryptedFolderPath(activity, it) &&
                        !CryptoManager.isVaultRecycleBinPath(activity, it) &&
                        !(it.isVirtualAlbumPath() && activity.isHiddenVirtualAlbumPath(it))
                }.forEach {
                    config.addFolderProtection(it, hash, type)
                    lockedFolderPaths.add(it)
                }

                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun unlockFolder() {
        val paths = getSelectedPaths()
        val firstPath = paths.first()
        val tabToShow = config.getFolderProtectionType(firstPath)
        val hashToCheck = config.getFolderProtectionHash(firstPath)
        SecurityDialog(activity, hashToCheck, tabToShow) { hash, type, success ->
            if (success) {
                paths.filter { config.isFolderProtected(it) && config.getFolderProtectionType(it) == tabToShow && config.getFolderProtectionHash(it) == hashToCheck }
                    .forEach {
                        config.removeFolderProtection(it)
                        lockedFolderPaths.remove(it)
                    }

                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun pinFolders(pin: Boolean) {
        if (pin) {
            config.addPinnedFolders(getSelectedPaths().toHashSet())
        } else {
            config.removePinnedFolders(getSelectedPaths().toHashSet())
        }

        currentDirectoriesHash = 0
        pinnedFolders = config.pinnedFolders
        listener?.recheckPinnedFolders()
    }

    private fun changeOrder() {
        isDragAndDropping = true
        notifyDataSetChanged()
        actMode?.invalidate()

        if (startReorderDragListener == null) {
            val touchHelper = ItemTouchHelper(ItemMoveCallback(this, true))
            touchHelper.attachToRecyclerView(recyclerView)

            startReorderDragListener = object : StartReorderDragListener {
                override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            }
        }
    }

    private fun copyFilesTo() {
        handleLockedFolderOpeningForFolders(getSelectedPaths()) {
            copyMoveTo(it, true)
        }
    }

    private fun moveFilesTo() {
        activity.handleDeletePasswordProtection {
            handleLockedFolderOpeningForFolders(getSelectedPaths()) {
                copyMoveTo(it, false)
            }
        }
    }

    private fun copyMoveTo(selectedPaths: Collection<String>, isCopyOperation: Boolean) {
        val validPaths = selectedPaths.filter {
            !it.isVirtualAlbumPath() &&
                it != FAVORITES &&
                it != RECYCLE_BIN &&
                !CryptoManager.isVaultRecycleBinPath(activity, it)
        }
        if (validPaths.isEmpty()) {
            return
        }
        val hasEncrypted = validPaths.any { CryptoManager.isEncryptedFolderPath(activity, it) }
        val hasUnencrypted = validPaths.any { !CryptoManager.isEncryptedFolderPath(activity, it) }
        if (hasEncrypted && hasUnencrypted) {
            activity.toast(R.string.cannot_mix_encrypted_and_unencrypted)
            return
        }

        val paths = ArrayList<String>()
        val showHidden = config.shouldShowHidden
        validPaths.forEach {
            val filter = config.filterMedia
            File(it).listFiles()?.filter {
                !File(it.absolutePath).isDirectory &&
                    (CryptoManager.isEncryptedFile(it.name) ||
                        (it.absolutePath.isMediaFile() && (showHidden || !it.name.startsWith('.')) &&
                            ((it.isImageFast() && filter and TYPE_IMAGES != 0) ||
                                (it.isVideoFast() && filter and TYPE_VIDEOS != 0) ||
                                (it.isGif() && filter and TYPE_GIFS != 0) ||
                                (it.isRawFast() && filter and TYPE_RAWS != 0) ||
                                (it.isSvg() && filter and TYPE_SVGS != 0))))
            }?.mapTo(paths) { it.absolutePath }
        }

        val fileDirItems = paths.map { FileDirItem(it, it.getFilenameFromPath()) } as ArrayList<FileDirItem>
        val sourceDirectoryPath = if (validPaths.size == 1) validPaths.first() else ""
        activity.tryCopyMoveFilesTo(fileDirItems, isCopyOperation, currentDirectoryPath = sourceDirectoryPath) {
            val destinationPath = it
            val affectedFolders = LinkedHashSet<String>(validPaths.size + 1).apply {
                addAll(validPaths)
                if (!destinationPath.isVirtualAlbumPath()) {
                    add(destinationPath)
                }
            }
            if (!destinationPath.isVirtualAlbumPath() && !CryptoManager.isEncryptedFolderPath(activity, destinationPath)) {
                val newPaths = fileDirItems.map { "$destinationPath/${it.name}" }.toMutableList() as ArrayList<String>
                activity.rescanPaths(newPaths) {
                    activity.fixDateTaken(newPaths, false)
                    activity.runOnUiThread {
                        listener?.refreshPaths(affectedFolders)
                    }
                }
            }

            if (!isCopyOperation && !destinationPath.isVirtualAlbumPath()) {
                activity.updateFavoritePaths(fileDirItems, destinationPath)
            }

            config.tempFolderPath = ""
            listener?.refreshPaths(affectedFolders)
            finishActMode()
        }
    }

    private fun tryCreateShortcut() {
        activity.handleLockedFolderOpening(getFirstSelectedItemPath() ?: "") { success ->
            if (success) {
                createShortcut()
            }
        }
    }

    private fun createShortcut() {
        val manager = activity.getSystemService(ShortcutManager::class.java)
        if (manager.isRequestPinShortcutSupported) {
            val dir = getFirstSelectedItem() ?: return
            val path = dir.path
            val drawable = resources.getDrawable(R.drawable.shortcut_image).mutate()
            val coverThumbnail = config.parseAlbumCovers().firstOrNull { it.tmb == dir.path }?.tmb ?: dir.tmb
            activity.getShortcutImage(coverThumbnail, drawable) {
                val intent = Intent(activity, MediaActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra(DIRECTORY, path)

                val shortcut = ShortcutInfo.Builder(activity, path)
                    .setShortLabel(dir.name)
                    .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                    .setIntent(intent)
                    .build()

                manager.requestPinShortcut(shortcut, null)
            }
        }
    }

    private fun askConfirmDelete() {
        val selectedReferenceAlbums = getSelectedItems().filter { it.isVirtualAlbum() || it.areFavorites() }
        if (selectedReferenceAlbums.isNotEmpty()) {
            if (selectedReferenceAlbums.size != selectedKeys.size) {
                // never mix deleting real folders with removing virtual albums or favorites
                activity.toast(R.string.virtual_album_cannot_be_modified)
            } else {
                confirmDeleteReferenceAlbums(selectedReferenceAlbums)
            }
            return
        }

        when {
            config.isDeletePasswordProtectionOn -> activity.handleDeletePasswordProtection {
                deleteFolders()
            }

            config.skipDeleteConfirmation -> deleteFolders()
            else -> {
                val itemsCnt = selectedKeys.size
                if (itemsCnt == 1 && getSelectedItems().first().isRecycleBin()) {
                    ConfirmationDialog(
                        activity,
                        "",
                        org.fossify.commons.R.string.empty_recycle_bin_confirmation,
                        org.fossify.commons.R.string.yes,
                        org.fossify.commons.R.string.no
                    ) {
                        deleteFolders()
                    }
                    return
                }

                val items = if (itemsCnt == 1) {
                    val folder = getSelectedItems().firstOrNull()?.name ?: getSelectedPaths().first().getFilenameFromPath()
                    "\"$folder\""
                } else {
                    resources.getQuantityString(org.fossify.commons.R.plurals.delete_items, itemsCnt, itemsCnt)
                }

                val fileDirItem = getFirstSelectedItem() ?: return
                val baseString = if (!config.useRecycleBin || config.tempSkipRecycleBin || (isOneItemSelected() && fileDirItem.areFavorites())) {
                    org.fossify.commons.R.string.deletion_confirmation
                } else {
                    org.fossify.commons.R.string.move_to_recycle_bin_confirmation
                }

                val question = String.format(resources.getString(baseString), items)
                val warning = resources.getQuantityString(org.fossify.commons.R.plurals.delete_warning, itemsCnt, itemsCnt)
                ConfirmDeleteFolderDialog(activity, question, warning) {
                    deleteFolders()
                }
            }
        }
    }

    private fun confirmDeleteReferenceAlbums(albums: List<Directory>) {
        val items = if (albums.size == 1) {
            "\"${albums.first().name}\""
        } else {
            resources.getQuantityString(org.fossify.commons.R.plurals.delete_items, albums.size, albums.size)
        }

        val question = String.format(resources.getString(R.string.delete_virtual_album_confirmation), items)
        ConfirmationDialog(
            activity,
            question,
            0,
            org.fossify.commons.R.string.yes,
            org.fossify.commons.R.string.no
        ) {
            ensureBackgroundThread {
                // only the album and its references are removed, the original files stay untouched
                albums.forEach { album ->
                    if (album.areFavorites()) {
                        clearFavoritesAlbum()
                    } else {
                        album.path.getVirtualAlbumId()?.let { activity.deleteVirtualAlbum(it) }
                    }
                }

                activity.runOnUiThread {
                    finishActMode()
                    listener?.refreshItems()
                }
            }
        }
    }

    private fun clearFavoritesAlbum() {
        activity.mediaDB.clearFavorites()
        activity.favoritesDB.clearFavorites()
        activity.directoryDB.deleteDirPath(FAVORITES)
        // The backup has to follow, otherwise the favorites would come back
        // after the app data was cleared.
        org.fossify.gallery.helpers.ReferenceBackupManager.saveFavorites(activity)
    }

    private fun deleteFolders() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val SAFPath = getFirstSelectedItemPath() ?: return
        val selectedDirs = getSelectedItems()
        activity.handleSAFDialog(SAFPath) {
            if (!it) {
                return@handleSAFDialog
            }

            activity.handleSAFDialogSdk30(SAFPath) {
                if (!it) {
                    return@handleSAFDialogSdk30
                }

                var foldersToDelete = ArrayList<File>(selectedKeys.size)
                selectedDirs.forEach {
                    if (it.areFavorites() || it.isRecycleBin()) {
                        if (it.isRecycleBin()) {
                            tryEmptyRecycleBin(false)
                        } else {
                            ensureBackgroundThread {
                                clearFavoritesAlbum()
                                activity.runOnUiThread {
                                    listener?.refreshItems()
                                }
                            }
                        }

                        if (selectedKeys.size == 1) {
                            finishActMode()
                        }
                    } else {
                        foldersToDelete.add(File(it.path))
                    }
                }

                handleLockedFolderOpeningForFolders(foldersToDelete.map { it.absolutePath }) {
                    listener?.deleteFolders(it.map { File(it) }.toMutableList() as ArrayList<File>)
                }
            }
        }
    }

    private fun handleLockedFolderOpeningForFolders(folders: Collection<String>, callback: (Collection<String>) -> Unit) {
        if (folders.size == 1) {
            activity.handleLockedFolderOpening(folders.first()) { success ->
                if (success) {
                    callback(folders)
                }
            }
        } else {
            val filtered = folders.filter { !config.isFolderProtected(it) }
            callback(filtered)
        }
    }

    private fun tryChangeAlbumCover(useDefault: Boolean) {
        activity.handleLockedFolderOpening(getFirstSelectedItemPath() ?: "") { success ->
            if (success) {
                changeAlbumCover(useDefault)
            }
        }
    }

    private fun changeAlbumCover(useDefault: Boolean) {
        if (selectedKeys.size != 1)
            return

        val path = getFirstSelectedItemPath() ?: return

        if (useDefault) {
            val albumCovers = getAlbumCoversWithout(path)
            storeCovers(albumCovers)
        } else {
            pickMediumFrom(path, path)
        }
    }

    private fun pickMediumFrom(targetFolder: String, path: String) {
        PickMediumDialog(activity, path) {
            if (File(it).isDirectory) {
                pickMediumFrom(targetFolder, it)
            } else {
                val albumCovers = getAlbumCoversWithout(path)
                val cover = AlbumCover(targetFolder, it)
                albumCovers.add(cover)
                storeCovers(albumCovers)
            }
        }
    }

    private fun getAlbumCoversWithout(path: String) = config.parseAlbumCovers().filterNot { it.path == path } as ArrayList

    private fun storeCovers(albumCovers: ArrayList<AlbumCover>) {
        config.albumCovers = Gson().toJson(albumCovers)
        finishActMode()
        listener?.refreshItems()
    }

    private fun getSelectedItems() = selectedKeys.mapNotNull { getItemWithKey(it) } as ArrayList<Directory>

    private fun getSelectedPaths() = getSelectedItems().map { it.path } as ArrayList<String>

    private fun getFirstSelectedItem() = getItemWithKey(selectedKeys.first())

    private fun getFirstSelectedItemPath() = getFirstSelectedItem()?.path

    private fun getItemWithKey(key: Int): Directory? = dirs.firstOrNull { it.path.hashCode() == key }

    private fun fillLockedFolders() {
        lockedFolderPaths.clear()
        dirs.map { it.path }.filter { config.isFolderProtected(it) }.forEach {
            lockedFolderPaths.add(it)
        }
    }

    fun updateDirs(newDirs: ArrayList<Directory>) {
        val directories = newDirs.clone() as ArrayList<Directory>
        if (directories.hashCode() != currentDirectoriesHash) {
            currentDirectoriesHash = directories.hashCode()
            dirs = directories
            pinnedFolders = config.pinnedFolders
            fillLockedFolders()
            notifyDataSetChanged()
            finishActMode()
        }
    }

    fun updateAnimateGifs(animateGifs: Boolean) {
        this.animateGifs = animateGifs
        notifyDataSetChanged()
    }

    fun updateCropThumbnails(cropThumbnails: Boolean) {
        this.cropThumbnails = cropThumbnails
        notifyDataSetChanged()
    }

    private fun setupView(view: View, directory: Directory, holder: ViewHolder) {
        val isSelected = selectedKeys.contains(directory.path.hashCode())
        bindItem(view).apply {
            dirPath?.text = "${directory.path.substringBeforeLast("/")}/"
            val tmbDisplayName = CryptoManager.getDisplayName(directory.tmb)
            val thumbnailType = when {
                tmbDisplayName.isVideoFast() -> TYPE_VIDEOS
                tmbDisplayName.isGif() -> TYPE_GIFS
                tmbDisplayName.isRawFast() -> TYPE_RAWS
                tmbDisplayName.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }

            dirCheck.beVisibleIf(isSelected)
            if (isSelected) {
                dirCheck.background?.applyColorFilter(properPrimaryColor)
                dirCheck.applyColorFilter(contrastColor)
            }

            if (isListViewType) {
                dirHolder.isSelected = isSelected
            }

            if (scrollHorizontally && !isListViewType && folderStyle == FOLDER_STYLE_ROUNDED_CORNERS) {
                (dirThumbnail.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ABOVE, dirName.id)

                val photoCntParams = (photoCnt.layoutParams as RelativeLayout.LayoutParams)
                val nameParams = (dirName.layoutParams as RelativeLayout.LayoutParams)
                nameParams.removeRule(RelativeLayout.BELOW)

                if (config.showFolderMediaCount == FOLDER_MEDIA_CNT_LINE) {
                    nameParams.addRule(RelativeLayout.ABOVE, photoCnt.id)
                    nameParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                    photoCntParams.removeRule(RelativeLayout.BELOW)
                    photoCntParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                } else {
                    nameParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                }
            }

            if (lockedFolderPaths.contains(directory.path)) {
                dirLock.beVisible()
                dirLock.background = ColorDrawable(root.context.getProperBackgroundColor())
                dirLock.applyColorFilter(root.context.getProperBackgroundColor().getContrastColor())
            } else {
                dirLock.beGone()
                val roundedCorners = when {
                    isListViewType -> ROUNDED_CORNERS_SMALL
                    folderStyle == FOLDER_STYLE_SQUARE -> ROUNDED_CORNERS_NONE
                    else -> ROUNDED_CORNERS_BIG
                }

                dirThumbnail.setBackgroundResource(
                    when (roundedCorners) {
                        ROUNDED_CORNERS_SMALL -> R.drawable.placeholder_rounded_small
                        ROUNDED_CORNERS_BIG -> R.drawable.placeholder_rounded_big
                        else -> R.drawable.placeholder_square
                    }
                )

                activity.loadImage(
                    type = thumbnailType,
                    path = directory.tmb,
                    target = dirThumbnail,
                    horizontalScroll = scrollHorizontally,
                    animateGifs = animateGifs,
                    cropThumbnails = cropThumbnails,
                    roundCorners = roundedCorners,
                    signature = directory.getKey(),
                    onError = {
                        dirThumbnail.scaleType = ImageView.ScaleType.CENTER
                        dirThumbnail.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_vector_warning_colored))
                    }
                )
            }

            dirPin.beVisibleIf(pinnedFolders.contains(directory.path))
            dirLocation.beVisibleIf(directory.location != LOCATION_INTERNAL)
            if (dirLocation.isVisible()) {
                dirLocation.setImageResource(if (directory.location == LOCATION_SD) org.fossify.commons.R.drawable.ic_sd_card_vector else org.fossify.commons.R.drawable.ic_usb_vector)
            }

            photoCnt.text = directory.subfoldersMediaCount.toString()
            photoCnt.beVisibleIf(showMediaCount == FOLDER_MEDIA_CNT_LINE)

            if (limitFolderTitle) {
                dirName.setSingleLine()
                dirName.ellipsize = TextUtils.TruncateAt.MIDDLE
            }

            var nameCount = directory.name
            if (showMediaCount == FOLDER_MEDIA_CNT_BRACKETS) {
                nameCount += " (${directory.subfoldersMediaCount})"
            }

            if (groupDirectSubfolders) {
                if (directory.subfoldersCount > 1) {
                    nameCount += " [${directory.subfoldersCount}]"
                }
            }

            dirName.text = nameCount

            if (isListViewType || folderStyle == FOLDER_STYLE_ROUNDED_CORNERS) {
                photoCnt.setTextColor(textColor)
                dirName.setTextColor(textColor)
                dirLocation.applyColorFilter(textColor)
            }

            if (isListViewType) {
                dirPath?.setTextColor(textColor)
                dirPin.applyColorFilter(textColor)
                dirLocation.applyColorFilter(textColor)
                dirDragHandle.beVisibleIf(isDragAndDropping)
            } else {
                dirDragHandleWrapper?.beVisibleIf(isDragAndDropping)
            }

            if (isDragAndDropping) {
                dirDragHandle.applyColorFilter(textColor)
                dirDragHandle.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startReorderDragListener?.requestDrag(holder)
                    }
                    false
                }
            }
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dirs, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dirs, i, i - 1)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {
        swipeRefreshLayout?.isEnabled = false
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {
        swipeRefreshLayout?.isEnabled = activity.config.enablePullToRefresh
    }

    override fun onChange(position: Int) = dirs.getOrNull(position)?.getBubbleText(directorySorting, activity, dateFormat, timeFormat) ?: ""

    private fun bindItem(view: View): DirectoryItemBinding {
        return when {
            isListViewType -> DirectoryItemListBinding.bind(view).toItemBinding()
            folderStyle == FOLDER_STYLE_SQUARE -> DirectoryItemGridSquareBinding.bind(view).toItemBinding()
            else -> DirectoryItemGridRoundedCornersBinding.bind(view).toItemBinding()
        }
    }
}
