package com.wire.android.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun WireCircularProgressIndicator(modifier: Modifier = Modifier, progressColor: Color, strokeWidth: Dp = 2.dp, size: Dp = 16.dp) {
    CircularProgressIndicator(
        strokeWidth = strokeWidth,
        color = progressColor,
        modifier = modifier.size(size)
    )
}
