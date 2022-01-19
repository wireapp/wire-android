package com.wire.android.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

val WireLightColors = lightColors(
    primary = WireColor.Blue,
    secondary = WireColor.LightBlue,
    onSecondary = Color.White,
    background =  WireColor.LightGray,
    onBackground = WireColor.LightBlack
)

val WireDarkColors = darkColors(

)


@Suppress("MagicNumber")
object WireColor {

    @Stable
    val Blue = Color(0xFF0667C8)

    @Stable
    val LightBlue = Color(0xFF0772DE)

    @Stable
    val Alpha10LightBlue = Color(0x1A0667C8)

    @Stable
    val DarkBlue = Color(0xFF0667C8)

    @Stable
    val LightRed = Color(0xFFC20013)

    @Stable
    val Alpha20LightRed = Color(0x33D70015)

    @Stable
    val LightGray = Color(0xFFEDEFF0)

    @Stable
    val Dark80Gray = Color(0xFF54585F)

    @Stable
    val Dark90Gray = Color(0xFF34373D)

    @Stable
    val LightBlack = Color(0xFF000000)
}
