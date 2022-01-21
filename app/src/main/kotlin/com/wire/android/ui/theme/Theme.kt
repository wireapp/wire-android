package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

private val WireLightColors = lightColors(
    primary = WireColor.Blue,
    secondary = WireColor.LightBlue,
    onSecondary = Color.White,
    background = WireColor.LightGray,
    onBackground = WireColor.LightBlack,
    error = WireColor.LightRed
)

private val WireDarkColors = darkColors(

)
