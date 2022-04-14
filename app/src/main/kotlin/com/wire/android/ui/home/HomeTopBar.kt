package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.model.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeTopBar(
    avatarAsset: UserAvatarAsset?,
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
                avatarAsset,
                isClickable = true,
                status = UserStatus.AVAILABLE
            ) {
                onNavigateToUserProfile()
            }
        },
        elevation = if (currentNavigationItem.isSearchable) 0.dp else dimensions().topBarElevationHeight,
    )
}
