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
import androidx.compose.runtime.snapshotFlow
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
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel.GroupMetadataOperationResult
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.GroupConversationDetailsBottomSheetEventsHandler
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptions
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipants
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.UIText
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@Composable
fun GroupConversationDetailsScreen(
    backNavArgs: ImmutableMap<String, Any> = persistentMapOf(),
    viewModel: GroupConversationDetailsViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs)
) {
    val context = LocalContext.current
    GroupConversationDetailsContent(
        conversationSheetContent = viewModel.conversationSheetContent,
        bottomSheetEventsHandler = viewModel,
        onBackPressed = viewModel::navigateBack,
        openFullListPressed = viewModel::navigateToFullParticipantsList,
        onProfilePressed = viewModel::openProfile,
        onAddParticipantsPressed = viewModel::navigateToAddParticipants,
        groupParticipantsState = viewModel.groupParticipantsState,
        onLeaveGroup = viewModel::leaveGroup,
        onDeleteGroup = viewModel::deleteGroup,
        isLoading = viewModel.requestInProgress,
        messages = viewModel.snackBarMessage,
        checkPendingSnackBarMessages = viewModel::checkForPendingMessages,
        context = context
    )
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalPagerApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    InternalCoroutinesApi::class
)
@Composable
private fun GroupConversationDetailsContent(
    conversationSheetContent: ConversationSheetContent?,
    bottomSheetEventsHandler: GroupConversationDetailsBottomSheetEventsHandler,
    onBackPressed: () -> Unit,
    openFullListPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    onAddParticipantsPressed: () -> Unit,
    onLeaveGroup: (GroupDialogState) -> Unit,
    onDeleteGroup: (GroupDialogState) -> Unit,
    groupParticipantsState: GroupConversationParticipantsState,
    isLoading: Boolean,
    messages: SharedFlow<UIText>,
    checkPendingSnackBarMessages: () -> GroupMetadataOperationResult = { GroupMetadataOperationResult.None },
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.values().map { rememberLazyListState() }
    val initialPageIndex = GroupConversationDetailsTabItem.OPTIONS.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }

    val conversationSheetState = rememberConversationSheetState(conversationSheetContent)

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val openBottomSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeBottomSheet: () -> Unit = remember { { scope.launch { sheetState.hide() } } }
    val getBottomSheetVisibility: () -> Boolean = remember(sheetState) { { sheetState.isVisible } }

    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val clearConversationDialogState = rememberVisibilityState<DialogState>()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(messages) {
        val result = checkPendingSnackBarMessages()
        if (result is GroupMetadataOperationResult.Result) {
            snackbarHostState.showSnackbar(result.message.asString(context.resources))
        } else {
            messages.collect {
                closeBottomSheet()
                snackbarHostState.showSnackbar(it.asString(context.resources))
            }
        }
    }

    LaunchedEffect(conversationSheetState.conversationSheetContent) {
        // on each closing BottomSheet we revert BSContent to Home.
        // So in case if user opened BS, went to MuteStatus BS and closed it by clicking outside of BS,
        // then opens BS again - Home BS suppose to be opened, not MuteStatus BS
        snapshotFlow { sheetState.isVisible }.collect(FlowCollector { isVisible ->
            if (!isVisible) conversationSheetState.toHome()
        })
    }

    if (!isLoading) {
        deleteGroupDialogState.dismiss()
        leaveGroupDialogState.dismiss()
        clearConversationDialogState.dismiss()
    }

    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = rememberCoroutineScope(),
        sheetContent = {
            ConversationSheetContent(
                isBottomSheetVisible = getBottomSheetVisibility,
                conversationSheetState = conversationSheetState,
                onMutingConversationStatusChange = {
                    bottomSheetEventsHandler.onMutingConversationStatusChange(
                        conversationSheetState.conversationId,
                        conversationSheetState.conversationSheetContent!!.mutingConversationState
                    )
                },
                addConversationToFavourites = bottomSheetEventsHandler::onAddConversationToFavourites,
                moveConversationToFolder = bottomSheetEventsHandler::onMoveConversationToFolder,
                moveConversationToArchive = bottomSheetEventsHandler::onMoveConversationToArchive,
                clearConversationContent = clearConversationDialogState::show,
                blockUser = {},
                unblockUser = {},
                leaveGroup = leaveGroupDialogState::show,
                deleteGroup = deleteGroupDialogState::show
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
                    actions = { MoreOptionIcon(onButtonClicked = openBottomSheet) }
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

    ClearConversationContentDialog(
        dialogState = clearConversationDialogState,
        isLoading = isLoading,
        onClearConversationContent = {
            bottomSheetEventsHandler.onClearConversationContent(it)
        }
    )
}

enum class GroupConversationDetailsTabItem(@StringRes override val titleResId: Int) : TabItem {
    OPTIONS(R.string.conversation_details_options_tab),
    PARTICIPANTS(R.string.conversation_details_participants_tab);
}

@Preview
@Composable
fun PreviewGroupConversationDetails() {
    WireTheme(isPreview = true) {
        GroupConversationDetailsContent(
            conversationSheetContent = null,
            bottomSheetEventsHandler = GroupConversationDetailsBottomSheetEventsHandler.PREVIEW,
            onBackPressed = {},
            openFullListPressed = {},
            onProfilePressed = {},
            onAddParticipantsPressed = {},
            onLeaveGroup = {},
            onDeleteGroup = {},
            groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
            isLoading = false,
            messages = MutableSharedFlow(),
        )
    }
}
