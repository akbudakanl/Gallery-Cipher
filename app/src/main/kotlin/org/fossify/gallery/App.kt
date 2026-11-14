package org.fossify.gallery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.github.ajalt.reprint.core.Reprint
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.gallery.activities.SplashActivity
import org.fossify.gallery.activities.VaultLockActivity
import org.fossify.gallery.helpers.VaultSessionManager
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.Collections
import java.util.WeakHashMap

class App : FossifyApp() {

    override val isAppLockFeatureAvailable = true
    private var startedActivities = 0
    private val trackedActivities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    companion object {
        private const val PRIVACY_CURTAIN_TAG = "vault_privacy_curtain"
    }

    override fun onCreate() {
        super.onCreate()
        installCodeLevelNetworkBlocker()
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    VaultSessionManager.onScreenOff(this@App)
                    syncPrivacyCurtainForTrackedActivities()
                }
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF))

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                trackedActivities.add(activity)
            }
            override fun onActivityStarted(activity: Activity) {
                trackedActivities.add(activity)
                startedActivities++
                if (startedActivities == 1) {
                    // Back in the foreground: wipe only if the 30 minute background deadline has passed.
                    VaultSessionManager.onAppForegrounded(this@App)
                }
                syncPrivacyCurtain(activity)
                maybeLaunchVaultLockScreen(activity)
            }
            override fun onActivityResumed(activity: Activity) {
                syncPrivacyCurtain(activity)
                maybeLaunchVaultLockScreen(activity)
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    if (!activity.isChangingConfigurations) {
                        // No immediate wipe on onStop / screen off / UI hidden any more. Only remember the last
                        // active time; the vault is wiped after VAULT_AUTO_LOCK_TIMEOUT_MS in the background.
                        VaultSessionManager.onAppBackgrounded(this@App)
                        syncPrivacyCurtainForTrackedActivities()
                    }
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                trackedActivities.remove(activity)
            }
        })

        try {
            org.fossify.gallery.helpers.CryptoManager.processPendingWipeJournal(this)
        } catch (_: Throwable) {}
    }

    private fun syncPrivacyCurtainForTrackedActivities() {
        for (activity in trackedActivities.toList()) {
            syncPrivacyCurtain(activity)
        }
    }

    /**
     * Synchronously hides the underlying activity content and overlays an opaque curtain whenever
     * vault re-authentication is pending so no frame of vault media leaks before VaultLockActivity draws.
     */
    private fun syncPrivacyCurtain(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (activity is VaultLockActivity || activity is SplashActivity) return

        val decorView = activity.window?.decorView as? ViewGroup ?: return
        val contentView = activity.findViewById<View>(android.R.id.content)
        val existingCurtain = decorView.findViewWithTag<View>(PRIVACY_CURTAIN_TAG)

        if (VaultSessionManager.isReauthPending()) {
            if (contentView != null && contentView.visibility != View.INVISIBLE) {
                contentView.visibility = View.INVISIBLE
            }
            if (existingCurtain == null) {
                val curtain = View(activity).apply {
                    tag = PRIVACY_CURTAIN_TAG
                    setBackgroundColor(activity.getProperBackgroundColor())
                    isClickable = true
                    isFocusable = true
                }
                decorView.addView(
                    curtain,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        } else {
            if (existingCurtain != null) {
                decorView.removeView(existingCurtain)
            }
            if (contentView != null && contentView.visibility != View.VISIBLE) {
                contentView.visibility = View.VISIBLE
            }
        }
    }

    private fun maybeLaunchVaultLockScreen(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (VaultSessionManager.shouldShowLockScreen(
                isLockScreenActivity = activity is VaultLockActivity,
                isSplashActivity = activity is SplashActivity
            )
        ) {
            VaultSessionManager.markLockScreenLaunching()
            val intent = Intent(activity, VaultLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            activity.startActivity(intent)
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }
    }

    /**
     * Defense-in-depth code-level network blocker complementing AndroidManifest INTERNET removal
     * and NetworkSecurityConfig lockdown. Blocks standard JVM/Android HTTP(S) and socket proxy paths.
     */
    private fun installCodeLevelNetworkBlocker() {
        val blockedSchemes = setOf("http", "https", "ftp", "ws", "wss", "gopher")

        try {
            URL.setURLStreamHandlerFactory { protocol ->
                if (protocol != null && protocol.lowercase() in blockedSchemes) {
                    object : URLStreamHandler() {
                        override fun openConnection(u: URL?): URLConnection {
                            throw SecurityException("Network access is permanently disabled in Fossify Gallery Cipher")
                        }

                        override fun openConnection(u: URL?, p: Proxy?): URLConnection {
                            throw SecurityException("Network access is permanently disabled in Fossify Gallery Cipher")
                        }
                    }
                } else {
                    null
                }
            }
        } catch (_: Throwable) {}

        try {
            ProxySelector.setDefault(object : ProxySelector() {
                override fun select(uri: URI?): MutableList<Proxy> {
                    val scheme = uri?.scheme?.lowercase()
                    if (scheme in blockedSchemes || scheme == "socket") {
                        throw SecurityException("Network access is permanently disabled in Fossify Gallery Cipher")
                    }
                    return mutableListOf(Proxy.NO_PROXY)
                }

                override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
            })
        } catch (_: Throwable) {}
    }
}
