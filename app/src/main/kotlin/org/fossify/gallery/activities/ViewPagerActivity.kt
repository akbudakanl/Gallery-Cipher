package org.fossify.gallery.activities

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.print.PrintHelper
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.AppBarLayout
import org.fossify.commons.dialogs.PropertiesDialog
import org.fossify.commons.dialogs.RenameItemDialog
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.convertToBitmap
import org.fossify.commons.extensions.formatSize
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getDataColumn
import org.fossify.commons.extensions.getDoesFilePathExist
import org.fossify.commons.extensions.getDuration
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getFinalUriFromPath
import org.fossify.commons.extensions.getImageResolution
import org.fossify.commons.extensions.getIsPathDirectory
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getUriMimeType
import org.fossify.commons.extensions.handleDeletePasswordProtection
import org.fossify.commons.extensions.handleLockedFolderOpening
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.internalStoragePath
import org.fossify.commons.extensions.isAStorageRootFolder
import org.fossify.commons.extensions.isExternalStorageManager
import org.fossify.commons.extensions.isGif
import org.fossify.commons.extensions.isMediaFile
import org.fossify.commons.extensions.isPortrait
import org.fossify.commons.extensions.isRawFast
import org.fossify.commons.extensions.isSvg
import org.fossify.commons.extensions.isVideoFast
import org.fossify.commons.extensions.needsStupidWritePermissions
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.commons.extensions.recycleBinPath
import org.fossify.commons.extensions.rescanPaths
import org.fossify.commons.extensions.scanPathRecursively
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.tryGenericMimeType
import org.fossify.commons.extensions.updateBrightness
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.FAVORITES
import org.fossify.commons.helpers.IS_FROM_GALLERY
import org.fossify.commons.helpers.NOMEDIA
import org.fossify.commons.helpers.REAL_FILE_PATH
import org.fossify.commons.helpers.REQUEST_EDIT_IMAGE
import org.fossify.commons.helpers.REQUEST_SET_AS
import org.fossify.commons.helpers.SORT_BY_RANDOM
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isRPlus
import org.fossify.commons.models.FileDirItem
import org.fossify.gallery.BuildConfig
import org.fossify.gallery.R
import org.fossify.gallery.adapters.MyPagerAdapter
import org.fossify.gallery.asynctasks.GetMediaAsynctask
import org.fossify.gallery.databinding.ActivityMediumBinding
import org.fossify.gallery.dialogs.DeleteWithRememberDialog
import org.fossify.gallery.dialogs.EncryptedPropertiesDialog
import org.fossify.gallery.dialogs.RenameEncryptedItemDialog
import org.fossify.gallery.dialogs.SaveAsDialog
import org.fossify.gallery.dialogs.SlideshowDialog
import org.fossify.gallery.extensions.canAccessHiddenVirtualAlbums
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.promptVaultUnlock
import org.fossify.gallery.extensions.deleteDBPath
import org.fossify.gallery.extensions.deleteVirtualAlbumItemFromAllAlbums
import org.fossify.gallery.extensions.favoritesDB
import org.fossify.gallery.extensions.fixDateTaken
import org.fossify.gallery.extensions.getFavoritePaths
import org.fossify.gallery.extensions.getVirtualAlbumId
import org.fossify.gallery.extensions.isHiddenVirtualAlbumPath
import org.fossify.gallery.extensions.isPathHiddenOrEncrypted
import org.fossify.gallery.extensions.isVirtualAlbumPath
import org.fossify.gallery.extensions.removePathsFromVirtualAlbum
import org.fossify.gallery.extensions.getShortcutImage
import org.fossify.gallery.extensions.handleMediaManagementPrompt
import org.fossify.gallery.extensions.hideSystemUI
import org.fossify.gallery.extensions.isDownloadsFolder
import org.fossify.gallery.extensions.launchResizeImageDialog
import org.fossify.gallery.extensions.launchSettings
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.extensions.movePathsInRecycleBin
import org.fossify.gallery.extensions.openEditor
import org.fossify.gallery.extensions.openPath
import org.fossify.gallery.extensions.restoreRecycleBinPath
import org.fossify.gallery.extensions.saveRotatedImageToFile
import org.fossify.gallery.extensions.setAs
import org.fossify.gallery.extensions.shareMediumPath
import org.fossify.gallery.extensions.showFileOnMap
import org.fossify.gallery.extensions.showSystemUI
import org.fossify.gallery.extensions.toggleFileVisibility
import org.fossify.gallery.extensions.tryCopyMoveFilesTo
import org.fossify.gallery.extensions.tryDeleteFileDirItem
import org.fossify.gallery.extensions.updateDBMediaPath
import org.fossify.gallery.extensions.updateFavorite
import org.fossify.gallery.extensions.updateFavoritePaths
import org.fossify.gallery.fragments.PhotoFragment
import org.fossify.gallery.fragments.VideoFragment
import org.fossify.gallery.fragments.ViewPagerFragment
import org.fossify.gallery.helpers.AspectRatioOrientation
import org.fossify.gallery.helpers.BOTTOM_ACTION_CHANGE_ORIENTATION
import org.fossify.gallery.helpers.BOTTOM_ACTION_COPY
import org.fossify.gallery.helpers.BOTTOM_ACTION_DELETE
import org.fossify.gallery.helpers.BOTTOM_ACTION_EDIT
import org.fossify.gallery.helpers.BOTTOM_ACTION_MOVE
import org.fossify.gallery.helpers.BOTTOM_ACTION_PROPERTIES
import org.fossify.gallery.helpers.BOTTOM_ACTION_RENAME
import org.fossify.gallery.helpers.BOTTOM_ACTION_RESIZE
import org.fossify.gallery.helpers.BOTTOM_ACTION_ROTATE
import org.fossify.gallery.helpers.BOTTOM_ACTION_SET_AS
import org.fossify.gallery.helpers.BOTTOM_ACTION_SHARE
import org.fossify.gallery.helpers.BOTTOM_ACTION_SHOW_ON_MAP
import org.fossify.gallery.helpers.BOTTOM_ACTION_SLIDESHOW
import org.fossify.gallery.helpers.BOTTOM_ACTION_TOGGLE_FAVORITE
import org.fossify.gallery.helpers.BOTTOM_ACTION_TOGGLE_VISIBILITY
import org.fossify.gallery.helpers.ColorModeHelper
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.VAULT_RESUME_STATE
import org.fossify.gallery.helpers.VaultResumeState
import org.fossify.gallery.helpers.DefaultPageTransformer
import org.fossify.gallery.helpers.FadePageTransformer
import org.fossify.gallery.helpers.GO_TO_NEXT_ITEM
import org.fossify.gallery.helpers.GO_TO_PREV_ITEM
import org.fossify.gallery.helpers.HIDE_SYSTEM_UI_DELAY
import org.fossify.gallery.helpers.IS_VIEW_INTENT
import org.fossify.gallery.helpers.MAX_PRINT_SIDE_SIZE
import org.fossify.gallery.helpers.PATH
import org.fossify.gallery.helpers.PORTRAIT_PATH
import org.fossify.gallery.helpers.RECYCLE_BIN
import org.fossify.gallery.helpers.ROTATE_BY_ASPECT_RATIO
import org.fossify.gallery.helpers.ROTATE_BY_DEVICE_ROTATION
import org.fossify.gallery.helpers.ROTATE_BY_SYSTEM_SETTING
import org.fossify.gallery.helpers.EXTRA_SECRET_VAULT_FILES
import org.fossify.gallery.helpers.SHOW_ALL
import org.fossify.gallery.helpers.SHOW_FAVORITES
import org.fossify.gallery.helpers.SHOW_VIRTUAL_ALBUM
import org.fossify.gallery.helpers.SHOW_NEXT_ITEM
import org.fossify.gallery.helpers.SHOW_PREV_ITEM
import org.fossify.gallery.helpers.SHOW_RECYCLE_BIN
import org.fossify.gallery.helpers.SKIP_AUTHENTICATION
import org.fossify.gallery.helpers.SLIDESHOW_ANIMATION_FADE
import org.fossify.gallery.helpers.SLIDESHOW_ANIMATION_NONE
import org.fossify.gallery.helpers.SLIDESHOW_ANIMATION_SLIDE
import org.fossify.gallery.helpers.SLIDESHOW_DEFAULT_INTERVAL
import org.fossify.gallery.helpers.SLIDESHOW_FADE_DURATION
import org.fossify.gallery.helpers.SLIDESHOW_SLIDE_DURATION
import org.fossify.gallery.helpers.SLIDESHOW_START_ON_ENTER
import org.fossify.gallery.helpers.TYPE_GIFS
import org.fossify.gallery.helpers.TYPE_IMAGES
import org.fossify.gallery.helpers.TYPE_PORTRAITS
import org.fossify.gallery.helpers.TYPE_RAWS
import org.fossify.gallery.helpers.TYPE_SVGS
import org.fossify.gallery.helpers.TYPE_VIDEOS
import org.fossify.gallery.helpers.getPermissionToRequest
import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.ThumbnailItem
import java.io.File
import kotlin.math.min

@Suppress("UNCHECKED_CAST")
class ViewPagerActivity : BaseViewerActivity(), ViewPager.OnPageChangeListener, ViewPagerFragment.FragmentListener {
    companion object {
        private const val REQUEST_VIEW_VIDEO = 1
        private const val SAVED_PATH = "current_path"
    }

    private var mPath = ""
    private var mDirectory = ""
    private var mIsFullScreen = false
    private var mPos = -1
    private var mShowAll = false
    private var mIsVaultFilesView = false
    private var mIsSlideshowActive = false
    private var mPrevHashcode = 0

    private var mSlideshowHandler = Handler()
    private var mSlideshowInterval = SLIDESHOW_DEFAULT_INTERVAL
    private var mSlideshowMoveBackwards = false
    private var mSlideshowMedia = mutableListOf<Medium>()
    private var mAreSlideShowMediaVisible = false
    private var mRandomSlideshowStopped = false

    private var mIsOrientationLocked = false

    // The screen orientation that was applied last, so that the media is not rotated twice.
    private var mAppliedScreenOrientation = 0
    // The callbacks waiting for the orientation of the media with the given path.
    private val mOrientationCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()
    // The paths whose orientation is currently being resolved in the background.
    private val mOngoingOrientationRequests = mutableSetOf<String>()

    private var mMediaFiles = ArrayList<Medium>()
    // MediaActivity keeps the album in a process-local cache. After Android reclaims the
    // process while this activity is in the background, that cache is empty and we must
    // reload the album even when random sorting would normally preserve the cached order.
    private var mHasCachedAlbumMedia = false

    // "Continue where I left off" after the vault keys were wiped from RAM (30 min in background / process restart)
    private var mIsVaultResumePrompting = false
    private var mVaultResumeBlob: String? = null
    private var mFavoritePaths = ArrayList<String>()
    private var mIgnoredPaths = ArrayList<String>()
    private val mVideoPlaybackPositions = mutableMapOf<String, Long>()
    private var mOriginalBrightness: Float? = null

    private val binding by viewBinding(ActivityMediumBinding::inflate)

    override val contentHolder: View
        get() = binding.fragmentHolder

    override val appBarLayout: AppBarLayout
        get() = binding.mediumViewerAppbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mVaultResumeBlob = savedInstanceState?.getString(VAULT_RESUME_STATE)
        setupEdgeToEdge(
            padBottomSystem = listOf(binding.bottomActions.bottomActionsWrapper),
        )

        setupOptionsMenu()
        refreshMenuItems()

        window.decorView.setBackgroundColor(getProperBackgroundColor())
        (MediaActivity.mMedia.clone() as ArrayList<ThumbnailItem>).filterIsInstanceTo(mMediaFiles, Medium::class.java)
        mHasCachedAlbumMedia = mMediaFiles.isNotEmpty()

        requestMediaPermissions {
            initViewPager(
                savedPath = savedInstanceState?.getString(SAVED_PATH).orEmpty()
            )
        }

        initFavorites()
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermission(getPermissionToRequest())) {
            finish()
            return
        }

        val currentPath = getCurrentPath().ifEmpty { mPath }
        val requiresVaultAccess = CryptoManager.isEncryptedFile(currentPath) ||
            (mDirectory.isNotEmpty() && !mDirectory.isVirtualAlbumPath() && CryptoManager.isEncryptedFolderPath(this, mDirectory)) ||
            (mDirectory.isVirtualAlbumPath() && isHiddenVirtualAlbumPath(mDirectory))
        applyVaultScreenProtection(requiresVaultAccess)
        if (requiresVaultAccess && !canAccessHiddenVirtualAlbums()) {
            // The vault keys were wiped from RAM: hide the viewer, ask for the password again and show the same
            // file again instead of closing it.
            resumeAfterVaultWipe()
            return
        }

        initBottomActions()
        mOriginalBrightness = window.updateBrightness(config.maxBrightness, mOriginalBrightness)
        setupOrientation()
        refreshMenuItems()

        val filename = getCurrentMedium()?.name?.ifEmpty { null } ?: CryptoManager.getDisplayName(mPath)
        binding.mediumViewerToolbar.title = filename
    }

    override fun onPause() {
        super.onPause()
        stopSlideshow()
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoFragment.clearFrameCache()
        ColorModeHelper.resetColorMode(this)

        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            config.temporarilyShowHidden = false
        }

        if (config.isThirdPartyIntent) {
            config.isThirdPartyIntent = false

            if (intent.extras == null || isExternalIntent()) {
                mMediaFiles.clear()
            }
        }
    }

    fun refreshMenuItems() {
        val currentMedium = getCurrentMedium() ?: return
        currentMedium.isFavorite = mFavoritePaths.contains(currentMedium.path)
        val visibleBottomActions = getVisibleBottomActions()

        runOnUiThread {
            val rotationDegrees = getCurrentPhotoFragment()?.mCurrentRotationDegrees ?: 0
            binding.mediumViewerToolbar.menu.apply {
                findItem(R.id.menu_show_on_map).isVisible = visibleBottomActions and BOTTOM_ACTION_SHOW_ON_MAP == 0
                findItem(R.id.menu_slideshow).isVisible = visibleBottomActions and BOTTOM_ACTION_SLIDESHOW == 0
                findItem(R.id.menu_properties).isVisible = visibleBottomActions and BOTTOM_ACTION_PROPERTIES == 0
                findItem(R.id.menu_delete).isVisible = visibleBottomActions and BOTTOM_ACTION_DELETE == 0
                findItem(R.id.menu_share).isVisible = visibleBottomActions and BOTTOM_ACTION_SHARE == 0
                findItem(R.id.menu_edit).isVisible = visibleBottomActions and BOTTOM_ACTION_EDIT == 0 && !currentMedium.isSVG()
                findItem(R.id.menu_shuffle).isVisible = isRandomSortingEnabled() && !isExternalIntent() && !mAreSlideShowMediaVisible && !mRandomSlideshowStopped && mMediaFiles.size > 1
                findItem(R.id.menu_rename).isVisible = visibleBottomActions and BOTTOM_ACTION_RENAME == 0 && !currentMedium.getIsInRecycleBin()
                findItem(R.id.menu_rotate).isVisible = currentMedium.isImage() && visibleBottomActions and BOTTOM_ACTION_ROTATE == 0
                findItem(R.id.menu_set_as).isVisible = visibleBottomActions and BOTTOM_ACTION_SET_AS == 0
                findItem(R.id.menu_copy_to_clipboard).isVisible = currentMedium.isImage() && !CryptoManager.isVaultUnlocked && !config.temporarilyShowHidden
                findItem(R.id.menu_copy_to).isVisible = visibleBottomActions and BOTTOM_ACTION_COPY == 0
                findItem(R.id.menu_move_to).isVisible = visibleBottomActions and BOTTOM_ACTION_MOVE == 0 && !mDirectory.isVirtualAlbumPath()
                findItem(R.id.menu_save_as).isVisible = rotationDegrees != 0
                findItem(R.id.menu_print).isVisible = currentMedium.isImage() || currentMedium.isRaw()
                findItem(R.id.menu_resize).isVisible = visibleBottomActions and BOTTOM_ACTION_RESIZE == 0 && currentMedium.isImage()
                findItem(R.id.menu_hide).isVisible =
                    (!isRPlus() || isExternalStorageManager()) && !currentMedium.isHidden() && visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY == 0 && !currentMedium.getIsInRecycleBin()

                findItem(R.id.menu_unhide).isVisible =
                    (!isRPlus() || isExternalStorageManager()) && currentMedium.isHidden() && visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY == 0 && !currentMedium.getIsInRecycleBin()

                findItem(R.id.menu_add_to_favorites).isVisible =
                    !currentMedium.isFavorite && visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE == 0 && !currentMedium.getIsInRecycleBin()

                findItem(R.id.menu_remove_from_favorites).isVisible =
                    currentMedium.isFavorite && visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE == 0 && !currentMedium.getIsInRecycleBin()
                findItem(R.id.menu_remove_from_virtual_album).isVisible = mDirectory.isVirtualAlbumPath()

                findItem(R.id.menu_restore_file).isVisible = currentMedium.getIsInRecycleBin() || currentMedium.path.startsWith(recycleBinPath) || CryptoManager.isVaultRecycleBinPath(currentMedium.path)
                findItem(R.id.menu_create_shortcut).isVisible = true
                findItem(R.id.menu_change_orientation).isVisible = rotationDegrees == 0 && visibleBottomActions and BOTTOM_ACTION_CHANGE_ORIENTATION == 0
                findItem(R.id.menu_rotate).setShowAsAction(
                    if (rotationDegrees != 0) {
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    } else {
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                    }
                )
            }

            // Inside the encrypted vault only management actions are allowed.
            // Exposure-type actions (share, reveal, edit, wallpaper...) are removed.
            val isVaultMedium = CryptoManager.isVaultMediaPath(this@ViewPagerActivity, currentMedium.path) ||
                (mDirectory.isVirtualAlbumPath() && isHiddenVirtualAlbumPath(mDirectory))
            binding.mediumViewerToolbar.menu.apply {
                // Favorites, share and edit are completely disabled for hidden files.
                if (isVaultMedium || currentMedium.isHidden()) {
                    findItem(R.id.menu_share).isVisible = false
                    findItem(R.id.menu_edit).isVisible = false
                    findItem(R.id.menu_add_to_favorites).isVisible = false
                    findItem(R.id.menu_remove_from_favorites).isVisible = false
                }
                if (isVaultMedium) {
                    // Delete replaces the slideshow button in the toolbar,
                    // the slideshow itself moves into the overflow menu.
                    findItem(R.id.menu_delete).apply {
                        isVisible = true
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    }
                    findItem(R.id.menu_slideshow).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                    findItem(R.id.menu_rotate).isVisible = false
                    findItem(R.id.menu_hide).isVisible = false
                    findItem(R.id.menu_unhide).isVisible = false
                    findItem(R.id.menu_create_shortcut).isVisible = false
                    findItem(R.id.menu_open_with).isVisible = false
                    findItem(R.id.menu_set_as).isVisible = false
                    findItem(R.id.menu_print).isVisible = false
                    findItem(R.id.menu_resize).isVisible = false
                    findItem(R.id.menu_show_on_map).isVisible = false
                }
            }

            if (visibleBottomActions != 0) {
                updateBottomActionIcons(currentMedium)
            }
            initBottomActionsLayout()
        }
    }

    private fun setupOptionsMenu() {
        binding.mediumViewerToolbar.apply {
            setTitleTextColor(Color.WHITE)
            overflowIcon = resources.getColoredDrawableWithColor(org.fossify.commons.R.drawable.ic_three_dots_vector, Color.WHITE)
            navigationIcon = resources.getColoredDrawableWithColor(org.fossify.commons.R.drawable.ic_arrow_left_vector, Color.WHITE)
        }

        updateMenuItemColors(binding.mediumViewerToolbar.menu, forceWhiteIcons = true)
        binding.mediumViewerToolbar.setOnMenuItemClickListener { menuItem ->
            if (getCurrentMedium() == null) {
                return@setOnMenuItemClickListener true
            }

            when (menuItem.itemId) {
                R.id.menu_set_as -> setAs(getCurrentPath())
                R.id.menu_slideshow -> initSlideshow()
                R.id.menu_copy_to -> checkMediaManagementAndCopy(true)
                R.id.menu_move_to -> moveFileTo()
                R.id.menu_open_with -> openPath(getCurrentPath(), true)
                R.id.menu_hide -> toggleFileVisibility(true)
                R.id.menu_unhide -> toggleFileVisibility(false)
                R.id.menu_share -> shareMediumPath(getCurrentPath())
                R.id.menu_delete -> checkDeleteConfirmation()
                R.id.menu_shuffle -> shuffleMedia()
                R.id.menu_rename -> checkMediaManagementAndRename()
                R.id.menu_print -> printFile()
                R.id.menu_edit -> openEditor(getCurrentPath())
                R.id.menu_properties -> showProperties()
                R.id.menu_show_on_map -> showFileOnMap(getCurrentPath())
                R.id.menu_rotate_right -> rotateImage(90)
                R.id.menu_rotate_left -> rotateImage(-90)
                R.id.menu_rotate_one_eighty -> rotateImage(180)
                R.id.menu_add_to_favorites -> toggleFavorite()
                R.id.menu_remove_from_favorites -> toggleFavorite()
                R.id.menu_remove_from_virtual_album -> removeCurrentFromVirtualAlbum()
                R.id.menu_restore_file -> restoreFile()
                R.id.menu_force_portrait -> toggleOrientation(SCREEN_ORIENTATION_PORTRAIT)
                R.id.menu_force_landscape -> toggleOrientation(SCREEN_ORIENTATION_LANDSCAPE)
                R.id.menu_force_landscape_reverse -> toggleOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                R.id.menu_default_orientation -> toggleOrientation(SCREEN_ORIENTATION_UNSPECIFIED)
                R.id.menu_save_as -> saveImageAs()
                R.id.menu_create_shortcut -> createShortcut()
                R.id.menu_resize -> resizeImage()
                R.id.menu_settings -> launchSettings()
                R.id.menu_copy_to_clipboard -> copyImageToClipboard()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

        binding.mediumViewerToolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE && resultCode == Activity.RESULT_OK && resultData != null) {
            mPos = -1
            mPrevHashcode = 0
            refreshViewPager()
        } else if (requestCode == REQUEST_SET_AS && resultCode == Activity.RESULT_OK) {
            toast(R.string.wallpaper_set_successfully)
        } else if (requestCode == REQUEST_VIEW_VIDEO && resultCode == Activity.RESULT_OK && resultData != null) {
            if (resultData.getBooleanExtra(GO_TO_NEXT_ITEM, false)) {
                goToNextItem()
            } else if (resultData.getBooleanExtra(GO_TO_PREV_ITEM, false)) {
                goToPrevItem()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initBottomActionsLayout()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_PATH, getCurrentPath())

        // Encrypted with the vault key: unreadable while locked, readable again after the password was entered.
        val currentPath = getCurrentPath().ifEmpty { mPath }
        val isVaultItem = CryptoManager.isEncryptedFile(currentPath) ||
            (mDirectory.isNotEmpty() && !mDirectory.isVirtualAlbumPath() && CryptoManager.isEncryptedFolderPath(this, mDirectory))
        if (!isVaultItem) return

        if (CryptoManager.isVaultUnlocked) {
            VaultResumeState(
                albumPath = mDirectory,
                openedFilePath = currentPath,
                openedFileName = CryptoManager.getDisplayName(currentPath),
                sorting = config.getFolderSorting(mDirectory),
                viewType = config.getFolderViewType(mDirectory),
                grouping = config.getFolderGrouping(mDirectory),
                scrollPosition = mPos.coerceAtLeast(0)
            ).encrypt(this)?.let { outState.putString(VAULT_RESUME_STATE, it) }
        } else {
            mVaultResumeBlob?.let { outState.putString(VAULT_RESUME_STATE, it) }
        }
    }

    private fun resumeAfterVaultWipe() {
        if (mIsVaultResumePrompting) return

        // remember which file was open, then drop the fragments (decrypted bitmaps) and the file list from RAM
        val currentPath = getCurrentPath()
        if (currentPath.isNotEmpty()) {
            mPath = currentPath
        }
        stopSlideshow()
        binding.viewPager.removeOnPageChangeListener(this)
        binding.viewPager.adapter = null
        binding.viewPager.visibility = View.INVISIBLE
        mMediaFiles = ArrayList()
        mHasCachedAlbumMedia = false
        mPrevHashcode = 0

        mIsVaultResumePrompting = true
        promptVaultUnlock { success ->
            mIsVaultResumePrompting = false
            if (!success) {
                finish()
                return@promptVaultUnlock
            }

            // process restart case: the live state is gone, read the encrypted blob now that the key is back
            VaultResumeState.decrypt(mVaultResumeBlob)?.let { saved ->
                if (currentPath.isEmpty() && saved.openedFilePath.isNotEmpty() && File(saved.openedFilePath).exists()) {
                    mPath = saved.openedFilePath
                }
            }
            mVaultResumeBlob = null

            binding.viewPager.visibility = View.VISIBLE
            refreshViewPager(true)
            refreshMenuItems()
        }
    }

    private fun initViewPager(savedPath: String) {
        val uri = intent.data
        if (uri != null) {
            mPath = savedPath.ifEmpty { getDataColumn(uri).orEmpty() }
        } else {
            try {
                mPath = savedPath.ifEmpty { intent.getStringExtra(PATH).orEmpty() }

                // make sure "Open Recycle Bin" works well with "Show all folders content"
                mIsVaultFilesView = intent.getBooleanExtra(EXTRA_SECRET_VAULT_FILES, false)
                mShowAll = (config.showAll || mIsVaultFilesView) && (mPath.isNotEmpty() && !mPath.startsWith(recycleBinPath))
            } catch (e: Exception) {
                showErrorToast(e)
                finish()
                return
            }
        }

        if (savedPath.isEmpty() && intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            mPath = intent.extras!!.getString(REAL_FILE_PATH)!!
        }

        if (mPath.isEmpty()) {
            toast(org.fossify.commons.R.string.unknown_error_occurred)
            finish()
            return
        }

        if (mPath.isPortrait() && getPortraitPath() == "") {
            val newIntent = Intent(this, ViewPagerActivity::class.java)
            newIntent.putExtras(intent!!.extras!!)
            newIntent.putExtra(PORTRAIT_PATH, mPath)
            newIntent.putExtra(PATH, "${mPath.getParentPath().getParentPath()}/${mPath.getFilenameFromPath()}")

            startActivity(newIntent)
            finish()
            return
        }

        if (!getDoesFilePathExist(mPath) && getPortraitPath() == "") {
            finish()
            return
        }

        showSystemUI()

        if (intent.getBooleanExtra(SKIP_AUTHENTICATION, false)) {
            initContinue()
        } else {
            handleLockedFolderOpening(mPath.getParentPath()) { success ->
                if (success) {
                    initContinue()
                } else {
                    finish()
                }
            }
        }
    }

    private fun initContinue() {
        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            if (isShowHiddenFlagNeeded()) {
                if (!config.isHiddenPasswordProtectionOn) {
                    config.temporarilyShowHidden = true
                }
            }

            config.isThirdPartyIntent = true
        }

        val isShowingFavorites = intent.getBooleanExtra(SHOW_FAVORITES, false)
        val isShowingRecycleBin = intent.getBooleanExtra(SHOW_RECYCLE_BIN, false)
        val virtualAlbumPath = intent.getStringExtra(SHOW_VIRTUAL_ALBUM) ?: ""
        mDirectory = when {
            isShowingFavorites -> FAVORITES
            virtualAlbumPath.isVirtualAlbumPath() -> virtualAlbumPath
            isShowingRecycleBin -> RECYCLE_BIN
            else -> mPath.getParentPath()
        }
        binding.mediumViewerToolbar.title = CryptoManager.getDisplayName(mPath)

        binding.viewPager.onGlobalLayout {
            if (!isDestroyed) {
                if (mMediaFiles.isNotEmpty()) {
                    gotMedia(mMediaFiles as ArrayList<ThumbnailItem>, refetchViewPagerPosition = true)
                    checkSlideshowOnEnter()
                }
            }
        }

        // show the selected image asap, while loading the rest in the background to allow swiping between them. Might be needed at third party intents
        if (mMediaFiles.isEmpty() && mPath.isNotEmpty() && mDirectory != FAVORITES && !mDirectory.isVirtualAlbumPath()) {
            val encMeta = if (CryptoManager.isEncryptedFile(mPath)) CryptoManager.readMetadata(File(mPath)) else null
            val filename = encMeta?.originalName ?: mPath.getFilenameFromPath()
            val folder = mPath.getParentPath()
            val type = encMeta?.mediaType ?: getTypeFromPath(mPath)
            val medium = Medium(
                null,
                filename,
                mPath,
                folder,
                encMeta?.lastModified ?: 0,
                encMeta?.dateTaken ?: 0,
                encMeta?.originalSize ?: 0,
                type,
                encMeta?.videoDuration ?: 0,
                false,
                0L,
                0L
            )
            mMediaFiles.add(medium)
            gotMedia(mMediaFiles as ArrayList<ThumbnailItem>, refetchViewPagerPosition = true)
        }

        refreshViewPager(true)
        binding.viewPager.offscreenPageLimit = 3

        if (config.blackBackground) {
            binding.fragmentHolder.background = Color.BLACK.toDrawable()
            binding.viewPager.background = Color.BLACK.toDrawable()
        }

        if (config.hideSystemUI) {
            binding.viewPager.onGlobalLayout {
                Handler().postDelayed({
                    fragmentClicked()
                }, HIDE_SYSTEM_UI_DELAY)
            }
        }

        if (
            intent.action == "com.android.camera.action.REVIEW"
            || intent.action == MediaStore.ACTION_REVIEW
        ) {
            ensureBackgroundThread {
                if (!isPathHiddenOrEncrypted(mPath) && mediaDB.getMediaFromPath(mPath).isEmpty()) {
                    val filename = mPath.getFilenameFromPath()
                    val parent = mPath.getParentPath()
                    val type = getTypeFromPath(mPath)
                    val isFavorite = favoritesDB.isFavorite(mPath)
                    val duration = if (type == TYPE_VIDEOS) getDuration(mPath) ?: 0 else 0
                    val ts = System.currentTimeMillis()
                    val medium = Medium(null, filename, mPath, parent, ts, ts, File(mPath).length(), type, duration, isFavorite, 0, 0L)
                    mediaDB.insert(medium)
                }
            }
        }
    }

    private fun getTypeFromPath(path: String): Int {
        return when {
            path.isVideoFast() -> TYPE_VIDEOS
            path.isGif() -> TYPE_GIFS
            path.isSvg() -> TYPE_SVGS
            path.isRawFast() -> TYPE_RAWS
            path.isPortrait() -> TYPE_PORTRAITS
            else -> TYPE_IMAGES
        }
    }

    private fun initBottomActions() {
        initBottomActionButtons()
        initBottomActionsLayout()
    }

    private fun initFavorites() {
        ensureBackgroundThread {
            mFavoritePaths = getFavoritePaths()
        }
    }

    private fun setupOrientation() {
        if (!mIsOrientationLocked) {
            if (config.screenRotation == ROTATE_BY_DEVICE_ROTATION) {
                requestedOrientation = SCREEN_ORIENTATION_SENSOR
            } else if (config.screenRotation == ROTATE_BY_SYSTEM_SETTING) {
                requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    private fun updatePagerItems(media: MutableList<Medium>) {
        val pagerAdapter = MyPagerAdapter(this, supportFragmentManager, media)
        if (!isDestroyed) {
            pagerAdapter.shouldInitFragment = mPos < 5
            binding.viewPager.apply {
                // must remove the listener before changing adapter, otherwise it might cause `mPos` to be set to 0
                removeOnPageChangeListener(this@ViewPagerActivity)
                adapter = pagerAdapter
                pagerAdapter.shouldInitFragment = true
                addOnPageChangeListener(this@ViewPagerActivity)
                currentItem = mPos
            }
        }
    }

    private fun checkSlideshowOnEnter() {
        if (intent.getBooleanExtra(SLIDESHOW_START_ON_ENTER, false)) {
            initSlideshow()
        }
    }

    private fun initSlideshow() {
        SlideshowDialog(this) {
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        if (getMediaForSlideshow()) {
            binding.viewPager.onGlobalLayout {
                if (!isDestroyed) {
                    if (config.slideshowAnimation == SLIDESHOW_ANIMATION_FADE) {
                        binding.viewPager.setPageTransformer(false, FadePageTransformer())
                    }

                    hideSystemUI()
                    if (!mIsFullScreen) {
                        mIsFullScreen = true
                        fullscreenToggled()
                    }
                    mRandomSlideshowStopped = false
                    mSlideshowInterval = config.slideshowInterval
                    mSlideshowMoveBackwards = config.slideshowMoveBackwards
                    mIsSlideshowActive = true
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    scheduleSwipe()
                }
            }
        }
    }

    private fun goToNextMedium(forward: Boolean) {
        val oldPosition = binding.viewPager.currentItem
        val newPosition = if (forward) oldPosition + 1 else oldPosition - 1
        if (newPosition == -1 || newPosition > binding.viewPager.adapter!!.count - 1) {
            slideshowEnded(forward)
        } else {
            binding.viewPager.setCurrentItem(newPosition, false)
        }
    }

    private fun animatePagerTransition(forward: Boolean) {
        val oldPosition = binding.viewPager.currentItem
        val animator = ValueAnimator.ofInt(0, binding.viewPager.width)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                if (binding.viewPager.isFakeDragging) {
                    try {
                        binding.viewPager.endFakeDrag()
                    } catch (ignored: Exception) {
                        stopSlideshow()
                    }

                    if (binding.viewPager.currentItem == oldPosition) {
                        slideshowEnded(forward)
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                binding.viewPager.endFakeDrag()
            }

            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })

        if (config.slideshowAnimation == SLIDESHOW_ANIMATION_SLIDE) {
            animator.interpolator = DecelerateInterpolator()
            animator.duration = SLIDESHOW_SLIDE_DURATION
        } else {
            animator.duration = SLIDESHOW_FADE_DURATION
        }

        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            var oldDragPosition = 0
            override fun onAnimationUpdate(animation: ValueAnimator) {
                if (binding.viewPager.isFakeDragging == true) {
                    val dragPosition = animation.animatedValue as Int
                    val dragOffset = dragPosition - oldDragPosition
                    oldDragPosition = dragPosition
                    try {
                        binding.viewPager.fakeDragBy(dragOffset * (if (forward) -1f else 1f))
                    } catch (e: Exception) {
                        stopSlideshow()
                    }
                }
            }
        })

        binding.viewPager.beginFakeDrag()
        animator.start()
    }

    private fun slideshowEnded(forward: Boolean) {
        if (config.loopSlideshow) {
            if (forward) {
                binding.viewPager.setCurrentItem(0, false)
            } else {
                binding.viewPager.setCurrentItem(binding.viewPager.adapter!!.count - 1, false)
            }
        } else {
            stopSlideshow()
            toast(R.string.slideshow_ended)
        }
    }

    private fun stopSlideshow() {
        if (mIsSlideshowActive) {
            binding.viewPager.setPageTransformer(false, DefaultPageTransformer())
            mIsSlideshowActive = false
            showSystemUI()
            mSlideshowHandler.removeCallbacksAndMessages(null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            mAreSlideShowMediaVisible = false

            if (config.slideshowRandomOrder) {
                mRandomSlideshowStopped = true
            }
        }
    }

    private fun scheduleSwipe() {
        mSlideshowHandler.removeCallbacksAndMessages(null)
        if (mIsSlideshowActive) {
            if (getCurrentMedium()!!.isImage() || getCurrentMedium()!!.isGIF() || getCurrentMedium()!!.isPortrait()) {
                mSlideshowHandler.postDelayed({
                    if (mIsSlideshowActive && !isDestroyed) {
                        swipeToNextMedium()
                    }
                }, mSlideshowInterval * 1000L)
            } else {
                (getCurrentFragment() as? VideoFragment)!!.playVideo()
            }
        }
    }

    private fun swipeToNextMedium() {
        if (config.slideshowAnimation == SLIDESHOW_ANIMATION_NONE) {
            goToNextMedium(!mSlideshowMoveBackwards)
        } else {
            animatePagerTransition(!mSlideshowMoveBackwards)
        }
    }

    private fun getMediaForSlideshow(): Boolean {
        mSlideshowMedia = mMediaFiles.filter {
            it.isImage() || it.isPortrait() || (config.slideshowIncludeVideos && it.isVideo() || (config.slideshowIncludeGIFs && it.isGIF()))
        }.toMutableList()

        if (config.slideshowRandomOrder) {
            mSlideshowMedia.shuffle()
            mPos = 0
        } else {
            mPath = getCurrentPath()
            mPos = getPositionInList(mSlideshowMedia)
        }

        return if (mSlideshowMedia.isEmpty()) {
            toast(R.string.no_media_for_slideshow)
            false
        } else {
            updatePagerItems(mSlideshowMedia)
            mAreSlideShowMediaVisible = true
            true
        }
    }

    private fun moveFileTo() {
        if (mDirectory.isVirtualAlbumPath()) {
            return
        }
        handleDeletePasswordProtection {
            checkMediaManagementAndCopy(false)
        }
    }

    private fun checkMediaManagementAndCopy(isCopyOperation: Boolean) {
        handleMediaManagementPrompt {
            copyMoveTo(isCopyOperation)
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val currPath = getCurrentPath()
        if (!isCopyOperation && currPath.startsWith(recycleBinPath)) {
            toast(org.fossify.commons.R.string.moving_recycle_bin_items_disabled, Toast.LENGTH_LONG)
            return
        }

        val fileDirItems = arrayListOf(FileDirItem(currPath, currPath.getFilenameFromPath()))
        tryCopyMoveFilesTo(fileDirItems, isCopyOperation, currentDirectoryPath = mDirectory) {
            config.tempFolderPath = ""
            if (it.isVirtualAlbumPath()) {
                return@tryCopyMoveFilesTo
            }
            val newPath = "$it/${currPath.getFilenameFromPath()}"
            rescanPaths(arrayListOf(newPath)) {
                fixDateTaken(arrayListOf(newPath), false)
            }

            if (!isCopyOperation) {
                refreshViewPager()
                updateFavoritePaths(fileDirItems, it)
            }
        }
    }

    private fun toggleFileVisibility(hide: Boolean, callback: (() -> Unit)? = null) {
        val currentPath = getCurrentPath()
        toggleFileVisibility(currentPath, hide) {
            if (it == currentPath && hide) {
                return@toggleFileVisibility
            }

            if (!config.shouldShowHidden && hide) {
                if (mPos in mMediaFiles.indices) {
                    mMediaFiles.removeAt(mPos)
                }
                if (mMediaFiles.isEmpty()) {
                    finish()
                } else {
                    refreshViewPager(refetchPosition = true)
                }
            } else {
                val newFileName = if (CryptoManager.isEncryptedFile(it)) {
                    CryptoManager.readMetadata(File(it))?.originalName ?: it.getFilenameFromPath()
                } else {
                    it.getFilenameFromPath()
                }
                binding.mediumViewerToolbar.title = newFileName

                getCurrentMedium()?.apply {
                    name = newFileName
                    path = it
                    getCurrentMedia().getOrNull(mPos)?.let { medium ->
                        medium.name = newFileName
                        medium.path = it
                    }
                }

                refreshMenuItems()
            }
            callback?.invoke()
        }
    }

    private fun rotateImage(degrees: Int) {
        val currentPath = getCurrentPath()
        if (needsStupidWritePermissions(currentPath)) {
            handleSAFDialog(currentPath) {
                if (it) {
                    rotateBy(degrees)
                }
            }
        } else {
            rotateBy(degrees)
        }
    }

    private fun rotateBy(degrees: Int) {
        getCurrentPhotoFragment()?.rotateImageViewBy(degrees)
        refreshMenuItems()
    }

    private fun toggleOrientation(orientation: Int) {
        requestedOrientation = orientation
        mIsOrientationLocked = orientation != SCREEN_ORIENTATION_UNSPECIFIED
        // The manually set orientation may be replaced by the aspect ratio of the media again.
        mAppliedScreenOrientation = 0
        refreshMenuItems()
    }

    private fun getChangeOrientationIcon(): Int {
        return if (mIsOrientationLocked) {
            if (requestedOrientation == SCREEN_ORIENTATION_PORTRAIT) {
                org.fossify.commons.R.drawable.ic_orientation_portrait_vector
            } else {
                org.fossify.commons.R.drawable.ic_orientation_landscape_vector
            }
        } else {
            org.fossify.commons.R.drawable.ic_orientation_auto_vector
        }
    }

    private fun saveImageAs() {
        val currPath = getCurrentPath()
        SaveAsDialog(this, currPath, false) {
            val newPath = it
            handleSAFDialog(it) {
                if (!it) {
                    return@handleSAFDialog
                }

                toast(org.fossify.commons.R.string.saving)
                ensureBackgroundThread {
                    val photoFragment = getCurrentPhotoFragment() ?: return@ensureBackgroundThread
                    saveRotatedImageToFile(currPath, newPath, photoFragment.mCurrentRotationDegrees, true) {
                        toast(org.fossify.commons.R.string.file_saved)
                        getCurrentPhotoFragment()?.mCurrentRotationDegrees = 0
                        refreshMenuItems()
                    }
                }
            }
        }
    }

    private fun createShortcut() {
        val manager = getSystemService(ShortcutManager::class.java)
        if (manager.isRequestPinShortcutSupported) {
            val medium = getCurrentMedium() ?: return
            val path = medium.path
            val drawable = resources.getDrawable(R.drawable.shortcut_image).mutate()
            getShortcutImage(path, drawable) {
                val intent = Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(PATH, path)
                    putExtra(SHOW_ALL, config.showAll)
                    putExtra(SHOW_FAVORITES, path == FAVORITES)
                    putExtra(SHOW_RECYCLE_BIN, path == RECYCLE_BIN)
                    action = Intent.ACTION_VIEW
                    flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                val shortcut = ShortcutInfo.Builder(this, path)
                    .setShortLabel(medium.name)
                    .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                    .setIntent(intent)
                    .build()

                manager.requestPinShortcut(shortcut, null)
            }
        }
    }

    private fun getCurrentPhotoFragment() = getCurrentFragment() as? PhotoFragment

    private fun getPortraitPath() = intent.getStringExtra(PORTRAIT_PATH) ?: ""

    private fun isShowHiddenFlagNeeded(): Boolean {
        val file = File(mPath)
        if (file.isHidden) {
            return true
        }

        var parent = file.parentFile ?: return false
        while (true) {
            if (parent.isHidden || parent.list()?.any { it.startsWith(NOMEDIA) } == true) {
                return true
            }

            if (parent.absolutePath == "/") {
                break
            }
            parent = parent.parentFile ?: return false
        }

        return false
    }

    private fun getCurrentFragment() = (binding.viewPager.adapter as? MyPagerAdapter)?.getCurrentFragment(binding.viewPager.currentItem)

    private fun showProperties() {
        if (getCurrentMedium() != null) {
            val currentPath = getCurrentPath()
            if (CryptoManager.isEncryptedFile(currentPath)) {
                EncryptedPropertiesDialog(this, currentPath)
            } else {
                PropertiesDialog(this, currentPath, false)
            }
        }
    }

    private fun initBottomActionsLayout() {
        if (config.bottomActions && !isCurrentMediumInVault() && !isCurrentMediumHidden()) {
            binding.bottomActions.root.beVisible()
        } else {
            binding.bottomActions.root.beGone()
        }
    }

    /**
     * The bottom actions are never shown for the files of the encrypted vault or for hidden files.
     * The actions that would be shown at the bottom stay available in the top menu in that case,
     * so that nothing gets lost.
     */
    private fun getVisibleBottomActions(): Int {
        if (!config.bottomActions || isCurrentMediumInVault() || isCurrentMediumHidden()) {
            return 0
        }

        return config.visibleBottomActions
    }

    private fun isCurrentMediumInVault(): Boolean {
        val currentMedium = getCurrentMedium() ?: return false
        return CryptoManager.isVaultMediaPath(this, currentMedium.path) ||
            (mDirectory.isVirtualAlbumPath() && isHiddenVirtualAlbumPath(mDirectory))
    }

    /**
     * True for the files that are hidden with the leading dot convention, no matter if they are
     * images, animated images or videos.
     */
    private fun isCurrentMediumHidden(): Boolean {
        val currentMedium = getCurrentMedium() ?: return false
        return isHiddenPath(currentMedium.path)
    }

    private fun isHiddenPath(path: String): Boolean {
        if (path.isEmpty() || CryptoManager.isEncryptedFile(path)) {
            return false
        }

        // A leading dot in the name of the file or in the name of any of its parent folders
        // hides it from the gallery.
        return path.contains("/.")
    }

    private fun initBottomActionButtons() {
        val currentMedium = getCurrentMedium()
        val visibleBottomActions = getVisibleBottomActions()
        binding.bottomActions.bottomFavorite.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE != 0 && currentMedium?.getIsInRecycleBin() == false)
        binding.bottomActions.bottomFavorite.setOnLongClickListener { toast(R.string.toggle_favorite); true }
        binding.bottomActions.bottomFavorite.setOnClickListener {
            toggleFavorite()
        }

        binding.bottomActions.bottomEdit.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_EDIT != 0 && currentMedium?.isSVG() == false)
        binding.bottomActions.bottomEdit.setOnLongClickListener { toast(R.string.edit); true }
        binding.bottomActions.bottomEdit.setOnClickListener {
            openEditor(getCurrentPath())
        }

        binding.bottomActions.bottomShare.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SHARE != 0)
        binding.bottomActions.bottomShare.setOnLongClickListener { toast(org.fossify.commons.R.string.share); true }
        binding.bottomActions.bottomShare.setOnClickListener {
            shareMediumPath(getCurrentPath())
        }

        binding.bottomActions.bottomDelete.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_DELETE != 0)
        binding.bottomActions.bottomDelete.setOnLongClickListener { toast(org.fossify.commons.R.string.delete); true }
        binding.bottomActions.bottomDelete.setOnClickListener {
            checkDeleteConfirmation()
        }

        binding.bottomActions.bottomRotate.beVisibleIf(
            visibleBottomActions and BOTTOM_ACTION_ROTATE != 0 && getCurrentMedium()?.isImage() == true
        )
        binding.bottomActions.bottomRotate.setOnLongClickListener { toast(R.string.rotate); true }
        binding.bottomActions.bottomRotate.setOnClickListener {
            rotateImage(90)
        }

        binding.bottomActions.bottomProperties.applyColorFilter(Color.WHITE)
        binding.bottomActions.bottomProperties.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_PROPERTIES != 0)
        binding.bottomActions.bottomProperties.setOnLongClickListener { toast(org.fossify.commons.R.string.properties); true }
        binding.bottomActions.bottomProperties.setOnClickListener {
            showProperties()
        }

        binding.bottomActions.bottomChangeOrientation.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_CHANGE_ORIENTATION != 0)
        binding.bottomActions.bottomChangeOrientation.setOnLongClickListener { toast(R.string.change_orientation); true }
        binding.bottomActions.bottomChangeOrientation.setOnClickListener {
            requestedOrientation = when (requestedOrientation) {
                SCREEN_ORIENTATION_PORTRAIT -> SCREEN_ORIENTATION_LANDSCAPE
                SCREEN_ORIENTATION_LANDSCAPE -> SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> SCREEN_ORIENTATION_UNSPECIFIED
                else -> SCREEN_ORIENTATION_PORTRAIT
            }
            mIsOrientationLocked = requestedOrientation != SCREEN_ORIENTATION_UNSPECIFIED
            updateBottomActionIcons(currentMedium)
        }

        binding.bottomActions.bottomSlideshow.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SLIDESHOW != 0)
        binding.bottomActions.bottomSlideshow.setOnLongClickListener { toast(R.string.slideshow); true }
        binding.bottomActions.bottomSlideshow.setOnClickListener {
            initSlideshow()
        }

        binding.bottomActions.bottomShowOnMap.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SHOW_ON_MAP != 0)
        binding.bottomActions.bottomShowOnMap.setOnLongClickListener { toast(R.string.show_on_map); true }
        binding.bottomActions.bottomShowOnMap.setOnClickListener {
            showFileOnMap(getCurrentPath())
        }

        binding.bottomActions.bottomToggleFileVisibility.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY != 0)
        binding.bottomActions.bottomToggleFileVisibility.setOnLongClickListener {
            toast(if (currentMedium?.isHidden() == true) org.fossify.commons.R.string.unhide else org.fossify.commons.R.string.hide); true
        }

        binding.bottomActions.bottomToggleFileVisibility.setOnClickListener {
            currentMedium?.apply {
                toggleFileVisibility(!isHidden()) {
                    updateBottomActionIcons(currentMedium)
                }
            }
        }

        binding.bottomActions.bottomRename.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_RENAME != 0 && currentMedium?.getIsInRecycleBin() == false)
        binding.bottomActions.bottomRename.setOnLongClickListener { toast(org.fossify.commons.R.string.rename); true }
        binding.bottomActions.bottomRename.setOnClickListener {
            checkMediaManagementAndRename()
        }

        binding.bottomActions.bottomSetAs.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SET_AS != 0)
        binding.bottomActions.bottomSetAs.setOnLongClickListener { toast(org.fossify.commons.R.string.set_as); true }
        binding.bottomActions.bottomSetAs.setOnClickListener {
            setAs(getCurrentPath())
        }

        binding.bottomActions.bottomCopy.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_COPY != 0)
        binding.bottomActions.bottomCopy.setOnLongClickListener { toast(org.fossify.commons.R.string.copy); true }
        binding.bottomActions.bottomCopy.setOnClickListener {
            checkMediaManagementAndCopy(true)
        }

        binding.bottomActions.bottomMove.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_MOVE != 0 && !mDirectory.isVirtualAlbumPath())
        binding.bottomActions.bottomMove.setOnLongClickListener { toast(org.fossify.commons.R.string.move); true }
        binding.bottomActions.bottomMove.setOnClickListener {
            moveFileTo()
        }

        binding.bottomActions.bottomResize.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_RESIZE != 0 && currentMedium?.isImage() == true)
        binding.bottomActions.bottomResize.setOnLongClickListener { toast(org.fossify.commons.R.string.resize); true }
        binding.bottomActions.bottomResize.setOnClickListener {
            resizeImage()
        }
    }

    private fun updateBottomActionIcons(medium: Medium?) {
        if (medium == null) {
            return
        }

        val favoriteIcon =
            if (medium.isFavorite) org.fossify.commons.R.drawable.ic_star_vector else org.fossify.commons.R.drawable.ic_star_outline_vector
        binding.bottomActions.bottomFavorite.setImageResource(favoriteIcon)

        val hideIcon =
            if (medium.isHidden()) org.fossify.commons.R.drawable.ic_unhide_vector else org.fossify.commons.R.drawable.ic_hide_vector
        binding.bottomActions.bottomToggleFileVisibility.setImageResource(hideIcon)

        binding.bottomActions.bottomRotate.beVisibleIf(
            getVisibleBottomActions() and BOTTOM_ACTION_ROTATE != 0 && getCurrentMedium()?.isImage() == true
        )
        binding.bottomActions.bottomChangeOrientation.setImageResource(getChangeOrientationIcon())
    }

    private fun removeCurrentFromVirtualAlbum() {
        val albumId = mDirectory.getVirtualAlbumId() ?: return
        val path = getCurrentPath()
        ensureBackgroundThread {
            removePathsFromVirtualAlbum(albumId, listOf(path))
            runOnUiThread {
                toast(R.string.removed_from_virtual_album)
                refreshViewPager()
            }
        }
    }

    private fun toggleFavorite() {
        val medium = getCurrentMedium() ?: return
        medium.isFavorite = !medium.isFavorite
        ensureBackgroundThread {
            updateFavorite(medium.path, medium.isFavorite)
            if (medium.isFavorite) {
                mFavoritePaths.add(medium.path)
            } else {
                mFavoritePaths.remove(medium.path)
            }

            runOnUiThread {
                refreshMenuItems()
            }
        }
    }

    private fun printFile() {
        sendPrintIntent(getCurrentPath())
    }

    private fun sendPrintIntent(path: String) {
        val printHelper = PrintHelper(this)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        printHelper.orientation = SCREEN_ORIENTATION_PORTRAIT

        try {
            val resolution = path.getImageResolution(this)
            if (resolution == null) {
                toast(org.fossify.commons.R.string.unknown_error_occurred)
                return
            }

            var requestedWidth = resolution.x
            var requestedHeight = resolution.y

            if (requestedWidth >= MAX_PRINT_SIDE_SIZE) {
                requestedHeight = (requestedHeight / (requestedWidth / MAX_PRINT_SIDE_SIZE.toFloat())).toInt()
                requestedWidth = MAX_PRINT_SIDE_SIZE
            } else if (requestedHeight >= MAX_PRINT_SIDE_SIZE) {
                requestedWidth = (requestedWidth / (requestedHeight / MAX_PRINT_SIDE_SIZE.toFloat())).toInt()
                requestedHeight = MAX_PRINT_SIDE_SIZE
            }

            val options = RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

            Glide.with(this)
                .asBitmap()
                .load(path)
                .apply(options)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>, isFirstResource: Boolean): Boolean {
                        showErrorToast(e?.localizedMessage ?: "")
                        return false
                    }

                    override fun onResourceReady(
                        bitmap: Bitmap,
                        model: Any,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        printHelper.printBitmap(getCurrentMedium()?.name?.ifEmpty { null } ?: CryptoManager.getDisplayName(path), bitmap)
                        return false
                    }
                }).submit(requestedWidth, requestedHeight)
        } catch (e: Exception) {
        }
    }

    private fun restoreFile() {
        val currentPath = getCurrentPath()
        if (CryptoManager.isVaultRecycleBinPath(this, currentPath) || CryptoManager.isVaultRecycleBinPath(currentPath)) {
            ensureBackgroundThread {
                CryptoManager.restoreVaultRecycleBinFiles(this, listOf(currentPath))
                runOnUiThread {
                    refreshViewPager()
                }
            }
        } else {
            restoreRecycleBinPath(currentPath) {
                refreshViewPager()
            }
        }
    }

    private fun resizeImage() {
        val oldPath = getCurrentPath()
        launchResizeImageDialog(oldPath)
    }

    private fun copyImageToClipboard() {
        if (CryptoManager.isVaultUnlocked || config.temporarilyShowHidden) {
            toast(R.string.copy_to_clipboard_disabled_in_vault)
            return
        }
        val clipboard = getSystemService(ClipboardManager::class.java) as ClipboardManager

        val imagePath = getCurrentMedium()?.path ?: return
        val clip = ClipData.newUri(contentResolver, "Image", getFinalUriFromPath(imagePath, BuildConfig.APPLICATION_ID))
        clipboard.setPrimaryClip(clip)
    }

    private fun checkDeleteConfirmation() {
        if (getCurrentMedium() == null) {
            return
        }

        handleMediaManagementPrompt {
            if (config.isDeletePasswordProtectionOn) {
                handleDeletePasswordProtection {
                    deleteConfirmed(config.tempSkipRecycleBin)
                }
            } else if (config.tempSkipDeleteConfirmation || config.skipDeleteConfirmation) {
                deleteConfirmed(config.tempSkipRecycleBin)
            } else {
                askConfirmDelete()
            }
        }
    }

    private fun askConfirmDelete() {
        val fileDirItem = getCurrentMedium()?.toFileDirItem() ?: return
        val size = fileDirItem.getProperSize(this, countHidden = true).formatSize()
        val displayName = getCurrentMedium()?.name?.ifEmpty { null } ?: CryptoManager.getDisplayName(getCurrentPath())
        val filename = "\"$displayName\""
        val filenameAndSize = "$filename ($size)"
        val isInRecycleBin = getCurrentMedium()!!.getIsInRecycleBin() || CryptoManager.isVaultRecycleBinPath(this, getCurrentPath()) || CryptoManager.isVaultRecycleBinPath(this, mDirectory)

        val baseString = if (config.useRecycleBin && !config.tempSkipRecycleBin && !isInRecycleBin) {
            org.fossify.commons.R.string.move_to_recycle_bin_confirmation
        } else {
            org.fossify.commons.R.string.deletion_confirmation
        }

        val message = String.format(resources.getString(baseString), filenameAndSize)
        val showSkipRecycleBinOption = config.useRecycleBin && !isInRecycleBin

        DeleteWithRememberDialog(this, message, showSkipRecycleBinOption) { remember, skipRecycleBin ->
            config.tempSkipDeleteConfirmation = remember

            if (remember) {
                config.tempSkipRecycleBin = skipRecycleBin
            }

            deleteConfirmed(skipRecycleBin)
        }
    }

    private fun deleteConfirmed(skipRecycleBin: Boolean) {
        val currentMedium = getCurrentMedium()
        val path = currentMedium?.path ?: return
        if (getIsPathDirectory(path) || (!path.isMediaFile() && !CryptoManager.isEncryptedFile(path))) {
            return
        }

        val fileDirItem = currentMedium.toFileDirItem()
        val isEncrypted = CryptoManager.isEncryptedFile(path)
        val isVaultRecycleBin = CryptoManager.isVaultRecycleBinPath(this, path) || CryptoManager.isVaultRecycleBinPath(this, mDirectory)
        val isHiddenOrVirtual = isPathHiddenOrEncrypted(path) || mDirectory.isVirtualAlbumPath()

        if (isVaultRecycleBin) {
            ensureBackgroundThread {
                CryptoManager.permanentlyDeleteVaultRecycleBinFiles(this, listOf(path))
                runOnUiThread {
                    mIgnoredPaths.add(path)
                    val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
                    if (media.isNotEmpty()) {
                        refreshUI(media, false)
                    }
                    if (media.size == 1) {
                        onPageSelected(0)
                    }
                    mIgnoredPaths.remove(path)
                    if (media.isEmpty()) {
                        finish()
                    }
                }
            }
            return
        }

        if (config.useRecycleBin && !skipRecycleBin && !currentMedium.getIsInRecycleBin()) {
            if (isEncrypted) {
                ensureBackgroundThread {
                    CryptoManager.moveEncryptedFilesToVaultRecycleBin(this, listOf(path))
                    runOnUiThread {
                        mIgnoredPaths.add(path)
                        val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
                        if (media.isNotEmpty()) {
                            refreshUI(media, false)
                        }
                        if (media.size == 1) {
                            onPageSelected(0)
                        }
                        mIgnoredPaths.remove(path)
                        if (mDirectory.isVirtualAlbumPath()) {
                            val albumId = mDirectory.getVirtualAlbumId()
                            if (albumId != null) {
                                removePathsFromVirtualAlbum(albumId, listOf(path))
                            }
                        }
                        if (media.isEmpty()) {
                            deleteDirectoryIfEmpty()
                            finish()
                        }
                    }
                }
            } else if (isHiddenOrVirtual) {
                ensureBackgroundThread {
                    val hiddenPaths = arrayListOf(path)
                    movePathsInRecycleBin(hiddenPaths) { success ->
                        ensureBackgroundThread {
                            try { File(path).delete() } catch (_: Exception) {}
                            deleteDBPath(path)
                            CryptoManager.purgeTracesForFile(this, path)
                        }
                    }
                    runOnUiThread {
                        mIgnoredPaths.add(path)
                        val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
                        if (media.isNotEmpty()) {
                            refreshUI(media, false)
                        }
                        if (media.size == 1) {
                            onPageSelected(0)
                        }
                        mIgnoredPaths.remove(path)
                        if (mDirectory.isVirtualAlbumPath()) {
                            val albumId = mDirectory.getVirtualAlbumId()
                            if (albumId != null) {
                                removePathsFromVirtualAlbum(albumId, listOf(path))
                            }
                        }
                        if (media.isEmpty()) {
                            deleteDirectoryIfEmpty()
                            finish()
                        }
                    }
                }
            } else {
                checkManageMediaOrHandleSAFDialogSdk30(fileDirItem.path) {
                    if (!it) {
                        return@checkManageMediaOrHandleSAFDialogSdk30
                    }

                    mIgnoredPaths.add(fileDirItem.path)
                    val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
                    if (media.isNotEmpty()) {
                        runOnUiThread {
                            refreshUI(media, false)
                        }
                    }

                    if (media.size == 1) {
                        onPageSelected(0)
                    }

                    movePathsInRecycleBin(arrayListOf(path)) {
                        if (it) {
                            tryDeleteFileDirItem(fileDirItem, false, false) {
                                mIgnoredPaths.remove(fileDirItem.path)
                                if (media.isEmpty()) {
                                    deleteDirectoryIfEmpty()
                                    finish()
                                }
                            }
                        } else {
                            toast(org.fossify.commons.R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        } else {
            // Permanent deletion
            if (isEncrypted) {
                ensureBackgroundThread {
                    CryptoManager.secureWipeAndDeleteFile(this, File(path))
                    deleteVirtualAlbumItemFromAllAlbums(path)
                    runOnUiThread {
                        mIgnoredPaths.add(path)
                        val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
                        if (media.isNotEmpty()) {
                            refreshUI(media, false)
                        }
                        if (media.size == 1) {
                            onPageSelected(0)
                        }
                        mIgnoredPaths.remove(path)
                        if (mDirectory.isVirtualAlbumPath()) {
                            val albumId = mDirectory.getVirtualAlbumId()
                            if (albumId != null) {
                                removePathsFromVirtualAlbum(albumId, listOf(path))
                            }
                        }
                        if (media.isEmpty()) {
                            deleteDirectoryIfEmpty()
                            finish()
                        }
                    }
                }
            } else if (isHiddenOrVirtual) {
                ensureBackgroundThread {
                    try { File(path).delete() } catch (_: Exception) {}
                    deleteDBPath(path)
                    CryptoManager.purgeTracesForFile(this, path)
                    runOnUiThread {
                        mIgnoredPaths.add(path)
                        val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
                        if (media.isNotEmpty()) {
                            refreshUI(media, false)
                        }
                        if (media.size == 1) {
                            onPageSelected(0)
                        }
                        mIgnoredPaths.remove(path)
                        if (mDirectory.isVirtualAlbumPath()) {
                            val albumId = mDirectory.getVirtualAlbumId()
                            if (albumId != null) {
                                removePathsFromVirtualAlbum(albumId, listOf(path))
                            }
                        }
                        if (media.isEmpty()) {
                            deleteDirectoryIfEmpty()
                            finish()
                        }
                    }
                }
            } else {
                handleDeletion(fileDirItem)
            }
        }
    }

    private fun handleDeletion(fileDirItem: FileDirItem) {
        checkManageMediaOrHandleSAFDialogSdk30(fileDirItem.path) {
            if (!it) {
                return@checkManageMediaOrHandleSAFDialogSdk30
            }

            mIgnoredPaths.add(fileDirItem.path)
            val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<Medium>
            if (media.isNotEmpty()) {
                runOnUiThread {
                    refreshUI(media, false)
                }
            }

            if (media.size == 1) {
                onPageSelected(0)
            }

            tryDeleteFileDirItem(fileDirItem, false, true) {
                mIgnoredPaths.remove(fileDirItem.path)
                if (media.isEmpty()) {
                    deleteDirectoryIfEmpty()
                    finish()
                }
            }
        }
    }

    private fun isDirEmpty(media: ArrayList<Medium>): Boolean {
        return if (media.isEmpty()) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else {
            false
        }
    }

    private fun checkMediaManagementAndRename() {
        handleMediaManagementPrompt {
            renameFile()
        }
    }

    private fun renameFile() {
        val oldPath = getCurrentPath()

        if (CryptoManager.isEncryptedFile(oldPath)) {
            RenameEncryptedItemDialog(this, oldPath) { newName ->
                getCurrentMedium()?.name = newName
                getCurrentMedia().getOrNull(mPos)?.name = newName
                mMediaFiles.getOrNull(mPos)?.name = newName
                updateActionbarTitle()
            }
            return
        }

        val isSDOrOtgRootFolder = isAStorageRootFolder(oldPath.getParentPath()) && !oldPath.startsWith(internalStoragePath)
        if (isRPlus() && isSDOrOtgRootFolder && !isExternalStorageManager()) {
            toast(org.fossify.commons.R.string.rename_in_sd_card_system_restriction, Toast.LENGTH_LONG)
            return
        }

        RenameItemDialog(this, oldPath) {
            getCurrentMedia().getOrNull(mPos)?.apply {
                path = it
                name = it.getFilenameFromPath()
            }

            ensureBackgroundThread {
                updateDBMediaPath(oldPath, it)
            }
            updateActionbarTitle()
        }
    }

    private fun isRandomSortingEnabled() = config.getFolderSorting(mDirectory) and SORT_BY_RANDOM != 0

    /**
     * Re-randomizes the order of the album's items while keeping the currently displayed item on screen,
     * so that swiping left/right afterwards reveals different neighbours.
     */
    private fun shuffleMedia() {
        if (mAreSlideShowMediaVisible || mRandomSlideshowStopped || mMediaFiles.size < 2) {
            return
        }

        val currentPath = mMediaFiles.getOrNull(mPos)?.path
        val previousOrder = mMediaFiles.map { it.path }
        val shuffled = ArrayList(mMediaFiles)
        // make sure the order really changes (relevant for tiny albums)
        var attempts = 0
        do {
            shuffled.shuffle()
            attempts++
        } while (shuffled.map { it.path } == previousOrder && attempts < 5)

        mMediaFiles = shuffled
        mPrevHashcode = mMediaFiles.hashCode()
        mPos = shuffled.indexOfFirst { it.path == currentPath }.takeIf { it != -1 } ?: 0

        updatePagerItems(mMediaFiles.toMutableList())
        updateActionbarTitle()
        refreshMenuItems()
    }

    private fun refreshViewPager(refetchPosition: Boolean = false) {
        val isRandomSorting = config.getFolderSorting(mDirectory) and SORT_BY_RANDOM != 0
        if (!isRandomSorting || isExternalIntent() || !mHasCachedAlbumMedia) {
            GetMediaAsynctask(
                applicationContext, mDirectory, isPickImage = false, isPickVideo = false,
                showAll = mShowAll, isSecretVaultView = mIsVaultFilesView
            ) {
                gotMedia(it, refetchViewPagerPosition = refetchPosition)
            }.execute()
        }
    }

    private fun gotMedia(thumbnailItems: ArrayList<ThumbnailItem>, ignorePlayingVideos: Boolean = false, refetchViewPagerPosition: Boolean = false) {
        val media = thumbnailItems.asSequence().filter {
            it is Medium && !mIgnoredPaths.contains(it.path)
        }.map { it as Medium }.toMutableList() as ArrayList<Medium>

        if (isDirEmpty(media) || media.hashCode() == mPrevHashcode) {
            return
        }

        val isPlaying = (getCurrentFragment() as? VideoFragment)?.mIsPlaying == true
        if (!ignorePlayingVideos && isPlaying && !isExternalIntent()) {
            return
        }

        refreshUI(media, refetchViewPagerPosition)
    }

    private fun refreshUI(media: ArrayList<Medium>, refetchViewPagerPosition: Boolean) {
        mPrevHashcode = media.hashCode()
        mMediaFiles = media

        if (refetchViewPagerPosition || mPos == -1) {
            mPos = getPositionInList(media)
            if (mPos == -1) {
                min(mPos, media.lastIndex)
            }
        }

        updateActionbarTitle()
        updatePagerItems(mMediaFiles.toMutableList())

        refreshMenuItems()
        checkOrientation()
        initBottomActions()
    }

    private fun getPositionInList(items: MutableList<Medium>): Int {
        mPos = 0
        for ((i, medium) in items.withIndex()) {
            val portraitPath = getPortraitPath()
            if (portraitPath != "") {
                val portraitPaths = File(portraitPath).parentFile?.list()
                if (portraitPaths != null) {
                    for (path in portraitPaths) {
                        if (medium.name == path) {
                            return i
                        }
                    }
                }
            } else if (medium.path.equals(mPath, true)) {
                return i
            }
        }
        return mPos
    }

    private fun deleteDirectoryIfEmpty() {
        if (config.deleteEmptyFolders) {
            val fileDirItem = FileDirItem(mDirectory, mDirectory.getFilenameFromPath(), File(mDirectory).isDirectory)
            if (!fileDirItem.isDownloadsFolder() && fileDirItem.isDirectory) {
                ensureBackgroundThread {
                    if (fileDirItem.getProperFileCount(this, true) == 0) {
                        tryDeleteFileDirItem(fileDirItem, true, true)
                        scanPathRecursively(mDirectory)
                    }
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun checkOrientation() {
        if (!mIsOrientationLocked && config.screenRotation == ROTATE_BY_ASPECT_RATIO) {
            resolveAspectRatioOrientation(getCurrentPath())
        }
    }

    /**
     * Applies the screen orientation matching the aspect ratio of the media with the given path.
     *
     * The dimensions of the media are read in the background, because reading them may require
     * decoding or even decrypting the file, which would delay the rotation. If the dimensions are
     * already known, they are used directly. [onApplied] is called on the UI thread once the
     * orientation has been applied, or once it turned out that no orientation can be applied.
     */
    override fun applyAspectRatioOrientation(path: String, width: Int, height: Int, onApplied: () -> Unit) {
        if (path.isEmpty() || mIsOrientationLocked || config.screenRotation != ROTATE_BY_ASPECT_RATIO) {
            onApplied()
            return
        }

        val key = AspectRatioOrientation.getKey(path)
        val decidedOrientation = AspectRatioOrientation.getDecision(key)
        if (decidedOrientation != null) {
            applyOrientationIfCurrent(path, decidedOrientation)
            notifyOrientationCallbacks(path)
            onApplied()
            return
        }

        if (width > 0 && height > 0) {
            val orientation = AspectRatioOrientation.getOrientation(width, height, 0)
            // A square media keeps the current orientation, which is stored as no rotation at all.
            AspectRatioOrientation.decide(key, orientation ?: 0)
            applyOrientationIfCurrent(path, orientation)

            // Everybody waiting for the orientation of this media can continue now.
            notifyOrientationCallbacks(path)
            onApplied()
            return
        }

        // The dimensions are not known yet, read them from the file itself.
        resolveAspectRatioOrientation(path, onApplied)
    }

    /**
     * Reads the dimensions of the media with the given path in the background and applies the
     * screen orientation matching its aspect ratio. [onApplied] is called on the UI thread once
     * the orientation has been applied, or once it turned out that no orientation can be applied.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun resolveAspectRatioOrientation(path: String, onApplied: () -> Unit = {}) {
        if (path.isEmpty() || mIsOrientationLocked || config.screenRotation != ROTATE_BY_ASPECT_RATIO) {
            onApplied()
            return
        }

        val key = AspectRatioOrientation.getKey(path)
        val decidedOrientation = AspectRatioOrientation.getDecision(key)
        if (decidedOrientation != null) {
            applyOrientationIfCurrent(path, decidedOrientation)
            onApplied()
            return
        }

        mOrientationCallbacks.getOrPut(path) { mutableListOf() }.add(onApplied)
        if (!mOngoingOrientationRequests.add(path)) {
            return
        }

        ensureBackgroundThread {
            val orientation = try {
                AspectRatioOrientation.resolve(path)
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
            runOnUiThread {
                mOngoingOrientationRequests.remove(path)
                if (orientation != null) {
                    // The orientation may have been decided already, for example by the media
                    // player. In that case the existing one has to be used.
                    val decided = AspectRatioOrientation.decide(key, orientation)
                    val effectiveOrientation = if (decided) {
                        orientation
                    } else {
                        AspectRatioOrientation.getDecision(key)
                    }

                    applyOrientationIfCurrent(path, effectiveOrientation)
                }

                notifyOrientationCallbacks(path)
            }
        }
    }

    /**
     * Applies the given orientation, but only if the media it belongs to is the one that is
     * currently shown.
     */
    private fun applyOrientationIfCurrent(path: String, orientation: Int?) {
        if (orientation != null && orientation != 0 && path == getCurrentPath()) {
            applyScreenOrientation(orientation)
        }
    }

    private fun notifyOrientationCallbacks(path: String) {
        mOrientationCallbacks.remove(path)?.forEach { it.invoke() }
    }

    /**
     * Rotates the screen to the given orientation, unless it is already showing that orientation.
     */
    private fun applyScreenOrientation(orientation: Int) {
        if (orientation == 0 || orientation == mAppliedScreenOrientation) {
            return
        }

        mAppliedScreenOrientation = orientation
        @SuppressLint("SourceLockedOrientationActivity")
        requestedOrientation = orientation
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        checkSystemUI()
        fullscreenToggled()
    }

    override fun videoEnded(): Boolean {
        if (mIsSlideshowActive) {
            swipeToNextMedium()
        }
        return mIsSlideshowActive
    }

    override fun isSlideShowActive() = mIsSlideshowActive

    override fun isFullScreen() = mIsFullScreen

    override fun saveVideoPlaybackPosition(path: String, position: Long) {
        mVideoPlaybackPositions[path] = position
    }

    override fun getVideoPlaybackPosition(path: String) = mVideoPlaybackPositions[path]

    override fun goToPrevItem() {
        binding.viewPager.setCurrentItem(binding.viewPager.currentItem - 1, false)
        checkOrientation()
    }

    override fun goToNextItem() {
        binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, false)
        checkOrientation()
    }

    override fun launchViewVideoIntent(path: String) {
        hideKeyboard()
        ensureBackgroundThread {
            val newUri = getFinalUriFromPath(path, BuildConfig.APPLICATION_ID) ?: return@ensureBackgroundThread
            val mimeType = getUriMimeType(path, newUri)
            Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(newUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(IS_FROM_GALLERY, true)
                putExtra(REAL_FILE_PATH, path)
                putExtra(SHOW_PREV_ITEM, binding.viewPager.currentItem != 0)
                putExtra(SHOW_NEXT_ITEM, binding.viewPager.currentItem != mMediaFiles.lastIndex)

                try {
                    startActivityForResult(this, REQUEST_VIEW_VIDEO)
                } catch (e: ActivityNotFoundException) {
                    if (!tryGenericMimeType(this, mimeType, newUri)) {
                        toast(org.fossify.commons.R.string.no_app_found)
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    private fun checkSystemUI() {
        if (mIsFullScreen) {
            hideSystemUI()
        } else {
            stopSlideshow()
            showSystemUI()
        }
    }

    private fun fullscreenToggled() {
        binding.viewPager.adapter?.let {
            (it as MyPagerAdapter).toggleFullscreen(mIsFullScreen)
            val newAlpha = if (mIsFullScreen) 0f else 1f
            binding.topShadow.animate().alpha(newAlpha).start()
            binding.bottomActions.root.animate().alpha(newAlpha).withStartAction {
                binding.bottomActions.root.beVisible()
            }.withEndAction {
                binding.bottomActions.root.beVisibleIf(newAlpha == 1f)
            }.start()

            binding.mediumViewerAppbar.animate().alpha(newAlpha).withStartAction {
                binding.mediumViewerAppbar.beVisible()
            }.withEndAction {
                binding.mediumViewerAppbar.beVisibleIf(newAlpha == 1f)
            }.start()
        }
    }

    private fun updateActionbarTitle() {
        runOnUiThread {
            val medium = getCurrentMedium()
            if (medium != null) {
                binding.mediumViewerToolbar.title = medium.name.ifEmpty { CryptoManager.getDisplayName(medium.path) }
            }
        }
    }

    private fun getCurrentMedium(): Medium? {
        return if (getCurrentMedia().isEmpty() || mPos == -1) {
            null
        } else {
            getCurrentMedia()[min(mPos, getCurrentMedia().lastIndex)]
        }
    }

    private fun getCurrentMedia() = if (mAreSlideShowMediaVisible || mRandomSlideshowStopped) mSlideshowMedia else mMediaFiles

    private fun getCurrentPath() = getCurrentMedium()?.path ?: ""

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        val path = mMediaFiles.getOrNull(position)?.path ?: mPath
        val requiresVault = CryptoManager.isEncryptedFile(path) ||
            (mDirectory.isNotEmpty() && !mDirectory.isVirtualAlbumPath() && CryptoManager.isEncryptedFolderPath(this, mDirectory)) ||
            (mDirectory.isVirtualAlbumPath() && isHiddenVirtualAlbumPath(mDirectory))
        applyVaultScreenProtection(requiresVault)
        if (mPos != position) {
            mPos = position
            updateActionbarTitle()
            refreshMenuItems()
            scheduleSwipe()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE && getCurrentMedium() != null) {
            checkOrientation()
        }
    }

    private fun isExternalIntent(): Boolean {
        return !intent.getBooleanExtra(IS_FROM_GALLERY, false)
    }
}
