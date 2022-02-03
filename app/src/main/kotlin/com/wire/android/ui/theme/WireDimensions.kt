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
    // UserProfile
    val userProfileLogoutBtnHeight: Dp,
    val userProfileStatusBtnHeight: Dp,
    val userProfileOtherAccItemHeight: Dp,
    // Spacing
    val spacing4: Dp,
    val spacing8: Dp,
    val spacing16: Dp,
    val spacing24: Dp,
    val spacing32: Dp,
    val spacing40: Dp,
    val spacing48: Dp,
    val spacing56: Dp,
    val spacing64: Dp,
    // Corners
    val corner2: Dp,
    val corner4: Dp,
    val corner6: Dp,
    val corner8: Dp,
    val corner10: Dp,
    val corner12: Dp,
    val corner14: Dp,
    val corner16: Dp,
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
    userProfileLogoutBtnHeight = 32.dp,
    userProfileStatusBtnHeight = 32.dp,
    userProfileOtherAccItemHeight = 56.dp,
    spacing4 = 4.dp,
    spacing8 = 8.dp,
    spacing16 = 16.dp,
    spacing24 = 24.dp,
    spacing32 = 32.dp,
    spacing40 = 40.dp,
    spacing48 = 48.dp,
    spacing56 = 56.dp,
    spacing64 = 64.dp,
    corner2 = 2.dp,
    corner4 = 4.dp,
    corner6 = 6.dp,
    corner8 = 8.dp,
    corner10 = 10.dp,
    corner12 = 12.dp,
    corner14 = 14.dp,
    corner16 = 16.dp
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
