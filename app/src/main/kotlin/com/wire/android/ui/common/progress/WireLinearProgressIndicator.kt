package com.wire.android.ui.common.progress

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun WireLinearProgressIndicator(modifier: Modifier = Modifier, progressColor: Color) {
    LinearProgressIndicator(
        color = progressColor,
        modifier = modifier
    )
}
