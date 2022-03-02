package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.SearchBarUI
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan


@ExperimentalMaterial3Api
@Composable
fun HomeTopBar(
    @StringRes title: Int,
    isSearchable: Boolean,
    scrollPosition: Int,
    onUserProfileClick: () -> Unit,
    onHamburgerMenuItemCLick: () -> Unit,
) {
    var isCollapsed: Boolean by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(scrollPosition) {
        snapshotFlow { scrollPosition }
            .scan(0 to 0) { prevPair, newScrollIndex ->
                if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) prevPair
                else prevPair.second to newScrollIndex
            }
            .map { (prevScrollIndex, newScrollIndex) ->
                newScrollIndex > prevScrollIndex + 1
            }
            .distinctUntilChanged().collect {
                isCollapsed = it
            }
    }

    Box(
        Modifier.background(Color.Transparent)
    ) {
        if (isSearchable) {
            val searchFieldFullHeightPx = LocalDensity.current.run {
                (dimensions().topBarSearchFieldHeight + dimensions().topBarElevationHeight).toPx()
            }

            val searchFieldPosition by animateFloatAsState(if (isCollapsed) -searchFieldFullHeightPx else 0f)

            Surface(
                modifier = Modifier
                    .padding(top = dimensions().smallTopBarHeight)
                    .height(dimensions().topBarSearchFieldHeight)
                    .graphicsLayer { translationY = searchFieldPosition },
                shadowElevation = dimensions().topBarElevationHeight
            ) {
                SearchBarUI(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    placeholderText = stringResource(R.string.search_bar_hint, stringResource(id = title).lowercase())
                )
            }
        }

        WireCenterAlignedTopAppBar(
            elevation = if (!isSearchable || isCollapsed) dimensions().topBarElevationHeight else 0.dp,
            title = stringResource(id = title),
            onNavigationPressed = { onHamburgerMenuItemCLick() },
            actions = {
                UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
                    onUserProfileClick()
                }
            },
        )
    }
}
