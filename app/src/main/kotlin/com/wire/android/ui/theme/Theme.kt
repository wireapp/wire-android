package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun WireTheme(
    useDarkColors: Boolean = isSystemInDarkTheme(),
    isPreview: Boolean = false,
    content: @Composable () -> Unit
) {
    val wireColorScheme = WireColorSchemeTypes.currentTheme
    val wireTypography = WireTypographyTypes.currentScreenSize
    val wireDimensions = WireDimensionsTypes.currentScreenSize.currentOrientation

    CompositionLocalProvider(
        LocalWireColors provides wireColorScheme,
        LocalWireTypography provides wireTypography,
        LocalWireDimensions provides wireDimensions
        ) {
        MaterialTheme(
            colorScheme = wireColorScheme.toColorScheme(),
            typography = wireTypography.toTypography()
        ) {
            if(!isPreview) {
                val systemUiController = rememberSystemUiController()
                val backgroundColor = MaterialTheme.colorScheme.background
                SideEffect { systemUiController.setSystemBarsColor(color = backgroundColor, darkIcons = !useDarkColors) }
            }
            content()
        }
    }
}

private val LocalWireColors = staticCompositionLocalOf { WireColorSchemeTypes.light }
private val LocalWireTypography = staticCompositionLocalOf { WireTypographyTypes.defaultPhone }
private val LocalWireDimensions = staticCompositionLocalOf { WireDimensionsTypes.defaultPhone.portrait }

val MaterialTheme.wireColorScheme
    @Composable
    get() = LocalWireColors.current

val MaterialTheme.wireTypography
    @Composable
    get() = LocalWireTypography.current

val MaterialTheme.wireDimensions
    @Composable
    get() = LocalWireDimensions.current
