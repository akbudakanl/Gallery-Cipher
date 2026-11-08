package org.fossify.gallery.activities

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.gallery.R
import org.fossify.gallery.dialogs.CryptoProgressDialog
import org.fossify.gallery.dialogs.PickVaultAlbumDialog
import org.fossify.gallery.dialogs.VaultSecurityDialog
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.VaultDownloader
import java.io.File

class VaultShareActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!CryptoManager.hasExistingVault(this)) {
            toast(R.string.vault_not_setup)
            finish()
            return
        }

        val (url, uri) = VaultDownloader.extractPayload(intent)
        if (url == null && uri == null) {
            toast(R.string.no_media_url_found)
            finish()
            return
        }

        // Step 1: Prompt for Vault Password / PIN / Pattern
        val initialType = CryptoManager.getVaultProtectionType(this) ?: config.hiddenProtectionType
        VaultSecurityDialog(
            activity = this,
            requiredHash = config.hiddenPasswordHash,
            showTabIndex = initialType,
            minimalist = true
        ) { _, _, success ->
            if (success && CryptoManager.isVaultUnlocked) {
                // Step 2: Show Hidden Physical Albums Picker
                showAlbumPicker(url, uri)
            } else {
                finish()
            }
        }
    }

    private fun showAlbumPicker(url: String?, uri: Uri?) {
        PickVaultAlbumDialog(
            activity = this,
            onCancel = { finish() },
            onAlbumSelected = { targetFolder, albumName ->
                startDownloadAndEncryption(url, uri, targetFolder, albumName)
            }
        )
    }

    private fun startDownloadAndEncryption(url: String?, uri: Uri?, targetFolder: File, albumName: String) {
        val progressDialog = CryptoProgressDialog(this, 100, R.string.downloading_and_encrypting)
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FossifyGallery:VaultDownload")
        try {
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (_: Exception) {}

        ensureBackgroundThread {
            val encryptedFile = VaultDownloader.downloadAndEncrypt(
                context = applicationContext,
                url = url,
                uri = uri,
                targetDir = targetFolder,
                onProgress = { bytesRead, totalBytes ->
                    runOnUiThread {
                        if (totalBytes > 0) {
                            val percent = ((bytesRead.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
                            progressDialog.updateProgress(percent, 100)
                        } else {
                            progressDialog.updateProgress((bytesRead / 1024).toInt(), 0)
                        }
                    }
                }
            )

            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}

            runOnUiThread {
                progressDialog.dismiss()
                if (encryptedFile != null && encryptedFile.exists()) {
                    toast(String.format(getString(R.string.encrypted_download_success), albumName))
                } else {
                    toast(R.string.encrypted_download_failed)
                }
                finish()
            }
        }
    }
}
