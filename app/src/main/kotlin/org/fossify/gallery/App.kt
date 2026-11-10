package org.fossify.gallery

import android.app.Activity
import android.os.Bundle
import com.github.ajalt.reprint.core.Reprint
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response
import org.fossify.commons.FossifyApp
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.CryptoManager
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class App : FossifyApp() {

    override val isAppLockFeatureAvailable = true
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        installCodeLevelNetworkBlocker()
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    if (!activity.isChangingConfigurations) {
                        try {
                            CryptoManager.lockVault()
                            CryptoManager.clearAppImageCaches(this@App)
                            config.temporarilyShowHidden = false
                        } catch (_: Throwable) {}
                    }
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        try {
            org.fossify.gallery.helpers.CryptoManager.processPendingWipeJournal(this)
        } catch (_: Throwable) {}
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
