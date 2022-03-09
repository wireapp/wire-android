package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.SearchBarUI
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun HomeTopBar(
    @StringRes title: Int?,
    isSearchable: Boolean,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: HomeViewModel
) {
    val titleText = stringResource(id = title ?: R.string.conversations_screen_title)
    val scrollDownState = viewModel.scrollDownFlow.collectAsState(false)
    val firstLineElevation = if (!isSearchable || scrollDownState.value) dimensions().topBarElevationHeight else 0.dp

    LaunchedEffect(viewModel) {
        viewModel.loadUserAvatar()
    }

    Box {
        if (isSearchable) {
            val searchFieldFullHeightPx = LocalDensity.current.run {
                (dimensions().topBarSearchFieldHeight + dimensions().topBarElevationHeight).toPx()
            }
            val searchFieldPosition by animateFloatAsState(if (scrollDownState.value) -searchFieldFullHeightPx else 0f)

            Surface(
                modifier = Modifier
                    .padding(top = dimensions().smallTopBarHeight)
                    .height(dimensions().topBarSearchFieldHeight)
                    .graphicsLayer { translationY = searchFieldPosition },
                shadowElevation = dimensions().topBarElevationHeight
            ) {
                SearchBarUI(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    placeholderText = stringResource(R.string.search_bar_hint, titleText.lowercase())
                )
            }
        }

        WireCenterAlignedTopAppBar(
            elevation = firstLineElevation,
            title = titleText,
            navigationIconType = NavigationIconType.Menu,
            onNavigationPressed = { scope.launch { drawerState.open() } },
            actions = {
                UserProfileAvatar(avatarAssetByteArray = viewModel.userAvatar, isEnabled = true, status = UserStatus.AVAILABLE) {
                    scope.launch { viewModel.navigateToUserProfile() }
                }
            },
        )
    }
}
