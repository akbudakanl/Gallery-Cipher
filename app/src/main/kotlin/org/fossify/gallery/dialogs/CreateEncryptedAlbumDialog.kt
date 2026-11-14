package org.fossify.gallery.dialogs

import android.app.Activity
import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.gallery.R
import org.fossify.gallery.helpers.CryptoManager
import java.io.File

/**
 * Prompts the user for a name and creates a new encrypted physical album in the vault.
 */
class CreateEncryptedAlbumDialog(
    val activity: Activity,
    val callback: (targetFolder: File, albumName: String) -> Unit
) {
    init {
        val padding = (20 * activity.resources.displayMetrics.density).toInt()
        val editText = AppCompatEditText(activity).apply {
            hint = activity.getString(R.string.new_hidden_album)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val container = FrameLayout(activity).apply {
            setPadding(padding, padding, padding, 0)
            addView(editText)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(container, this, titleId = R.string.create_new_hidden_album) { alertDialog ->
                    alertDialog.showKeyboard(editText)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = editText.text.toString().trim()
                        if (name.isEmpty()) {
                            activity.toast(R.string.hidden_album_name_empty)
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            val newFolder = CryptoManager.createHiddenPhysicalAlbum(activity, name)
                            activity.runOnUiThread {
                                if (newFolder == null) {
                                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                } else {
                                    alertDialog.dismiss()
                                    callback(newFolder, name)
                                }
                            }
                        }
                    }
                }
            }
    }
}
