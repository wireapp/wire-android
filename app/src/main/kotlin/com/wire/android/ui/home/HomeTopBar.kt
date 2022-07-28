package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireLinearProgressIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.sync.SyncViewState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeTopBar(
    avatarAsset: UserAvatarAsset?,
    status: UserAvailabilityStatus,
    currentNavigationItem: HomeItem,
    syncState: SyncViewState,
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
                isClickable = true
            ) {
                onNavigateToUserProfile()
            }
        },
        elevation = if (currentNavigationItem.isSearchable) 0.dp else dimensions().topBarElevationHeight,
    ) {
        val shouldShowState = syncState !in setOf(SyncViewState.WAITING, SyncViewState.LIVE)

        if (shouldShowState) {
            WireLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progressColor = syncState.color
            )
        }
    }
}

private val SyncViewState.color: Color
    get() = when (this) {
        SyncViewState.WAITING, SyncViewState.LIVE -> Color.White
        SyncViewState.SLOW_SYNC -> Color.Blue
        SyncViewState.GATHERING_EVENTS -> Color.Green
        SyncViewState.LACK_OF_CONNECTION -> Color.Yellow
        SyncViewState.UNKNOWN_FAILURE -> Color.Red
    }

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)
@Preview
@Composable
fun topBar() {
    HomeTopBar(
        null, UserAvailabilityStatus.AVAILABLE, HomeItem.Conversations, SyncViewState.SLOW_SYNC, {}, {}
    )
    // TODO: Re-enable and recheck when we have Archive
//    HomeTopBar(
//        null, UserAvailabilityStatus.AVAILABLE, HomeNavigationItem.Archive, SyncViewState.SLOW_SYNC, {}, {}
//    )
}
