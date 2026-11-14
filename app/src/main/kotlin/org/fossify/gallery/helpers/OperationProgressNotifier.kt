package org.fossify.gallery.helpers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.PERMISSION_POST_NOTIFICATIONS
import org.fossify.commons.helpers.isOreoPlus
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.gallery.extensions.config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Displays an ongoing notification with a horizontal progress bar for long-running file
 * operations (copy, move, encrypt, decrypt), matching Fossify Commons' CopyMoveTask behavior.
 */
class OperationProgressNotifier(
    private val context: Context,
    private val channelId: String,
    private val channelName: String,
    private val title: String,
    private val smallIconRes: Int,
    private val initialDelayMs: Long = INITIAL_PROGRESS_DELAY_MS,
    private val recheckIntervalMs: Long = PROGRESS_RECHECK_INTERVAL_MS
) {
    private val handler = Handler(Looper.getMainLooper())
    private val notifId = nextNotificationId()
    private var notificationBuilder: NotificationCompat.Builder? = null

    @Volatile
    private var isTaskOver = false

    @Volatile
    private var currentProgress = 0

    @Volatile
    private var maxProgress = 1

    @Volatile
    private var isIndeterminate = true

    @Volatile
    private var contentText = ""

    private val initialRunnable = Runnable {
        if (!isTaskOver) {
            initProgressNotification()
            updateNotification()
        }
    }

    private val recheckRunnable = Runnable {
        updateNotification()
    }

    fun start() {
        if (!canPostNotifications()) {
            return
        }
        handler.postDelayed(initialRunnable, initialDelayMs)
    }

    fun update(current: Int, max: Int, contentText: String? = null) {
        maxProgress = maxOf(max, 1)
        isIndeterminate = max <= 0
        currentProgress = current.coerceIn(0, maxProgress)
        if (contentText != null) {
            this.contentText = contentText
        }
    }

    fun finish() {
        isTaskOver = true
        handler.removeCallbacksAndMessages(null)
        try {
            context.notificationManager.cancel(notifId)
        } catch (_: Exception) {
        }
    }

    private fun canPostNotifications(): Boolean {
        return !isTiramisuPlus() || context.hasPermission(PERMISSION_POST_NOTIFICATIONS)
    }

    private fun initProgressNotification() {
        if (isOreoPlus()) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW).apply {
                enableLights(false)
                enableVibration(false)
            }
            try {
                context.notificationManager.createNotificationChannel(channel)
            } catch (_: Exception) {
            }
        }

        notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(smallIconRes)
            .setChannelId(channelId)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        if (isTaskOver || !canPostNotifications()) {
            try {
                context.notificationManager.cancel(notifId)
            } catch (_: Exception) {
            }
            return
        }

        val builder = notificationBuilder ?: return
        val max = maxProgress
        val current = currentProgress.coerceIn(0, max)
        val indeterminate = isIndeterminate
        val text = OperationProgressFormatter.resolveNotificationContentText(
            current = current,
            max = max,
            indeterminate = indeterminate,
            explicitText = contentText
        )

        builder.setContentText(text)
            .setProgress(max, current, indeterminate)

        try {
            context.notificationManager.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            return
        } catch (_: Exception) {
        }

        handler.removeCallbacks(recheckRunnable)
        handler.postDelayed(recheckRunnable, recheckIntervalMs)
    }

    companion object {
        const val CHANNEL_COPY_MOVE = "Copy/Move"
        const val CHANNEL_CRYPTO = "Encrypt/Decrypt"
        const val INITIAL_PROGRESS_DELAY_MS = 3000L
        const val PROGRESS_RECHECK_INTERVAL_MS = 500L

        private val notificationIdGenerator = AtomicInteger(
            ((System.currentTimeMillis() % Int.MAX_VALUE).toInt()).coerceAtLeast(1)
        )

        private fun nextNotificationId(): Int {
            val next = notificationIdGenerator.incrementAndGet()
            return if (next == 0) notificationIdGenerator.incrementAndGet() else next
        }
    }
}

/**
 * Pure formatting helpers shared between the progress dialog and notification updater.
 */
object OperationProgressFormatter {
    private const val PERCENT_MAX = 100

    fun computePercent(current: Int, max: Int): Int {
        if (max <= 0) {
            return 0
        }
        val clamped = current.coerceIn(0, max)
        return ((clamped.toFloat() / max) * PERCENT_MAX).toInt().coerceIn(0, PERCENT_MAX)
    }

    fun formatCountSummary(
        current: Int,
        total: Int,
        barCurrent: Int = current,
        barMax: Int = total
    ): String {
        val percent = when {
            barMax > 0 -> computePercent(barCurrent, barMax)
            total > 0 -> computePercent(current, total)
            else -> 0
        }
        return "$current / $total ($percent%)"
    }

    fun resolveNotificationContentText(
        current: Int,
        max: Int,
        indeterminate: Boolean,
        explicitText: String
    ): String {
        if (explicitText.isNotEmpty()) {
            return explicitText
        }
        if (indeterminate) {
            return ""
        }
        return formatCountSummary(current = current, total = max, barCurrent = current, barMax = max)
    }
}

fun BaseSimpleActivity.ensureNotificationPermissionThenRun(onReady: (notificationsAllowed: Boolean) -> Unit) {
    if (!isTiramisuPlus() || hasPermission(PERMISSION_POST_NOTIFICATIONS)) {
        onReady(true)
        return
    }
    if (config.wasNotificationPermissionAsked) {
        onReady(false)
        return
    }
    config.wasNotificationPermissionAsked = true
    handleNotificationPermission { granted ->
        onReady(granted)
    }
}
