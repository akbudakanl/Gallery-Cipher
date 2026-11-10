package org.fossify.gallery.dialogs

import android.app.Activity
import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.gallery.R
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.VaultPhysicalAlbum
import java.io.File

class PickVaultAlbumDialog(
    val activity: Activity,
    val callback: (targetFolder: File, albumName: String) -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        ensureBackgroundThread {
            CryptoManager.getOrCreateDefaultHiddenAlbum(activity)
            val albums = CryptoManager.getVaultDestinationAlbums(activity)

            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@runOnUiThread
                }
                show(albums)
            }
        }
    }

    private fun show(albums: List<VaultPhysicalAlbum>) {
        val newAlbumLabel = "+ " + activity.getString(R.string.create_new_hidden_album)
        val labels = albums.map { "${it.name} (${it.itemCount})" } + newAlbumLabel

        val listView = ListView(activity).apply {
            adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, labels)
            setOnItemClickListener { _, _, position, _ ->
                dialog?.dismiss()
                if (position == albums.size) {
                    promptCreateNewAlbum()
                } else {
                    val selected = albums[position]
                    callback(selected.folder, selected.name)
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(listView, this, R.string.select_hidden_album) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun promptCreateNewAlbum() {
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
