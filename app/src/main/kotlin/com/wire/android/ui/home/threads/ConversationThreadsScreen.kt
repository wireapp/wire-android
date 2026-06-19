/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.threadNavigationCommand
import com.wire.android.ui.home.conversations.conversationThreadsViewModel
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@WireRootDestination(
    navArgs = ConversationThreadsNavArgs::class
)
@Composable
fun ConversationThreadsScreen(
    navigator: Navigator,
    viewModel: ConversationThreadsViewModel = conversationThreadsViewModel(),
) {
    val lazyListState = rememberLazyListState()
    val topBarElevation = lazyListState.topBarElevation(MaterialTheme.wireDimensions.topBarShadowElevation)

    WireScaffold(
        modifier = Modifier.background(colorsScheme().surfaceContainerLow),
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = topBarElevation,
                title = stringResource(R.string.threads_screen_title),
                subtitleContent = {
                    Text(
                        text = viewModel.conversationName,
                        style = MaterialTheme.wireTypography.subline01,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                },
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = { navigator.navigateBack() },
            )
        },
    ) { paddingValues ->
        when {
            viewModel.state.isLoading -> LoadingListContent(lazyListState = lazyListState)
            viewModel.state.threads.isEmpty() -> ThreadsEmptyContent(
                isSearching = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            else -> ThreadsOverviewList(
                threads = viewModel.state.threads,
                lazyListState = lazyListState,
                onOpenThread = remember(navigator, viewModel.conversationId) {
                    {
                        thread ->
                        navigator.navigate(
                            threadNavigationCommand(
                                conversationId = viewModel.conversationId,
                                threadId = thread.threadId,
                                rootMessageId = thread.rootMessageId,
                                rootMessageSelfDeletionDurationMillis = thread.rootMessageSelfDeletionDurationMillis,
                            )
                        )
                    }
                },
                onUnfollowThread = null,
                showConversationLabel = false,
                contentPadding = PaddingValues(
                    start = dimensions().spacing12x,
                    top = paddingValues.calculateTopPadding() + dimensions().spacing8x,
                    end = dimensions().spacing12x,
                    bottom = paddingValues.calculateBottomPadding() + dimensions().spacing8x,
                ),
            )
        }
    }
}
