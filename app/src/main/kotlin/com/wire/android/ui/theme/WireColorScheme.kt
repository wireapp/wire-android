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
import io.github.esentsov.PackagePrivate

@Immutable
data class WireColorScheme(
    val useDarkSystemBarIcons: Boolean,
    val connectivityBarShouldUseDarkIcons: Boolean,
    val primary: Color, val onPrimary: Color,
    val primaryVariant: Color, val onPrimaryVariant: Color,
    val error: Color, val onError: Color,
    val errorOutline: Color,
    val warning: Color, val onWarning: Color,
    val positive: Color, val onPositive: Color,
    val background: Color, val onBackground: Color,
    val backgroundVariant: Color, val onBackgroundVariant: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val inverted: Color, val onInverted: Color,
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
    val switchEnabledThumb: Color, val switchDisabledThumb: Color,
    val switchEnabledChecked: Color, val switchDisabledChecked: Color,
    val switchEnabledUnchecked: Color, val switchDisabledUnchecked: Color,
    val divider: Color,
    val secondaryText: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val labelText: Color,
    val badge: Color, val onBadge: Color,
    val highlight: Color, val onHighlight: Color,
    val uncheckedColor: Color,
    val disabledCheckedColor: Color,
    val disabledIndeterminateColor: Color,
    val disabledUncheckedColor: Color,
    val messageErrorBackgroundColor: Color,
    val muteButtonColor: Color,
    val groupAvatarColors: List<Color>,
    val callingParticipantTileBackgroundColor: Color,
    val callingPagerIndicatorBackground: Color,
    val callingActiveIndicator: Color,
    val callingInActiveIndicator: Color,
    val callingInActiveBorderIndicator: Color,
    val callingControlButtonActive: Color,
    val callingControlButtonActiveOutline: Color,
    val onCallingControlButtonActive: Color,
    val callingControlButtonInactive: Color,
    val callingControlButtonInactiveOutline: Color,
    val onCallingControlButtonInactive: Color,
    val callingHangupButtonColor: Color,
    val onCallingHangupButtonColor: Color,
    val callingAnswerButtonColor: Color,
    val onCallingAnswerButtonColor: Color,
    val connectivityBarIssueBackgroundColor: Color,
    val messageComposerBackgroundColor: Color,
    val messageComposerEditBackgroundColor: Color,
    val classifiedBannerBackgroundColor: Color,
    val classifiedBannerForegroundColor: Color,
    val unclassifiedBannerBackgroundColor: Color,
    val unclassifiedBannerForegroundColor: Color,
    val recordAudioStartColor: Color,
    val recordAudioStopColor: Color,
    val scrollToBottomButtonColor: Color,
    val onScrollToBottomButtonColor: Color,
    val validE2eiStatusColor: Color,
    val mlsVerificationTextColor: Color,
    val wireAccentColors: WireAccentColors,
    val checkboxTextDisabled: Color
) {
    fun toColorScheme(): ColorScheme = ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = secondaryButtonSelected,
        onPrimaryContainer = onSecondaryButtonSelected,
        inversePrimary = secondaryButtonSelected,
        secondary = badge,
        onSecondary = onBadge,
        secondaryContainer = backgroundVariant,
        onSecondaryContainer = onBackgroundVariant,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = secondaryButtonSelected,
        onTertiaryContainer = onSecondaryButtonSelected,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onPrimaryButtonDisabled, inverseOnSurface = Color.White,
        error = error, onError = onError,
        errorContainer = errorOutline, onErrorContainer = error,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim
    )
}

// Light WireColorScheme
private val LightWireColorScheme = WireColorScheme(
    useDarkSystemBarIcons = true,
    connectivityBarShouldUseDarkIcons = false,
    primary = WireColorPalette.LightBlue500, onPrimary = Color.White,
    primaryVariant = WireColorPalette.LightBlue50, onPrimaryVariant = WireColorPalette.LightBlue500,
    error = WireColorPalette.LightRed500, onError = Color.White,
    errorOutline = WireColorPalette.LightRed200,
    warning = WireColorPalette.LightAmber500, onWarning = Color.White,
    positive = WireColorPalette.LightGreen500, onPositive = Color.White,
    background = WireColorPalette.Gray20, onBackground = Color.Black,
    backgroundVariant = WireColorPalette.Gray10, onBackgroundVariant = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color.White, onSurfaceVariant = Color.Black,
    inverted = Color.Black, onInverted = Color.White,
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
    switchEnabledThumb = Color.White, switchDisabledThumb = WireColorPalette.Gray20,
    switchEnabledChecked = WireColorPalette.LightGreen500, switchDisabledChecked = WireColorPalette.LightGreen200,
    switchEnabledUnchecked = WireColorPalette.Gray70, switchDisabledUnchecked = WireColorPalette.Gray50,
    divider = WireColorPalette.Gray20,
    secondaryText = WireColorPalette.Gray70,
    outline = WireColorPalette.Gray40,
    outlineVariant = WireColorPalette.Gray20,
    scrim = WireColorPalette.BlackAlpha55,
    labelText = WireColorPalette.Gray80,
    badge = WireColorPalette.Gray90, onBadge = Color.White,
    highlight = WireColorPalette.DarkAmber200, onHighlight = Color.Black,
    uncheckedColor = WireColorPalette.Gray80,
    disabledCheckedColor = WireColorPalette.Gray80,
    disabledIndeterminateColor = WireColorPalette.Gray80,
    disabledUncheckedColor = WireColorPalette.Gray80,
    messageErrorBackgroundColor = WireColorPalette.DarkRed50,
    muteButtonColor = WireColorPalette.DarkRed500,
    groupAvatarColors = listOf(
        // Red
        WireColorPalette.LightRed300,
        WireColorPalette.LightRed500,
        WireColorPalette.LightRed700,
        // Green
        WireColorPalette.LightGreen300,
        WireColorPalette.LightGreen500,
        WireColorPalette.LightGreen700,
        // Blue
        WireColorPalette.LightBlue300,
        WireColorPalette.LightBlue500,
        WireColorPalette.LightBlue700,
        // Purple
        WireColorPalette.LightPurple300,
        WireColorPalette.LightPurple500,
        WireColorPalette.LightPurple700,
        // Yellow - Amber
        WireColorPalette.LightAmber300,
        WireColorPalette.LightAmber500,
        WireColorPalette.LightAmber700,
        // Petrol
        WireColorPalette.LightPetrol300,
        WireColorPalette.LightPetrol500,
        WireColorPalette.LightPetrol700,
        // Gray
        WireColorPalette.Gray30,
        WireColorPalette.Gray50,
        WireColorPalette.Gray70,
    ),
    callingParticipantTileBackgroundColor = WireColorPalette.Gray90,
    callingPagerIndicatorBackground = WireColorPalette.Gray40,
    callingActiveIndicator = WireColorPalette.LightBlue500,
    callingInActiveIndicator = Color.White,
    callingInActiveBorderIndicator = WireColorPalette.Gray60,
    callingControlButtonActive = Color.Black,
    callingControlButtonActiveOutline = Color.Black,
    onCallingControlButtonActive = Color.White,
    callingControlButtonInactive = Color.White,
    callingControlButtonInactiveOutline = WireColorPalette.Gray40,
    onCallingControlButtonInactive = WireColorPalette.Gray90,
    callingHangupButtonColor = WireColorPalette.LightRed500,
    onCallingHangupButtonColor = Color.White,
    callingAnswerButtonColor = WireColorPalette.LightGreen500,
    onCallingAnswerButtonColor = Color.White,
    connectivityBarIssueBackgroundColor = WireColorPalette.LightBlue500,
    messageComposerBackgroundColor = Color.White,
    messageComposerEditBackgroundColor = WireColorPalette.LightBlue50,
    classifiedBannerBackgroundColor = WireColorPalette.LightGreen50,
    classifiedBannerForegroundColor = WireColorPalette.LightGreen500,
    unclassifiedBannerBackgroundColor = WireColorPalette.LightRed600,
    unclassifiedBannerForegroundColor = Color.White,
    recordAudioStartColor = WireColorPalette.LightBlue500,
    recordAudioStopColor = WireColorPalette.LightRed500,
    scrollToBottomButtonColor = WireColorPalette.Gray70,
    onScrollToBottomButtonColor = Color.White,
    validE2eiStatusColor = WireColorPalette.LightGreen550,
    mlsVerificationTextColor = WireColorPalette.DarkGreen700,
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
    checkboxTextDisabled = WireColorPalette.Gray70
)

// Dark WireColorScheme
private val DarkWireColorScheme = WireColorScheme(
    useDarkSystemBarIcons = false,
    connectivityBarShouldUseDarkIcons = true,
    primary = WireColorPalette.DarkBlue500, onPrimary = Color.Black,
    primaryVariant = WireColorPalette.DarkBlue800, onPrimaryVariant = WireColorPalette.DarkBlue300,
    error = WireColorPalette.DarkRed500, onError = Color.Black,
    errorOutline = WireColorPalette.DarkRed800,
    warning = WireColorPalette.DarkAmber500, onWarning = Color.Black,
    positive = WireColorPalette.DarkGreen500, onPositive = Color.Black,
    background = WireColorPalette.Gray100, onBackground = Color.White,
    backgroundVariant = WireColorPalette.Gray95, onBackgroundVariant = Color.White,
    surface = WireColorPalette.Gray95, onSurface = Color.White,
    surfaceVariant = WireColorPalette.Gray90, onSurfaceVariant = Color.White,
    inverted = Color.White, onInverted = Color.Black,
    primaryButtonEnabled = WireColorPalette.DarkBlue500, onPrimaryButtonEnabled = Color.Black,
    primaryButtonDisabled = WireColorPalette.Gray70, onPrimaryButtonDisabled = Color.Black,
    primaryButtonSelected = WireColorPalette.DarkBlue400, onPrimaryButtonSelected = Color.Black,
    primaryButtonRipple = Color.White,
    secondaryButtonEnabled = WireColorPalette.Gray90, onSecondaryButtonEnabled = Color.White,
    secondaryButtonEnabledOutline = WireColorPalette.Gray100,
    secondaryButtonDisabled = WireColorPalette.Gray95, onSecondaryButtonDisabled = WireColorPalette.Gray70,
    secondaryButtonDisabledOutline = WireColorPalette.Gray80,
    secondaryButtonSelected = WireColorPalette.DarkBlue800, onSecondaryButtonSelected = Color.White,
    secondaryButtonSelectedOutline = WireColorPalette.DarkBlue600,
    secondaryButtonRipple = Color.White,
    tertiaryButtonEnabled = Color.Transparent, onTertiaryButtonEnabled = Color.White,
    tertiaryButtonDisabled = Color.Transparent, onTertiaryButtonDisabled = WireColorPalette.Gray60,
    tertiaryButtonSelected = WireColorPalette.DarkBlue50, onTertiaryButtonSelected = WireColorPalette.DarkBlue500,
    tertiaryButtonSelectedOutline = WireColorPalette.DarkBlue300,
    tertiaryButtonRipple = Color.White,
    switchEnabledThumb = Color.Black, switchDisabledThumb = WireColorPalette.Gray90,
    switchEnabledChecked = WireColorPalette.DarkGreen500, switchDisabledChecked = WireColorPalette.DarkGreen200,
    switchEnabledUnchecked = WireColorPalette.Gray40, switchDisabledUnchecked = WireColorPalette.Gray60,
    divider = WireColorPalette.Gray100,
    secondaryText = WireColorPalette.Gray60,
    outline = WireColorPalette.Gray90,
    outlineVariant = WireColorPalette.Gray100,
    scrim = WireColorPalette.BlackAlpha55,
    labelText = WireColorPalette.Gray30,
    badge = WireColorPalette.Gray10, onBadge = Color.Black,
    highlight = WireColorPalette.DarkAmber300, onHighlight = Color.Black,
    uncheckedColor = WireColorPalette.Gray60,
    disabledCheckedColor = WireColorPalette.Gray80,
    disabledIndeterminateColor = WireColorPalette.Gray80,
    disabledUncheckedColor = WireColorPalette.Gray80,
    messageErrorBackgroundColor = WireColorPalette.GrayRed900,
    muteButtonColor = WireColorPalette.DarkRed500,
    groupAvatarColors = listOf(
        // Red
        WireColorPalette.DarkRed300,
        WireColorPalette.DarkRed500,
        WireColorPalette.DarkRed700,
        // Green
        WireColorPalette.DarkGreen300,
        WireColorPalette.DarkGreen500,
        WireColorPalette.DarkGreen700,
        // Blue
        WireColorPalette.DarkBlue300,
        WireColorPalette.DarkBlue500,
        WireColorPalette.DarkBlue700,
        // Purple
        WireColorPalette.DarkPurple300,
        WireColorPalette.DarkPurple500,
        WireColorPalette.DarkPurple700,
        // Yellow - Amber
        WireColorPalette.DarkAmber300,
        WireColorPalette.DarkAmber500,
        WireColorPalette.DarkAmber700,
        // Petrol
        WireColorPalette.DarkPetrol300,
        WireColorPalette.DarkPetrol500,
        WireColorPalette.DarkPetrol700,
        // Gray
        WireColorPalette.Gray50,
        WireColorPalette.Gray70,
        WireColorPalette.Gray90,
    ),
    callingParticipantTileBackgroundColor = WireColorPalette.Gray95,
    callingPagerIndicatorBackground = WireColorPalette.Gray40,
    callingActiveIndicator = WireColorPalette.LightBlue500,
    callingInActiveIndicator = Color.White,
    callingInActiveBorderIndicator = WireColorPalette.Gray60,
    callingControlButtonActive = Color.White,
    callingControlButtonActiveOutline = Color.White,
    onCallingControlButtonActive = Color.Black,
    callingControlButtonInactive = WireColorPalette.Gray90,
    callingControlButtonInactiveOutline = WireColorPalette.Gray100,
    onCallingControlButtonInactive = Color.White,
    callingHangupButtonColor = WireColorPalette.DarkRed500,
    onCallingHangupButtonColor = Color.Black,
    callingAnswerButtonColor = WireColorPalette.DarkGreen500,
    onCallingAnswerButtonColor = Color.Black,
    connectivityBarIssueBackgroundColor = WireColorPalette.LightBlue500,
    messageComposerBackgroundColor = WireColorPalette.Gray100,
    messageComposerEditBackgroundColor = WireColorPalette.DarkBlue800,
    classifiedBannerBackgroundColor = WireColorPalette.DarkGreen900,
    classifiedBannerForegroundColor = WireColorPalette.DarkGreen500,
    unclassifiedBannerBackgroundColor = WireColorPalette.DarkRed500,
    unclassifiedBannerForegroundColor = Color.Black,
    recordAudioStartColor = WireColorPalette.DarkBlue500,
    recordAudioStopColor = WireColorPalette.DarkRed500,
    scrollToBottomButtonColor = WireColorPalette.Gray60,
    onScrollToBottomButtonColor = Color.Black,
    validE2eiStatusColor = WireColorPalette.DarkGreen500,
    mlsVerificationTextColor = WireColorPalette.DarkGreen700,
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
    checkboxTextDisabled = WireColorPalette.Gray70
)

@PackagePrivate
val WireColorSchemeTypes: ThemeDependent<WireColorScheme> = ThemeDependent(
    light = LightWireColorScheme,
    dark = DarkWireColorScheme
)
