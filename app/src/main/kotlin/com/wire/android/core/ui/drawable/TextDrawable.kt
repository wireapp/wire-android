package com.wire.android.core.ui.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class TextDrawable(
    private val text: String,
    @ColorInt private val textColor: Int = Color.WHITE, //TODO: get colors from xml or theme
    @ColorInt private val backgroundColor: Int = Color.RED
) : Drawable() {

    private val textPaint = Paint().apply {
        color = textColor
        textAlign = Paint.Align.CENTER
    }

    private val backgroundPaint = Paint().apply {
        color = backgroundColor
    }

    override fun draw(canvas: Canvas) {
        val radius = minOf(bounds.width(), bounds.height()) / 2
        textPaint.textSize = radius * TEXT_SIZE_MULTIPLIER

        val y = bounds.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
        val x = bounds.centerX().toFloat()

        canvas.drawCircle(x,bounds.centerY().toFloat(), radius.toFloat(), backgroundPaint);
        canvas.drawText(text, x, y, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private const val TEXT_SIZE_MULTIPLIER = 1.1f
    }
}
