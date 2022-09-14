package com.wire.android.ui.home.conversations.details

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.collectAsStateLifecycleAware
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.details.menu.ConversationGroupDetailsBottomSheet
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptions
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipants
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun GroupConversationDetailsScreen(viewModel: GroupConversationDetailsViewModel) {
    val context = LocalContext.current
    GroupConversationDetailsContent(
        onBackPressed = viewModel::navigateBack,
        openFullListPressed = viewModel::navigateToFullParticipantsList,
        onProfilePressed = viewModel::openProfile,
        onAddParticipantsPressed = viewModel::navigateToAddParticants,
        groupOptionsStateFlow = viewModel.groupOptionsState,
        groupParticipantsState = viewModel.groupParticipantsState,
        onLeaveGroup = viewModel::leaveGroup,
        onDeleteGroup = viewModel::deleteGroup,
        isLoading = viewModel.requestInProgress,
        messages = viewModel.snackBarMessage,
        context = context
    )
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalPagerApi::class,
    ExperimentalMaterialApi::class, ExperimentalFoundationApi::class
)
@Composable
private fun GroupConversationDetailsContent(
    onBackPressed: () -> Unit,
    openFullListPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    onAddParticipantsPressed: () -> Unit,
    onLeaveGroup: (GroupDialogState) -> Unit,
    onDeleteGroup: (GroupDialogState) -> Unit,
    groupOptionsStateFlow: StateFlow<GroupConversationOptionsState>,
    groupParticipantsState: GroupConversationParticipantsState,
    isLoading: Boolean,
    messages: SharedFlow<UIText>,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val groupOptionsState by groupOptionsStateFlow.collectAsStateLifecycleAware()
    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.values().map { rememberLazyListState() }
    val initialPageIndex = GroupConversationDetailsTabItem.OPTIONS.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val openBottomSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeBottomSheet: () -> Unit = remember { { scope.launch { sheetState.hide() } } }

    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        messages.collect { snackbarHostState.showSnackbar(it.asString(context.resources)) }
    }

    if (!isLoading) {
        deleteGroupDialogState.dismiss()
        leaveGroupDialogState.dismiss()
    }
    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = rememberCoroutineScope(),
        sheetContent = {
            ConversationGroupDetailsBottomSheet(
                conversationOptionsState = groupOptionsState,
                closeBottomSheet = closeBottomSheet,
                onDeleteGroup = deleteGroupDialogState::show,
                onLeaveGroup = leaveGroupDialogState::show
            )
        }
    ) {
        Scaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = elevationState,
                    title = stringResource(R.string.conversation_details_title),
                    navigationIconType = NavigationIconType.Close,
                    onNavigationPressed = onBackPressed,
                    actions = {
                        MoreOptionIcon(openBottomSheet)
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
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
        ) { internalPadding ->
            var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    count = GroupConversationDetailsTabItem.values().size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(internalPadding)
                ) { pageIndex ->
                    when (GroupConversationDetailsTabItem.values()[pageIndex]) {
                        GroupConversationDetailsTabItem.OPTIONS -> GroupConversationOptions(
                            lazyListState = lazyListStates[pageIndex]
                        )
                        GroupConversationDetailsTabItem.PARTICIPANTS -> GroupConversationParticipants(
                            groupParticipantsState = groupParticipantsState,
                            openFullListPressed = openFullListPressed,
                            onAddParticipantsPressed = onAddParticipantsPressed,
                            onProfilePressed = onProfilePressed,
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

    DeleteConversationGroupDialog(
        isLoading = isLoading,
        dialogState = deleteGroupDialogState,
        onDeleteGroup = onDeleteGroup
    )

    LeaveConversationGroupDialog(
        dialogState = leaveGroupDialogState,
        isLoading = isLoading,
        onLeaveGroup = onLeaveGroup
    )

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
            onProfilePressed = {},
            onAddParticipantsPressed = {},
            onLeaveGroup = {},
            onDeleteGroup = {},
            groupOptionsStateFlow = MutableStateFlow(
                GroupConversationOptionsState(
                    conversationId = ConversationId("someValue", "someDomain"),
                    groupName = "Group name"
                )
            ),
            groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
            isLoading = false,
            messages = MutableSharedFlow(),
        )
    }
}

