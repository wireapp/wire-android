package com.wire.android.core.ui.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import kotlin.math.min

class TextDrawable(
    private val text: String,
    private val width: Float,
    private val height: Float,
    @ColorInt private val textColor: Int = Color.WHITE, //TODO: get colors from xml or theme
    @ColorInt private val backgroundColor: Int = Color.RED
) : Drawable() {

    private val paint by lazy {
        Paint().apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            val radius = min(width, height) / 2
            textSize = radius * TEXT_SIZE_MULTIPLIER
        }
    }

    private val textStartY by lazy {
        height / 2 - ((paint.descent() + paint.ascent()) / 2f)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        canvas.drawText(text, width / 2, textStartY, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private const val TEXT_SIZE_MULTIPLIER = 1.1f
    }
}
