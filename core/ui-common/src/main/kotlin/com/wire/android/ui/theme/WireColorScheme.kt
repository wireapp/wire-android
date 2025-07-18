/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

@file:Suppress("ParameterListWrapping")

package com.wire.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.theme.WireColorPalette.Gray30
import com.wire.android.ui.theme.WireColorPalette.Gray80
import com.wire.android.ui.theme.WireColorPalette.LightAmber50
import com.wire.android.ui.theme.WireColorPalette.LightBlue50
import com.wire.android.ui.theme.WireColorPalette.LightGreen50
import com.wire.android.ui.theme.WireColorPalette.LightPetrol50
import com.wire.android.ui.theme.WireColorPalette.LightPurple50
import com.wire.android.ui.theme.WireColorPalette.LightRed50
import io.github.esentsov.PackagePrivate

@Suppress("LongParameterList")
@Immutable
class WireColorScheme(
    val useDarkSystemBarIcons: Boolean,
    val connectivityBarShouldUseDarkIcons: Boolean,

    // basic colors
    val primary: Color, val onPrimary: Color,
    val primaryVariant: Color, val onPrimaryVariant: Color,
    val inversePrimary: Color,
    val error: Color, val onError: Color,
    val errorVariant: Color, val onErrorVariant: Color,
    val warning: Color, val onWarning: Color,
    val highlight: Color, val onHighlight: Color,
    val positive: Color, val onPositive: Color,
    val positiveVariant: Color, val onPositiveVariant: Color,
    val secondaryText: Color,

    // background colors
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val inverseSurface: Color, val inverseOnSurface: Color,
    val surfaceBright: Color, val surfaceDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color, // backgroundVariant
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    // buttons
    val primaryButtonEnabled: Color, val onPrimaryButtonEnabled: Color,
    val primaryButtonDisabled: Color, val onPrimaryButtonDisabled: Color,
    val primaryButtonSelected: Color, val onPrimaryButtonSelected: Color,
    val primaryButtonRipple: Color,
    val secondaryButtonEnabled: Color, val onSecondaryButtonEnabled: Color,
    val secondaryButtonEnabledOutline: Color,
    val secondaryButtonDisabled: Color, val onSecondaryButtonDisabled: Color,
    val secondaryButtonDisabledOutline: Color,
    val secondaryButtonSelected: Color, val onSecondaryButtonSelected: Color,
    val secondaryButtonSelectedOutline: Color,
    val secondaryButtonRipple: Color,
    val tertiaryButtonEnabled: Color, val onTertiaryButtonEnabled: Color,
    val tertiaryButtonDisabled: Color, val onTertiaryButtonDisabled: Color,
    val tertiaryButtonSelected: Color, val onTertiaryButtonSelected: Color,
    val tertiaryButtonSelectedOutline: Color,
    val tertiaryButtonRipple: Color,

    // strokes and shadows
    val outline: Color,
    val divider: Color,
    val scrim: Color,

    // accents
    val groupAvatarColors: List<GroupAvatarColors>,
    val channelAvatarColors: List<ChannelAvatarColors>,
    val wireAccentColors: WireAccentColors,

    val emojiBackgroundColor: Color,

    ) {
    fun toColorScheme(): ColorScheme = ColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryVariant, onPrimaryContainer = onPrimaryVariant,
        inversePrimary = inversePrimary,
        secondary = positive, onSecondary = onPositive,
        secondaryContainer = positiveVariant, onSecondaryContainer = onPositiveVariant,
        tertiary = warning, onTertiary = onWarning,
        tertiaryContainer = highlight, onTertiaryContainer = onHighlight,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
        error = error, onError = onError,
        errorContainer = errorVariant, onErrorContainer = onErrorVariant,
        outline = outline,
        outlineVariant = divider,
        scrim = scrim,
        surfaceBright = surfaceBright, surfaceDim = surfaceDim,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}

// Light WireColorScheme
private val LightWireColorScheme = WireColorScheme(
    useDarkSystemBarIcons = true,
    connectivityBarShouldUseDarkIcons = false,

    // basic colors
    primary = WireColorPalette.LightBlue500, onPrimary = Color.White,
    primaryVariant = WireColorPalette.LightBlue50, onPrimaryVariant = WireColorPalette.LightBlue500,
    inversePrimary = WireColorPalette.DarkBlue500,
    error = WireColorPalette.LightRed500, onError = Color.White,
    errorVariant = WireColorPalette.LightRed50, onErrorVariant = WireColorPalette.LightRed500,
    warning = WireColorPalette.LightAmber500, onWarning = Color.White,
    highlight = WireColorPalette.DarkAmber200, onHighlight = Color.Black,
    positive = WireColorPalette.LightGreen500, onPositive = Color.White,
    positiveVariant = WireColorPalette.LightGreen50, onPositiveVariant = WireColorPalette.LightGreen500,
    secondaryText = WireColorPalette.Gray70,

    // background colors
    background = WireColorPalette.Gray20, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color.White, onSurfaceVariant = Color.Black,
    inverseSurface = Color.Black, inverseOnSurface = Color.White,
    surfaceBright = Color.White, surfaceDim = WireColorPalette.Gray40,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WireColorPalette.Gray10,
    surfaceContainer = WireColorPalette.Gray20,
    surfaceContainerHigh = WireColorPalette.Gray30,
    surfaceContainerHighest = WireColorPalette.Gray40,

    // buttons
    primaryButtonEnabled = WireColorPalette.LightBlue500, onPrimaryButtonEnabled = Color.White,
    primaryButtonDisabled = WireColorPalette.Gray50, onPrimaryButtonDisabled = WireColorPalette.Gray80,
    primaryButtonSelected = WireColorPalette.LightBlue700, onPrimaryButtonSelected = Color.White,
    primaryButtonRipple = Color.Black,
    secondaryButtonEnabled = Color.White, onSecondaryButtonEnabled = Color.Black,
    secondaryButtonEnabledOutline = WireColorPalette.Gray40,
    secondaryButtonDisabled = WireColorPalette.Gray20, onSecondaryButtonDisabled = WireColorPalette.Gray70,
    secondaryButtonDisabledOutline = WireColorPalette.Gray40,
    secondaryButtonSelected = WireColorPalette.LightBlue50, onSecondaryButtonSelected = WireColorPalette.LightBlue500,
    secondaryButtonSelectedOutline = WireColorPalette.LightBlue300,
    secondaryButtonRipple = Color.Black,
    tertiaryButtonEnabled = Color.Transparent, onTertiaryButtonEnabled = Color.Black,
    tertiaryButtonDisabled = Color.Transparent, onTertiaryButtonDisabled = WireColorPalette.Gray60,
    tertiaryButtonSelected = WireColorPalette.LightBlue50, onTertiaryButtonSelected = WireColorPalette.LightBlue500,
    tertiaryButtonSelectedOutline = WireColorPalette.LightBlue300,
    tertiaryButtonRipple = Color.Black,

    // strokes and shadows
    outline = WireColorPalette.Gray40,
    divider = WireColorPalette.Gray20,
    scrim = WireColorPalette.BlackAlpha55,

    // accents
    groupAvatarColors = listOf(
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightPurple500, WireColorPalette.LightRed500),
        GroupAvatarColors(WireColorPalette.LightBlue500, WireColorPalette.LightGreen500, WireColorPalette.LightAmber500),
        GroupAvatarColors(WireColorPalette.LightBlue500, WireColorPalette.LightRed500, WireColorPalette.LightPurple500),
        GroupAvatarColors(WireColorPalette.LightAmber500, WireColorPalette.LightGreen500, WireColorPalette.LightPurple500),
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightBlue500, WireColorPalette.LightPurple500),
        GroupAvatarColors(WireColorPalette.LightBlue500, WireColorPalette.LightAmber500, WireColorPalette.LightGreen500),
        GroupAvatarColors(WireColorPalette.LightRed500, WireColorPalette.LightPetrol500, WireColorPalette.LightPurple500),
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightRed500, WireColorPalette.LightBlue500),
        GroupAvatarColors(WireColorPalette.LightAmber500, WireColorPalette.LightBlue500, WireColorPalette.LightPurple500),
        GroupAvatarColors(WireColorPalette.LightPurple500, WireColorPalette.LightPetrol500, WireColorPalette.LightGreen500),
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightPurple500, WireColorPalette.LightRed500),
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightBlue500, WireColorPalette.LightRed500),
        GroupAvatarColors(WireColorPalette.LightPurple500, WireColorPalette.LightBlue500, WireColorPalette.LightGreen500),
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightBlue500, WireColorPalette.LightAmber500),
        GroupAvatarColors(WireColorPalette.LightGreen500, WireColorPalette.LightAmber500, WireColorPalette.LightPurple500)
    ),
    channelAvatarColors = listOf(
        ChannelAvatarColors(WireColorPalette.LightBlue100, LightBlue50, WireColorPalette.LightBlue500),
        ChannelAvatarColors(WireColorPalette.LightPurple100, LightPurple50, WireColorPalette.LightPurple500),
        ChannelAvatarColors(WireColorPalette.LightRed100, LightRed50, WireColorPalette.LightRed500),
        ChannelAvatarColors(WireColorPalette.LightGreen100, LightGreen50, WireColorPalette.LightGreen500),
        ChannelAvatarColors(WireColorPalette.LightAmber100, LightAmber50, WireColorPalette.LightAmber500),
        ChannelAvatarColors(WireColorPalette.LightPetrol100, LightPetrol50, WireColorPalette.LightPetrol500),
        ChannelAvatarColors(WireColorPalette.Gray40, Gray30, Gray80)
    ),
    wireAccentColors = WireAccentColors {
        when (it) {
            Accent.Amber -> WireColorPalette.LightAmber500
            Accent.Blue -> WireColorPalette.LightBlue500
            Accent.Green -> WireColorPalette.LightGreen500
            Accent.Purple -> WireColorPalette.LightPurple500
            Accent.Red -> WireColorPalette.LightRed500
            Accent.Petrol -> WireColorPalette.LightPetrol500
            Accent.Unknown -> WireColorPalette.LightBlue500
        }
    },
    emojiBackgroundColor = Color.White,
)

// Dark WireColorScheme
private val DarkWireColorScheme = WireColorScheme(
    useDarkSystemBarIcons = false,
    connectivityBarShouldUseDarkIcons = true,

    // basic colors
    primary = WireColorPalette.DarkBlue500, onPrimary = Color.Black,
    primaryVariant = WireColorPalette.DarkBlue800, onPrimaryVariant = WireColorPalette.DarkBlue300,
    inversePrimary = WireColorPalette.LightBlue500,
    error = WireColorPalette.DarkRed500, onError = Color.Black,
    errorVariant = WireColorPalette.LightRed900, onErrorVariant = WireColorPalette.DarkRed500,
    warning = WireColorPalette.DarkAmber500, onWarning = Color.Black,
    highlight = WireColorPalette.DarkAmber300, onHighlight = Color.Black,
    positive = WireColorPalette.DarkGreen500, onPositive = Color.Black,
    positiveVariant = WireColorPalette.DarkGreen900, onPositiveVariant = WireColorPalette.DarkGreen500,
    secondaryText = WireColorPalette.Gray60,

    // background colors
    background = WireColorPalette.Gray100, onBackground = Color.White,
    surface = WireColorPalette.Gray95, onSurface = Color.White,
    surfaceVariant = WireColorPalette.Gray90, onSurfaceVariant = Color.White,
    inverseSurface = Color.White, inverseOnSurface = Color.Black,
    surfaceBright = WireColorPalette.Gray70, surfaceDim = WireColorPalette.Gray95,
    surfaceContainerLowest = WireColorPalette.Gray100,
    surfaceContainerLow = WireColorPalette.Gray95,
    surfaceContainer = WireColorPalette.Gray90,
    surfaceContainerHigh = WireColorPalette.Gray80,
    surfaceContainerHighest = WireColorPalette.Gray70,

    // buttons
    primaryButtonEnabled = WireColorPalette.DarkBlue500, onPrimaryButtonEnabled = Color.Black,
    primaryButtonDisabled = WireColorPalette.Gray80, onPrimaryButtonDisabled = WireColorPalette.Gray50,
    primaryButtonSelected = WireColorPalette.DarkBlue400, onPrimaryButtonSelected = Color.Black,
    primaryButtonRipple = Color.White,
    secondaryButtonEnabled = WireColorPalette.Gray90, onSecondaryButtonEnabled = Color.White,
    secondaryButtonEnabledOutline = WireColorPalette.Gray90,
    secondaryButtonDisabled = WireColorPalette.Gray95, onSecondaryButtonDisabled = WireColorPalette.Gray50,
    secondaryButtonDisabledOutline = WireColorPalette.Gray95,
    secondaryButtonSelected = WireColorPalette.DarkBlue800, onSecondaryButtonSelected = Color.White,
    secondaryButtonSelectedOutline = WireColorPalette.DarkBlue600,
    secondaryButtonRipple = Color.White,
    tertiaryButtonEnabled = Color.Transparent, onTertiaryButtonEnabled = Color.White,
    tertiaryButtonDisabled = Color.Transparent, onTertiaryButtonDisabled = WireColorPalette.Gray60,
    tertiaryButtonSelected = WireColorPalette.DarkBlue800, onTertiaryButtonSelected = WireColorPalette.DarkBlue500,
    tertiaryButtonSelectedOutline = WireColorPalette.DarkBlue800,
    tertiaryButtonRipple = Color.White,

    // strokes and shadows
    outline = WireColorPalette.Gray90,
    divider = WireColorPalette.Gray100,
    scrim = WireColorPalette.BlackAlpha55,

    // accents
    groupAvatarColors = listOf(
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkPurple500, WireColorPalette.DarkRed500),
        GroupAvatarColors(WireColorPalette.DarkBlue500, WireColorPalette.DarkGreen500, WireColorPalette.DarkAmber500),
        GroupAvatarColors(WireColorPalette.DarkBlue500, WireColorPalette.DarkRed500, WireColorPalette.DarkPurple500),
        GroupAvatarColors(WireColorPalette.DarkAmber500, WireColorPalette.DarkGreen500, WireColorPalette.DarkPurple500),
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkBlue500, WireColorPalette.DarkPurple500),
        GroupAvatarColors(WireColorPalette.DarkBlue500, WireColorPalette.DarkAmber500, WireColorPalette.DarkGreen500),
        GroupAvatarColors(WireColorPalette.DarkRed500, WireColorPalette.DarkPetrol500, WireColorPalette.DarkPurple500), //
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkRed500, WireColorPalette.DarkBlue500),
        GroupAvatarColors(WireColorPalette.DarkAmber500, WireColorPalette.DarkBlue500, WireColorPalette.DarkPurple500),
        GroupAvatarColors(WireColorPalette.DarkPurple500, WireColorPalette.DarkPetrol500, WireColorPalette.DarkGreen500), //
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkPurple500, WireColorPalette.DarkRed500),
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkBlue500, WireColorPalette.DarkRed500),
        GroupAvatarColors(WireColorPalette.DarkPurple500, WireColorPalette.DarkBlue500, WireColorPalette.DarkGreen500),
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkBlue500, WireColorPalette.DarkAmber500),
        GroupAvatarColors(WireColorPalette.DarkGreen500, WireColorPalette.DarkAmber500, WireColorPalette.DarkPurple500)
    ),
    channelAvatarColors = listOf(
        ChannelAvatarColors(WireColorPalette.DarkBlue900, WireColorPalette.DarkBlue800, WireColorPalette.DarkBlue500),
        ChannelAvatarColors(WireColorPalette.DarkPurple900, WireColorPalette.DarkPurple800, WireColorPalette.DarkPurple500),
        ChannelAvatarColors(WireColorPalette.DarkRed900, WireColorPalette.DarkRed800, WireColorPalette.DarkRed500),
        ChannelAvatarColors(WireColorPalette.DarkGreen900, WireColorPalette.DarkGreen800, WireColorPalette.DarkGreen500),
        ChannelAvatarColors(WireColorPalette.DarkAmber900, WireColorPalette.DarkAmber800, WireColorPalette.DarkAmber500),
        ChannelAvatarColors(WireColorPalette.DarkPetrol900, WireColorPalette.DarkPetrol800, WireColorPalette.DarkPetrol500),
        ChannelAvatarColors(WireColorPalette.Gray90, Gray80, WireColorPalette.Gray40)
    ),
    wireAccentColors = WireAccentColors {
        when (it) {
            Accent.Amber -> WireColorPalette.DarkAmber500
            Accent.Blue -> WireColorPalette.DarkBlue500
            Accent.Green -> WireColorPalette.DarkGreen500
            Accent.Purple -> WireColorPalette.DarkPurple500
            Accent.Red -> WireColorPalette.DarkRed500
            Accent.Petrol -> WireColorPalette.DarkPetrol500
            Accent.Unknown -> WireColorPalette.DarkBlue500
        }
    },
    emojiBackgroundColor = Color.Black,
)

@PackagePrivate
val WireColorSchemeTypes: ThemeDependent<WireColorScheme> = ThemeDependent(
    light = LightWireColorScheme,
    dark = DarkWireColorScheme
)
