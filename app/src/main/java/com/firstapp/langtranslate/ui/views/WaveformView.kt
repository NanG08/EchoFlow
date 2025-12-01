package com.firstapp.langtranslate.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.firstapp.langtranslate.R
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var animator: ValueAnimator? = null
    private var phase = 0f
    private val waves = 3
    private var amplitude = 50f

    init {
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
        }
    }

    fun startAnimation() {
        animator?.start()
    }

    fun stopAnimation() {
        animator?.cancel()
        phase = 0f
        invalidate()
    }

    fun setAmplitude(amp: Float) {
        amplitude = amp.coerceIn(10f, 100f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f
        val width = width.toFloat()

        for (i in 0 until waves) {
            val alpha = (255 * (1f - i / waves.toFloat())).toInt()
            paint.alpha = alpha

            for (x in 0 until width.toInt() step 4) {
                val y = centerY + amplitude * sin((x / 50f + phase / 50f + i * 1.2f))

                if (x == 0) {
                    canvas.drawPoint(x.toFloat(), y, paint)
                } else {
                    val prevX = (x - 4).toFloat()
                    val prevY = centerY + amplitude * sin(((x - 4) / 50f + phase / 50f + i * 1.2f))
                    canvas.drawLine(prevX, prevY, x.toFloat(), y, paint)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
