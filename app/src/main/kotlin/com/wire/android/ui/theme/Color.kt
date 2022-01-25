package com.wire.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


@Immutable
data class WireColorScheme(
    val blue: Color = WireColor.LightUIBlue,
    val green: Color = WireColor.LightUIGreen,
    val petrol: Color = WireColor.LightUIPetrol,
    val purple: Color = WireColor.LightUIPurple,
    val red: Color = WireColor.LightUIRed,
    val yellow: Color = WireColor.LightUIYellow,
    val gray10: Color = WireColor.Gray10,
    val gray20: Color = WireColor.Gray20,
    val gray30: Color = WireColor.Gray30,
    val gray40: Color = WireColor.Gray40,
    val gray50: Color = WireColor.Gray50,
    val gray60: Color = WireColor.Gray60,
    val gray70: Color = WireColor.Gray70,
    val gray80: Color = WireColor.Gray80,
    val gray90: Color = WireColor.Gray90,
    val gray100: Color = WireColor.Gray100
)

internal val LocalWireColors = staticCompositionLocalOf { WireColorScheme() }

@Suppress("MagicNumber")
object WireColor {

    // Light UI Theme Colors
    @Stable
    val LightUIBlue: Color = Color(0xFF0667C8)
    @Stable
    val LightUIGreen: Color = Color(0xFF207C37)
    @Stable
    val LightUIPetrol: Color = Color(0xFF01819D)
    @Stable
    val LightUIPurple: Color = Color(0xFF8944AB)
    @Stable
    val LightUIRed: Color = Color(0xFFC20013)
    @Stable
    val LightUIYellow: Color = Color(0xFF7F6545)

    // Dark UI Theme
    @Stable
    val DarkUIBlue: Color = Color(0xFF54A6FF)
    @Stable
    val DarkUIGreen: Color = Color(0xFF30DB5B)
    @Stable
    val DarkUIPetrol: Color = Color(0xFF5DE6FF)
    @Stable
    val DarkUIPurple: Color = Color(0xFFDA8FFF)
    @Stable
    val DarkUIRed: Color = Color(0xFFFF7770)
    @Stable
    val DarkUIYellow: Color = Color(0xFFFFD426)


    @Stable
    val Gray10 = Color(0xFFFAFAFA)
    @Stable
    val Gray20 = Color(0xFFEDEFF0)
    @Stable
    val Gray30 = Color(0xFFE5E8EA)
    @Stable
    val Gray40 = Color(0xFFDCE0E3)
    @Stable
    val Gray50 = Color(0xFFCBCED1)
    @Stable
    val Gray60 = Color(0xFF9FA1A7)
    @Stable
    val Gray70 = Color(0xFF676B71)
    @Stable
    val Gray80 = Color(0xFF54585F)
    @Stable
    val Gray90 = Color(0xFF34373D)
    @Stable
    val Gray100 = Color(0xFF17181A)
}
