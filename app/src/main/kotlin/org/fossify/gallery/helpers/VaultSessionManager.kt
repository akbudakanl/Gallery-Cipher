package org.fossify.gallery.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.fossify.gallery.extensions.config

/**
 * Decides WHEN the unlocked vault (master key, decrypted metadata, image caches) is wiped from RAM.
 *
 * Previously the vault was wiped the moment the last activity stopped (home button, screen off, app switch).
 * Now only the "last active time" is remembered: the moment the app left the foreground. The vault is wiped
 * once the app has stayed in the background for [VAULT_AUTO_LOCK_TIMEOUT_MS] (30 minutes), or never when that
 * constant is <= 0. While the app is in the foreground it is never wiped by this class.
 *
 * Two independent mechanisms make the timeout reliable:
 *  1. a delayed task wipes RAM while the app sits in the background (frees memory / keys on time);
 *  2. a deadline check when the app comes back to the foreground is the authoritative one, because a delayed
 *     Handler task does not advance while the device is in deep sleep, but SystemClock.elapsedRealtime() does.
 */
object VaultSessionManager {
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastActiveAt = SystemClock.elapsedRealtime()

    @Volatile
    private var isInBackground = false

    /**
     * Set when the person dismissed the "unlock to continue" prompt, so that the next screen does not ask again
     * right away. Consumed once via [consumeUnlockDeclined].
     */
    @Volatile
    private var unlockDeclinedAt = 0L

    private const val DECLINE_WINDOW_MS = 3000L

    /**
     * True after the timeout wiped an unlocked vault, until the person unlocked it again or declined. Only an
     * automatic wipe leads to the "enter the password to continue where you left off" prompt; locking on purpose
     * (settings, leaving the vault) must not.
     */
    @Volatile
    private var autoWiped = false

    private fun timeoutMs() = VAULT_AUTO_LOCK_TIMEOUT_MS

    private fun isExpired(): Boolean {
        val timeout = timeoutMs()
        return timeout > 0 && SystemClock.elapsedRealtime() - lastActiveAt >= timeout
    }

    /** Called when the first activity of the app becomes visible. */
    fun onAppForegrounded(context: Context) {
        handler.removeCallbacksAndMessages(null)
        if (isInBackground && isExpired()) {
            lockNow(context)
        }
        isInBackground = false
        lastActiveAt = SystemClock.elapsedRealtime()
    }

    /** Called when the last visible activity of the app stopped (not for rotation / theme changes). */
    fun onAppBackgrounded(context: Context) {
        isInBackground = true
        lastActiveAt = SystemClock.elapsedRealtime()
        handler.removeCallbacksAndMessages(null)

        val timeout = timeoutMs()
        if (timeout > 0) {
            val appContext = context.applicationContext
            handler.postDelayed({ lockNow(appContext) }, timeout)
        }
    }

    /** Wipes the keys and the decrypted caches from RAM right now. */
    fun lockNow(context: Context) {
        try {
            if (CryptoManager.isVaultUnlocked) {
                autoWiped = true
            }
            CryptoManager.lockVault()
            CryptoManager.clearAppImageCaches(context)
            context.applicationContext.config.temporarilyShowHidden = false
        } catch (_: Throwable) {
        }
    }

    fun isAutoWiped() = autoWiped

    fun clearAutoWipe() {
        autoWiped = false
    }

    fun markUnlockDeclined() {
        unlockDeclinedAt = SystemClock.elapsedRealtime()
    }

    /** True only if the prompt was dismissed a moment ago (the screen above finished right after it). */
    fun consumeUnlockDeclined(): Boolean {
        val declinedAt = unlockDeclinedAt
        unlockDeclinedAt = 0L
        return declinedAt != 0L && SystemClock.elapsedRealtime() - declinedAt < DECLINE_WINDOW_MS
    }
}
