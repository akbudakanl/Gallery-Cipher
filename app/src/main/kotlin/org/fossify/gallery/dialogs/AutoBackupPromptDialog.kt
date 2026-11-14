package org.fossify.gallery.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.gallery.databinding.DialogAutoBackupPromptBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.FavoritesAndVirtualAlbumsBackupManager

/**
 * Full-screen prompt asking whether favorites and non-encrypted virtual albums should be
 * automatically backed up to shared storage outside app memory.
 */
class AutoBackupPromptDialog(
    private val activity: Activity,
    private val onChoice: (accepted: Boolean) -> Unit
) {
    init {
        val binding = DialogAutoBackupPromptBinding.inflate(activity.layoutInflater)
        val bgColor = activity.getProperBackgroundColor()
        val textColor = activity.getProperTextColor()
        val primaryColor = activity.getProperPrimaryColor()

        binding.autoBackupPromptRoot.setBackgroundColor(bgColor)
        binding.autoBackupPromptIcon.applyColorFilter(primaryColor)
        binding.autoBackupPromptMessage.setTextColor(textColor)

        binding.autoBackupBtnYes.apply {
            text = activity.getString(org.fossify.commons.R.string.yes)
            backgroundTintList = ColorStateList.valueOf(primaryColor)
            setTextColor(primaryColor.getContrastColor())
        }
        binding.autoBackupBtnNo.apply {
            text = activity.getString(org.fossify.commons.R.string.no)
            strokeColor = ColorStateList.valueOf(primaryColor)
            setTextColor(textColor)
        }

        val dialog = Dialog(activity, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar).apply {
            setContentView(binding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundDrawable(ColorDrawable(bgColor))
                statusBarColor = bgColor
                navigationBarColor = bgColor
            }
        }

        binding.autoBackupBtnYes.setOnClickListener {
            dialog.dismiss()
            onChoice(true)
        }

        binding.autoBackupBtnNo.setOnClickListener {
            dialog.dismiss()
            onChoice(false)
        }

        dialog.show()
    }
}

/**
 * Prompts once on the first non-encrypted virtual album creation or first favorite addition
 * when shared-storage all-files access has been granted, then invokes [onComplete].
 */
fun Activity.promptAutoBackupIfNeeded(onComplete: () -> Unit) {
    if (isFinishing || isDestroyed) {
        return
    }
    if (FavoritesAndVirtualAlbumsBackupManager.canWriteInternalBackups(this) && !config.wasAutoBackupPromptShown) {
        AutoBackupPromptDialog(this) { accepted ->
            config.wasAutoBackupPromptShown = true
            config.autoBackupFavoritesAndVirtualAlbums = accepted
            onComplete()
        }
    } else {
        onComplete()
    }
}
