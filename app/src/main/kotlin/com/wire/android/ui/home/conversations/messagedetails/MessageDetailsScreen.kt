/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
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
import kotlinx.coroutines.launch

@RootNavGraph
@Destination(
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

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun MessageDetailsScreenContent(
    messageDetailsState: MessageDetailsState,
    onBackPressed: () -> Unit,
    onReactionsLearnMore: () -> Unit,
    onReadReceiptsLearnMore: () -> Unit
) {
    val tabItems = provideMessageDetailsTabItems(
        messageDetailsState = messageDetailsState,
        isSelfMessage = messageDetailsState.isSelfMessage
    )
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = MessageDetailsTab.values().map { rememberLazyListState() }
    val initialPageIndex = MessageDetailsTab.REACTIONS.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { tabItems.size })
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = elevationState,
                title = stringResource(R.string.message_details_title),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = onBackPressed
            ) {
                WireTabRow(
                    tabs = tabItems,
                    selectedTabIndex = currentTabState,
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing16x),
                    divider = {} // no divider
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
                when (MessageDetailsTab.values()[pageIndex]) {
                    MessageDetailsTab.REACTIONS -> MessageDetailsReactions(
                        reactionsData = messageDetailsState.reactionsData,
                        lazyListState = lazyListStates[pageIndex],
                        onReactionsLearnMore = onReactionsLearnMore
                    )

                    MessageDetailsTab.READ_RECEIPTS -> MessageDetailsReadReceipts(
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

enum class MessageDetailsTab(@StringRes override val titleResId: Int) : TabItem {
    REACTIONS(R.string.message_details_reactions_tab),
    READ_RECEIPTS(R.string.message_details_read_receipts_tab)
}

/**
 * This method creates a new TabItem (data class) and NOT Enum due to enums not being dynamic and we needing to pass
 * the total reactions count into [WireTabRow]
 */
private fun provideMessageDetailsTabItems(
    messageDetailsState: MessageDetailsState,
    isSelfMessage: Boolean
): List<TabItem> {
    val reactions = MessageDetailsTabItem(
        titleResId = MessageDetailsTab.REACTIONS.titleResId,
        count = messageDetailsState.reactionsData.reactions.map { it.value.size }.sum()
    )
    val readReceipts = MessageDetailsTabItem(
        titleResId = MessageDetailsTab.READ_RECEIPTS.titleResId,
        count = messageDetailsState.readReceiptsData.readReceipts.size
    )

    return if (isSelfMessage) listOf(reactions, readReceipts) else listOf(reactions)
}

data class MessageDetailsTabItem(@StringRes override val titleResId: Int, val count: Int) : TabItem
