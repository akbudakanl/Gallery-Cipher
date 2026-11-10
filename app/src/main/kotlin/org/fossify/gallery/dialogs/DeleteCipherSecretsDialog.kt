package org.fossify.gallery.dialogs

import android.os.CountDownTimer
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogDeleteCipherSecretsBinding

class DeleteCipherSecretsDialog(
    private val activity: BaseSimpleActivity,
    private val messageText: String? = null,
    private val onConfirmed: () -> Unit
) {
    private var dialog: AlertDialog? = null
    private var countDownTimer: CountDownTimer? = null

    init {
        val binding = DialogDeleteCipherSecretsBinding.inflate(activity.layoutInflater)
        if (messageText != null) {
            binding.cipherSecretsWarningMessage.text = messageText
        }
        binding.cipherSecretsConfirmCheckbox.isEnabled = false
        binding.cipherSecretsConfirmCheckbox.isChecked = false
        binding.cipherSecretsBtnYes.isEnabled = false

        binding.cipherSecretsBtnYes.text = activity.getString(org.fossify.commons.R.string.yes)
        binding.cipherSecretsBtnNo.text = activity.getString(org.fossify.commons.R.string.no)

        val baseConfirmText = activity.getString(R.string.confirm_checkbox_label)
        binding.cipherSecretsConfirmCheckbox.text = "$baseConfirmText (10)"

        countDownTimer = object : CountDownTimer(10000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ((millisUntilFinished + 999) / 1000).toInt()
                binding.cipherSecretsConfirmCheckbox.text = "$baseConfirmText ($seconds)"
            }

            override fun onFinish() {
                binding.cipherSecretsConfirmCheckbox.text = baseConfirmText
                binding.cipherSecretsConfirmCheckbox.isEnabled = true
            }
        }.start()

        binding.cipherSecretsConfirmCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.cipherSecretsBtnYes.isEnabled = isChecked
        }

        binding.cipherSecretsBtnYes.setOnClickListener {
            if (binding.cipherSecretsConfirmCheckbox.isChecked) {
                countDownTimer?.cancel()
                dialog?.dismiss()
                onConfirmed()
            }
        }

        binding.cipherSecretsBtnNo.setOnClickListener {
            countDownTimer?.cancel()
            dialog?.dismiss()
        }

        val builder = activity.getAlertDialogBuilder()
            .setOnDismissListener {
                countDownTimer?.cancel()
            }

        activity.setupDialogStuff(binding.root, builder) { alertDialog ->
            dialog = alertDialog
        }
    }
}
