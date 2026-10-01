package com.supportai.assistant.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Apple Siri / Apple Intelligence uslubidagi ko'p qatlamli, suyuq nurlanuvchi (Liquid Glow Orb)
 */
class SiriWaveOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaintCyan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x8800F2FE.toInt()
    }
    private val wavePaintMagenta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x88E100FF.toInt()
    }
    private val wavePaintPurple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x887F00FF.toInt()
    }

    private var phase = 0f
    private var amplitudeMultiplier = 1.0f
    private var isListening = false
    private var animator: ValueAnimator? = null

    init {
        startSiriAnimation()
    }

    private fun startSiriAnimation() {
        animator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = 3200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                phase = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setListeningState(listening: Boolean) {
        isListening = listening
        amplitudeMultiplier = if (listening) 1.8f else 1.0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = minOf(cx, cy) * 0.65f

        // 1. Markaziy nur (Central Glowing Radial Core)
        val gradient = RadialGradient(
            cx, cy, baseRadius * 1.2f,
            intArrayOf(
                0xDD00F2FE.toInt(),
                0xAA7F00FF.toInt(),
                0x55E100FF.toInt(),
                0x00000000
            ),
            floatArrayOf(0.0f, 0.45f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )
        orbPaint.shader = gradient
        canvas.drawCircle(cx, cy, baseRadius * 1.15f, orbPaint)

        // 2. Suyuq to'lqinlar (Fluid Siri Harmonic Liquid Waves)
        drawLiquidWave(canvas, cx, cy, baseRadius * 0.85f, phase, wavePaintCyan, 1.0f)
        drawLiquidWave(canvas, cx, cy, baseRadius * 0.82f, phase + 1.2f, wavePaintMagenta, 0.9f)
        drawLiquidWave(canvas, cx, cy, baseRadius * 0.80f, phase + 2.5f, wavePaintPurple, 0.8f)
    }

    private fun drawLiquidWave(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        currentPhase: Float,
        paint: Paint,
        speedScale: Float
    ) {
        val path = Path()
        val points = 72
        val step = (2 * PI / points).toFloat()

        for (i in 0..points) {
            val angle = i * step
            // Garmonik tebranish (Siri wave effect)
            val waveOffset = (sin(angle * 3 + currentPhase * speedScale) * 14f +
                    sin(angle * 2 - currentPhase * 0.7f) * 10f) * amplitudeMultiplier
            val r = radius + waveOffset
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
