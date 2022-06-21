package com.wire.android.ui.home.conversations.details

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.coroutines.launch

@Composable
fun GroupConversationDetailsScreen(viewModel: GroupConversationDetailsViewModel) {
    GroupConversationDetailsContent(
        conversationId = viewModel.conversationId,
        onBackPressed = viewModel::navigateBack
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun GroupConversationDetailsContent(conversationId: QualifiedID, onBackPressed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val initialPageIndex = GroupConversationDetailsTabItem.OPTIONS.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.appBarElevation(),
                title = stringResource(R.string.conversation_details_title),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = onBackPressed,
                actions = {
                    MoreOptionIcon({ })
                }
            ) {
                WireTabRow(
                    tabs = GroupConversationDetailsTabItem.values().toList(),
                    selectedTabIndex = pagerState.calculateCurrentTab(),
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    modifier = Modifier.padding(
                        start = MaterialTheme.wireDimensions.spacing16x,
                        end = MaterialTheme.wireDimensions.spacing16x,
                        top = MaterialTheme.wireDimensions.spacing16x
                    ),
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
                    // TODO: to be implemented
                    GroupConversationDetailsTabItem.OPTIONS -> UnderConstructionScreen(screenName = "Conversation Options")
                    GroupConversationDetailsTabItem.PARTICIPANTS -> UnderConstructionScreen(screenName = "Conversation Participants")
                }
            }
            if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage)
                LaunchedEffect(Unit) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    focusedTabIndex = pagerState.currentPage
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
        GroupConversationDetailsContent(QualifiedID("", ""), onBackPressed = { })
    }
}

