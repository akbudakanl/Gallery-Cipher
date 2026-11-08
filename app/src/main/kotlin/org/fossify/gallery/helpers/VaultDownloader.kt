package org.fossify.gallery.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object VaultDownloader {

    private val URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE)

    /**
     * Extracts either a web URL or a content/file stream URI from a share Intent.
     */
    fun extractPayload(intent: Intent): Pair<String?, Uri?> {
        // 1. Check EXTRA_STREAM (e.g. browser or app sharing image/video via ContentProvider)
        val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: intent.clipData?.let { clip ->
            if (clip.itemCount > 0) clip.getItemAt(0).uri else null
        } ?: intent.data

        if (streamUri != null && (streamUri.scheme == "content" || streamUri.scheme == "file")) {
            return Pair(null, streamUri)
        }

        // 2. Check EXTRA_TEXT for a direct web link or text containing a URL
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_STREAM)?.toString()
            ?: intent.dataString

        if (!text.isNullOrBlank()) {
            val trimmed = text.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                return Pair(trimmed, null)
            }
            val matcher = URL_PATTERN.matcher(text)
            if (matcher.find()) {
                return Pair(matcher.group(), null)
            }
        }

        return Pair(null, streamUri)
    }

    /**
     * Streams network socket or content resolver directly into the CryptoManager
     * without writing any unencrypted data to disk.
     */
    fun downloadAndEncrypt(
        context: Context,
        url: String?,
        uri: Uri?,
        targetDir: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): File? {
        return when {
            uri != null -> downloadFromUri(context, uri, targetDir, onProgress)
            !url.isNullOrBlank() -> downloadFromUrl(context, url, targetDir, onProgress)
            else -> null
        }
    }

    private fun downloadFromUri(
        context: Context,
        uri: Uri,
        targetDir: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): File? {
        var filename = "shared_media_${System.currentTimeMillis()}"
        var size = -1L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { filename = it }
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (!filename.contains(".")) {
            val mime = context.contentResolver.getType(uri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            filename = "$filename.$ext"
        }

        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.use { input ->
                CryptoManager.encryptStreamToDirectory(
                    context = context,
                    inputStream = input,
                    targetDir = targetDir,
                    originalName = filename,
                    contentLength = size,
                    onProgress = onProgress
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadFromUrl(
        context: Context,
        initialUrl: String,
        targetDir: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): File? {
        var currentUrl = initialUrl
        var redirects = 0
        val maxRedirects = 5

        while (redirects < maxRedirects) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                useCaches = false
                defaultUseCaches = false
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept", "*/*")
            }

            try {
                connection.connect()
                val responseCode = connection.responseCode

                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrBlank()) return null
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(URL(currentUrl), location).toString()
                    }
                    redirects++
                    continue
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return null
                }

                val contentLength = connection.contentLengthLong
                val contentType = connection.contentType ?: ""
                var filename = extractFilename(currentUrl, connection.getHeaderField("Content-Disposition"), contentType)

                val input: InputStream = connection.inputStream
                return try {
                    input.use { stream ->
                        CryptoManager.encryptStreamToDirectory(
                            context = context,
                            inputStream = stream,
                            targetDir = targetDir,
                            originalName = filename,
                            contentLength = contentLength,
                            onProgress = onProgress
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                connection.disconnect()
                return null
            }
        }
        return null
    }

    private fun extractFilename(url: String, contentDisposition: String?, contentType: String): String {
        // 1. Try Content-Disposition
        if (!contentDisposition.isNullOrBlank()) {
            val fnMatcher = Pattern.compile("filename\\*?=(?:UTF-8'')?[\"']?([^\"';\\r\\n]+)[\"']?", Pattern.CASE_INSENSITIVE)
                .matcher(contentDisposition)
            if (fnMatcher.find()) {
                val candidate = fnMatcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    return candidate.substringAfterLast('/')
                }
            }
        }

        // 2. Try URL path
        try {
            val path = URL(url).path
            val name = path.substringAfterLast('/').substringBefore('?')
            if (name.contains(".") && name.length >= 3) {
                return name
            }
        } catch (_: Exception) {}

        // 3. Fallback to MIME type
        val ext = when {
            contentType.contains("jpeg", true) || contentType.contains("jpg", true) -> "jpg"
            contentType.contains("png", true) -> "png"
            contentType.contains("webp", true) -> "webp"
            contentType.contains("gif", true) -> "gif"
            contentType.contains("mp4", true) -> "mp4"
            contentType.contains("webm", true) -> "webm"
            contentType.contains("quicktime", true) -> "mov"
            contentType.contains("matroska", true) -> "mkv"
            contentType.contains("avif", true) -> "avif"
            contentType.contains("heic", true) || contentType.contains("heif", true) -> "heic"
            else -> "bin"
        }

        return "download_${System.currentTimeMillis()}.$ext"
    }
}
