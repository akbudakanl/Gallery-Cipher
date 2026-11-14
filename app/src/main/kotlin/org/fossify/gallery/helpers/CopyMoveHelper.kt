package org.fossify.gallery.helpers

import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.canManageMedia
import org.fossify.commons.extensions.createDirectorySync
import org.fossify.commons.extensions.deleteFileBg
import org.fossify.commons.extensions.deleteFromMediaStore
import org.fossify.commons.extensions.formatSize
import org.fossify.commons.extensions.getAndroidSAFFileItems
import org.fossify.commons.extensions.getAvailableStorageB
import org.fossify.commons.extensions.getDocumentFile
import org.fossify.commons.extensions.getDocumentSdk30
import org.fossify.commons.extensions.getDoesFilePathExist
import org.fossify.commons.extensions.getFileInputStreamSync
import org.fossify.commons.extensions.getFileOutputStreamSync
import org.fossify.commons.extensions.getFileUrisFromFileDirItems
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getMimeType
import org.fossify.commons.extensions.isAccessibleWithSAFSdk30
import org.fossify.commons.extensions.isMediaFile
import org.fossify.commons.extensions.isPathOnOTG
import org.fossify.commons.extensions.isPathOnSD
import org.fossify.commons.extensions.isRecycleBinPath
import org.fossify.commons.extensions.isRestrictedSAFOnlyRoot
import org.fossify.commons.extensions.isRestrictedWithSAFSdk30
import org.fossify.commons.extensions.needsStupidWritePermissions
import org.fossify.commons.extensions.rescanPath
import org.fossify.commons.extensions.rescanPaths
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toFileDirItem
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.CONFLICT_KEEP_BOTH
import org.fossify.commons.helpers.CONFLICT_OVERWRITE
import org.fossify.commons.helpers.CONFLICT_SKIP
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.getConflictResolution
import org.fossify.commons.models.FileDirItem
import org.fossify.gallery.dialogs.CryptoProgressDialog
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val COPY_BUFFER_SIZE = 8192
private const val BYTES_PER_KB = 1000L
private const val UI_PROGRESS_UPDATE_INTERVAL_MS = 100L

/**
 * Executes unencrypted copy/move operations with both an in-app progress bar dialog and the
 * standard background notification progress bar.
 */
fun BaseSimpleActivity.copyMoveFilesWithProgress(
    fileDirItems: ArrayList<FileDirItem>,
    source: String,
    destination: String,
    isCopyOperation: Boolean,
    copyMediaOnly: Boolean,
    copyHidden: Boolean,
    callback: (destinationPath: String) -> Unit
) {
    if (fileDirItems.isEmpty()) {
        toast(org.fossify.commons.R.string.unknown_error_occurred)
        return
    }
    if (source == destination) {
        toast(org.fossify.commons.R.string.source_and_destination_same)
        return
    }
    if (!getDoesFilePathExist(destination)) {
        toast(org.fossify.commons.R.string.invalid_destination)
        return
    }

    handleSAFDialog(destination) { safGranted ->
        if (!safGranted) {
            toast(org.fossify.commons.R.string.copy_move_failed)
            return@handleSAFDialog
        }
        handleSAFDialogSdk30(destination) { sdk30Granted ->
            if (!sdk30Granted) {
                toast(org.fossify.commons.R.string.copy_move_failed)
                return@handleSAFDialogSdk30
            }

            val useStreamCopy = isCopyOperation || needsStreamCopyMove(source, destination)
            if (isCopyOperation && canManageMedia() && !fileDirItems.first().isRecycleBinPath(this)) {
                val uris = getFileUrisFromFileDirItems(fileDirItems)
                updateSDK30Uris(uris) { updated ->
                    if (updated) {
                        prepareAndRunCopyMove(
                            fileDirItems = fileDirItems,
                            destination = destination,
                            isCopyOperation = isCopyOperation,
                            useStreamCopy = useStreamCopy,
                            copyMediaOnly = copyMediaOnly,
                            copyHidden = copyHidden,
                            callback = callback
                        )
                    } else {
                        toast(org.fossify.commons.R.string.copy_move_failed)
                    }
                }
            } else {
                prepareAndRunCopyMove(
                    fileDirItems = fileDirItems,
                    destination = destination,
                    isCopyOperation = isCopyOperation,
                    useStreamCopy = useStreamCopy,
                    copyMediaOnly = copyMediaOnly,
                    copyHidden = copyHidden,
                    callback = callback
                )
            }
        }
    }
}

private fun BaseSimpleActivity.needsStreamCopyMove(source: String, destination: String): Boolean {
    return isPathOnOTG(source) || isPathOnOTG(destination) ||
        isPathOnSD(source) || isPathOnSD(destination) ||
        isRestrictedSAFOnlyRoot(source) || isRestrictedSAFOnlyRoot(destination) ||
        isAccessibleWithSAFSdk30(source) || isAccessibleWithSAFSdk30(destination)
}

private fun BaseSimpleActivity.prepareAndRunCopyMove(
    fileDirItems: ArrayList<FileDirItem>,
    destination: String,
    isCopyOperation: Boolean,
    useStreamCopy: Boolean,
    copyMediaOnly: Boolean,
    copyHidden: Boolean,
    callback: (destinationPath: String) -> Unit
) {
    val startAfterSpaceCheck = {
        checkConflicts(fileDirItems, destination, 0, LinkedHashMap()) { conflictResolutions ->
            ensureNotificationPermissionThenRun { notificationsAllowed ->
                executeCopyMoveWithProgress(
                    fileDirItems = fileDirItems,
                    destination = destination,
                    isCopyOperation = isCopyOperation,
                    useStreamCopy = useStreamCopy,
                    copyMediaOnly = copyMediaOnly,
                    copyHidden = copyHidden,
                    conflictResolutions = conflictResolutions,
                    enableNotification = notificationsAllowed,
                    callback = callback
                )
            }
        }
    }

    if (!useStreamCopy) {
        startAfterSpaceCheck()
        return
    }

    ensureBackgroundThread {
        val availableSpace = destination.getAvailableStorageB()
        val sumToCopy = fileDirItems.sumOf { item ->
            if (item.size == 0L) {
                item.size = item.getProperSize(applicationContext, copyHidden)
            }
            item.size
        }
        runOnUiThread {
            if (isFinishing || isDestroyed) {
                return@runOnUiThread
            }
            if (availableSpace != -1L && sumToCopy >= availableSpace) {
                val text = String.format(
                    getString(org.fossify.commons.R.string.no_space),
                    sumToCopy.formatSize(),
                    availableSpace.formatSize()
                )
                toast(text, Toast.LENGTH_LONG)
            } else {
                startAfterSpaceCheck()
            }
        }
    }
}

private fun BaseSimpleActivity.executeCopyMoveWithProgress(
    fileDirItems: ArrayList<FileDirItem>,
    destination: String,
    isCopyOperation: Boolean,
    useStreamCopy: Boolean,
    copyMediaOnly: Boolean,
    copyHidden: Boolean,
    conflictResolutions: LinkedHashMap<String, Int>,
    enableNotification: Boolean,
    callback: (destinationPath: String) -> Unit
) {
    val titleId = if (isCopyOperation) {
        org.fossify.commons.R.string.copying
    } else {
        org.fossify.commons.R.string.moving
    }
    val progressDialog = CryptoProgressDialog(
        activity = this,
        max = fileDirItems.size,
        titleId = titleId,
        notificationChannelId = OperationProgressNotifier.CHANNEL_COPY_MOVE,
        notificationIconRes = org.fossify.commons.R.drawable.ic_copy_vector,
        enableNotification = enableNotification
    )
    val config = CopyMoveConfig(
        destination = destination,
        isCopyOperation = isCopyOperation,
        useStreamCopy = useStreamCopy,
        copyMediaOnly = copyMediaOnly,
        copyHidden = copyHidden,
        conflictResolutions = conflictResolutions
    )

    ensureBackgroundThread {
        val runner = CopyMoveRunner(
            activity = this,
            config = config,
            progressDialog = progressDialog
        )
        val result = runner.execute(fileDirItems)

        runOnUiThread {
            progressDialog.dismiss()
            val finishUi = {
                showCopyMoveCompletionToast(
                    isCopyOperation = isCopyOperation,
                    taskSucceeded = result.succeeded,
                    transferredCount = result.transferredCount,
                    expectedCount = result.expectedCount
                )
                if (result.succeeded) {
                    callback(destination)
                }
            }
            if (result.itemsToDeleteSdk30.isNotEmpty()) {
                val uris = getFileUrisFromFileDirItems(result.itemsToDeleteSdk30)
                deleteSDK30Uris(uris) { deleted ->
                    if (deleted) {
                        ensureBackgroundThread {
                            result.itemsToDeleteSdk30.forEach { deleteFromMediaStore(it.path) }
                        }
                    }
                    finishUi()
                }
            } else {
                finishUi()
            }
        }
    }
}

private fun BaseSimpleActivity.showCopyMoveCompletionToast(
    isCopyOperation: Boolean,
    taskSucceeded: Boolean,
    transferredCount: Int,
    expectedCount: Int
) {
    if (!taskSucceeded) {
        toast(org.fossify.commons.R.string.copy_move_failed)
        return
    }
    val allSucceeded = transferredCount >= expectedCount
    val isSingle = transferredCount == 1
    val msgId = if (isCopyOperation) {
        when {
            !allSucceeded -> org.fossify.commons.R.string.copying_success_partial
            isSingle -> org.fossify.commons.R.string.copying_success_one
            else -> org.fossify.commons.R.string.copying_success
        }
    } else {
        when {
            !allSucceeded -> org.fossify.commons.R.string.moving_success_partial
            isSingle -> org.fossify.commons.R.string.moving_success_one
            else -> org.fossify.commons.R.string.moving_success
        }
    }
    toast(msgId)
}

private data class CopyMoveConfig(
    val destination: String,
    val isCopyOperation: Boolean,
    val useStreamCopy: Boolean,
    val copyMediaOnly: Boolean,
    val copyHidden: Boolean,
    val conflictResolutions: LinkedHashMap<String, Int>
)

private data class CopyMoveResult(
    val succeeded: Boolean,
    val transferredCount: Int,
    val expectedCount: Int,
    val itemsToDeleteSdk30: ArrayList<FileDirItem>
)

private class CopyMoveRunner(
    private val activity: BaseSimpleActivity,
    private val config: CopyMoveConfig,
    private val progressDialog: CryptoProgressDialog
) {
    private val transferredFiles = ArrayList<FileDirItem>()
    private val itemsToDeleteSdk30 = ArrayList<FileDirItem>()
    private val movedPathsToRescan = ArrayList<String>()
    private val documents = LinkedHashMap<String, DocumentFile?>()

    private var totalItems = 0
    private var processedItems = 0
    private var totalBytes = 0L
    private var transferredBytes = 0L
    private var lastUiUpdateMs = 0L

    fun execute(items: ArrayList<FileDirItem>): CopyMoveResult {
        totalItems = items.size
        var expectedCount = items.size
        totalBytes = computeTotalBytes(items)

        val succeeded = try {
            for (item in items) {
                val destItem = resolveTopLevelDestination(item)
                if (destItem == null) {
                    expectedCount--
                    processedItems++
                    publishProgress(forceUi = true, filename = item.name)
                    continue
                }

                publishProgress(forceUi = true, filename = item.name)
                transferTopLevelItem(item, destItem)
                processedItems++
                publishProgress(forceUi = true, filename = item.name)
            }
            if (movedPathsToRescan.isNotEmpty()) {
                activity.rescanPaths(movedPathsToRescan)
            }
            true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            false
        }

        return CopyMoveResult(
            succeeded = succeeded,
            transferredCount = transferredFiles.size,
            expectedCount = expectedCount,
            itemsToDeleteSdk30 = itemsToDeleteSdk30
        )
    }

    private fun computeTotalBytes(items: ArrayList<FileDirItem>): Long {
        var bytes = 0L
        for (item in items) {
            if (item.size == 0L) {
                item.size = item.getProperSize(activity.applicationContext, config.copyHidden)
            }
            val targetPath = "${config.destination}/${item.name}"
            val exists = activity.getDoesFilePathExist(targetPath)
            val resolution = getConflictResolution(config.conflictResolutions, targetPath)
            if (resolution != CONFLICT_SKIP || !exists) {
                bytes += item.size
            }
        }
        return bytes
    }

    private fun resolveTopLevelDestination(item: FileDirItem): FileDirItem? {
        val targetPath = "${config.destination}/${item.name}"
        var destItem = FileDirItem(targetPath, targetPath.getFilenameFromPath(), item.isDirectory)
        if (!activity.getDoesFilePathExist(targetPath)) {
            return destItem
        }

        when (getConflictResolution(config.conflictResolutions, targetPath)) {
            CONFLICT_SKIP -> return null
            CONFLICT_KEEP_BOTH -> {
                val altFile = activity.getAlternativeFile(File(destItem.path))
                destItem = FileDirItem(altFile.path, altFile.name, altFile.isDirectory)
            }
            CONFLICT_OVERWRITE -> {
                if (!config.useStreamCopy && !item.isDirectory) {
                    File(destItem.path).delete()
                }
            }
        }
        return destItem
    }

    private fun transferTopLevelItem(source: FileDirItem, dest: FileDirItem) {
        if (!config.useStreamCopy && !source.isDirectory) {
            val srcFile = File(source.path)
            val dstFile = File(dest.path)
            if (!dstFile.exists() && srcFile.renameTo(dstFile)) {
                if (!activity.baseConfig.keepLastModified) {
                    dstFile.setLastModified(System.currentTimeMillis())
                }
                movedPathsToRescan.add(dstFile.absolutePath)
                activity.deleteFromMediaStore(source.path)
                transferredFiles.add(source)
                transferredBytes += source.size
                return
            }
        }
        copy(source, dest)
    }

    private fun copy(source: FileDirItem, dest: FileDirItem) {
        if (source.isDirectory) {
            copyDirectory(source, dest.path)
        } else {
            copyFile(source, dest)
        }
    }

    private fun copyDirectory(source: FileDirItem, destPath: String) {
        if (!activity.createDirectorySync(destPath)) {
            val msg = activity.getString(org.fossify.commons.R.string.could_not_create_folder)
            activity.showErrorToast(String.format(msg, destPath))
            return
        }

        when {
            activity.isPathOnOTG(source.path) -> {
                val children = activity.getDocumentFile(source.path)?.listFiles() ?: return
                copyDocumentChildren(source, destPath, children)
            }
            activity.isRestrictedSAFOnlyRoot(source.path) -> {
                activity.getAndroidSAFFileItems(source.path, true, false) { files ->
                    copyFileDirChildren(source, destPath, files)
                }
            }
            activity.isAccessibleWithSAFSdk30(source.path) -> {
                val children = activity.getDocumentSdk30(source.path)?.listFiles() ?: return
                copyDocumentChildren(source, destPath, children)
            }
            else -> {
                val childNames = File(source.path).list() ?: return
                copyLocalChildren(source, destPath, childNames)
            }
        }
    }

    private fun copyDocumentChildren(
        source: FileDirItem,
        destPath: String,
        children: Array<DocumentFile>
    ) {
        children.forEach { child ->
            val childName = child.name ?: return@forEach
            val targetPath = "$destPath/$childName"
            if (!File(targetPath).exists()) {
                val srcChild = FileDirItem(
                    path = "${source.path}/$childName",
                    name = childName,
                    isDirectory = child.isDirectory,
                    size = child.length()
                )
                copy(srcChild, FileDirItem(targetPath, childName, child.isDirectory))
            }
        }
        transferredFiles.add(source)
    }

    private fun copyFileDirChildren(
        source: FileDirItem,
        destPath: String,
        children: List<FileDirItem>
    ) {
        children.forEach { file ->
            val targetPath = "$destPath/${file.name}"
            if (!activity.getDoesFilePathExist(targetPath)) {
                val srcChild = FileDirItem(
                    path = "${source.path}/${file.name}",
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = file.size
                )
                copy(srcChild, FileDirItem(targetPath, file.name, file.isDirectory))
            }
        }
        transferredFiles.add(source)
    }

    private fun copyLocalChildren(
        source: FileDirItem,
        destPath: String,
        childNames: Array<String>
    ) {
        childNames.forEach { childName ->
            val targetPath = "$destPath/$childName"
            if (!activity.getDoesFilePathExist(targetPath)) {
                val srcFile = File(source.path, childName)
                val srcChild = srcFile.toFileDirItem(activity)
                val dstChild = FileDirItem(targetPath, targetPath.getFilenameFromPath(), srcFile.isDirectory)
                copy(srcChild, dstChild)
            }
        }
        transferredFiles.add(source)
    }

    private fun copyFile(source: FileDirItem, dest: FileDirItem) {
        if (config.copyMediaOnly && !source.path.isMediaFile()) {
            transferredBytes += source.size
            return
        }

        val destParent = dest.getParentPath()
        if (!activity.createDirectorySync(destParent)) {
            val msg = activity.getString(org.fossify.commons.R.string.could_not_create_folder)
            activity.showErrorToast(String.format(msg, destParent))
            transferredBytes += source.size
            return
        }

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var transferSuccess = false
        try {
            if (!documents.containsKey(destParent) && activity.needsStupidWritePermissions(dest.path)) {
                documents[destParent] = activity.getDocumentFile(destParent)
            }
            outputStream = activity.getFileOutputStreamSync(
                dest.path,
                source.path.getMimeType(),
                documents[destParent]
            )
            inputStream = activity.getFileInputStreamSync(source.path)
            val bytesCopied = streamCopy(inputStream, outputStream, source.name)

            if (source.size == bytesCopied && activity.getDoesFilePathExist(dest.path)) {
                transferSuccess = true
                transferredFiles.add(source)
                finalizeCopiedFile(source, dest)
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            closeQuietly(inputStream)
            closeQuietly(outputStream)
        }

        if (transferSuccess && !config.isCopyOperation) {
            deleteSourceFile(source)
        }
    }

    private fun streamCopy(
        inputStream: InputStream?,
        outputStream: OutputStream?,
        filename: String
    ): Long {
        if (inputStream == null || outputStream == null) {
            return 0L
        }
        var bytesCopied = 0L
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        var bytesRead = inputStream.read(buffer)
        while (bytesRead >= 0) {
            outputStream.write(buffer, 0, bytesRead)
            bytesCopied += bytesRead
            transferredBytes += bytesRead
            publishProgress(forceUi = false, filename = filename)
            bytesRead = inputStream.read(buffer)
        }
        outputStream.flush()
        return bytesCopied
    }

    private fun finalizeCopiedFile(source: FileDirItem, dest: FileDirItem) {
        if (config.isCopyOperation) {
            activity.rescanPath(dest.path) {
                if (activity.baseConfig.keepLastModified) {
                    updateLastModifiedValues(source, dest)
                    activity.rescanPath(dest.path)
                }
            }
        } else if (activity.baseConfig.keepLastModified) {
            updateLastModifiedValues(source, dest)
            activity.rescanPath(dest.path)
        }
    }

    private fun closeQuietly(stream: java.io.Closeable?) {
        try {
            stream?.close()
        } catch (_: Exception) {
        }
    }

    private fun deleteSourceFile(source: FileDirItem) {
        if (activity.isRestrictedWithSAFSdk30(source.path) && !activity.canManageMedia()) {
            itemsToDeleteSdk30.add(source)
        } else {
            activity.deleteFileBg(source, allowDeleteFolder = false, isDeletingMultipleFiles = false)
            activity.deleteFromMediaStore(source.path)
        }
    }

    private fun updateLastModifiedValues(source: FileDirItem, dest: FileDirItem) {
        copyOldLastModified(source.path, dest.path)
        val newTimestamp = File(source.path).lastModified()
        if (newTimestamp != 0L) {
            File(dest.path).setLastModified(newTimestamp)
        }
    }

    private fun copyOldLastModified(sourcePath: String, destPath: String) {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        try {
            activity.applicationContext.contentResolver.query(
                uri,
                projection,
                selection,
                arrayOf(sourcePath),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateTakenIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    val dateModifiedIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val values = ContentValues().apply {
                        if (dateTakenIdx >= 0) {
                            put(MediaStore.Images.Media.DATE_TAKEN, cursor.getLong(dateTakenIdx))
                        }
                        if (dateModifiedIdx >= 0) {
                            put(MediaStore.Files.FileColumns.DATE_MODIFIED, cursor.getInt(dateModifiedIdx))
                        }
                    }
                    if (values.size() > 0) {
                        activity.applicationContext.contentResolver.update(uri, values, selection, arrayOf(destPath))
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun publishProgress(forceUi: Boolean, filename: String) {
        val totalKb = (totalBytes / BYTES_PER_KB).toInt()
        val currentKb = (transferredBytes / BYTES_PER_KB).toInt()
        val barMax = if (totalKb > 0) totalKb else maxOf(totalItems, 1)
        val barCurrent = if (totalKb > 0) currentKb.coerceIn(0, barMax) else processedItems.coerceIn(0, barMax)

        progressDialog.updateNotifierOnly(current = barCurrent, max = barMax, contentText = filename)

        val now = System.currentTimeMillis()
        if (forceUi || now - lastUiUpdateMs >= UI_PROGRESS_UPDATE_INTERVAL_MS) {
            lastUiUpdateMs = now
            activity.runOnUiThread {
                progressDialog.updateProgress(
                    current = processedItems,
                    total = totalItems,
                    barCurrent = barCurrent,
                    barMax = barMax,
                    notificationText = filename
                )
            }
        }
    }
}
