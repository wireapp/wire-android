package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun WireTheme(
    useDarkColors: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalWireColors provides if (useDarkColors) DarkWireColors else LightWireColors) {
        MaterialTheme(
            colorScheme = if (useDarkColors) DarkColors else LightColors,
            typography = Typography
        ) {
            val systemUiController = rememberSystemUiController()
            val backgroundColor = MaterialTheme.colorScheme.background
            SideEffect { systemUiController.setSystemBarsColor(color = backgroundColor, darkIcons = !useDarkColors) }
            content()
        }
    }
}

internal val LocalWireColors = staticCompositionLocalOf { LightWireColors }

val MaterialTheme.wireColorScheme
    @Composable
    get() = LocalWireColors.current


// Default MaterialTheme Typography mapping
private val Typography = Typography(
    titleLarge = WireTypography.Title01,
    titleMedium = WireTypography.Title02,
    titleSmall = WireTypography.Title03,
    labelLarge = WireTypography.Button02,
    labelMedium = WireTypography.Label02,
    labelSmall = WireTypography.Label03,
    bodyLarge = WireTypography.Body01,
    bodyMedium = WireTypography.Label04,
    bodySmall = WireTypography.SubLine01
)


// Dark WireColorScheme
private val LightWireColors = WireColorScheme(
    primary = WireColor.LightBlue500,                       onPrimary = Color.White,
    error = WireColor.LightRed500,                          onError = Color.White,
    errorOutline = WireColor.LightRed200,
    warning = WireColor.LightYellow500,                     onWarning = Color.White,
    positive = WireColor.LightGreen500,                     onPositive = Color.White,
    background = WireColor.Gray20,                          onBackground = Color.Black,
    backgroundVariant = WireColor.Gray10,                   onBackgroundVariant = Color.Black,
    surface = Color.White,                                  onSurface = Color.Black,
    primaryButtonEnabled = WireColor.LightBlue500,          onPrimaryButtonEnabled = Color.White,
    primaryButtonDisabled = WireColor.Gray50,               onPrimaryButtonDisabled = WireColor.Gray80,
    primaryButtonSelected = WireColor.LightBlue700,         onPrimaryButtonSelected = Color.White,
    primaryButtonRipple = Color.Black,
    secondaryButtonEnabled = Color.White,                   onSecondaryButtonEnabled = Color.Black,
    secondaryButtonEnabledOutline = WireColor.Gray40,
    secondaryButtonDisabled = WireColor.Gray20,             onSecondaryButtonDisabled = WireColor.Gray70,
    secondaryButtonDisabledOutline = WireColor.Gray40,
    secondaryButtonSelected = WireColor.LightBlue50,        onSecondaryButtonSelected = WireColor.LightBlue500,
    secondaryButtonSelectedOutline = WireColor.LightBlue300,
    secondaryButtonRipple = Color.Black,
    tertiaryButtonEnabled = Color.Transparent,              onTertiaryButtonEnabled = Color.Black,
    tertiaryButtonDisabled = Color.Transparent,             onTertiaryButtonDisabled = WireColor.Gray60,
    tertiaryButtonSelected = WireColor.LightBlue50,         onTertiaryButtonSelected = WireColor.LightBlue500,
    tertiaryButtonSelectedOutline = WireColor.LightBlue300,
    tertiaryButtonRipple = Color.Black,
    divider = WireColor.Gray40,
    secondaryText = WireColor.Gray70,
    labelText = WireColor.Gray80,
    badge = WireColor.Gray90,                               onBadge = Color.White
)
// Default MaterialTheme light ColorScheme mapping
private val LightColors = LightWireColors.toColorScheme()


// Dark WireColorScheme
private val DarkWireColors = WireColorScheme(
    primary = WireColor.DarkBlue500,                        onPrimary = Color.Black,
    error = WireColor.DarkRed500,                           onError = Color.Black,
    errorOutline = WireColor.DarkRed200,
    warning = WireColor.DarkYellow500,                      onWarning = Color.Black,
    positive = WireColor.DarkGreen500,                      onPositive = Color.Black,
    background = WireColor.Gray90,                          onBackground = Color.White,
    backgroundVariant = WireColor.Gray100,                  onBackgroundVariant = Color.White,
    surface = Color.Black,                                  onSurface = Color.White,
    primaryButtonEnabled = WireColor.DarkBlue500,           onPrimaryButtonEnabled = Color.Black,
    primaryButtonDisabled = WireColor.Gray60,               onPrimaryButtonDisabled = WireColor.Gray30,
    primaryButtonSelected = WireColor.DarkBlue700,          onPrimaryButtonSelected = Color.Black,
    primaryButtonRipple = Color.White,
    secondaryButtonEnabled = Color.Black,                   onSecondaryButtonEnabled = Color.White,
    secondaryButtonEnabledOutline = WireColor.Gray40,
    secondaryButtonDisabled = WireColor.Gray20,             onSecondaryButtonDisabled = WireColor.Gray70,
    secondaryButtonDisabledOutline = WireColor.Gray70,
    secondaryButtonSelected = WireColor.DarkBlue50,         onSecondaryButtonSelected = WireColor.DarkBlue500,
    secondaryButtonSelectedOutline = WireColor.DarkBlue300,
    secondaryButtonRipple = Color.White,
    tertiaryButtonEnabled = Color.Transparent,              onTertiaryButtonEnabled = Color.White,
    tertiaryButtonDisabled = Color.Transparent,             onTertiaryButtonDisabled = WireColor.Gray60,
    tertiaryButtonSelected = WireColor.DarkBlue50,          onTertiaryButtonSelected = WireColor.DarkBlue500,
    tertiaryButtonSelectedOutline = WireColor.DarkBlue300,
    tertiaryButtonRipple = Color.White,
    divider = WireColor.Gray70,
    secondaryText = WireColor.Gray40,
    labelText = WireColor.Gray30,
    badge = WireColor.Gray10,                               onBadge = Color.Black
)
// Default MaterialTheme dark ColorScheme mapping
private val DarkColors = DarkWireColors.toColorScheme()
