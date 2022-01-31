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
    val bottomNavigationPadding: Dp,
    val bottomNavigationItemPadding: Dp,
    // Conversation
    val conversationItemRowPadding: Dp,
    val conversationItemPadding: Dp,
    // Message
    val messageImagePortraitModeWidth: Dp,
)

private val DefaultWireDimensions: WireDimensions = WireDimensions(
    userAvatarDefaultSize = 24.dp,
    bottomNavigationPadding = 4.dp,
    bottomNavigationItemPadding = 6.dp,
    conversationItemRowPadding = 8.dp,
    conversationItemPadding = 0.5.dp,
    messageImagePortraitModeWidth = 200.dp
)

@PackagePrivate
val WireDimensionsTypes: ScreenSizeDependent<WireDimensions> = ScreenSizeDependent(
    compactPhone = DefaultWireDimensions,
    defaultPhone = DefaultWireDimensions,
    tablet7 = DefaultWireDimensions,
    tablet10 = DefaultWireDimensions
)
