package org.fossify.gallery.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogCryptoProgressBinding
import org.fossify.gallery.helpers.OperationProgressFormatter
import org.fossify.gallery.helpers.OperationProgressNotifier

class CryptoProgressDialog(
    private val activity: Activity,
    private val max: Int,
    private val titleId: Int = R.string.encrypting_folder,
    notificationChannelId: String = OperationProgressNotifier.CHANNEL_CRYPTO,
    notificationIconRes: Int = defaultNotificationIcon(titleId),
    enableNotification: Boolean = true
) {
    private var dialog: AlertDialog? = null

    @Volatile
    private var isDismissed = false
    private val binding = DialogCryptoProgressBinding.inflate(activity.layoutInflater)
    private val notifier: OperationProgressNotifier?

    init {
        binding.cryptoProgressMessage.setText(titleId)
        binding.cryptoProgressBar.max = maxOf(max, 1)
        binding.cryptoProgressBar.progress = 0
        binding.cryptoProgressBar.isIndeterminate = max <= 0
        val initialSummary = OperationProgressFormatter.formatCountSummary(current = 0, total = max)
        binding.cryptoProgressCount.text = initialSummary

        notifier = if (enableNotification) {
            val title = activity.getString(titleId)
            val channelName = if (notificationChannelId == OperationProgressNotifier.CHANNEL_COPY_MOVE) {
                OperationProgressNotifier.CHANNEL_COPY_MOVE
            } else {
                activity.getString(R.string.encrypt_decrypt_notifications)
            }
            OperationProgressNotifier(
                context = activity.applicationContext,
                channelId = notificationChannelId,
                channelName = channelName,
                title = title,
                smallIconRes = notificationIconRes
            ).apply {
                update(current = 0, max = max, contentText = initialSummary)
                start()
            }
        } else {
            null
        }

        activity.getAlertDialogBuilder().apply {
            setCancelable(false)
            activity.setupDialogStuff(binding.root, this, titleId = titleId) { alertDialog ->
                if (isDismissed || activity.isFinishing || activity.isDestroyed) {
                    try {
                        alertDialog.dismiss()
                    } catch (_: Exception) {
                    }
                } else {
                    dialog = alertDialog
                    alertDialog.setCanceledOnTouchOutside(false)
                    alertDialog.setCancelable(false)
                }
            }
        }
    }

    fun updateProgress(
        current: Int,
        total: Int,
        barCurrent: Int = current,
        barMax: Int = total,
        notificationText: String? = null
    ) {
        val isIndeterminate = barMax <= 0 && total <= 0
        val safeBarMax = maxOf(barMax, 1)
        val clampedBarCurrent = barCurrent.coerceIn(0, safeBarMax)
        binding.cryptoProgressBar.isIndeterminate = isIndeterminate
        binding.cryptoProgressBar.max = safeBarMax
        binding.cryptoProgressBar.progress = clampedBarCurrent

        val countSummary = OperationProgressFormatter.formatCountSummary(
            current = current,
            total = total,
            barCurrent = clampedBarCurrent,
            barMax = barMax
        )
        binding.cryptoProgressCount.text = countSummary
        notifier?.update(
            current = clampedBarCurrent,
            max = if (isIndeterminate) 0 else safeBarMax,
            contentText = notificationText ?: countSummary
        )
    }

    fun updateNotifierOnly(current: Int, max: Int, contentText: String) {
        notifier?.update(current = current, max = max, contentText = contentText)
    }

    fun dismiss() {
        isDismissed = true
        notifier?.finish()
        try {
            dialog?.dismiss()
        } catch (_: Exception) {
        }
        dialog = null
    }

    companion object {
        private fun defaultNotificationIcon(titleId: Int): Int {
            return when (titleId) {
                R.string.decrypting_files,
                R.string.decrypting_folder -> org.fossify.commons.R.drawable.ic_unhide_vector
                R.string.encrypting_files,
                R.string.encrypting_folder -> org.fossify.commons.R.drawable.ic_hide_vector
                else -> org.fossify.commons.R.drawable.ic_copy_vector
            }
        }
    }
}
