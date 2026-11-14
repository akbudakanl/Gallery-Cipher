package org.fossify.gallery.activities

import android.app.assist.AssistContent
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.Images
import android.provider.MediaStore.Video
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isPiePlus
import org.fossify.gallery.R
import org.fossify.gallery.dialogs.StoragePermissionRequiredDialog
import org.fossify.gallery.extensions.addPathToDB
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.isPathHiddenOrEncrypted
import org.fossify.gallery.extensions.updateDirectoryPath
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.VaultSessionManager
import org.fossify.gallery.helpers.getPermissionsToRequest

open class SimpleActivity : BaseSimpleActivity() {

    private var dialog: AlertDialog? = null
    private val mediaObserverHandler = Handler(Looper.getMainLooper())
    private val pendingObserverFolders = LinkedHashSet<String>()
    private var hasPendingWildcardObserverChange = false

    private val dispatchObserverChangeRunnable = Runnable {
        val folders: Set<String>
        val isWildcard: Boolean
        synchronized(pendingObserverFolders) {
            folders = LinkedHashSet(pendingObserverFolders)
            pendingObserverFolders.clear()
            isWildcard = hasPendingWildcardObserverChange
            hasPendingWildcardObserverChange = false
        }
        if (!isDestroyed && !isFinishing && (folders.isNotEmpty() || isWildcard)) {
            onMediaStoreContentChanged(folders, isWildcard)
        }
    }

    override fun onResume() {
        super.onResume()
        val isVaultActive = CryptoManager.isVaultUnlocked
        if (isVaultActive) {
            applyVaultScreenProtection(true)
        } else {
            applyVaultScreenProtection(false)
        }
    }

    protected open fun isVaultAssistProtectionActive(): Boolean {
        return CryptoManager.isVaultUnlocked || VaultSessionManager.isReauthPending()
    }

    override fun onProvideAssistData(data: Bundle?) {
        if (isVaultAssistProtectionActive()) {
            data?.clear()
            return
        }
        super.onProvideAssistData(data)
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        if (isVaultAssistProtectionActive()) {
            outContent?.apply {
                clipData = null
                intent = Intent()
                structuredData = null
                webUri = null
            }
            return
        }
        super.onProvideAssistContent(outContent)
    }

    override fun onDestroy() {
        mediaObserverHandler.removeCallbacks(dispatchObserverChangeRunnable)
        super.onDestroy()
    }

    protected open fun onMediaStoreContentChanged(changedFolderPaths: Set<String>, isWildcardChange: Boolean) {}

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            ensureBackgroundThread {
                if (uri != null) {
                    val path = getRealPathFromURI(uri)
                    if (path != null) {
                        if (!isPathHiddenOrEncrypted(path)) {
                            val parentPath = path.getParentPath()
                            updateDirectoryPath(parentPath)
                            addPathToDB(path)
                            synchronized(pendingObserverFolders) {
                                pendingObserverFolders.add(parentPath)
                            }
                        }
                    } else {
                        synchronized(pendingObserverFolders) {
                            hasPendingWildcardObserverChange = true
                        }
                    }
                } else {
                    synchronized(pendingObserverFolders) {
                        hasPendingWildcardObserverChange = true
                    }
                }

                mediaObserverHandler.removeCallbacks(dispatchObserverChangeRunnable)
                mediaObserverHandler.postDelayed(dispatchObserverChangeRunnable, 300L)
            }
        }
    }

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher_red,
        R.mipmap.ic_launcher_pink,
        R.mipmap.ic_launcher_purple,
        R.mipmap.ic_launcher_deep_purple,
        R.mipmap.ic_launcher_indigo,
        R.mipmap.ic_launcher_blue,
        R.mipmap.ic_launcher_light_blue,
        R.mipmap.ic_launcher_cyan,
        R.mipmap.ic_launcher_teal,
        R.mipmap.ic_launcher,
        R.mipmap.ic_launcher_light_green,
        R.mipmap.ic_launcher_lime,
        R.mipmap.ic_launcher_yellow,
        R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_orange,
        R.mipmap.ic_launcher_deep_orange,
        R.mipmap.ic_launcher_brown,
        R.mipmap.ic_launcher_blue_grey,
        R.mipmap.ic_launcher_grey_black
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    override fun getRepositoryName() = "Gallery"

    protected fun checkNotchSupport() {
        if (isPiePlus()) {
            val cutoutMode = when {
                config.showNotch -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            window.attributes.layoutInDisplayCutoutMode = cutoutMode
            if (config.showNotch) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
        }
    }

    protected fun registerFileUpdateListener() {
        try {
            contentResolver.registerContentObserver(Images.Media.EXTERNAL_CONTENT_URI, true, observer)
            contentResolver.registerContentObserver(Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        } catch (ignored: Exception) {
        }
    }

    protected fun unregisterFileUpdateListener() {
        try {
            contentResolver.unregisterContentObserver(observer)
        } catch (ignored: Exception) {
        }
    }

    protected fun showAddIncludedFolderDialog(callback: () -> Unit) {
        FilePickerDialog(this, config.lastFilepickerPath, false, config.shouldShowHidden, false, true) {
            config.lastFilepickerPath = it
            config.addIncludedFolder(it)
            callback()
            ensureBackgroundThread {
                scanPathRecursively(it)
            }
        }
    }

    protected fun requestMediaPermissions(enableRationale: Boolean = false, onGranted: () -> Unit) {
        when {
            hasAllPermissions(getPermissionsToRequest()) -> onGranted()
            config.showPermissionRationale -> {
                if (enableRationale) {
                    showPermissionRationale()
                } else {
                    onPermissionDenied()
                }
            }

            else -> {
                handlePartialMediaPermissions(getPermissionsToRequest(), force = true) { granted ->
                    if (granted) {
                        onGranted()
                    } else {
                        config.showPermissionRationale = true
                        showPermissionRationale()
                    }
                }
            }
        }
    }

    private fun showPermissionRationale() {
        dialog?.dismiss()
        StoragePermissionRequiredDialog(
            activity = this,
            onOkay = ::openDeviceSettings,
            onCancel = ::onPermissionDenied
        ) { dialog ->
            this.dialog = dialog
        }
    }

    private fun onPermissionDenied() {
        toast(org.fossify.commons.R.string.no_storage_permissions)
        finish()
    }
}
