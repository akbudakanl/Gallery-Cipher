package org.fossify.gallery.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogVaultExitBinding
import org.fossify.gallery.extensions.config

class VaultExitDialog(
    private val activity: Activity,
    private val encryptedAlbumCount: Int,
    private val callback: (keepShowing: Boolean, alwaysAsk: Boolean) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogVaultExitBinding.inflate(activity.layoutInflater)

    init {
        val questionRes = if (encryptedAlbumCount == 1) {
            R.string.vault_exit_question_single
        } else {
            R.string.vault_exit_question_multiple
        }
        binding.vaultExitQuestion.text = activity.getString(questionRes)
        binding.vaultExitAlwaysAskCheckbox.isChecked = activity.config.vaultExitAskCheckboxChecked

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.yes) { _, _ -> dialogConfirmed(keepShowing = true) }
            .setNegativeButton(org.fossify.commons.R.string.no) { _, _ -> dialogConfirmed(keepShowing = false) }
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.vault_exit_dialog_title) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed(keepShowing: Boolean) {
        val alwaysAsk = binding.vaultExitAlwaysAskCheckbox.isChecked
        activity.config.vaultExitAskCheckboxChecked = alwaysAsk
        dialog?.dismiss()
        callback(keepShowing, alwaysAsk)
    }
}
