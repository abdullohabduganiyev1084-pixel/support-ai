package com.supportai.assistant.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.supportai.assistant.R

/**
 * Support AI uchun maxsus neon animatsiyalik orqa fon (Pulse Rings & Glowing Neural Waves)
 */
class AnimatedLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = context.getColor(R.color.neon_cyan)
    }

    private val purplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = context.getColor(R.color.neon_purple)
    }

    private var animationProgress = 0f
    private var isAnimatingActive = true
    private var pulseAnimator: ValueAnimator? = null

    init {
        startAnimation()
    }

    private fun startAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setActivePulse(active: Boolean) {
        isAnimatingActive = active
        if (active) {
            pulseAnimator?.start()
        } else {
            pulseAnimator?.cancel()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = minOf(centerX, centerY) * 0.95f
        val baseRadius = minOf(centerX, centerY) * 0.38f

        if (!isAnimatingActive) {
            // O'chirilgan holatda tinch statik doira
            cyanPaint.alpha = 50
            canvas.drawCircle(centerX, centerY, baseRadius, cyanPaint)
            return
        }

        // 3 ta ketma-ket kengayuvchi va so'nuvchi neon to'lqinlar
        for (i in 0 until 3) {
            val waveProgress = (animationProgress + (i * 0.33f)) % 1f
            val currentRadius = baseRadius + (maxRadius - baseRadius) * waveProgress
            val alpha = ((1f - waveProgress) * 220).toInt().coerceIn(0, 255)

            if (i % 2 == 0) {
                cyanPaint.alpha = alpha
                cyanPaint.strokeWidth = 3f + (1f - waveProgress) * 5f
                canvas.drawCircle(centerX, centerY, currentRadius, cyanPaint)
            } else {
                purplePaint.alpha = alpha
                purplePaint.strokeWidth = 2.5f + (1f - waveProgress) * 4f
                canvas.drawCircle(centerX, centerY, currentRadius, purplePaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}
