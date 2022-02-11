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
    // Drawer Navigation
    val homeDrawerHorizontalPadding: Dp,
    val homeDrawerBottomPadding: Dp,
    val homeDrawerLogoHorizontalPadding: Dp,
    val homeDrawerLogoVerticalPadding: Dp,
    val homeDrawerLogoWidth: Dp,
    val homeDrawerLogoHeight: Dp,
    // FAB
    val fabIconSize: Dp,
    // BottomNavigation
    val bottomNavigationHorizontalPadding: Dp,
    val bottomNavigationVerticalPadding: Dp,
    val bottomNavigationBetweenItemsPadding: Dp,
    val bottomNavigationItemPadding: Dp,
    // Conversation
    val conversationItemRowHeight: Dp,
    val conversationItemPadding: Dp,
    // Conversation BottomSheet
    val conversationBottomSheetItemHeight: Dp,
    val conversationBottomSheetItemPadding: Dp,
    val conversationBottomSheetShapeCorner: Dp,
    val conversationBottomSheetItemSize: Dp,
    // Message
    val messageImagePortraitModeWidth: Dp,
    // Message composer
    val messageComposerActiveInputMaxHeight: Dp,
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
    // Welcome
    val welcomeImageHorizontalPadding: Dp,
    val welcomeTextHorizontalPadding: Dp,
    val welcomeButtonHorizontalPadding: Dp,
    val welcomeButtonVerticalPadding: Dp,
    val welcomeVerticalPadding: Dp,
    val welcomeVerticalSpacing: Dp,
    // Spacing
    val spacing2x: Dp,
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
    // Notifications
    val notificationBadgeHeight: Dp,
    val notificationBadgeRadius: Dp,
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
    homeDrawerHorizontalPadding = 8.dp,
    homeDrawerBottomPadding = 16.dp,
    homeDrawerLogoHorizontalPadding = 8.dp,
    homeDrawerLogoVerticalPadding = 32.dp,
    homeDrawerLogoWidth = 80.dp,
    homeDrawerLogoHeight = 24.dp,
    fabIconSize = 16.dp,
    bottomNavigationHorizontalPadding = 8.dp,
    bottomNavigationVerticalPadding = 4.dp,
    bottomNavigationBetweenItemsPadding = 12.dp,
    bottomNavigationItemPadding = 6.dp,
    conversationItemRowHeight = 56.dp,
    conversationItemPadding = 0.5.dp,
    conversationBottomSheetItemHeight = 48.dp,
    conversationBottomSheetItemPadding = 16.dp,
    conversationBottomSheetShapeCorner = 12.dp,
    conversationBottomSheetItemSize = 16.dp,
    messageImagePortraitModeWidth = 200.dp,
    messageComposerActiveInputMaxHeight = 168.dp,
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
    welcomeImageHorizontalPadding = 64.dp,
    welcomeTextHorizontalPadding = 24.dp,
    welcomeButtonHorizontalPadding = 16.dp,
    welcomeButtonVerticalPadding = 8.dp,
    welcomeVerticalPadding = 56.dp,
    welcomeVerticalSpacing = 40.dp,
    spacing2x = 2.dp,
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
    corner16x = 16.dp,
    notificationBadgeHeight = 18.dp,
    notificationBadgeRadius = 6.dp,
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
