/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.messagedetails

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.launch

@WireDestination(
    navArgsDelegate = MessageDetailsNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun MessageDetailsScreen(
    navigator: Navigator,
    viewModel: MessageDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val reactionsLearnMoreUrl = stringResource(id = R.string.url_message_details_reactions_learn_more)
    val onReactionsLearnMore = remember {
        {
            CustomTabsHelper.launchUrl(
                context,
                reactionsLearnMoreUrl
            )
        }
    }

    val readReceiptsLearnMoreUrl = stringResource(id = R.string.url_message_details_read_receipts_learn_more)
    val onReadReceiptsLearnMore = remember {
        {
            CustomTabsHelper.launchUrl(
                context,
                readReceiptsLearnMoreUrl
            )
        }
    }

    MessageDetailsScreenContent(
        messageDetailsState = viewModel.messageDetailsState,
        onBackPressed = navigator::navigateBack,
        onReactionsLearnMore = onReactionsLearnMore,
        onReadReceiptsLearnMore = onReadReceiptsLearnMore
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageDetailsScreenContent(
    messageDetailsState: MessageDetailsState,
    onBackPressed: () -> Unit,
    onReactionsLearnMore: () -> Unit,
    onReadReceiptsLearnMore: () -> Unit
) {
    val tabItems by remember(messageDetailsState) {
        derivedStateOf {
            val reactions = MessageDetailsTabItem.Reactions(messageDetailsState.reactionsData.reactions.map { it.value.size }.sum())
            val readReceipts = MessageDetailsTabItem.ReadReceipts(messageDetailsState.readReceiptsData.readReceipts.size)
            if (messageDetailsState.isSelfMessage) listOf(reactions, readReceipts) else listOf(reactions)
        }
    }
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = tabItems.map { rememberLazyListState() }
    val initialPageIndex = tabItems.indexOfFirst { it is MessageDetailsTabItem.Reactions }
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { tabItems.size })
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember {
        derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) }
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = elevationState,
                title = stringResource(R.string.message_details_title),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onBackPressed
            ) {
                WireTabRow(
                    tabs = tabItems,
                    selectedTabIndex = currentTabState,
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing16x),
                )
            }
        },
        modifier = Modifier.fillMaxHeight(),
    ) { internalPadding ->
        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(internalPadding)
            ) { pageIndex ->
                when (tabItems[pageIndex]) {
                    is MessageDetailsTabItem.Reactions -> MessageDetailsReactions(
                        reactionsData = messageDetailsState.reactionsData,
                        lazyListState = lazyListStates[pageIndex],
                        onReactionsLearnMore = onReactionsLearnMore
                    )

                    is MessageDetailsTabItem.ReadReceipts -> MessageDetailsReadReceipts(
                        readReceiptsData = messageDetailsState.readReceiptsData,
                        lazyListState = lazyListStates[pageIndex],
                        onReadReceiptsLearnMore = onReadReceiptsLearnMore
                    )
                }
            }

            LaunchedEffect(pagerState.isScrollInProgress, focusedTabIndex, pagerState.currentPage) {
                if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    focusedTabIndex = pagerState.currentPage
                }
            }
        }
    }
}

sealed class MessageDetailsTabItem(@StringRes val titleResId: Int, argument: String) : TabItem {
    override val title: UIText = UIText.StringResource(titleResId, argument)

    data class Reactions(val count: Int) : MessageDetailsTabItem(R.string.message_details_reactions_tab, "$count")
    data class ReadReceipts(val count: Int) : MessageDetailsTabItem(R.string.message_details_read_receipts_tab, "$count")
}
