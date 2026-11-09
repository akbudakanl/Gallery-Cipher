package org.fossify.gallery.helpers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Ephemeral Zero-Disk ContentProvider for streaming decrypted vault media to Chromium WebView.
 * Plaintext bytes are piped on-the-fly through a Linux kernel pipe in RAM without ever touching flash storage.
 */
class VaultStreamProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY_SUFFIX = ".vaultstream"
        private val activeStreams = ConcurrentHashMap<String, File>()
        private val tokenTimestamps = ConcurrentHashMap<String, Long>()
        private const val TOKEN_MAX_LIFETIME_MS = 10 * 60 * 1000L // 10 minutes

        fun createStreamUri(context: Context, encryptedFile: File): Uri {
            val token = UUID.randomUUID().toString()
            activeStreams[token] = encryptedFile
            tokenTimestamps[token] = System.currentTimeMillis()
            val authority = "${context.packageName}$AUTHORITY_SUFFIX"
            return Uri.parse("content://$authority/$token")
        }

        fun clearAll() {
            activeStreams.clear()
            tokenTimestamps.clear()
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val token = uri.lastPathSegment ?: return null
        val file = activeStreams[token] ?: return null
        if (!CryptoManager.isVaultUnlocked) return null

        val meta = CryptoManager.readMetadata(file)
        val displayName = meta?.originalName ?: "vault_${file.nameWithoutExtension}.jpg"
        val size = meta?.originalSize ?: file.length()

        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(cols)
        val row = cursor.newRow()
        for (col in cols) {
            when (col) {
                OpenableColumns.DISPLAY_NAME -> row.add(displayName)
                OpenableColumns.SIZE -> row.add(size)
                else -> row.add(null)
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        val token = uri.lastPathSegment ?: return null
        val file = activeStreams[token] ?: return null
        if (!CryptoManager.isVaultUnlocked) return null

        val meta = CryptoManager.readMetadata(file)
        val name = meta?.originalName ?: file.name
        val ext = name.substringAfterLast('.', "").lowercase()
        return if (ext.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        } else {
            "image/jpeg"
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (!mode.contains("r")) {
            throw SecurityException("Write access denied to VaultStreamProvider")
        }

        val token = uri.lastPathSegment ?: return null
        val file = activeStreams[token] ?: return null
        if (!CryptoManager.isVaultUnlocked) return null

        val timestamp = tokenTimestamps[token] ?: 0L
        if (System.currentTimeMillis() - timestamp > TOKEN_MAX_LIFETIME_MS) {
            activeStreams.remove(token)
            tokenTimestamps.remove(token)
            return null
        }

        // Create Linux Kernel pipe in volatile RAM (zero flash writes)
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        Thread {
            try {
                FileOutputStream(writeSide.fileDescriptor).use { outputStream ->
                    CryptoManager.openDecryptedContentStream(file)?.use { inputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }
            } catch (_: IOException) {
                // Pipe closed by reader (Chromium) or cancelled upload
            } catch (_: Exception) {
            } finally {
                try {
                    writeSide.close()
                } catch (_: Exception) {}
            }
        }.start()

        return readSide
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
