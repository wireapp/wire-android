package com.wire.android.ui.home.conversations.details

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptions
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipants
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.launch

@Composable
fun GroupConversationDetailsScreen(viewModel: GroupConversationDetailsViewModel) {
    GroupConversationDetailsContent(
        onBackPressed = viewModel::navigateBack,
        openFullListPressed = viewModel::navigateToFullParticipantsList,
        groupOptionsState = viewModel.groupOptionsState,
        groupParticipantsState = viewModel.groupParticipantsState
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun GroupConversationDetailsContent(
    onBackPressed: () -> Unit,
    openFullListPressed: () -> Unit,
    groupOptionsState: GroupConversationOptionsState,
    groupParticipantsState: GroupConversationParticipantsState
) {
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.values().map { rememberLazyListState() }
    val initialPageIndex = GroupConversationDetailsTabItem.OPTIONS.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = elevationState,
                title = stringResource(R.string.conversation_details_title),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = onBackPressed,
                actions = {
                    MoreOptionIcon({ })
                }
            ) {
                WireTabRow(
                    tabs = GroupConversationDetailsTabItem.values().toList(),
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
        CompositionLocalProvider(LocalOverScrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                count = GroupConversationDetailsTabItem.values().size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(internalPadding)
            ) { pageIndex ->
                when (GroupConversationDetailsTabItem.values()[pageIndex]) {
                    GroupConversationDetailsTabItem.OPTIONS -> GroupConversationOptions(
                        groupOptionsState = groupOptionsState,
                        lazyListState = lazyListStates[pageIndex]
                    )
                    GroupConversationDetailsTabItem.PARTICIPANTS -> GroupConversationParticipants(
                        groupParticipantsState = groupParticipantsState,
                        openFullListPressed = openFullListPressed,
                        lazyListState = lazyListStates[pageIndex]
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

enum class GroupConversationDetailsTabItem(@StringRes override val titleResId: Int) : TabItem {
    OPTIONS(R.string.conversation_details_options_tab),
    PARTICIPANTS(R.string.conversation_details_participants_tab);
}

@Preview
@Composable
private fun GroupConversationDetailsPreview() {
    WireTheme(isPreview = true) {
        GroupConversationDetailsContent(
            onBackPressed = {},
            openFullListPressed = {},
            groupOptionsState = GroupConversationOptionsState(groupName = "Group name"),
            groupParticipantsState = GroupConversationParticipantsState.PREVIEW
        )
    }
}

