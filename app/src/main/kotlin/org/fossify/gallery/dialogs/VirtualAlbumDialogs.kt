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
import org.fossify.gallery.extensions.createVirtualAlbum
import org.fossify.gallery.extensions.getVirtualAlbums
import org.fossify.gallery.extensions.renameVirtualAlbum
import org.fossify.gallery.extensions.virtualAlbumNameExists

private fun Activity.buildAlbumNameField(initialText: String): Pair<FrameLayout, AppCompatEditText> {
    val padding = (20 * resources.displayMetrics.density).toInt()
    val editText = AppCompatEditText(this).apply {
        hint = getString(R.string.album_name)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        imeOptions = EditorInfo.IME_ACTION_DONE
        setSingleLine(true)
        setText(initialText)
        setSelection(text?.length ?: 0)
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    val container = FrameLayout(this).apply {
        setPadding(padding, padding, padding, 0)
        addView(editText)
    }
    return container to editText
}

/** Asks for a name and creates a new, empty virtual album. [callback] receives the id of the new album. */
class CreateVirtualAlbumDialog(val activity: Activity, val callback: (albumId: Long) -> Unit) {
    init {
        val (container, editText) = activity.buildAlbumNameField("")
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(container, this, R.string.create_virtual_album) { alertDialog ->
                    alertDialog.showKeyboard(editText)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = editText.text.toString().trim()
                        if (name.isEmpty()) {
                            activity.toast(R.string.virtual_album_name_empty)
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            if (activity.virtualAlbumNameExists(name)) {
                                activity.runOnUiThread { activity.toast(R.string.virtual_album_exists) }
                                return@ensureBackgroundThread
                            }

                            val albumId = activity.createVirtualAlbum(name)
                            activity.runOnUiThread {
                                if (albumId == -1L) {
                                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                } else {
                                    alertDialog.dismiss()
                                    callback(albumId)
                                }
                            }
                        }
                    }
                }
            }
    }
}

class RenameVirtualAlbumDialog(
    val activity: Activity,
    val albumId: Long,
    val currentName: String,
    val callback: () -> Unit
) {
    init {
        val (container, editText) = activity.buildAlbumNameField(currentName)
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(container, this, R.string.rename_virtual_album) { alertDialog ->
                    alertDialog.showKeyboard(editText)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = editText.text.toString().trim()
                        if (name.isEmpty()) {
                            activity.toast(R.string.virtual_album_name_empty)
                            return@setOnClickListener
                        }

                        if (name == currentName) {
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            val renamed = activity.renameVirtualAlbum(albumId, name)
                            activity.runOnUiThread {
                                if (renamed) {
                                    alertDialog.dismiss()
                                    callback()
                                } else {
                                    activity.toast(R.string.virtual_album_exists)
                                }
                            }
                        }
                    }
                }
            }
    }
}

/**
 * Lets the user pick one of the existing virtual albums, or create a new one.
 * [callback] receives the id and the name of the chosen album.
 * @param excludedAlbumId an album that should not be offered, e.g. the one the items are already being viewed in
 */
class PickVirtualAlbumDialog(
    val activity: Activity,
    val excludedAlbumId: Long? = null,
    val callback: (albumId: Long, albumName: String) -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        ensureBackgroundThread {
            val albums = activity.getVirtualAlbums().filter { it.id != null && it.id != excludedAlbumId }
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@runOnUiThread
                }

                show(albums.map { it.id!! to it.name })
            }
        }
    }

    private fun show(albums: List<Pair<Long, String>>) {
        val newAlbumLabel = activity.getString(R.string.new_virtual_album)
        val labels = albums.map { it.second } + newAlbumLabel
        val listView = ListView(activity).apply {
            adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, labels)
            setOnItemClickListener { _, _, position, _ ->
                dialog?.dismiss()
                if (position == albums.size) {
                    CreateVirtualAlbumDialog(activity) { albumId ->
                        ensureBackgroundThread {
                            val name = activity.getVirtualAlbums().firstOrNull { it.id == albumId }?.name ?: ""
                            activity.runOnUiThread { callback(albumId, name) }
                        }
                    }
                } else {
                    callback(albums[position].first, albums[position].second)
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(listView, this, R.string.add_to_virtual_album) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }
}
