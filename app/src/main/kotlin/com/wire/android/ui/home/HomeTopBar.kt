package com.wire.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun HomeTopBar(
    avatarAsset: UserAvatarAsset?,
    status: UserAvailabilityStatus,
    title: String,
    elevation: Dp,
    onHamburgerMenuClick: () -> Unit,
    onNavigateToSelfUserProfile: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        title = title,
        onNavigationPressed = onHamburgerMenuClick,
        navigationIconType = NavigationIconType.Menu,
        actions = {
            UserProfileAvatar(
                avatarData = UserAvatarData(avatarAsset, status),
                clickable = remember { Clickable(enabled = true) { onNavigateToSelfUserProfile() } }
            )
        },
        elevation = elevation,
    )
}

@Preview
@Composable
fun PreviewTopBar() {
    HomeTopBar(null, UserAvailabilityStatus.AVAILABLE,  "Title", 0.dp, {}, {})
}
