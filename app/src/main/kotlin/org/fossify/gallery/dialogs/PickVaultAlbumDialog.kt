package org.fossify.gallery.dialogs

import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.gallery.R
import org.fossify.gallery.helpers.CryptoManager
import java.io.File

class PickVaultAlbumDialog(
    private val activity: BaseSimpleActivity,
    private val onCancel: () -> Unit,
    private val onAlbumSelected: (File, String) -> Unit,
    private val titleId: Int = R.string.select_vault_album,
    private val allowNewAlbum: Boolean = true
) {
    private var dialog: AlertDialog? = null

    init {
        showAlbumList()
    }

    private fun showAlbumList() {
        val albums = CryptoManager.getHiddenPhysicalAlbums(activity)
        if (albums.isEmpty()) {
            activity.toast(R.string.no_vault_albums_found)
            onCancel()
            return
        }
        val albumLabels = albums.map { "${it.name} (${it.itemCount})" }.toTypedArray()

        val builder = activity.getAlertDialogBuilder()
            .setTitle(titleId)
            .setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ ->
                onCancel()
            }
            .setOnCancelListener {
                onCancel()
            }

        if (allowNewAlbum) {
            builder.setNeutralButton(R.string.create_new_vault_album) { _, _ ->
                promptNewAlbumName()
            }
        }

        builder.setItems(albumLabels) { _, which ->
            if (which in albums.indices) {
                val selected = albums[which]
                dialog?.dismiss()
                onAlbumSelected(selected.folder, selected.name)
            }
        }

        dialog = builder.create()
        dialog?.show()
    }

    private fun promptNewAlbumName() {
        val padding = (20 * activity.resources.displayMetrics.density).toInt()
        val editText = AppCompatEditText(activity).apply {
            hint = activity.getString(R.string.enter_album_name)
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
            .setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ ->
                showAlbumList()
            }
            .setOnCancelListener {
                showAlbumList()
            }
            .apply {
                activity.setupDialogStuff(container, this, titleId = R.string.create_new_vault_album) { alertDialog ->
                    alertDialog.showKeyboard(editText)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = editText.text.toString().trim()
                        if (name.isEmpty()) {
                            activity.toast(R.string.vault_album_name_empty)
                            return@setOnClickListener
                        }

                        val newFolder = CryptoManager.createHiddenPhysicalAlbum(activity, name)
                        if (newFolder != null) {
                            alertDialog.dismiss()
                            dialog?.dismiss()
                            onAlbumSelected(newFolder, name)
                        } else {
                            activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                        }
                    }
                }
            }
    }
}
