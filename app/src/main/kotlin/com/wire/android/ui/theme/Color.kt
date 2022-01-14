package com.wire.android.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

val WireLightColors = lightColors(
    secondary = WireColor.LightBlue,
    onSecondary = Color.White,
    background =  WireColor.LightGray
)

val WireDarkColors = darkColors(

)


object WireColor {
    @Stable
    val LightBlue = Color(0xFF0772DE)

    @Stable
    val LightGray = Color(0xFFEDEFF0)
}
