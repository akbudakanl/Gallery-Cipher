package org.fossify.gallery

import com.github.ajalt.reprint.core.Reprint
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response
import org.fossify.commons.FossifyApp
import org.fossify.gallery.extensions.config

class App : FossifyApp() {

    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())

        try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
            registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                        try {
                            this@App.config.temporarilyShowHidden = false
                            org.fossify.gallery.helpers.CryptoManager.lockVault()
                            org.fossify.gallery.helpers.CryptoManager.clearAppImageCaches(this@App)
                        } catch (_: Throwable) {}
                    }
                }
            }, filter)
        } catch (_: Throwable) {}

        try {
            org.fossify.gallery.helpers.CryptoManager.processPendingWipeJournal(this)
        } catch (_: Throwable) {}
    }
}
