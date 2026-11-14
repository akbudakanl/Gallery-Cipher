package org.fossify.gallery.dialogs

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.databinding.DialogDeleteWithRememberBinding

class DeleteWithRememberDialog(
    private val activity: Activity,
    private val message: String,
    private val showSkipRecycleBinOption: Boolean,
    @param:StringRes private val keepOriginalOptionTextRes: Int? = null,
    private val keepOriginalDefaultChecked: Boolean = false,
    private val callback: (remember: Boolean, skipRecycleBin: Boolean, keepOriginal: Boolean) -> Unit
) {

    private var dialog: AlertDialog? = null
    private val binding = DialogDeleteWithRememberBinding.inflate(activity.layoutInflater)

    init {
        binding.deleteRememberTitle.text = message
        binding.skipTheRecycleBinCheckbox.beGoneIf(!showSkipRecycleBinOption)

        if (keepOriginalOptionTextRes != null) {
            binding.keepOriginalFileCheckbox.beVisible()
            binding.keepOriginalFileCheckbox.setText(keepOriginalOptionTextRes)
            binding.keepOriginalFileCheckbox.isChecked = keepOriginalDefaultChecked
            updateSkipRecycleBinEnabled(keepOriginalDefaultChecked)
            binding.keepOriginalFileCheckbox.setOnCheckedChangeListener { _, isChecked ->
                updateSkipRecycleBinEnabled(isChecked)
            }
        } else {
            binding.keepOriginalFileCheckbox.beGone()
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.yes) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.no, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun updateSkipRecycleBinEnabled(keepOriginal: Boolean) {
        if (showSkipRecycleBinOption) {
            binding.skipTheRecycleBinCheckbox.isEnabled = !keepOriginal
            binding.skipTheRecycleBinCheckbox.alpha = if (keepOriginal) 0.5f else 1f
        }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        val keepOriginal = keepOriginalOptionTextRes != null && binding.keepOriginalFileCheckbox.isChecked
        val skipRecycleBin = binding.skipTheRecycleBinCheckbox.isChecked && !keepOriginal
        callback(binding.deleteRememberCheckbox.isChecked, skipRecycleBin, keepOriginal)
    }
}
