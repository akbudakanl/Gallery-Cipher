package org.fossify.gallery.helpers

import android.content.Context
import android.content.Intent
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object VaultDownloader {

    private val URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE)

    /**
     * Extracts ONLY a web URL (HTTP/HTTPS) from a share Intent.
     * File-based streams (content://, file://, EXTRA_STREAM) are strictly rejected
     * to prevent plaintext disk remanence from third-party FileProviders.
     */
    fun extractUrl(intent: Intent): String? {
        // Explicitly reject any file/content streams for strict anti-forensic zero-disk security
        val hasStreamExtra = intent.hasExtra(Intent.EXTRA_STREAM) ||
            (intent.clipData?.let { it.itemCount > 0 && it.getItemAt(0).uri != null } ?: false) ||
            (intent.data != null && (intent.data?.scheme == "content" || intent.data?.scheme == "file"))

        if (hasStreamExtra) {
            return null
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.dataString

        if (!text.isNullOrBlank()) {
            val trimmed = text.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                return trimmed
            }
            val matcher = URL_PATTERN.matcher(text)
            if (matcher.find()) {
                return matcher.group()
            }
        }

        return null
    }

    /**
     * Streams network socket directly into the CryptoManager in 64 KB RAM buffers
     * without writing any unencrypted data to disk.
     */
    fun downloadAndEncrypt(
        context: Context,
        url: String,
        targetDir: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): File? {
        if (url.isBlank()) return null
        return downloadFromUrl(context, url, targetDir, onProgress)
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
