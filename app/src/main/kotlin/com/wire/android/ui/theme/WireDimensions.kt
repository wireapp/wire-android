package com.wire.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.esentsov.PackagePrivate

@Immutable
data class WireDimensions(
    // Avatar
    val userAvatarDefaultSize: Dp,
    // BottomNavigation
    val bottomNavigationHorizontalPadding: Dp,
    val bottomNavigationVerticalPadding: Dp,
    val bottomNavigationBetweenItemsPadding: Dp,
    val bottomNavigationItemPadding: Dp,
    // Conversation
    val conversationItemRowPadding: Dp,
    val conversationItemPadding: Dp,
    // Message
    val messageImagePortraitModeWidth: Dp,
    // Buttons
    val buttonMinHeight: Dp,
    val buttonMinWidth: Dp,
    val buttonHorizontalContentPadding: Dp,
    val buttonVerticalContentPadding: Dp,
)

private val DefaultPhonePortraitWireDimensions: WireDimensions = WireDimensions(
    userAvatarDefaultSize = 24.dp,
    bottomNavigationHorizontalPadding = 8.dp,
    bottomNavigationVerticalPadding = 4.dp,
    bottomNavigationBetweenItemsPadding = 12.dp,
    bottomNavigationItemPadding = 6.dp,
    conversationItemRowPadding = 8.dp,
    conversationItemPadding = 0.5.dp,
    messageImagePortraitModeWidth = 200.dp,
    buttonMinHeight = 48.dp,
    buttonMinWidth = 60.dp,
    buttonHorizontalContentPadding = 16.dp,
    buttonVerticalContentPadding = 8.dp
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
