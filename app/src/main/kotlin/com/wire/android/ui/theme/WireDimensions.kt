package com.wire.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.esentsov.PackagePrivate

@Immutable
data class WireDimensions(
    // Avatar
    val userAvatarDefaultSize: Dp,
    val userAvatarClickablePadding: Dp,
    val userAvatarStatusSize: Dp,
    val userAvatarBorderSize: Dp,
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
    // Conversation BottomSheet
    val conversationBottomSheetItemHeight: Dp,
    val conversationBottomSheetItemPadding: Dp,
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
)

private val DefaultPhonePortraitWireDimensions: WireDimensions = WireDimensions(
    userAvatarDefaultSize = 32.dp,
    userAvatarClickablePadding = 8.dp,
    userAvatarStatusSize = 10.dp,
    userAvatarBorderSize = 14.dp,
    groupAvatarCornerRadius = 10.dp,
    groupAvatarSize = 32.dp,
    bottomNavigationHorizontalPadding = 8.dp,
    bottomNavigationVerticalPadding = 4.dp,
    bottomNavigationBetweenItemsPadding = 12.dp,
    bottomNavigationItemPadding = 6.dp,
    conversationItemRowHeight = 56.dp,
    conversationItemPadding = 0.5.dp,
    conversationBottomSheetItemHeight = 48.dp,
    conversationBottomSheetItemPadding = 16.dp,
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
