/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

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
    val avatarDefaultSize: Dp,
    val avatarDefaultBigSize: Dp,
    val avatarClickablePadding: Dp,
    val userAvatarStatusSize: Dp,
    val userAvatarBusyVerticalPadding: Dp,
    val userAvatarBusyHorizontalPadding: Dp,
    val avatarStatusBorderSize: Dp,
    val groupAvatarCornerRadius: Dp,
    val avatarConversationTopBarSize: Dp,
    val groupAvatarConversationTopBarCornerRadius: Dp,
    val groupAvatarConversationDetailsTopBarSize: Dp,
    val groupAvatarConversationDetailsCornerRadius: Dp,
    val avatarConversationTopBarClickablePadding: Dp,
    // Drawer Navigation
    val homeDrawerHorizontalPadding: Dp,
    val homeDrawerBottomPadding: Dp,
    val homeDrawerLogoHorizontalPadding: Dp,
    val homeDrawerLogoVerticalPadding: Dp,
    val homeDrawerLogoWidth: Dp,
    val homeDrawerLogoHeight: Dp,
    val homeDrawerSheetEndPadding: Dp,
    // FAB
    val fabIconSize: Dp,
    // BottomNavigation
    val bottomNavigationHorizontalPadding: Dp,
    val bottomNavigationVerticalPadding: Dp,
    val bottomNavigationBetweenItemsPadding: Dp,
    val bottomNavigationItemPadding: Dp,
    val bottomNavigationHeight: Dp,
    val bottomNavigationShadowElevation: Dp,
    // Conversation
    val conversationItemRowHeight: Dp,
    val conversationItemPadding: Dp,
    val conversationsListBottomPadding: Dp,
    // Conversation BottomSheet
    val conversationBottomSheetItemHeight: Dp,
    val conversationBottomSheetItemPadding: Dp,
    val conversationBottomSheetShapeCorner: Dp,
    val wireIconButtonSize: Dp,
    // Message
    val messageImageMaxWidth: Dp,
    val messageQuoteBorderWidth: Dp,
    val messageQuoteBorderRadius: Dp,
    val messageQuoteIconSize: Dp,
    val messageAssetBorderRadius: Dp,
    // Message composer
    val messageComposerActiveInputMaxHeight: Dp,
    val attachmentButtonSize: Dp,
    val messageComposerPaddingEnd: Dp,
    val systemMessageIconSize: Dp,
    val systemMessageIconLargeSize: Dp,
    val typingIndicatorHeight: Dp,
    // TextFields
    val textFieldMinHeight: Dp,
    val textFieldCornerSize: Dp,
    val codeFieldItemWidth: Dp,
    val codeFieldItemHeight: Dp,
    // Buttons
    val buttonMinSize: DpSize,
    val buttonSmallMinSize: DpSize,
    val buttonMediumMinSize: DpSize,
    val buttonCircleMinSize: DpSize,
    val buttonMinClickableSize: DpSize,
    val buttonHorizontalContentPadding: Dp,
    val buttonVerticalContentPadding: Dp,
    val buttonCornerSize: Dp,
    val buttonSmallCornerSize: Dp,
    val badgeSmallMinSize: DpSize,
    val badgeSmallMinClickableSize: DpSize,
    val onMoreOptionsButtonCornerRadius: Dp,
    // Dialog
    val dialogButtonsSpacing: Dp,
    val dialogTextsSpacing: Dp,
    val dialogContentPadding: Dp,
    val dialogCornerSize: Dp,
    val dialogCardMargin: Dp,
    // UserProfile
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
    val spacing0x: Dp,
    val spacing1x: Dp,
    val spacing2x: Dp,
    val spacing4x: Dp,
    val spacing6x: Dp,
    val spacing8x: Dp,
    val spacing12x: Dp,
    val spacing16x: Dp,
    val spacing18x: Dp,
    val spacing20x: Dp,
    val spacing24x: Dp,
    val spacing28x: Dp,
    val spacing32x: Dp,
    val spacing40x: Dp,
    val spacing48x: Dp,
    val spacing56x: Dp,
    val spacing64x: Dp,
    val spacing72x: Dp,
    val spacing80x: Dp,
    val spacing100x: Dp,
    val spacing200x: Dp,
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
    val modalBottomSheetHeaderHorizontalPadding: Dp,
    val modalBottomSheetHeaderVerticalPadding: Dp,
    val modalBottomSheetNoHeaderVerticalPadding: Dp,
    // Divider
    val dividerThickness: Dp,
    // Search People
    val defaultSearchLazyColumnHeight: Dp,
    val groupButtonHeight: Dp,
    // Calling
    val defaultCallingControlsSize: Dp,
    val defaultCallingHangUpButtonSize: Dp,
    val defaultSheetPeekHeight: Dp,
    val defaultInitiatingCallSheetPeekHeight: Dp,
    val onGoingCallUserAvatarSize: Dp,
    val onGoingCallUserAvatarMinimizedSize: Dp,
    val onGoingCallTileUsernameMaxWidth: Dp,
    val initiatingCallUserAvatarSize: Dp,
    val defaultIncomingCallSheetPeekHeight: Dp,
    val callingIncomingUserAvatarSize: Dp,
    val initiatingCallHangUpButtonSize: Dp,
    val ongoingCallLabelHeight: Dp,
    // Message item
    val messageItemBottomPadding: Dp,
    val messageItemHorizontalPadding: Dp,
    // audio message
    val audioMessageHeight: Dp,
    // Conversation options
    val conversationOptionsItemMinHeight: Dp,
    // Import media
    val importedMediaAssetSize: Dp
)

private val DefaultPhonePortraitWireDimensions: WireDimensions = WireDimensions(
    placeholderShimmerCornerSize = 8.dp,
    topBarShadowElevation = 4.dp,
    smallTopBarHeight = 64.dp,
    topBarSearchFieldHeight = 64.dp,
    topBarElevationHeight = 8.dp,
    avatarDefaultSize = 32.dp,
    avatarDefaultBigSize = 160.dp,
    avatarClickablePadding = 6.dp,
    userAvatarStatusSize = 16.dp,
    avatarStatusBorderSize = 2.dp,
    userAvatarBusyVerticalPadding = 5.dp,
    userAvatarBusyHorizontalPadding = 3.dp,
    groupAvatarCornerRadius = 10.dp,
    avatarConversationTopBarSize = 24.dp,
    groupAvatarConversationTopBarCornerRadius = 8.dp,
    groupAvatarConversationDetailsTopBarSize = 64.dp,
    groupAvatarConversationDetailsCornerRadius = 20.dp,
    avatarConversationTopBarClickablePadding = 2.dp,
    homeDrawerHorizontalPadding = 8.dp,
    homeDrawerBottomPadding = 16.dp,
    homeDrawerLogoHorizontalPadding = 8.dp,
    homeDrawerLogoVerticalPadding = 32.dp,
    homeDrawerLogoWidth = 80.dp,
    homeDrawerLogoHeight = 24.dp,
    homeDrawerSheetEndPadding = 56.dp,
    fabIconSize = 16.dp,
    bottomNavigationHorizontalPadding = 8.dp,
    bottomNavigationVerticalPadding = 4.dp,
    bottomNavigationBetweenItemsPadding = 12.dp,
    bottomNavigationItemPadding = 6.dp,
    bottomNavigationHeight = 60.dp,
    bottomNavigationShadowElevation = 8.dp,
    conversationItemRowHeight = 56.dp,
    conversationItemPadding = 0.5.dp,
    conversationsListBottomPadding = 74.dp,
    conversationBottomSheetItemHeight = 48.dp,
    conversationBottomSheetItemPadding = 14.dp,
    conversationBottomSheetShapeCorner = 12.dp,
    wireIconButtonSize = 16.dp,
    messageImageMaxWidth = 200.dp,
    messageQuoteBorderWidth = 1.dp,
    messageQuoteBorderRadius = 1.dp,
    messageQuoteIconSize = 10.dp,
    messageAssetBorderRadius = 10.dp,
    messageComposerActiveInputMaxHeight = 128.dp,
    attachmentButtonSize = 40.dp,
    textFieldMinHeight = 48.dp,
    textFieldCornerSize = 16.dp,
    codeFieldItemWidth = 44.dp,
    codeFieldItemHeight = 60.dp,
    buttonMinSize = DpSize(60.dp, 48.dp),
    buttonSmallMinSize = DpSize(40.dp, 32.dp),
    buttonMediumMinSize = DpSize(51.dp, 32.dp),
    buttonCircleMinSize = DpSize(40.dp, 40.dp),
    buttonMinClickableSize = DpSize(48.dp, 48.dp),
    buttonHorizontalContentPadding = 16.dp,
    buttonVerticalContentPadding = 8.dp,
    buttonCornerSize = 12.dp,
    buttonSmallCornerSize = 12.dp,
    onMoreOptionsButtonCornerRadius = 16.dp,
    badgeSmallMinSize = DpSize(32.dp, 24.dp),
    badgeSmallMinClickableSize = DpSize(48.dp, 48.dp),
    dialogButtonsSpacing = 8.dp,
    dialogTextsSpacing = 16.dp,
    dialogContentPadding = 24.dp,
    dialogCornerSize = 20.dp,
    dialogCardMargin = 16.dp,
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
    spacing0x = 0.dp,
    spacing1x = 1.dp,
    spacing2x = 2.dp,
    spacing4x = 4.dp,
    spacing6x = 6.dp,
    spacing8x = 8.dp,
    spacing12x = 12.dp,
    spacing16x = 16.dp,
    spacing18x = 18.dp,
    spacing20x = 20.dp,
    spacing24x = 24.dp,
    spacing28x = 28.dp,
    spacing32x = 32.dp,
    spacing40x = 40.dp,
    spacing48x = 48.dp,
    spacing56x = 56.dp,
    spacing64x = 64.dp,
    spacing72x = 72.dp,
    spacing80x = 80.dp,
    spacing100x = 100.dp,
    spacing200x = 200.dp,
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
    modalBottomSheetHeaderHorizontalPadding = 8.dp,
    modalBottomSheetHeaderVerticalPadding = 16.dp,
    modalBottomSheetNoHeaderVerticalPadding = 24.dp,
    dividerThickness = 0.5.dp,
    defaultSearchLazyColumnHeight = 320.dp,
    messageComposerPaddingEnd = 82.dp,
    systemMessageIconSize = 12.dp,
    systemMessageIconLargeSize = 16.dp,
    groupButtonHeight = 82.dp,
    defaultCallingControlsSize = 56.dp,
    defaultCallingHangUpButtonSize = 56.dp,
    defaultSheetPeekHeight = 100.dp,
    defaultInitiatingCallSheetPeekHeight = 281.dp,
    onGoingCallUserAvatarSize = 90.dp,
    onGoingCallUserAvatarMinimizedSize = 60.dp,
    onGoingCallTileUsernameMaxWidth = 120.dp,
    initiatingCallUserAvatarSize = 128.dp,
    defaultIncomingCallSheetPeekHeight = 280.dp,
    callingIncomingUserAvatarSize = 128.dp,
    initiatingCallHangUpButtonSize = 72.dp,
    messageItemBottomPadding = 6.dp,
    messageItemHorizontalPadding = 12.dp,
    conversationOptionsItemMinHeight = 57.dp,
    ongoingCallLabelHeight = 28.dp,
    audioMessageHeight = 48.dp,
    importedMediaAssetSize = 120.dp,
    typingIndicatorHeight = 24.dp
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

const val DEFAULT_WEIGHT = 1f
