package org.fossify.gallery.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogVaultExitBinding

class VaultExitDialog(
    private val activity: Activity,
    private val callback: (keepShowing: Boolean, rememberChoice: Boolean) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogVaultExitBinding.inflate(activity.layoutInflater)

    init {
        binding.vaultExitRadioKeepShowing.setOnClickListener {
            binding.vaultExitRadioGroup.check(R.id.vault_exit_radio_keep_showing)
        }

        binding.vaultExitRadioHide.setOnClickListener {
            binding.vaultExitRadioGroup.check(R.id.vault_exit_radio_hide)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.vault_exit_dialog_title) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed() {
        val keepShowing = binding.vaultExitRadioKeepShowing.isChecked
        val rememberChoice = binding.vaultExitRememberCheckbox.isChecked
        dialog?.dismiss()
        callback(keepShowing, rememberChoice)
    }
}
