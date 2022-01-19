package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable


@Composable
fun WireTheme(
    useDarkColors: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (useDarkColors) WireDarkColors else WireLightColors,
        typography = typography,
        content = content
    )
}
