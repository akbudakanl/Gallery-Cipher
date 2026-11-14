package org.fossify.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogImportFavoritesConflictBinding
import org.fossify.gallery.helpers.FavoritesImportMode

/**
 * Prompts the user to decide how imported favorites should be applied when a favorites list
 * already exists: either delete the existing list or merge both lists.
 */
class ImportFavoritesConflictDialog(
    val activity: BaseSimpleActivity,
    val onConfirm: (mode: FavoritesImportMode) -> Unit,
    val onCancel: () -> Unit = {}
) {
    init {
        val binding = DialogImportFavoritesConflictBinding.inflate(activity.layoutInflater)
        var selectedMode: FavoritesImportMode? = null

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.confirm, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ ->
                onCancel()
            }
            .setOnCancelListener {
                onCancel()
            }
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.favorites_already_exist) { alertDialog ->
                    val confirmButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    confirmButton.isEnabled = false

                    binding.importFavoritesConflictGroup.setOnCheckedChangeListener { _, checkedId ->
                        selectedMode = when (checkedId) {
                            R.id.import_favorites_delete_existing -> FavoritesImportMode.DELETE_EXISTING
                            R.id.import_favorites_merge -> FavoritesImportMode.MERGE
                            else -> null
                        }
                        confirmButton.isEnabled = selectedMode != null
                    }

                    confirmButton.setOnClickListener {
                        val mode = selectedMode ?: return@setOnClickListener
                        alertDialog.dismiss()
                        onConfirm(mode)
                    }
                }
            }
    }
}
