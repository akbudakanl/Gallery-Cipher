package org.fossify.gallery.dialogs

import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.formatDate
import org.fossify.commons.extensions.formatSize
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getFormattedDuration
import org.fossify.commons.extensions.isAValidFilename
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogRenameEncryptedItemBinding
import org.fossify.gallery.helpers.CryptoManager
import org.fossify.gallery.helpers.TYPE_VIDEOS
import java.io.File

class RenameEncryptedItemDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val callback: (newOriginalName: String) -> Unit
) {
    private val binding = DialogRenameEncryptedItemBinding.inflate(activity.layoutInflater)

    init {
        val currentName = CryptoManager.getDisplayName(path)
        val dotAt = currentName.lastIndexOf('.')
        val initialBase = if (dotAt > 0) currentName.substring(0, dotAt) else currentName
        val initialExt = if (dotAt > 0) currentName.substring(dotAt + 1) else ""

        binding.apply {
            renameEncryptedFilenameValue.setText(initialBase)
            renameEncryptedExtensionValue.setText(initialExt)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameEncryptedFilenameValue)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newBase = binding.renameEncryptedFilenameValue.value.trim()
                        val newExt = binding.renameEncryptedExtensionValue.value.trim()

                        if (newBase.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val updatedName = if (newExt.isEmpty()) newBase else "$newBase.$newExt"
                        if (!updatedName.isAValidFilename()) {
                            activity.toast(org.fossify.commons.R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (updatedName == currentName) {
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            val renamed = CryptoManager.renameEncryptedFile(File(path), updatedName)
                            activity.runOnUiThread {
                                if (renamed) {
                                    alertDialog.dismiss()
                                    callback(updatedName)
                                } else {
                                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                }
                            }
                        }
                    }
                }
            }
    }
}

class RenameEncryptedItemsDialog(
    val activity: BaseSimpleActivity,
    val paths: ArrayList<String>,
    val callback: () -> Unit
) {
    init {
        val padding = (20 * activity.resources.displayMetrics.density).toInt()
        val editText = AppCompatEditText(activity).apply {
            hint = activity.getString(org.fossify.commons.R.string.filename)
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
                activity.setupDialogStuff(container, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(editText)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val baseName = editText.text.toString().trim()
                        if (baseName.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        if (!baseName.isAValidFilename()) {
                            activity.toast(org.fossify.commons.R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            var allSuccess = true
                            paths.forEachIndexed { index, itemPath ->
                                val currentName = CryptoManager.getDisplayName(itemPath)
                                val dotAt = currentName.lastIndexOf('.')
                                val ext = if (dotAt > 0) currentName.substring(dotAt + 1) else ""
                                val indexedName = if (ext.isNotEmpty()) {
                                    "${baseName}_${index + 1}.$ext"
                                } else {
                                    "${baseName}_${index + 1}"
                                }
                                if (!CryptoManager.renameEncryptedFile(File(itemPath), indexedName)) {
                                    allSuccess = false
                                }
                            }
                            activity.runOnUiThread {
                                if (!allSuccess) {
                                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                }
                                alertDialog.dismiss()
                                callback()
                            }
                        }
                    }
                }
            }
    }
}

class RenameEncryptedFolderDialog(
    val activity: BaseSimpleActivity,
    val folder: File,
    val currentName: String,
    val callback: (newName: String) -> Unit
) {
    init {
        val padding = (20 * activity.resources.displayMetrics.density).toInt()
        val editText = AppCompatEditText(activity).apply {
            setText(currentName)
            setSelection(currentName.length)
            hint = activity.getString(R.string.album_name)
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
                activity.setupDialogStuff(container, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(editText)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newName = editText.text.toString().trim()
                        if (newName.isEmpty()) {
                            activity.toast(R.string.hidden_album_name_empty)
                            return@setOnClickListener
                        }

                        if (!newName.isAValidFilename()) {
                            activity.toast(org.fossify.commons.R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (newName == currentName) {
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            val renamed = CryptoManager.renameEncryptedFolder(activity, folder, newName)
                            activity.runOnUiThread {
                                if (renamed) {
                                    alertDialog.dismiss()
                                    callback(newName)
                                } else {
                                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                }
                            }
                        }
                    }
                }
            }
    }
}

class EncryptedPropertiesDialog(
    val activity: BaseSimpleActivity,
    val paths: List<String>
) {
    constructor(activity: BaseSimpleActivity, path: String) : this(activity, listOf(path))

    init {
        ensureBackgroundThread {
            val message = if (paths.size == 1) {
                val file = File(paths.first())
                val meta = CryptoManager.readMetadata(file)
                val displayName = meta?.originalName ?: file.name
                val albumName = file.parentFile?.let { CryptoManager.getDecryptedFolderName(it, activity) }
                    ?: activity.getString(R.string.secret_vault)
                val sb = StringBuilder()
                sb.appendLine("${activity.getString(org.fossify.commons.R.string.filename)}: $displayName")
                sb.appendLine("${activity.getString(R.string.album_name)}: $albumName")
                if (meta != null) {
                    sb.appendLine("${activity.getString(org.fossify.commons.R.string.size)}: ${meta.originalSize.formatSize()}")
                    if (meta.lastModified > 0L) {
                        sb.appendLine("${activity.getString(org.fossify.commons.R.string.last_modified)}: ${meta.lastModified.formatDate(activity)}")
                    }
                    if (meta.dateTaken > 0L) {
                        sb.appendLine("${activity.getString(org.fossify.commons.R.string.date_taken)}: ${meta.dateTaken.formatDate(activity)}")
                    }
                    if (meta.mediaType == TYPE_VIDEOS && meta.videoDuration > 0) {
                        sb.appendLine(meta.videoDuration.getFormattedDuration())
                    }
                }
                sb.toString().trim()
            } else {
                var totalSize = 0L
                paths.forEach { p ->
                    val meta = CryptoManager.readMetadata(File(p))
                    totalSize += meta?.originalSize ?: File(p).length()
                }
                "${activity.getString(org.fossify.commons.R.string.size)}: ${totalSize.formatSize()}"
            }

            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.getAlertDialogBuilder()
                        .setTitle(org.fossify.commons.R.string.properties)
                        .setMessage(message)
                        .setPositiveButton(org.fossify.commons.R.string.ok, null)
                        .show()
                }
            }
        }
    }
}
