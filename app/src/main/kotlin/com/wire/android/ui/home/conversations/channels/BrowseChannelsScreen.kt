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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@HomeNavGraph
@WireDestination
@Composable
fun BrowseChannelsScreen() {
    Content(
        searchQueryTextState = TextFieldState(),
        onChannelClick = { /*TODO*/ },
        onCloseSearchClicked = { /*TODO*/ }
    )
}

@Composable
private fun Content(
    searchQueryTextState: TextFieldState,
    onChannelClick: () -> Unit,
    onCloseSearchClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    WireScaffold(
        modifier = modifier,
        topBar = {
            Surface(
                shadowElevation = lazyListState.topBarElevation(maxAppBarElevation),
                color = MaterialTheme.wireColorScheme.background
            ) {
                SearchTopBar(
                    isSearchActive = true, // todo get from vm?
                    searchBarHint = stringResource(id = R.string.label_search_channels),
                    searchQueryTextState = searchQueryTextState,
                    onCloseSearchClicked = onCloseSearchClicked,
                    isLoading = false // todo get from vm
                )
            }
        },
        content = { internalPadding ->
            Column(modifier = Modifier.padding(internalPadding)) {
//                val lazyPagingChannels = state.searchResult.collectAsLazyPagingItems()
                val lazyPagingChannels = listOf(1, 2)
                if (searchQueryTextState.text.isEmpty()) {
                    BrowseChannelsEmptyScreen()
                } else {
                    if (lazyPagingChannels.size > 0) {
                        // load channels list
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
            onChannelClick = { /*TODO*/ },
            onCloseSearchClicked = { /*TODO*/ }
        )
    }
}
