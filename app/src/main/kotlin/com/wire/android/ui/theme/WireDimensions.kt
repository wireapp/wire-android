package com.wire.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.esentsov.PackagePrivate

@Immutable
data class WireDimensions(
    // Avatar
    val userAvatarDefaultSize: Dp,
    val userAvatarDefaultBigSize: Dp,
    val userAvatarClickablePadding: Dp,
    val userAvatarStatusSize: Dp,
    val userAvatarBusyVerticalPadding: Dp,
    val userAvatarBusyHorizontalPadding: Dp,
    val userAvatarStatusBorderSize: Dp,
    val groupAvatarCornerRadius: Dp,
    val groupAvatarSize: Dp,
    // BottomNavigation
    val bottomNavigationHorizontalPadding: Dp,
    val bottomNavigationVerticalPadding: Dp,
    val bottomNavigationBetweenItemsPadding: Dp,
    val bottomNavigationItemPadding: Dp,
    // Conversation
    val conversationItemRowHeight: Dp,
    val conversationItemPadding: Dp,
    // Message
    val messageImagePortraitModeWidth: Dp,
    // TextFields
    val textFieldMinHeight: Dp,
    val textFieldCornerSize: Dp,
    // Buttons
    val buttonMinHeight: Dp,
    val buttonMinWidth: Dp,
    val buttonHorizontalContentPadding: Dp,
    val buttonVerticalContentPadding: Dp,
    val buttonCornerSize: Dp,
    // Dialog
    val dialogButtonsSpacing: Dp,
    val dialogTextsSpacing: Dp,
    val dialogContentPadding: Dp,
    val dialogCornerSize: Dp,
    val dialogCardMargin: Dp,
    // UserProfile
    val userProfileLogoutBtnHeight: Dp,
    val userProfileStatusBtnHeight: Dp,
    val userProfileOtherAccItemHeight: Dp,
    // Spacing
    val spacing4x: Dp,
    val spacing8x: Dp,
    val spacing16x: Dp,
    val spacing24x: Dp,
    val spacing32x: Dp,
    val spacing40x: Dp,
    val spacing48x: Dp,
    val spacing56x: Dp,
    val spacing64x: Dp,
    // Corners
    val corner2x: Dp,
    val corner4x: Dp,
    val corner6x: Dp,
    val corner8x: Dp,
    val corner10x: Dp,
    val corner12x: Dp,
    val corner14x: Dp,
    val corner16x: Dp,
)

private val DefaultPhonePortraitWireDimensions: WireDimensions = WireDimensions(
    userAvatarDefaultSize = 32.dp,
    userAvatarDefaultBigSize = 160.dp,
    userAvatarClickablePadding = 6.dp,
    userAvatarStatusSize = 16.dp,
    userAvatarStatusBorderSize = 2.dp,
    userAvatarBusyVerticalPadding = 5.dp,
    userAvatarBusyHorizontalPadding = 3.dp,
    groupAvatarCornerRadius = 10.dp,
    groupAvatarSize = 32.dp,
    bottomNavigationHorizontalPadding = 8.dp,
    bottomNavigationVerticalPadding = 4.dp,
    bottomNavigationBetweenItemsPadding = 12.dp,
    bottomNavigationItemPadding = 6.dp,
    conversationItemRowHeight = 56.dp,
    conversationItemPadding = 0.5.dp,
    messageImagePortraitModeWidth = 200.dp,
    textFieldMinHeight = 48.dp,
    textFieldCornerSize = 16.dp,
    buttonMinHeight = 48.dp,
    buttonMinWidth = 60.dp,
    buttonHorizontalContentPadding = 16.dp,
    buttonVerticalContentPadding = 8.dp,
    buttonCornerSize = 16.dp,
    dialogButtonsSpacing = 8.dp,
    dialogTextsSpacing = 16.dp,
    dialogContentPadding = 24.dp,
    dialogCornerSize = 20.dp,
    dialogCardMargin = 16.dp,
    userProfileLogoutBtnHeight = 32.dp,
    userProfileStatusBtnHeight = 32.dp,
    userProfileOtherAccItemHeight = 56.dp,
    spacing4x = 4.dp,
    spacing8x = 8.dp,
    spacing16x = 16.dp,
    spacing24x = 24.dp,
    spacing32x = 32.dp,
    spacing40x = 40.dp,
    spacing48x = 48.dp,
    spacing56x = 56.dp,
    spacing64x = 64.dp,
    corner2x = 2.dp,
    corner4x = 4.dp,
    corner6x = 6.dp,
    corner8x = 8.dp,
    corner10x = 10.dp,
    corner12x = 12.dp,
    corner14x = 14.dp,
    corner16x = 16.dp
)

private val DefaultPhoneLandscapeWireDimensions: WireDimensions = DefaultPhonePortraitWireDimensions

private val DefaultPhoneOrientationDependentWireDimensions: OrientationDependent<WireDimensions> = OrientationDependent(
    portrait = DefaultPhonePortraitWireDimensions,
    landscape = DefaultPhoneLandscapeWireDimensions
)

@PackagePrivate
val WireDimensionsTypes: ScreenSizeDependent<OrientationDependent<WireDimensions>> = ScreenSizeDependent(
    compactPhone = DefaultPhoneOrientationDependentWireDimensions,
    defaultPhone = DefaultPhoneOrientationDependentWireDimensions,
    tablet7 = DefaultPhoneOrientationDependentWireDimensions,
    tablet10 = DefaultPhoneOrientationDependentWireDimensions
)
