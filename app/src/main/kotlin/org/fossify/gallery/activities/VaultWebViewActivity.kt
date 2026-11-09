package org.fossify.gallery.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.gallery.R
import org.fossify.gallery.databinding.ActivityVaultWebViewBinding
import org.fossify.gallery.dialogs.CryptoProgressDialog
import org.fossify.gallery.dialogs.PickVaultAlbumDialog
import org.fossify.gallery.dialogs.VaultSecurityDialog
import org.fossify.gallery.extensions.applyVaultScreenProtection
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.CryptoManager
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class VaultWebViewActivity : SimpleActivity() {

    private lateinit var binding: ActivityVaultWebViewBinding
    private var isDesktopSite = false
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Strict Screen Protection
        applyVaultScreenProtection(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }

        if (!CryptoManager.hasExistingVault(this)) {
            toast(R.string.vault_not_setup)
            finish()
            return
        }

        setupWebView()
        setupTopBar()
        handleInitialIntent()
    }

    override fun onResume() {
        super.onResume()
        applyVaultScreenProtection(true)
        binding.vaultWebView.onResume()
    }

    override fun onPause() {
        binding.vaultWebView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        clearSessionData()
        try {
            binding.vaultWebView.stopLoading()
            binding.vaultWebView.destroy()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun clearSessionData() {
        try {
            mFilePathCallback?.onReceiveValue(null)
            mFilePathCallback = null
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            binding.vaultWebView.clearCache(true)
            binding.vaultWebView.clearHistory()
            binding.vaultWebView.clearFormData()
        } catch (_: Exception) {}
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.vaultWebView
        val settings = webView.settings

        settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = false
            setGeolocationEnabled(false)
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            saveFormData = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false

            // Remove typical WebView identification token to avoid restricted rendering
            userAgentString = userAgentString.replace("; wv", "")
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.webProgressBar.beVisibleIf(newProgress in 1..99)
                binding.webProgressBar.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!binding.etAddress.hasFocus() && view?.url != null) {
                    binding.etAddress.setText(view.url)
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // Deny all hardware sensor, camera, mic and location requests automatically
                request?.deny()
            }

            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return handleFileChooser(filePathCallback, fileChooserParams)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = Uri.parse(url).scheme?.lowercase() ?: ""
                if (scheme != "http" && scheme != "https") {
                    // Block external intent leakages (e.g. intent://, market://)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                url?.let {
                    if (!binding.etAddress.hasFocus()) {
                        binding.etAddress.setText(it)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let {
                    if (!binding.etAddress.hasFocus()) {
                        binding.etAddress.setText(it)
                    }
                }
            }
        }

        // Web download listener (e.g. user clicks "Download" button on Google Photos or any web page)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            promptSaveMediaToVault(url, userAgent, contentDisposition, mimetype, contentLength)
        }

        // Long press on image or video on the webpage
        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            val mediaUrl = result.extra
            if (!mediaUrl.isNullOrBlank() && (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                promptSaveMediaToVault(mediaUrl, null, null, null, -1L)
                true
            } else {
                false
            }
        }
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener {
            if (binding.vaultWebView.canGoBack()) {
                binding.vaultWebView.goBack()
            } else {
                finish()
            }
        }

        binding.btnRefresh.setOnClickListener {
            binding.vaultWebView.reload()
        }

        binding.btnMenu.setOnClickListener {
            showOverflowMenu(it)
        }

        binding.etAddress.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                navigateToEnteredUrl()
                true
            } else {
                false
            }
        }

        binding.fabSaveToVault.setOnClickListener {
            val currentUrl = binding.vaultWebView.url
            if (!currentUrl.isNullOrBlank()) {
                promptSaveMediaToVault(currentUrl, null, null, null, -1L)
            } else {
                toast(R.string.no_media_url_found)
            }
        }
    }

    private fun navigateToEnteredUrl() {
        hideKeyboard()
        binding.etAddress.clearFocus()
        val input = binding.etAddress.text.toString().trim()
        if (input.isEmpty()) return

        val urlToLoad = when {
            input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true) -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://duckduckgo.com/?q=" + Uri.encode(input)
        }

        binding.vaultWebView.loadUrl(urlToLoad)
    }

    private fun handleInitialIntent() {
        val intentData = intent.dataString
        val extraText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)

        val targetUrl = when {
            !intentData.isNullOrBlank() -> intentData
            !extraText.isNullOrBlank() -> {
                val matcher = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE).matcher(extraText)
                if (matcher.find()) matcher.group() else extraText.trim()
            }
            else -> "https://duckduckgo.com"
        }

        binding.etAddress.setText(targetUrl)
        binding.vaultWebView.loadUrl(targetUrl)
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.apply {
            add(0, 1, 0, getString(R.string.desktop_site)).apply {
                isCheckable = true
                isChecked = isDesktopSite
            }
            add(0, 2, 1, getString(R.string.clear_session_and_exit))
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> toggleDesktopSite()
                2 -> {
                    clearSessionData()
                    finish()
                }
            }
            true
        }
        popup.show()
    }

    private fun toggleDesktopSite() {
        isDesktopSite = !isDesktopSite
        val settings = binding.vaultWebView.settings
        if (isDesktopSite) {
            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            settings.userAgentString = settings.userAgentString.replace("X11; Linux x86_64", "Linux; Android 14; Mobile")
        }
        binding.vaultWebView.reload()
    }

    private fun promptSaveMediaToVault(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_media_to_vault)
            .setMessage(R.string.save_to_vault_dialog)
            .setPositiveButton(org.fossify.commons.R.string.yes) { _, _ ->
                ensureVaultUnlockedAndDownload(url, userAgent, contentDisposition, mimetype, contentLength)
            }
            .setNegativeButton(org.fossify.commons.R.string.no, null)
            .show()
    }

    private fun ensureVaultUnlockedAndDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (CryptoManager.isVaultUnlocked) {
            showAlbumPickerAndDownload(url, userAgent, contentDisposition, mimetype, contentLength)
        } else {
            val initialType = CryptoManager.getVaultProtectionType(this) ?: config.hiddenProtectionType
            VaultSecurityDialog(
                activity = this,
                requiredHash = config.hiddenPasswordHash,
                showTabIndex = initialType,
                minimalist = true
            ) { _, _, success ->
                if (success && CryptoManager.isVaultUnlocked) {
                    showAlbumPickerAndDownload(url, userAgent, contentDisposition, mimetype, contentLength)
                }
            }
        }
    }

    private fun showAlbumPickerAndDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        PickVaultAlbumDialog(
            activity = this,
            onCancel = {},
            onAlbumSelected = { targetFolder, albumName ->
                executeStreamingDownloadToVault(url, userAgent, contentDisposition, mimetype, contentLength, targetFolder, albumName)
            }
        )
    }

    private fun executeStreamingDownloadToVault(
        initialUrl: String,
        customUserAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        expectedLength: Long,
        targetFolder: File,
        albumName: String
    ) {
        val progressDialog = CryptoProgressDialog(this, 100, R.string.downloading_and_encrypting)
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FossifyGallery:VaultWebDownload")
        try {
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (_: Exception) {}

        // Extract session cookies for authenticated sites (like Google Photos)
        val cookies = try {
            CookieManager.getInstance().getCookie(initialUrl)
        } catch (_: Exception) {
            null
        }

        val ua = customUserAgent ?: binding.vaultWebView.settings.userAgentString

        ensureBackgroundThread {
            val encryptedFile = downloadAndEncryptStream(
                initialUrl = initialUrl,
                cookies = cookies,
                userAgent = ua,
                contentDisposition = contentDisposition,
                mimetype = mimetype,
                expectedLength = expectedLength,
                targetFolder = targetFolder,
                onProgress = { bytesRead, totalBytes ->
                    runOnUiThread {
                        if (totalBytes > 0) {
                            val percent = ((bytesRead.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
                            progressDialog.updateProgress(percent, 100)
                        } else {
                            progressDialog.updateProgress((bytesRead / 1024).toInt(), 0)
                        }
                    }
                }
            )

            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}

            runOnUiThread {
                progressDialog.dismiss()
                if (encryptedFile != null && encryptedFile.exists()) {
                    toast(String.format(getString(R.string.encrypted_download_success), albumName))
                } else {
                    toast(R.string.encrypted_download_failed)
                }
            }
        }
    }

    private fun downloadAndEncryptStream(
        initialUrl: String,
        cookies: String?,
        userAgent: String,
        contentDisposition: String?,
        mimetype: String?,
        expectedLength: Long,
        targetFolder: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): File? {
        var currentUrl = initialUrl
        var redirects = 0
        val maxRedirects = 5

        while (redirects < maxRedirects) {
            val connection = try {
                (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    useCaches = false
                    defaultUseCaches = false
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", userAgent)
                    setRequestProperty("Accept", "*/*")
                    if (!cookies.isNullOrBlank()) {
                        setRequestProperty("Cookie", cookies)
                    }
                }
            } catch (e: Exception) {
                return null
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

                val finalLength = if (connection.contentLengthLong > 0) connection.contentLengthLong else expectedLength
                val contentType = connection.contentType ?: mimetype ?: ""
                val disp = connection.getHeaderField("Content-Disposition") ?: contentDisposition
                val filename = extractFilename(currentUrl, disp, contentType)

                val input: InputStream = connection.inputStream
                return try {
                    input.use { stream ->
                        CryptoManager.encryptStreamToDirectory(
                            context = applicationContext,
                            inputStream = stream,
                            targetDir = targetFolder,
                            originalName = filename,
                            contentLength = finalLength,
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

        try {
            val path = URL(url).path
            val name = path.substringAfterLast('/').substringBefore('?')
            if (name.contains(".") && name.length >= 3) {
                return name
            }
        } catch (_: Exception) {}

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

        return "vault_download_${System.currentTimeMillis()}.$ext"
    }

    companion object {
        private const val REQUEST_CODE_FILE_CHOOSER = 10001
    }

    private fun handleFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        // Cancel any pending callback
        mFilePathCallback?.onReceiveValue(null)
        mFilePathCallback = filePathCallback

        return try {
            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CODE_FILE_CHOOSER)
            true
        } catch (_: Exception) {
            mFilePathCallback?.onReceiveValue(null)
            mFilePathCallback = null
            false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            val uris = WebChromeClient.FileChooserParams.parseResult(resultCode, resultData)
            mFilePathCallback?.onReceiveValue(uris)
            mFilePathCallback = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.vaultWebView.canGoBack()) {
            binding.vaultWebView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
