package com.wire.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.esentsov.PackagePrivate

@Immutable
data class WireColorScheme(
    val primary: Color,                                 val onPrimary: Color,
    val error: Color,                                   val onError: Color,
    val errorOutline: Color, //TODO missing on designs
    val warning: Color,                                 val onWarning: Color,
    val positive: Color,                                val onPositive: Color,
    val background: Color,                              val onBackground: Color,
    val backgroundVariant: Color,                       val onBackgroundVariant: Color,
    val surface: Color,                                 val onSurface: Color,
    val primaryButtonEnabled: Color,                    val onPrimaryButtonEnabled: Color,
    val primaryButtonDisabled: Color,                   val onPrimaryButtonDisabled: Color,
    val primaryButtonFocus: Color,                      val onPrimaryButtonFocus: Color,
    val primaryButtonSelected: Color,                   val onPrimaryButtonSelected: Color,
    val secondaryButtonEnabled: Color,                  val onSecondaryButtonEnabled: Color,
    val secondaryButtonEnabledOutline: Color,
    val secondaryButtonDisabled: Color,                 val onSecondaryButtonDisabled: Color,
    val secondaryButtonDisabledOutline: Color,
    val secondaryButtonFocus: Color,                    val onSecondaryButtonFocus: Color,
    val secondaryButtonFocusOutline: Color,
    val secondaryButtonSelected: Color,                 val onSecondaryButtonSelected: Color,
    val secondaryButtonSelectedOutline: Color,
    val tertiaryButtonEnabled: Color,                   val onTertiaryButtonEnabled: Color,
    val tertiaryButtonDisabled: Color,                  val onTertiaryButtonDisabled: Color,
    val tertiaryButtonFocus: Color,                     val onTertiaryButtonFocus: Color,
    val tertiaryButtonSelected: Color,                  val onTertiaryButtonSelected: Color,
    val tertiaryButtonSelectedOutline: Color,
    val divider: Color,
    val secondaryText: Color,
    val labelText: Color, //TODO missing on designs
    val badge: Color, val onBadge: Color //TODO missing on designs
) {
    fun toColorScheme(): ColorScheme = ColorScheme(
        primary = primary,                              onPrimary = onPrimary,
        primaryContainer = secondaryButtonSelected,     onPrimaryContainer = onSecondaryButtonSelected,
        inversePrimary = secondaryButtonSelected,
        secondary = badge,                              onSecondary = onBadge,
        secondaryContainer = backgroundVariant,         onSecondaryContainer = onBackgroundVariant,
        tertiary = primary,                             onTertiary = onPrimary,
        tertiaryContainer = secondaryButtonSelected,    onTertiaryContainer = onSecondaryButtonSelected,
        background = background,                        onBackground = onBackground,
        surface = surface,                              onSurface = onSurface,
        surfaceVariant = backgroundVariant,             onSurfaceVariant = onBackgroundVariant,
        inverseSurface = onPrimaryButtonDisabled,       inverseOnSurface = Color.White,
        error = error,                                  onError = onError,
        errorContainer = errorOutline,                  onErrorContainer = error,
        outline = divider
    )
}

// Light WireColorScheme
private val LightWireColorScheme = WireColorScheme(
    primary = WireColorPalette.LightBlue500,                       onPrimary = Color.White,
    error = WireColorPalette.LightRed500,                          onError = Color.White,
    errorOutline = WireColorPalette.LightRed200,
    warning = WireColorPalette.LightYellow500,                     onWarning = Color.White,
    positive = WireColorPalette.LightGreen500,                     onPositive = Color.White,
    background = WireColorPalette.Gray20,                          onBackground = Color.Black,
    backgroundVariant = WireColorPalette.Gray10,                   onBackgroundVariant = Color.Black,
    surface = Color.White,                                         onSurface = Color.Black,
    primaryButtonEnabled = WireColorPalette.LightBlue500,          onPrimaryButtonEnabled = Color.White,
    primaryButtonDisabled = WireColorPalette.Gray50,               onPrimaryButtonDisabled = WireColorPalette.Gray80,
    primaryButtonFocus = WireColorPalette.LightBlue700,            onPrimaryButtonFocus = Color.White,
    primaryButtonSelected = WireColorPalette.LightBlue700,         onPrimaryButtonSelected = Color.White,
    secondaryButtonEnabled = Color.White,                           onSecondaryButtonEnabled = Color.Black,
    secondaryButtonEnabledOutline = WireColorPalette.Gray40,
    secondaryButtonDisabled = WireColorPalette.Gray20,             onSecondaryButtonDisabled = WireColorPalette.Gray70,
    secondaryButtonDisabledOutline = WireColorPalette.Gray40,
    secondaryButtonFocus = WireColorPalette.Gray30,                onSecondaryButtonFocus = Color.Black,
    secondaryButtonFocusOutline = WireColorPalette.Gray50,
    secondaryButtonSelected = WireColorPalette.LightBlue50,        onSecondaryButtonSelected = WireColorPalette.LightBlue500,
    secondaryButtonSelectedOutline = WireColorPalette.LightBlue300,
    tertiaryButtonEnabled = Color.Transparent,                     onTertiaryButtonEnabled = Color.Black,
    tertiaryButtonDisabled = Color.Transparent,                    onTertiaryButtonDisabled = WireColorPalette.Gray60,
    tertiaryButtonFocus = WireColorPalette.Gray30,                 onTertiaryButtonFocus = Color.Black,
    tertiaryButtonSelected = WireColorPalette.LightBlue50,         onTertiaryButtonSelected = WireColorPalette.LightBlue500,
    tertiaryButtonSelectedOutline = WireColorPalette.LightBlue300,
    divider = WireColorPalette.Gray40,
    secondaryText = WireColorPalette.Gray70,
    labelText = WireColorPalette.Gray80,
    badge = WireColorPalette.Gray90,                               onBadge = Color.White
)


// Dark WireColorScheme
private val DarkWireColorScheme = WireColorScheme(
    primary = WireColorPalette.DarkBlue500,                        onPrimary = Color.Black,
    error = WireColorPalette.DarkRed500,                           onError = Color.Black,
    errorOutline = WireColorPalette.DarkRed200,
    warning = WireColorPalette.DarkYellow500,                      onWarning = Color.Black,
    positive = WireColorPalette.DarkGreen500,                      onPositive = Color.Black,
    background = WireColorPalette.Gray90,                          onBackground = Color.White,
    backgroundVariant = WireColorPalette.Gray100,                  onBackgroundVariant = Color.White,
    surface = Color.Black,                                         onSurface = Color.White,
    primaryButtonEnabled = WireColorPalette.DarkBlue500,           onPrimaryButtonEnabled = Color.Black,
    primaryButtonDisabled = WireColorPalette.Gray60,               onPrimaryButtonDisabled = WireColorPalette.Gray30,
    primaryButtonFocus = WireColorPalette.DarkBlue700,             onPrimaryButtonFocus = Color.Black,
    primaryButtonSelected = WireColorPalette.DarkBlue700,          onPrimaryButtonSelected = Color.Black,
    secondaryButtonEnabled = Color.Black,                          onSecondaryButtonEnabled = Color.White,
    secondaryButtonEnabledOutline = WireColorPalette.Gray40,
    secondaryButtonDisabled = WireColorPalette.Gray20,             onSecondaryButtonDisabled = WireColorPalette.Gray70,
    secondaryButtonDisabledOutline = WireColorPalette.Gray70,
    secondaryButtonFocus = WireColorPalette.Gray80,                onSecondaryButtonFocus = Color.White,
    secondaryButtonFocusOutline = WireColorPalette.Gray70,
    secondaryButtonSelected = WireColorPalette.DarkBlue50,         onSecondaryButtonSelected = WireColorPalette.DarkBlue500,
    secondaryButtonSelectedOutline = WireColorPalette.DarkBlue300,
    tertiaryButtonEnabled = Color.Transparent,                     onTertiaryButtonEnabled = Color.White,
    tertiaryButtonDisabled = Color.Transparent,                    onTertiaryButtonDisabled = WireColorPalette.Gray60,
    tertiaryButtonFocus = WireColorPalette.Gray80,                 onTertiaryButtonFocus = Color.White,
    tertiaryButtonSelected = WireColorPalette.DarkBlue50,          onTertiaryButtonSelected = WireColorPalette.DarkBlue500,
    tertiaryButtonSelectedOutline = WireColorPalette.DarkBlue300,
    divider = WireColorPalette.Gray70,
    secondaryText = WireColorPalette.Gray40,
    labelText = WireColorPalette.Gray30,
    badge = WireColorPalette.Gray10,                               onBadge = Color.Black
)

@PackagePrivate
val WireColorSchemeTypes: ThemeDependent<WireColorScheme> = ThemeDependent(
    light = LightWireColorScheme,
    dark = DarkWireColorScheme
)
