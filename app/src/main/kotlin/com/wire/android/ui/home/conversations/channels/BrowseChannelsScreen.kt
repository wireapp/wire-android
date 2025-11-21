/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
 */
package com.wire.android.ui.home.conversations.channels
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Destination<WireRootNavGraph>(
    style = PopUpNavigationAnimation::class,
)
@Composable
fun BrowseChannelsScreen(navigator: Navigator) {
    Content(
        searchQueryTextState = TextFieldState(),
//        onChannelClick = { /*TODO when BE supports public search*/ },
        onCloseSearchClicked = navigator::navigateBack
    )
}

@Composable
private fun Content(
    searchQueryTextState: TextFieldState,
//    onChannelClick: () -> Unit,
    onCloseSearchClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = lazyListState.rememberTopBarElevationState().value,
                title = stringResource(R.string.label_public_channels),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onCloseSearchClicked
            ) {
                SearchTopBar(
                    isSearchActive = true,
                    searchBarHint = stringResource(id = R.string.label_search_public_channels),
                    searchQueryTextState = searchQueryTextState,
                    isLoading = false
                )
            }
        },
        content = { internalPadding ->
            Column(modifier = Modifier.padding(internalPadding)) {
                val lazyPagingChannels = listOf(1, 2)
                if (searchQueryTextState.text.isEmpty()) {
                    BrowseChannelsEmptyScreen()
                } else {
                    if (lazyPagingChannels.size > 0) {
                        // todo: load channels list when BE supports it
                    } else {
                        BrowseChannelsEmptyScreen()
                    }
                }
            }
        }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewBrowseChannelsScreen() {
    WireTheme {
        Content(
            searchQueryTextState = TextFieldState(),
            onCloseSearchClicked = { }
        )
    }
}
