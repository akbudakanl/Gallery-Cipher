package org.fossify.gallery.helpers

import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import org.fossify.commons.extensions.isGif
import org.fossify.commons.extensions.isVideoFast
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Reads the dimensions of media files.
 *
 * The helpers of the commons library dispatch by the file extension, so they cannot be used for the
 * files of the encrypted vault: their content is encrypted and their extension does not tell
 * anything about their type. Vault files are read from their decrypted content instead.
 */
object MediaDimensions {
    private const val ROTATION_90 = 90
    private const val ROTATION_180 = 180
    private const val ROTATION_270 = 270

    /**
     * Returns the rotation (0, 90, 180 or 270 degrees) stored in the EXIF data of the given image.
     * Returns 0 if there is none or if it cannot be read.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun getRotationDegrees(path: String): Int {
        return try {
            if (CryptoManager.isEncryptedFile(path)) {
                CryptoManager.openDecryptedContentStream(File(path))?.use { stream ->
                    degreesForOrientation(
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    )
                } ?: 0
            } else {
                degreesForOrientation(
                    ExifInterface(path).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                )
            }
        } catch (e: Exception) {
            0
        } catch (e: OutOfMemoryError) {
            0
        }
    }

    private fun degreesForOrientation(orientation: Int): Int {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> ROTATION_90
            ExifInterface.ORIENTATION_ROTATE_180 -> ROTATION_180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> ROTATION_270
            else -> 0
        }
    }

    /**
     * True if the given rotation swaps the width and the height of the media.
     */
    fun flipsSides(rotationDegrees: Int) = rotationDegrees == ROTATION_90 || rotationDegrees == ROTATION_270

    /**
     * Returns the size of an image or of an animated image, or null if it cannot be determined.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun getImageSize(path: String): Point? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (CryptoManager.isEncryptedFile(path)) {
                CryptoManager.openDecryptedContentStream(File(path))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }

            if (options.outWidth > 0 && options.outHeight > 0) {
                Point(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Returns the displayed size of a video, or null if it cannot be determined. The size already
     * takes the rotation stored in the video into account, so it can be compared with the size of
     * the video surface directly.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun getVideoSize(path: String): Point? {
        if (CryptoManager.isEncryptedFile(path)) {
            getEncryptedVideoSize(path)?.let { return it }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.readVideoSize()
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * The size of an encrypted video cannot be read with the regular file based helpers, because its
     * content is encrypted. The thumbnail embedded into the encrypted header is a frame of the
     * video, so it has the same aspect ratio. Only if there is no thumbnail at all, the metadata
     * of the decrypted content is used.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun getEncryptedVideoSize(path: String): Point? {
        val file = File(path)
        getThumbnailSize(file)?.let { return it }

        val retriever = MediaMetadataRetriever()
        val dataSource = EncryptedVideoDataSource(file)
        return try {
            retriever.setDataSource(dataSource)
            retriever.readVideoSize()
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {
            }
            dataSource.close()
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun getThumbnailSize(file: File): Point? {
        val bytes = try {
            CryptoManager.readMetadata(file)?.thumbnailBytes
        } catch (e: Exception) {
            null
        }

        if (bytes == null || bytes.isEmpty()) {
            return null
        }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Point(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Returns the type of the given media file. Encrypted files are classified by the type stored
     * in their header, as their extension does not tell anything about their content.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun getMediaType(path: String): Int {
        if (CryptoManager.isEncryptedFile(path)) {
            val meta = try {
                CryptoManager.readMetadata(File(path))
            } catch (e: Exception) {
                null
            }
            return meta?.mediaType ?: TYPE_IMAGES
        }

        return when {
            path.isVideoFast() -> TYPE_VIDEOS
            path.isGif() -> TYPE_GIFS
            else -> TYPE_IMAGES
        }
    }

    private fun MediaMetadataRetriever.readVideoSize(): Point? {
        val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        if (width <= 0 || height <= 0) {
            return null
        }

        val rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        return if (flipsSides(rotation)) {
            Point(height, width)
        } else {
            Point(width, height)
        }
    }
}

/**
 * Gives [MediaMetadataRetriever] random access to the decrypted content of an encrypted file.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
private class EncryptedVideoDataSource(private val file: File) : MediaDataSource() {
    private var stream: InputStream? = null
    private var streamPosition = 0L
    private var size = 0L

    init {
        size = try {
            CryptoManager.readMetadata(file)?.originalSize ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    @Suppress("NestedBlockDepth")
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (size <= 0) {
            return 0
        }

        var currentStream = stream
        if (currentStream == null || position < streamPosition) {
            currentStream = CryptoManager.openDecryptedContentStream(file, position) ?: return -1
            stream = currentStream
            streamPosition = position
        } else if (position > streamPosition) {
            var toSkip = position - streamPosition
            while (toSkip > 0) {
                val skipped = currentStream.skip(toSkip)
                if (skipped <= 0) {
                    if (currentStream.read() == -1) {
                        break
                    }
                    toSkip--
                } else {
                    toSkip -= skipped
                }
            }
            streamPosition = position
        }

        return try {
            currentStream.read(buffer, offset, size)
        } catch (e: IOException) {
            -1
        }
    }

    override fun getSize(): Long = size

    override fun close() {
        try {
            stream?.close()
        } catch (ignored: Exception) {
        }
        stream = null
        streamPosition = 0
    }
}
