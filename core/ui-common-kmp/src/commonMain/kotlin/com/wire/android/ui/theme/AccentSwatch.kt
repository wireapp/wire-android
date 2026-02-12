/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

@file:Suppress("ParameterListWrapping", "Wrapping")

package com.wire.android.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

enum class Tone {
    T50, T100, T200, T300, T400, T500, T600, T700, T800, T900,
    Black, White,
    G10, G20, G30, G40, G50, G60, G70, G80, G90, G95, G100
}

@Stable
data class Shades(
    val t50: Color, val t100: Color, val t200: Color, val t300: Color, val t400: Color,
    val t500: Color, val t600: Color, val t700: Color, val t800: Color, val t900: Color,
    val g10: Color = WireColorPalette.Gray10, val g20: Color = WireColorPalette.Gray20,
    val g30: Color = WireColorPalette.Gray30, val g40: Color = WireColorPalette.Gray40,
    val g50: Color = WireColorPalette.Gray50, val g60: Color = WireColorPalette.Gray60,
    val g70: Color = WireColorPalette.Gray70, val g80: Color = WireColorPalette.Gray80,
    val g90: Color = WireColorPalette.Gray90, val g95: Color = WireColorPalette.Gray95,
    val g100: Color = WireColorPalette.Gray100,
    val black: Color = Color.Black, val white: Color = Color.White,
) {
    @Suppress("CyclomaticComplexMethod")
    operator fun get(t: Tone) = when (t) {
        Tone.T50 -> t50; Tone.T100 -> t100; Tone.T200 -> t200; Tone.T300 -> t300; Tone.T400 -> t400
        Tone.T500 -> t500; Tone.T600 -> t600; Tone.T700 -> t700; Tone.T800 -> t800; Tone.T900 -> t900
        Tone.G10 -> g10; Tone.G20 -> g20; Tone.G30 -> g30; Tone.G40 -> g40; Tone.G50 -> g50
        Tone.G60 -> g60; Tone.G70 -> g70; Tone.G80 -> g80; Tone.G90 -> g90; Tone.G95 -> g95; Tone.G100 -> g100
        Tone.Black -> black; Tone.White -> white
    }
}

@Stable
data class AccentSwatch(val light: Shades, val dark: Shades)
object AccentSwatches {
    val Blue = AccentSwatch(
        light = Shades(
            WireColorPalette.LightBlue50, WireColorPalette.LightBlue100, WireColorPalette.LightBlue200,
            WireColorPalette.LightBlue300, WireColorPalette.LightBlue400, WireColorPalette.LightBlue500,
            WireColorPalette.LightBlue600, WireColorPalette.LightBlue700, WireColorPalette.LightBlue800,
            WireColorPalette.LightBlue900
        ),
        dark = Shades(
            WireColorPalette.DarkBlue50, WireColorPalette.DarkBlue100, WireColorPalette.DarkBlue200,
            WireColorPalette.DarkBlue300, WireColorPalette.DarkBlue400, WireColorPalette.DarkBlue500,
            WireColorPalette.DarkBlue600, WireColorPalette.DarkBlue700, WireColorPalette.DarkBlue800,
            WireColorPalette.DarkBlue900
        )
    )
    val Green = AccentSwatch(
        light = Shades(
            WireColorPalette.LightGreen50, WireColorPalette.LightGreen100, WireColorPalette.LightGreen200,
            WireColorPalette.LightGreen300, WireColorPalette.LightGreen400, WireColorPalette.LightGreen500,
            WireColorPalette.LightGreen600, WireColorPalette.LightGreen700, WireColorPalette.LightGreen800,
            WireColorPalette.LightGreen900
        ),
        dark = Shades(
            WireColorPalette.DarkGreen50, WireColorPalette.DarkGreen100, WireColorPalette.DarkGreen200,
            WireColorPalette.DarkGreen300, WireColorPalette.DarkGreen400, WireColorPalette.DarkGreen500,
            WireColorPalette.DarkGreen600, WireColorPalette.DarkGreen700, WireColorPalette.DarkGreen800,
            WireColorPalette.DarkGreen900
        )
    )
    val Petrol = AccentSwatch(
        light = Shades(
            WireColorPalette.LightPetrol50, WireColorPalette.LightPetrol100, WireColorPalette.LightPetrol200,
            WireColorPalette.LightPetrol300, WireColorPalette.LightPetrol400, WireColorPalette.LightPetrol500,
            WireColorPalette.LightPetrol600, WireColorPalette.LightPetrol700, WireColorPalette.LightPetrol800,
            WireColorPalette.LightPetrol900
        ),
        dark = Shades(
            WireColorPalette.DarkPetrol50, WireColorPalette.DarkPetrol100, WireColorPalette.DarkPetrol200,
            WireColorPalette.DarkPetrol300, WireColorPalette.DarkPetrol400, WireColorPalette.DarkPetrol500,
            WireColorPalette.DarkPetrol600, WireColorPalette.DarkPetrol700, WireColorPalette.DarkPetrol800,
            WireColorPalette.DarkPetrol900
        )
    )
    val Purple = AccentSwatch(
        light = Shades(
            WireColorPalette.LightPurple50, WireColorPalette.LightPurple100, WireColorPalette.LightPurple200,
            WireColorPalette.LightPurple300, WireColorPalette.LightPurple400, WireColorPalette.LightPurple500,
            WireColorPalette.LightPurple600, WireColorPalette.LightPurple700, WireColorPalette.LightPurple800,
            WireColorPalette.LightPurple900
        ),
        dark = Shades(
            WireColorPalette.DarkPurple50, WireColorPalette.DarkPurple100, WireColorPalette.DarkPurple200,
            WireColorPalette.DarkPurple300, WireColorPalette.DarkPurple400, WireColorPalette.DarkPurple500,
            WireColorPalette.DarkPurple600, WireColorPalette.DarkPurple700, WireColorPalette.DarkPurple800,
            WireColorPalette.DarkPurple900
        )
    )
    val Red = AccentSwatch(
        light = Shades(
            WireColorPalette.LightRed50, WireColorPalette.LightRed100, WireColorPalette.LightRed200,
            WireColorPalette.LightRed300, WireColorPalette.LightRed400, WireColorPalette.LightRed500,
            WireColorPalette.LightRed600, WireColorPalette.LightRed700, WireColorPalette.LightRed800,
            WireColorPalette.LightRed900
        ),
        dark = Shades(
            WireColorPalette.DarkRed50, WireColorPalette.DarkRed100, WireColorPalette.DarkRed200,
            WireColorPalette.DarkRed300, WireColorPalette.DarkRed400, WireColorPalette.DarkRed500,
            WireColorPalette.DarkRed600, WireColorPalette.DarkRed700, WireColorPalette.DarkRed800,
            WireColorPalette.DarkRed900
        )
    )
    val Amber = AccentSwatch(
        light = Shades(
            WireColorPalette.LightAmber50, WireColorPalette.LightAmber100, WireColorPalette.LightAmber200,
            WireColorPalette.LightAmber300, WireColorPalette.LightAmber400, WireColorPalette.LightAmber500,
            WireColorPalette.LightAmber600, WireColorPalette.LightAmber700, WireColorPalette.LightAmber800,
            WireColorPalette.LightAmber900
        ),
        dark = Shades(
            WireColorPalette.DarkAmber50, WireColorPalette.DarkAmber100, WireColorPalette.DarkAmber200,
            WireColorPalette.DarkAmber300, WireColorPalette.DarkAmber400, WireColorPalette.DarkAmber500,
            WireColorPalette.DarkAmber600, WireColorPalette.DarkAmber700, WireColorPalette.DarkAmber800,
            WireColorPalette.DarkAmber900
        )
    )
}

private fun Accent.asSwatch(): AccentSwatch = when (this) {
    Accent.Blue -> AccentSwatches.Blue
    Accent.Green -> AccentSwatches.Green
    Accent.Petrol -> AccentSwatches.Petrol
    Accent.Purple -> AccentSwatches.Purple
    Accent.Red -> AccentSwatches.Red
    Accent.Amber -> AccentSwatches.Amber
    Accent.Unknown -> AccentSwatches.Blue
}

data class AccentRoleMap(
    // Base
    val primary: Tone,
    val primaryFocus: Tone,
    val primaryVariant: Tone,
    val onPrimaryVariant: Tone,
    val focus: Tone,

    // Backgrounds
    val primaryContainer: Tone,

    // Primary buttons
    val primaryButtonEnabled: Tone,
    val primaryButtonFocus: Tone,
    val primaryButtonSelected: Tone,

    // Secondary buttons
    val secondaryButtonFocus: Tone,
    val secondaryButtonFocusOutline: Tone,
    val secondaryButtonSelected: Tone,
    val secondaryButtonSelectedOutline: Tone,
    val onSecondaryButtonSelected: Tone,

    // Tertiary buttons
    val tertiaryButtonFocusOutline: Tone,
    val tertiaryButtonSelected: Tone,
    val tertiaryButtonSelectedOutline: Tone,
    val onTertiaryButtonSelected: Tone,

    // Avatars
    val groupAvatar: Tone,
    val channelAvatarOutline: Tone,
    val channelAvatarBackground: Tone,
    val channelOnAvatarBackground: Tone,

    // Chat Bubbles
    val bubbleSelfPrimary: Tone,
    val bubbleSelfSecondary: Tone,
    val bubbleSelfPrimaryOnSecondary: Tone,
    val bubbleOtherPrimaryOnSecondary: Tone
)

private val LightRoles = AccentRoleMap(
    primary = Tone.T500,
    primaryFocus = Tone.T700,
    primaryVariant = Tone.T50,
    onPrimaryVariant = Tone.T500,
    focus = Tone.T300,

    primaryContainer = Tone.T400,

    primaryButtonEnabled = Tone.T500,
    primaryButtonFocus = Tone.T700,
    primaryButtonSelected = Tone.T700,

    secondaryButtonFocus = Tone.G30,
    secondaryButtonFocusOutline = Tone.T500,
    secondaryButtonSelected = Tone.T50,
    secondaryButtonSelectedOutline = Tone.T300,
    onSecondaryButtonSelected = Tone.T500,

    tertiaryButtonFocusOutline = Tone.T500,
    tertiaryButtonSelected = Tone.T50,
    tertiaryButtonSelectedOutline = Tone.T300,
    onTertiaryButtonSelected = Tone.T500,

    groupAvatar = Tone.T500,
    channelAvatarOutline = Tone.T50,
    channelAvatarBackground = Tone.T100,
    channelOnAvatarBackground = Tone.T500,

    bubbleSelfPrimary = Tone.T500,
    bubbleSelfSecondary = Tone.T600,
    bubbleSelfPrimaryOnSecondary = Tone.T200,
    bubbleOtherPrimaryOnSecondary = Tone.T500,
)
private val DarkRoles = AccentRoleMap(
    primary = Tone.T500,
    primaryFocus = Tone.T300,
    primaryVariant = Tone.T800,
    onPrimaryVariant = Tone.T300,
    focus = Tone.T100,

    primaryContainer = Tone.T400, // TODO for bubbles should be 800 or 900

    primaryButtonEnabled = Tone.T500,
    primaryButtonFocus = Tone.T400, // TODO should be darker
    primaryButtonSelected = Tone.T400,

    secondaryButtonFocus = Tone.T800,
    secondaryButtonFocusOutline = Tone.T500,
    secondaryButtonSelected = Tone.T800,
    secondaryButtonSelectedOutline = Tone.T500,
    onSecondaryButtonSelected = Tone.White,

    tertiaryButtonFocusOutline = Tone.T500,
    tertiaryButtonSelected = Tone.G95,
    tertiaryButtonSelectedOutline = Tone.G90,
    onTertiaryButtonSelected = Tone.T500,

    groupAvatar = Tone.T500,
    channelAvatarOutline = Tone.T50,
    channelAvatarBackground = Tone.T100,
    channelOnAvatarBackground = Tone.T500,

    bubbleSelfPrimary = Tone.T800,
    bubbleSelfSecondary = Tone.T900,
    bubbleSelfPrimaryOnSecondary = Tone.T400,
    bubbleOtherPrimaryOnSecondary = Tone.T500,
)

fun WireColorScheme.withAccent(accent: Accent): WireColorScheme {
    val swatch = accent.asSwatch().let { if (useDarkSystemBarIcons) it.light else it.dark }
    val roles = if (useDarkSystemBarIcons) LightRoles else DarkRoles

    return copy(
        primary = swatch[roles.primary],

        primaryVariant = swatch[roles.primaryVariant],
        onPrimaryVariant = swatch[roles.onPrimaryVariant],
        primaryButtonSelected = swatch[roles.primaryButtonSelected],
        primaryButtonRipple = swatch[roles.primaryButtonFocus],

        primaryButtonEnabled = swatch[roles.primaryButtonEnabled],
        onPrimaryButtonSelected = swatch[roles.primaryButtonEnabled],

        secondaryButtonSelected = swatch[roles.secondaryButtonSelected],
        onSecondaryButtonSelected = swatch[roles.onSecondaryButtonSelected],
        secondaryButtonSelectedOutline = swatch[roles.secondaryButtonSelectedOutline],
        secondaryButtonRipple = swatch[roles.secondaryButtonFocus],

        tertiaryButtonSelected = swatch[roles.tertiaryButtonSelected],
        onTertiaryButtonSelected = swatch[roles.onTertiaryButtonSelected],
        tertiaryButtonSelectedOutline = swatch[roles.tertiaryButtonSelectedOutline],
        tertiaryButtonRipple = swatch[roles.tertiaryButtonFocusOutline],
        selfBubble = selfBubble.copy(
            primary = swatch[roles.bubbleSelfPrimary],
            secondary = swatch[roles.bubbleSelfSecondary],
            primaryOnSecondary = swatch[roles.bubbleSelfPrimaryOnSecondary]
        ),
        otherBubble = otherBubble.copy(
            primaryOnSecondary = swatch[roles.bubbleOtherPrimaryOnSecondary]
        )
    )
}
