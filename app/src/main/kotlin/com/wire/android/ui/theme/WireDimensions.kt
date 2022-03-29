package com.wire.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.esentsov.PackagePrivate

@Immutable
data class WireDimensions(
    // Placeholder
    val placeholderShimmerCornerSize: Dp,
    // Top bar
    val topBarShadowElevation: Dp,
    val smallTopBarHeight: Dp,
    val topBarSearchFieldHeight: Dp,
    val topBarElevationHeight: Dp,
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
    val conversationsListBottomPadding: Dp,
    // Conversation BottomSheet
    val conversationBottomSheetItemHeight: Dp,
    val conversationBottomSheetItemPadding: Dp,
    val conversationBottomSheetShapeCorner: Dp,
    val conversationBottomSheetItemSize: Dp,
    // Message
    val messageImagePortraitModeWidth: Dp,
    // Message composer
    val messageComposerActiveInputMaxHeight: Dp,
    val attachmentButtonSize: Dp,
    val messageComposerPaddingEnd : Dp,
    // TextFields
    val textFieldMinHeight: Dp,
    val textFieldCornerSize: Dp,
    val codeFieldItemWidth: Dp,
    val codeFieldItemHeight: Dp,
    // Buttons
    val buttonMinSize: DpSize,
    val buttonSmallMinSize: DpSize,
    val buttonHorizontalContentPadding: Dp,
    val buttonVerticalContentPadding: Dp,
    val buttonCornerSize: Dp,
    val buttonSmallCornerSize: Dp,
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
    // Profile Image
    val imagePreviewHeight: Dp,
    // Welcome
    val welcomeImageHorizontalPadding: Dp,
    val welcomeTextHorizontalPadding: Dp,
    val welcomeButtonHorizontalPadding: Dp,
    val welcomeButtonVerticalPadding: Dp,
    val welcomeVerticalPadding: Dp,
    val welcomeVerticalSpacing: Dp,
    // Remove device
    val removeDeviceHorizontalPadding: Dp,
    val removeDeviceMessageVerticalPadding: Dp,
    val removeDeviceLabelVerticalPadding: Dp,
    val removeDeviceItemPadding: Dp,
    val removeDeviceItemTitleVerticalPadding: Dp,
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
    val spacing72x: Dp,
    val spacing80x: Dp,
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
    // Wire ModalSheetLayout
    val modalBottomSheetDividerWidth: Dp,
    val modalBottomSheetHeaderStartPadding: Dp,
    val modalBottomSheetHeaderTopPadding: Dp,
    val modalBottomSheetHeaderBottomPadding: Dp,
    // Divider
    val dividerThickness: Dp,
    // Search People
    val defaultSearchLazyColumnHeight: Dp,
    val showAllCollapseButtonMinHeight : Dp
)

private val DefaultPhonePortraitWireDimensions: WireDimensions = WireDimensions(
    placeholderShimmerCornerSize = 8.dp,
    topBarShadowElevation = 4.dp,
    smallTopBarHeight = 64.dp,
    topBarSearchFieldHeight = 64.dp,
    topBarElevationHeight = 8.dp,
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
    conversationsListBottomPadding = 74.dp,
    conversationBottomSheetItemHeight = 48.dp,
    conversationBottomSheetItemPadding = 14.dp,
    conversationBottomSheetShapeCorner = 12.dp,
    conversationBottomSheetItemSize = 16.dp,
    messageImagePortraitModeWidth = 200.dp,
    messageComposerActiveInputMaxHeight = 168.dp,
    attachmentButtonSize = 40.dp,
    textFieldMinHeight = 48.dp,
    textFieldCornerSize = 16.dp,
    codeFieldItemWidth = 44.dp,
    codeFieldItemHeight = 60.dp,
    buttonMinSize = DpSize(60.dp, 48.dp),
    buttonSmallMinSize = DpSize(40.dp, 32.dp),
    buttonHorizontalContentPadding = 16.dp,
    buttonVerticalContentPadding = 8.dp,
    buttonCornerSize = 12.dp,
    buttonSmallCornerSize = 12.dp,
    dialogButtonsSpacing = 8.dp,
    dialogTextsSpacing = 16.dp,
    dialogContentPadding = 24.dp,
    dialogCornerSize = 20.dp,
    dialogCardMargin = 16.dp,
    userProfileLogoutBtnHeight = 32.dp,
    userProfileStatusBtnHeight = 32.dp,
    userProfileOtherAccItemHeight = 56.dp,
    imagePreviewHeight = 360.dp,
    welcomeImageHorizontalPadding = 64.dp,
    welcomeTextHorizontalPadding = 24.dp,
    welcomeButtonHorizontalPadding = 16.dp,
    welcomeButtonVerticalPadding = 8.dp,
    welcomeVerticalPadding = 56.dp,
    welcomeVerticalSpacing = 40.dp,
    removeDeviceHorizontalPadding = 16.dp,
    removeDeviceMessageVerticalPadding = 24.dp,
    removeDeviceLabelVerticalPadding = 5.dp,
    removeDeviceItemPadding = 12.dp,
    removeDeviceItemTitleVerticalPadding = 8.dp,
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
    spacing72x = 72.dp,
    spacing80x = 80.dp,
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
    modalBottomSheetDividerWidth = 48.dp,
    modalBottomSheetHeaderStartPadding = 8.dp,
    modalBottomSheetHeaderTopPadding = 16.dp,
    modalBottomSheetHeaderBottomPadding = 8.dp,
    dividerThickness = 0.5.dp,
    defaultSearchLazyColumnHeight = 320.dp,
    showAllCollapseButtonMinHeight = 32.dp,
    messageComposerPaddingEnd = 82.dp
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
