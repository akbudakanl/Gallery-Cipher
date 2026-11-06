package org.fossify.gallery.helpers

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class EncryptedStringModelLoader : ModelLoader<String, InputStream> {
    override fun handles(model: String): Boolean {
        val cleanPath = model.removePrefix("file://")
        return CryptoManager.isEncryptedFile(cleanPath)
    }

    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        val cleanPath = model.removePrefix("file://")
        val file = File(cleanPath)
        return ModelLoader.LoadData(
            ObjectKey("${file.absolutePath}_${file.lastModified()}_${width}x${height}"),
            EncryptedStreamFetcher(file, width, height)
        )
    }

    class Factory : ModelLoaderFactory<String, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, InputStream> {
            return EncryptedStringModelLoader()
        }

        override fun teardown() {}
    }
}

class EncryptedFileModelLoader : ModelLoader<File, InputStream> {
    override fun handles(model: File): Boolean {
        return CryptoManager.isEncryptedFile(model.name)
    }

    override fun buildLoadData(
        model: File,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(
            ObjectKey("${model.absolutePath}_${model.lastModified()}_${width}x${height}"),
            EncryptedStreamFetcher(model, width, height)
        )
    }

    class Factory : ModelLoaderFactory<File, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<File, InputStream> {
            return EncryptedFileModelLoader()
        }

        override fun teardown() {}
    }
}

class EncryptedStreamFetcher(
    private val file: File,
    private val width: Int,
    private val height: Int
) : DataFetcher<InputStream> {
    private var stream: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        if (!CryptoManager.isVaultUnlocked) {
            callback.onLoadFailed(FileNotFoundException("Vault is locked"))
            return
        }

        try {
            val meta = CryptoManager.readMetadata(file)
            val isThumbnailSize = width in 1..600 && height in 1..600
            val isVideo = meta?.mediaType == TYPE_VIDEOS

            if (isVideo || (isThumbnailSize && meta?.mediaType != TYPE_GIFS)) {
                val thumbBytes = CryptoManager.readEmbeddedThumbnailBytes(file)
                if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                    stream = ByteArrayInputStream(thumbBytes)
                    callback.onDataReady(stream)
                    return
                }
            }

            stream = CryptoManager.openDecryptedContentStream(file)
            if (stream != null) {
                callback.onDataReady(stream)
            } else {
                callback.onLoadFailed(FileNotFoundException("Unable to decrypt stream for ${file.name}"))
            }
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {
        try {
            stream?.close()
        } catch (_: Exception) {
        }
        stream = null
    }

    override fun cancel() {}

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.LOCAL
}
