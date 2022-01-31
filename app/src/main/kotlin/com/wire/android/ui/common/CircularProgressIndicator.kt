package com.wire.android.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp


@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, progressColor: Color, strokeWidth: Dp = 2.dp, size: Dp = 16.dp) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val trackWidthPx =  with(LocalDensity.current) { strokeWidth.roundToPx() }
    AndroidView( //TODO replace with proper CircularProgressIndicator when available for material3
        modifier = modifier.size(size),
        factory = {
            CircularProgressIndicator(it).apply {
                isIndeterminate = true
                indicatorSize = sizePx
                trackThickness = trackWidthPx
            }
        },
        update = {
            it.setIndicatorColor(progressColor.toArgb())
            it.show()
        }
    )
}
