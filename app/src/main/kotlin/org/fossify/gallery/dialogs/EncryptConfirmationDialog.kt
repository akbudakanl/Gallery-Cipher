package org.fossify.gallery.dialogs

import android.app.Activity
import org.fossify.commons.extensions.getAlertDialogBuilder

/**
 * Asks for confirmation before encrypting an album or a file.
 *
 * The layout mirrors the decryption confirmation dialog: "No" is placed on the right side of the
 * dialog and "Yes" on the left side, so both confirmations behave the same way.
 */
class EncryptConfirmationDialog(
    val activity: Activity,
    val message: String,
    val onCancel: (() -> Unit)? = null,
    val callback: () -> Unit
) {
    init {
        activity.getAlertDialogBuilder()
            .setMessage(message)
            .setPositiveButton(org.fossify.commons.R.string.no) { _, _ -> onCancel?.invoke() }
            .setNegativeButton(org.fossify.commons.R.string.yes) { _, _ -> callback() }
            .create()
            .show()
    }
}
