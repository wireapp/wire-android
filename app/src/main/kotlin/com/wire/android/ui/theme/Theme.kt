package com.wire.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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

val MaterialTheme.wireColorScheme
    @Composable
    get() = LocalWireColors.current

// Default MaterialTheme Typography mapping
val Typography = Typography(
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

// Default MaterialTheme light ColorScheme mapping
private val LightColors = lightColorScheme(
    primary = WireColor.LightUIBlue,
    onPrimary = Color.White,
    primaryContainer = WireColor.LightUIBlue.copy(alpha = 0.1f),
    onPrimaryContainer = WireColor.LightUIBlue,
    inversePrimary = WireColor.DarkUIBlue,
    secondary = WireColor.Gray90,
    onSecondary = Color.White,
    secondaryContainer = WireColor.Gray20,
    onSecondaryContainer = WireColor.Gray90,
    tertiary = WireColor.LightUIGreen,
    onTertiary = Color.White,
    tertiaryContainer = WireColor.LightUIGreen.copy(alpha = 0.1f),
    onTertiaryContainer = WireColor.LightUIGreen,
    background = WireColor.Gray20,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = WireColor.Gray20,
    onSurfaceVariant = WireColor.Gray90,
    inverseSurface = WireColor.Gray90,
    inverseOnSurface = WireColor.Gray20,
    error = WireColor.LightUIRed,
    onError = Color.White,
    errorContainer = WireColor.LightUIRed.copy(alpha = 0.1f),
    onErrorContainer = WireColor.LightUIRed,
    outline = WireColor.Gray30
)

// Dark WireColorScheme
private val LightWireColors = WireColorScheme(
    blue = WireColor.LightUIBlue,
    green = WireColor.LightUIGreen,
    petrol = WireColor.LightUIPetrol,
    purple = WireColor.LightUIPurple,
    red = WireColor.LightUIRed,
    yellow = WireColor.LightUIYellow,
    gray10 = WireColor.Gray10,
    gray20 = WireColor.Gray20,
    gray30 = WireColor.Gray30,
    gray40 = WireColor.Gray40,
    gray50 = WireColor.Gray50,
    gray60 = WireColor.Gray60,
    gray70 = WireColor.Gray70,
    gray80 = WireColor.Gray80,
    gray90 = WireColor.Gray90,
    gray100 = WireColor.Gray100
)


// Default MaterialTheme dark ColorScheme mapping
private val DarkColors = darkColorScheme(
    primary = WireColor.DarkUIBlue,
    onPrimary = Color.Black,
    primaryContainer = WireColor.DarkUIBlue.copy(alpha = 0.1f),
    onPrimaryContainer = WireColor.DarkUIBlue,
    inversePrimary = WireColor.LightUIBlue,
    secondary = WireColor.Gray20,
    onSecondary = Color.Black,
    secondaryContainer = WireColor.Gray90,
    onSecondaryContainer = WireColor.Gray20,
    tertiary = WireColor.DarkUIGreen,
    onTertiary = Color.Black,
    tertiaryContainer = WireColor.DarkUIGreen.copy(alpha = 0.1f),
    onTertiaryContainer = WireColor.DarkUIGreen,
    background = WireColor.Gray90,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = WireColor.Gray90,
    onSurfaceVariant = WireColor.Gray20,
    inverseSurface = WireColor.Gray20,
    inverseOnSurface = WireColor.Gray90,
    error = WireColor.DarkUIRed,
    onError = Color.Black,
    errorContainer = WireColor.DarkUIRed.copy(alpha = 0.1f),
    onErrorContainer = WireColor.DarkUIRed,
    outline = WireColor.Gray80
)

// Light WireColorScheme
private val DarkWireColors = WireColorScheme(
    blue = WireColor.DarkUIBlue,
    green = WireColor.DarkUIGreen,
    petrol = WireColor.DarkUIPetrol,
    purple = WireColor.DarkUIPurple,
    red = WireColor.DarkUIRed,
    yellow = WireColor.DarkUIYellow,
    gray10 = WireColor.Gray100,
    gray20 = WireColor.Gray90,
    gray30 = WireColor.Gray80,
    gray40 = WireColor.Gray70,
    gray50 = WireColor.Gray60,
    gray60 = WireColor.Gray50,
    gray70 = WireColor.Gray40,
    gray80 = WireColor.Gray30,
    gray90 = WireColor.Gray20,
    gray100 = WireColor.Gray10
)
