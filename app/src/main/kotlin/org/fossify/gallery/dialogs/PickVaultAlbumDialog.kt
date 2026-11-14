package org.fossify.gallery.dialogs

import android.app.Activity
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
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
                    CreateEncryptedAlbumDialog(activity, callback)
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
}
