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
    val error: Color, val onError: Color,
    val errorOutline: Color,
    val warning: Color, val onWarning: Color,
    val positive: Color, val onPositive: Color,
    val background: Color, val onBackground: Color,
    val backgroundVariant: Color, val onBackgroundVariant: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
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
    val labelText: Color,
    val badge: Color, val onBadge: Color,
    val highLight: Color,
    val checkedCheckBoxBorderColor: Color,
    val uncheckedCheckBoxBorderColor: Color,
    val disabledIndeterminateCheckBoxBorderColor: Color,
    val disabledCheckBoxBorderColor: Color,
    val checkedBoxColor: Color,
    val uncheckedBoxColor: Color,
    val disabledCheckedBoxColor: Color,
    val disabledIndeterminateBoxColor: Color,
    val disabledUncheckedBoxColor: Color,
    val uncheckedCheckmarkColor: Color,
    val checkedCheckmarkColor: Color,
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
    val connectivityBarOngoingCallBackgroundColor: Color,
    val connectivityBarIssueBackgroundColor: Color,
    val connectivityBarTextColor: Color,
    val connectivityBarIconColor: Color,
    val messageComposerBackgroundColor: Color,
    val messageComposerEditBackgroundColor: Color,
    val messageMentionBackground: Color,
    val messageMentionText: Color
) {
    fun toColorScheme(): ColorScheme = ColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = secondaryButtonSelected, onPrimaryContainer = onSecondaryButtonSelected,
        inversePrimary = secondaryButtonSelected,
        secondary = badge, onSecondary = onBadge,
        secondaryContainer = backgroundVariant, onSecondaryContainer = onBackgroundVariant,
        tertiary = primary, onTertiary = onPrimary,
        tertiaryContainer = secondaryButtonSelected, onTertiaryContainer = onSecondaryButtonSelected,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onPrimaryButtonDisabled, inverseOnSurface = Color.White,
        error = error, onError = onError,
        errorContainer = errorOutline, onErrorContainer = error,
        outline = divider
    )
}

// Light WireColorScheme
private val LightWireColorScheme = WireColorScheme(
    useDarkSystemBarIcons = true,
    connectivityBarShouldUseDarkIcons = false,
    primary = WireColorPalette.LightBlue500, onPrimary = Color.White,
    error = WireColorPalette.LightRed500, onError = Color.White,
    errorOutline = WireColorPalette.LightRed200,
    warning = WireColorPalette.LightYellow500, onWarning = Color.White,
    positive = WireColorPalette.LightGreen500, onPositive = Color.White,
    background = WireColorPalette.Gray20, onBackground = Color.Black,
    backgroundVariant = WireColorPalette.Gray10, onBackgroundVariant = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color.White, onSurfaceVariant = Color.Black,
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
    divider = WireColorPalette.Gray40,
    secondaryText = WireColorPalette.Gray70,
    labelText = WireColorPalette.Gray80,
    badge = WireColorPalette.Gray90, onBadge = Color.White,
    highLight = WireColorPalette.DarkYellow300,
    checkedCheckBoxBorderColor = WireColorPalette.LightBlue500,
    uncheckedCheckBoxBorderColor = WireColorPalette.Gray80,
    disabledIndeterminateCheckBoxBorderColor = WireColorPalette.Gray80,
    disabledCheckBoxBorderColor = WireColorPalette.Gray80,
    checkedBoxColor = WireColorPalette.LightBlue500,
    uncheckedBoxColor = WireColorPalette.Gray20,
    disabledCheckedBoxColor = WireColorPalette.Gray80,
    disabledIndeterminateBoxColor = WireColorPalette.Gray80,
    disabledUncheckedBoxColor = WireColorPalette.Gray80,
    uncheckedCheckmarkColor = WireColorPalette.Gray20,
    checkedCheckmarkColor = Color.White,
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
        WireColorPalette.LightYellow300,
        WireColorPalette.LightYellow500,
        WireColorPalette.LightYellow700,
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
    connectivityBarOngoingCallBackgroundColor = WireColorPalette.DarkGreen700,
    connectivityBarIssueBackgroundColor = WireColorPalette.LightBlue500,
    connectivityBarTextColor = Color.White,
    connectivityBarIconColor = Color.White,
    messageComposerBackgroundColor = Color.White,
    messageComposerEditBackgroundColor = WireColorPalette.LightBlue50,
    messageMentionBackground = WireColorPalette.messageMentionBackground,
    messageMentionText = WireColorPalette.messageMentionText
)

// Dark WireColorScheme
private val DarkWireColorScheme = WireColorScheme(
    useDarkSystemBarIcons = false,
    connectivityBarShouldUseDarkIcons = true,
    primary = WireColorPalette.DarkBlue500, onPrimary = Color.Black,
    error = WireColorPalette.DarkRed500, onError = Color.Black,
    errorOutline = WireColorPalette.DarkRed200,
    warning = WireColorPalette.DarkYellow500, onWarning = Color.Black,
    positive = WireColorPalette.DarkGreen500, onPositive = Color.Black,
    background = WireColorPalette.Gray100, onBackground = Color.White,
    backgroundVariant = WireColorPalette.Gray95, onBackgroundVariant = Color.White,
    surface = WireColorPalette.Gray95, onSurface = Color.White,
    surfaceVariant = WireColorPalette.Gray90, onSurfaceVariant = Color.White,
    primaryButtonEnabled = WireColorPalette.DarkBlue500, onPrimaryButtonEnabled = Color.Black,
    primaryButtonDisabled = WireColorPalette.Gray70, onPrimaryButtonDisabled = Color.Black,
    primaryButtonSelected = WireColorPalette.DarkBlue400, onPrimaryButtonSelected = Color.Black,
    primaryButtonRipple = Color.White,
    secondaryButtonEnabled = WireColorPalette.Gray90, onSecondaryButtonEnabled = Color.White,
    secondaryButtonEnabledOutline = WireColorPalette.Gray100,
    secondaryButtonDisabled = WireColorPalette.Gray95, onSecondaryButtonDisabled = WireColorPalette.Gray70,
    secondaryButtonDisabledOutline = WireColorPalette.Gray90,
    secondaryButtonSelected = WireColorPalette.DarkBlue800, onSecondaryButtonSelected = Color.White,
    secondaryButtonSelectedOutline = WireColorPalette.DarkBlue700,
    secondaryButtonRipple = Color.White,
    tertiaryButtonEnabled = Color.Transparent, onTertiaryButtonEnabled = Color.White,
    tertiaryButtonDisabled = Color.Transparent, onTertiaryButtonDisabled = WireColorPalette.Gray60,
    tertiaryButtonSelected = WireColorPalette.DarkBlue50, onTertiaryButtonSelected = WireColorPalette.DarkBlue500,
    tertiaryButtonSelectedOutline = WireColorPalette.DarkBlue300,
    tertiaryButtonRipple = Color.White,
    switchEnabledThumb = Color.Black, switchDisabledThumb = WireColorPalette.Gray90,
    switchEnabledChecked = WireColorPalette.DarkGreen500, switchDisabledChecked = WireColorPalette.DarkGreen200,
    switchEnabledUnchecked = WireColorPalette.Gray40, switchDisabledUnchecked = WireColorPalette.Gray60,
    divider = WireColorPalette.Gray80,
    secondaryText = WireColorPalette.Gray60,
    labelText = WireColorPalette.Gray30,
    badge = WireColorPalette.Gray10, onBadge = Color.Black,
    highLight = WireColorPalette.DarkYellow300,
    checkedCheckBoxBorderColor = WireColorPalette.LightBlue500,
    uncheckedCheckBoxBorderColor = WireColorPalette.Gray80,
    disabledIndeterminateCheckBoxBorderColor = WireColorPalette.Gray80,
    disabledCheckBoxBorderColor = WireColorPalette.Gray80,
    checkedBoxColor = WireColorPalette.LightBlue500,
    uncheckedBoxColor = WireColorPalette.Gray20,
    disabledCheckedBoxColor = WireColorPalette.Gray80,
    disabledIndeterminateBoxColor = WireColorPalette.Gray80,
    disabledUncheckedBoxColor = WireColorPalette.Gray80,
    uncheckedCheckmarkColor = WireColorPalette.Gray20,
    checkedCheckmarkColor = Color.White,
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
        WireColorPalette.DarkYellow300,
        WireColorPalette.DarkYellow500,
        WireColorPalette.DarkYellow700,
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
    connectivityBarOngoingCallBackgroundColor = WireColorPalette.DarkGreen700,
    connectivityBarIssueBackgroundColor = WireColorPalette.LightBlue500,
    connectivityBarTextColor = Color.White,
    connectivityBarIconColor = Color.White,
    messageComposerBackgroundColor = WireColorPalette.Gray100,
    messageComposerEditBackgroundColor = WireColorPalette.DarkBlue800,
    messageMentionBackground = WireColorPalette.messageMentionBackground,
    messageMentionText = WireColorPalette.messageMentionText
)

@PackagePrivate
val WireColorSchemeTypes: ThemeDependent<WireColorScheme> = ThemeDependent(
    light = LightWireColorScheme,
    dark = DarkWireColorScheme
)
