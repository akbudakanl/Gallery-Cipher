package org.fossify.gallery.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import kotlin.math.hypot
import kotlin.math.min

class VaultPatternView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val selectedNodes = ArrayList<Int>()
    private var currentTouchX = 0f
    private var currentTouchY = 0f
    private var isDrawing = false

    var onPatternDetectedListener: ((String) -> Unit)? = null

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getProperTextColor()
        alpha = 150
    }

    private val selectedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getProperPrimaryColor()
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = context.getProperPrimaryColor()
        alpha = 120
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = context.getProperPrimaryColor()
        alpha = 180
    }

    fun updateColors(textColor: Int, primaryColor: Int) {
        dotPaint.color = textColor
        dotPaint.alpha = 150
        selectedDotPaint.color = primaryColor
        ringPaint.color = primaryColor
        ringPaint.alpha = 120
        linePaint.color = primaryColor
        linePaint.alpha = 180
        invalidate()
    }

    fun clearPattern() {
        selectedNodes.clear()
        isDrawing = false
        invalidate()
    }

    private fun getNodeCenter(index: Int): Pair<Float, Float> {
        val row = index / 3
        val col = index % 3
        val size = min(width, height).toFloat()
        val offsetX = (width - size) / 2f
        val offsetY = (height - size) / 2f
        val cellSize = size / 3f
        val cx = offsetX + col * cellSize + cellSize / 2f
        val cy = offsetY + row * cellSize + cellSize / 2f
        return Pair(cx, cy)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val cellSize = size / 3f
        val dotRadius = (cellSize * 0.12f).coerceIn(12f, 24f)
        val ringRadius = dotRadius * 2.2f

        // Draw connecting lines
        if (selectedNodes.isNotEmpty()) {
            for (i in 0 until selectedNodes.size - 1) {
                val (x1, y1) = getNodeCenter(selectedNodes[i])
                val (x2, y2) = getNodeCenter(selectedNodes[i + 1])
                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }
            if (isDrawing) {
                val (lastX, lastY) = getNodeCenter(selectedNodes.last())
                canvas.drawLine(lastX, lastY, currentTouchX, currentTouchY, linePaint)
            }
        }

        // Draw 3x3 dots
        for (i in 0 until 9) {
            val (cx, cy) = getNodeCenter(i)
            if (selectedNodes.contains(i)) {
                canvas.drawCircle(cx, cy, dotRadius * 1.15f, selectedDotPaint)
                canvas.drawCircle(cx, cy, ringRadius, ringPaint)
            } else {
                canvas.drawCircle(cx, cy, dotRadius, dotPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                selectedNodes.clear()
                isDrawing = true
                currentTouchX = event.x
                currentTouchY = event.y
                checkHit(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentTouchX = event.x
                currentTouchY = event.y
                checkHit(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isDrawing = false
                if (selectedNodes.isNotEmpty()) {
                    val patternString = selectedNodes.joinToString("")
                    onPatternDetectedListener?.invoke(patternString)
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun checkHit(x: Float, y: Float) {
        val size = min(width, height).toFloat()
        val cellSize = size / 3f
        val hitRadius = cellSize * 0.36f

        for (i in 0 until 9) {
            if (selectedNodes.contains(i)) continue
            val (cx, cy) = getNodeCenter(i)
            if (hypot(x - cx, y - cy) <= hitRadius) {
                if (selectedNodes.isNotEmpty()) {
                    val last = selectedNodes.last()
                    val mid = getIntermediateNode(last, i)
                    if (mid != -1 && !selectedNodes.contains(mid)) {
                        selectedNodes.add(mid)
                    }
                }
                selectedNodes.add(i)
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                break
            }
        }
    }

    private fun getIntermediateNode(a: Int, b: Int): Int {
        val r1 = a / 3
        val c1 = a % 3
        val r2 = b / 3
        val c2 = b % 3
        if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
            return ((r1 + r2) / 2) * 3 + ((c1 + c2) / 2)
        }
        return -1
    }
}
