package org.fossify.gallery.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.gallery.R
import kotlin.math.abs

/**
 * Handles the pull gesture at the top of a list that can open the encrypted vault.
 *
 * - Pulling past [HOLD_OPEN_THRESHOLD_DP] and keeping the pull sustained for
 *   [VAULT_HOLD_OPEN_MS] plays an unlocking animation; releasing the gesture
 *   afterwards opens the vault.
 * - Releasing a shorter pull behaves like the classic pull-to-refresh and simply
 *   refreshes the list contents.
 */
class VaultPullGestureHandler(
    private val containerView: View,
    private val indicatorView: View,
    private val iconView: ImageView,
    private val textView: TextView,
    /** Returns true while the underlying list cannot scroll up, i.e. it is at the top. */
    private val isListAtTop: () -> Boolean,
    private val primaryColor: () -> Int,
    private val textColor: () -> Int,
    /** Lets the owning activity deliver the generated ACTION_CANCEL to its children. */
    private val dispatchToChildren: (MotionEvent) -> Unit,
    private val onVaultOpen: () -> Unit,
    private val onListRefresh: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(containerView.context).scaledTouchSlop
    private val density = containerView.context.resources.displayMetrics.density
    private val maxTranslation = MAX_TRANSLATION_DP * density
    private val holdThreshold = HOLD_OPEN_THRESHOLD_DP * density

    private var startRawX = 0f
    private var startRawY = 0f
    private var gestureStarted = false
    private var cancelDispatched = false
    private var thresholdMet = false
    private var thresholdMetAt = 0L
    private var holdLongEnough = false
    private var holdAnimator: ValueAnimator? = null

    /**
     * Feeds a touch event into the gesture. Returns true when the event has been
     * consumed and must not reach the children of the owning activity.
     */
    fun handleTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStarted = isListAtTop()
                if (gestureStarted) {
                    startRawX = ev.rawX
                    startRawY = ev.rawY
                }
                resetThresholdState()
                cancelDispatched = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (gestureStarted) {
                    val deltaX = abs(ev.rawX - startRawX)
                    val deltaY = ev.rawY - startRawY
                    if (deltaY > 0 && deltaY > deltaX) {
                        val translation = (deltaY * TRANSLATION_FACTOR).coerceIn(0f, maxTranslation)
                        if (translation > touchSlop) {
                            if (!cancelDispatched) {
                                cancelDispatched = true
                                val cancelEvent = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                                dispatchToChildren(cancelEvent)
                                cancelEvent.recycle()
                            }

                            containerView.translationY = translation

                            indicatorView.beVisible()
                            indicatorView.bringToFront()
                            iconView.applyColorFilter(primaryColor())
                            textView.setTextColor(textColor())

                            val progress = (translation / holdThreshold).coerceIn(0f, 1f)
                            indicatorView.alpha = (progress * 1.4f).coerceIn(0.1f, 1f)
                            if (!thresholdMet) {
                                iconView.rotation = progress * 180f
                            }

                            if (translation >= holdThreshold) {
                                if (!thresholdMet) {
                                    thresholdMet = true
                                    thresholdMetAt = System.currentTimeMillis()
                                    startHoldAnimation()
                                }
                                textView.text = textView.context.getString(
                                    if (holdLongEnough) {
                                        R.string.release_to_open_vault
                                    } else {
                                        R.string.keep_holding_to_open_vault
                                    }
                                )
                            } else {
                                resetThresholdState()
                                textView.text = textView.context.getString(R.string.pull_down_to_open_vault)
                            }
                            return true
                        }
                    } else if (deltaY < 0) {
                        if (containerView.translationY > 0f) {
                            containerView.translationY = 0f
                            indicatorView.beGone()
                        }
                        if (isListAtTop()) {
                            gestureStarted = false
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (gestureStarted) {
                    val wasThresholdMet = thresholdMet
                    val holdDuration = if (thresholdMet) System.currentTimeMillis() - thresholdMetAt else 0L
                    val shouldOpenVault = wasThresholdMet && holdDuration >= VAULT_HOLD_OPEN_MS

                    resetGesture()
                    gestureStarted = false

                    if (shouldOpenVault) {
                        onVaultOpen()
                        return true
                    }

                    if (wasThresholdMet) {
                        onListRefresh()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (gestureStarted) {
                    resetGesture()
                    gestureStarted = false
                }
            }
        }
        return false
    }

    private fun startHoldAnimation() {
        cancelHoldAnimator()
        holdLongEnough = false
        holdAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = VAULT_HOLD_OPEN_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                // the lock icon keeps rotating one full turn while the pull is held
                iconView.rotation = 180f + (animator.animatedValue as Float) * 360f
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled && thresholdMet) {
                        armVaultOpen()
                    }
                }
            })
            start()
        }
    }

    private fun armVaultOpen() {
        holdLongEnough = true
        textView.text = textView.context.getString(R.string.release_to_open_vault)

        // short bounce on the lock icon to signal that the vault can be opened
        iconView.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(ARM_PULSE_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                iconView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ARM_PULSE_MS)
                    .start()
            }
            .start()
        indicatorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun resetThresholdState() {
        thresholdMet = false
        thresholdMetAt = 0L
        holdLongEnough = false
        cancelHoldAnimator()
        iconView.scaleX = 1f
        iconView.scaleY = 1f
    }

    private fun resetGesture() {
        containerView.animate()
            .translationY(0f)
            .setDuration(RESET_ANIM_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        indicatorView.animate()
            .alpha(0f)
            .setDuration(RESET_ANIM_MS)
            .withEndAction {
                indicatorView.beGone()
                indicatorView.alpha = 1f
            }
            .start()

        resetThresholdState()
    }

    private fun cancelHoldAnimator() {
        holdAnimator?.cancel()
        holdAnimator = null
    }

    companion object {
        private const val MAX_TRANSLATION_DP = 160f
        private const val HOLD_OPEN_THRESHOLD_DP = 95f
        private const val TRANSLATION_FACTOR = 0.45f

        /** How long the pull must be sustained before the vault can be opened. */
        private const val VAULT_HOLD_OPEN_MS = 1250L

        private const val RESET_ANIM_MS = 220L
        private const val ARM_PULSE_MS = 120L
    }
}
