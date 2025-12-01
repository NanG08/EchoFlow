package com.firstapp.langtranslate.ui.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.BounceInterpolator
import androidx.appcompat.widget.AppCompatButton

class AnimatedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    init {
        // Add elevation for depth
        elevation = 8f
        translationZ = 4f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animatePress()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animateRelease()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animatePress() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0.95f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0.95f)
        val elevation = ObjectAnimator.ofFloat(this, "elevation", 4f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, elevation)
            duration = 100
            start()
        }
    }

    private fun animateRelease() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f)
        val elevation = ObjectAnimator.ofFloat(this, "elevation", 8f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, elevation)
            duration = 200
            interpolator = BounceInterpolator()
            start()
        }
    }

    fun animateSuccess() {
        val rotation = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f)
        rotation.duration = 600
        rotation.start()
    }

    fun animateError() {
        val shake = ObjectAnimator.ofFloat(
            this,
            "translationX",
            0f,
            25f,
            -25f,
            25f,
            -25f,
            15f,
            -15f,
            6f,
            -6f,
            0f
        )
        shake.duration = 500
        shake.start()
    }
}
