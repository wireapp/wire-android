/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.sketch

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

class PathProperties(
    var path: Path = Path(),
    var strokeWidth: Float = 10f,
    var color: Color = Color.Blue,
    var drawMode: DrawMode = DrawMode.Pen
) {
    private val paint: Paint
        get() {
            return if (drawMode == DrawMode.Pen) {
                Paint().apply {
                    color = this@PathProperties.color.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = this@PathProperties.strokeWidth
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            } else {
                Paint().apply {
                    color = Color.Transparent.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = this@PathProperties.strokeWidth
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        blendMode = android.graphics.BlendMode.CLEAR
                    }
                }
            }
        }

    private val androidPath
        get() = path.asAndroidPath()

    fun draw(scope: DrawScope, bitmap: Bitmap? = null) {
        when (drawMode) {
            DrawMode.Pen -> {
                if (bitmap != null) {
                    val brush = ShaderBrush(
                        shader = BitmapShader(
                            bitmap,
                            Shader.TileMode.REPEAT,
                            Shader.TileMode.REPEAT
                        )
                    )
                    scope.drawPath(
                        path,
                        brush,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    scope.drawPath(
                        color = color,
                        path = path,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            DrawMode.Eraser -> {
                scope.drawPath(
                    color = Color.Transparent,
                    path = path,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = BlendMode.Clear
                )
            }

            DrawMode.None -> {}
        }
    }

    fun drawNative(canvas: Canvas) {
        when (drawMode) {
            DrawMode.Pen -> {
                canvas.drawPath(
                    androidPath,
                    paint
                )
            }

            DrawMode.Eraser -> {
                canvas.drawPath(
                    androidPath,
                    paint
                )
            }

            DrawMode.None -> {}
        }
    }
}
