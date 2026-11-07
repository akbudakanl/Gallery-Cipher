package org.fossify.gallery.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogCryptoProgressBinding

class CryptoProgressDialog(
    private val activity: Activity,
    private val max: Int,
    private val titleId: Int = R.string.encrypting_folder
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogCryptoProgressBinding.inflate(activity.layoutInflater)

    init {
        binding.cryptoProgressBar.max = maxOf(max, 1)
        binding.cryptoProgressBar.progress = 0
        binding.cryptoProgressBar.isIndeterminate = max <= 0
        val percent = 0
        binding.cryptoProgressCount.text = "0 / $max (%$percent)"

        activity.getAlertDialogBuilder().apply {
            setCancelable(false)
            activity.setupDialogStuff(binding.root, this, titleId = titleId) { alertDialog ->
                dialog = alertDialog
                alertDialog.setCanceledOnTouchOutside(false)
                alertDialog.setCancelable(false)
            }
        }
    }

    fun updateProgress(current: Int, total: Int) {
        val safeTotal = maxOf(total, 1)
        binding.cryptoProgressBar.isIndeterminate = total <= 0
        binding.cryptoProgressBar.max = safeTotal
        binding.cryptoProgressBar.progress = current
        val percent = if (total > 0) ((current.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0
        binding.cryptoProgressCount.text = "$current / $total (%$percent)"
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
