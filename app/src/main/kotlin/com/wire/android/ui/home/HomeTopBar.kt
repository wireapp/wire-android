package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeTopBar(
    avatarAsset: UserAvatarAsset?,
    status: UserAvailabilityStatus,
    currentNavigationItem: HomeNavigationItem,
    onOpenDrawerClicked: () -> Unit,
    onNavigateToUserProfile: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        title = stringResource(id = currentNavigationItem.title),
        onNavigationPressed = onOpenDrawerClicked,
        navigationIconType = NavigationIconType.Menu,
        actions = {
            UserProfileAvatar(
                avatarData = UserAvatarData(avatarAsset, status),
                clickable = remember { Clickable(enabled = true) { onNavigateToUserProfile() } }
            )
        },
        elevation = if (currentNavigationItem.isSearchable) 0.dp else dimensions().topBarElevationHeight,
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)
@Preview
@Composable
fun topBar() {
    HomeTopBar(
        null, UserAvailabilityStatus.AVAILABLE, HomeNavigationItem.Conversations, {}, {}
    )
    HomeTopBar(
        null, UserAvailabilityStatus.AVAILABLE, HomeNavigationItem.Settings, {}, {}
    )
    // TODO: Re-enable and recheck when we have Archive
//    HomeTopBar(
//        null, UserAvailabilityStatus.AVAILABLE, HomeNavigationItem.Archive, SyncViewState.SLOW_SYNC, {}, {}
//    )
}
