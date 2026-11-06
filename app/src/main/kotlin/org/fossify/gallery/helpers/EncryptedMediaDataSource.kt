package org.fossify.gallery.helpers

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

@UnstableApi
class EncryptedMediaDataSource : BaseDataSource(false) {
    private var uri: Uri? = null
    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        val path = dataSpec.uri.path ?: throw FileNotFoundException("Null path in URI")
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("Encrypted file not found: $path")
        }

        val meta = CryptoManager.readMetadata(file)
            ?: throw IOException("Unable to read encrypted file metadata (Vault locked?)")

        if (dataSpec.position > meta.originalSize) {
            throw EOFException("Position out of range: ${dataSpec.position} > ${meta.originalSize}")
        }

        inputStream = CryptoManager.openDecryptedContentStream(file, dataSpec.position)
            ?: throw IOException("Failed to open decrypted AES-CTR stream")

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            meta.originalSize - dataSpec.position
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = minOf(bytesRemaining, length.toLong()).toInt()
        val read = try {
            inputStream?.read(buffer, offset, bytesToRead) ?: C.RESULT_END_OF_INPUT
        } catch (e: IOException) {
            throw e
        }

        if (read == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong() && bytesRemaining > 0) {
                throw EOFException()
            }
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= read
        }
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            inputStream?.close()
        } catch (_: IOException) {
        } finally {
            inputStream = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = EncryptedMediaDataSource()
    }
}
