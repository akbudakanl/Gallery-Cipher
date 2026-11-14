package org.fossify.gallery.helpers

import android.content.Context
import android.provider.MediaStore
import org.fossify.gallery.models.Medium
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * A stable description of a media file that survives renames and moves.
 *
 * The absolute path is deliberately NOT part of the identity: it is only a cache of where the
 * file currently lives. Everything needed to find the file again lives here instead.
 *
 * @param mediaStoreId the MediaStore `_ID`, which is preserved by renames and moves. 0 when unknown.
 * @param size file size in bytes at the time the identity was captured.
 * @param lastModified last modification timestamp in milliseconds.
 * @param fingerprint content fingerprint, see [computeFingerprint]. Empty when it could not be computed.
 */
data class MediaIdentity(
    val mediaStoreId: Long = 0L,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val fingerprint: String = "",
    val folderInode: Long = 0L,
    val folderSignature: String = "",
    val capturedAtSec: Long = 0L
) {
    val hasFingerprint: Boolean
        get() = fingerprint.isNotEmpty()

    val hasFolderEvidence: Boolean
        get() = folderInode > 0L || folderSignature.isNotEmpty()

    val isUsable: Boolean
        get() = mediaStoreId > 0L || hasFingerprint || size > 0L

    fun withFolder(folder: FolderIdentity): MediaIdentity {
        return copy(
            folderInode = folder.dirInode,
            folderSignature = folder.serializedSignature
        )
    }

    companion object {
        /**
         * Captures the identity of a file on disk. The fingerprint is computed from the first and
         * last 64 KB, so the cost does not depend on the file size.
         */
        fun of(file: File, mediaStoreId: Long = 0L): MediaIdentity {
            if (!file.exists() || !file.isFile) {
                return MediaIdentity(mediaStoreId = mediaStoreId)
            }

            return MediaIdentity(
                mediaStoreId = mediaStoreId,
                size = file.length(),
                lastModified = file.lastModified(),
                fingerprint = computeFingerprint(file),
                capturedAtSec = System.currentTimeMillis() / 1000L
            )
        }

        /**
         * Captures both file and parent-folder identity without writing any marker files to disk.
         */
        fun ofWithFolder(
            context: Context,
            file: File,
            mediaStoreId: Long = 0L,
            captureSession: FolderIdentity.CaptureSession? = null
        ): MediaIdentity {
            val base = of(context, file, mediaStoreId)
            if (!file.isFile) return base
            val folder = captureSession?.captureForFile(file) ?: FolderIdentity.captureForFile(file)
            return base.withFolder(folder)
        }

        /**
         * Captures the identity of a file on disk, looking up its MediaStore id when needed.
         */
        fun of(context: Context, file: File, mediaStoreId: Long = 0L): MediaIdentity {
            val resolvedId = if (mediaStoreId > 0L || !file.isFile || CryptoManager.isEncryptedFile(file.name)) {
                mediaStoreId
            } else {
                findMediaStoreId(context, file.absolutePath)
            }
            return of(file, resolvedId)
        }

        /**
         * Captures the identity of an already known medium. The MediaStore id is reused when
         * present, or looked up once from MediaStore when missing.
         */
        fun of(context: Context, medium: Medium): MediaIdentity {
            val file = File(medium.path)
            val identity = of(context, file, medium.mediaStoreId)
            return if (identity.hasFingerprint) {
                identity
            } else {
                identity.copy(
                    size = medium.size,
                    lastModified = medium.modified
                )
            }
        }

        /**
         * Looks up the MediaStore `_ID` for an existing file path. Returns 0 when not indexed.
         */
        fun findMediaStoreId(context: Context, path: String): Long {
            if (path.isEmpty() || CryptoManager.isEncryptedFile(path)) {
                return 0L
            }
            return try {
                val uri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                context.contentResolver.query(uri, projection, selection, arrayOf(path), null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                } ?: 0L
            } catch (_: Exception) {
                0L
            }
        }

        /**
         * Content fingerprint: SHA-256 over the first and the last 64 KB of the file.
         *
         * It is stable under renames, moves and copies, and it changes whenever the content
         * changes. Two different files sharing the same fingerprint are therefore virtually
         * impossible, which is what makes it usable as an identity.
         */
        fun computeFingerprint(file: File): String {
            if (!file.exists() || !file.isFile) {
                return ""
            }

            val length = file.length()
            if (length == 0L) {
                return ""
            }

            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                RandomAccessFile(file, "r").use { raf ->
                    val chunk = ByteArray(FINGERPRINT_CHUNK_SIZE)
                    val head = minOf(length, FINGERPRINT_CHUNK_SIZE.toLong()).toInt()
                    raf.readFully(chunk, 0, head)
                    digest.update(chunk, 0, head)

                    if (length > FINGERPRINT_CHUNK_SIZE) {
                        val tail = minOf(length - FINGERPRINT_CHUNK_SIZE, FINGERPRINT_CHUNK_SIZE.toLong()).toInt()
                        raf.seek(length - tail)
                        raf.readFully(chunk, 0, tail)
                        digest.update(chunk, 0, tail)
                    }
                }
                digest.digest().toHexString()
            } catch (_: Exception) {
                ""
            }
        }

        private const val FINGERPRINT_CHUNK_SIZE = 64 * 1024
    }
}

fun ByteArray.toHexString(): String {
    val builder = StringBuilder(size * 2)
    for (byte in this) {
        val value = byte.toInt() and 0xFF
        builder.append(HEX_CHARS[value ushr 4])
        builder.append(HEX_CHARS[value and 0x0F])
    }
    return builder.toString()
}

private const val HEX_CHARS = "0123456789abcdef"
