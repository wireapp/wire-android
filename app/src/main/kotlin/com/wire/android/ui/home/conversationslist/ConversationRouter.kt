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

package com.wire.android.ui.home.conversationslist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionNavigation
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversationslist.all.AllConversationScreen
import com.wire.android.ui.home.conversationslist.call.CallsScreen
import com.wire.android.ui.home.conversationslist.mention.MentionScreen
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.search.SearchConversationScreen
import kotlinx.coroutines.flow.MutableSharedFlow

// Since the HomeScreen is responsible for displaying the bottom sheet content,
// we create a bridge that passes the content of the BottomSheet
// also we expose the lambda which expands the BottomSheet from the HomeScreen
@Composable
fun ConversationRouterHomeBridge(
    conversationItemType: ConversationItemType,
    onHomeBottomSheetContentChanged: (@Composable ColumnScope.() -> Unit) -> Unit,
    onOpenBottomSheet: () -> Unit,
    onCloseBottomSheet: () -> Unit,
    onSnackBarStateChanged: (HomeSnackbarState) -> Unit,
    searchBarState: SearchBarState,
    isBottomSheetVisible: () -> Boolean
) {
    val viewModel: ConversationListViewModel = hiltViewModel()

    val conversationRouterHomeState = rememberConversationRouterState(
        initialConversationItemType = conversationItemType,
        homeSnackBarState = viewModel.homeSnackBarState,
        closeBottomSheetState = viewModel.closeBottomSheet,
        requestInProgress = viewModel.requestInProgress,
        onSnackBarStateChanged = onSnackBarStateChanged,
        onCloseBottomSheet = onCloseBottomSheet,
    )

    with(searchBarState) {
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                conversationRouterHomeState.openSearch()
            } else {
                conversationRouterHomeState.closeSearch()
            }
        }

        LaunchedEffect(searchQuery) {
            viewModel.searchConversation(searchQuery)
        }
    }

    with(conversationRouterHomeState) {
        fun openConversationBottomSheet(
            conversationItem: ConversationItem,
            conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
        ) {
            onHomeBottomSheetContentChanged {
                // if we just use [conversationItem] we won't be able to observe changes in conversation details (e.g. name changing).
                // So we need to find ConversationItem in the State by id and use it for BottomSheet content.
                val item: ConversationItem? = viewModel.conversationListState.findConversationById(conversationItem.conversationId)

                val conversationState = rememberConversationSheetState(
                    conversationItem = item ?: conversationItem,
                    conversationOptionNavigation = conversationOptionNavigation
                )
                // if we reopen the BottomSheet of the previous conversation for example:
                // when the user swipes down the BottomSheet manually when having mute option open
                // we want to reopen it in the "home" section, but ONLY when the user reopens the BottomSheet
                // by holding the conversation item, not when the notification icon is pressed, therefore when
                // conversationOptionNavigation is equal to ConversationOptionNavigation.MutingNotificationOption
                conversationState.conversationId?.let { conversationId ->
                    if (conversationId == conversationItem.conversationId &&
                        conversationOptionNavigation != ConversationOptionNavigation.MutingNotificationOption
                    ) {
                        conversationState.toHome()
                    }
                }

                ConversationSheetContent(
                    conversationSheetState = conversationState,
                    onMutingConversationStatusChange = {
                        viewModel.muteConversation(
                            conversationId = conversationState.conversationId,
                            mutedConversationStatus = conversationState.conversationSheetContent!!.mutingConversationState
                        )
                    },
                    addConversationToFavourites = viewModel::addConversationToFavourites,
                    moveConversationToFolder = viewModel::moveConversationToFolder,
                    moveConversationToArchive = viewModel::moveConversationToArchive,
                    clearConversationContent = clearContentDialogState::show,
                    blockUser = blockUserDialogState::show,
                    unblockUser = unblockUserDialogState::show,
                    leaveGroup = leaveGroupDialogState::show,
                    deleteGroup = deleteGroupDialogState::show,
                    closeBottomSheet = onCloseBottomSheet,
                    isBottomSheetVisible = isBottomSheetVisible
                )
            }
            onOpenBottomSheet()
        }

        val onEditConversationItem: (ConversationItem) -> Unit = remember {
            { conversationItem ->
                openConversationBottomSheet(
                    conversationItem = conversationItem
                )
            }
        }

        val onEditNotifications: (ConversationItem) -> Unit = remember {
            { conversationItem ->
                openConversationBottomSheet(
                    conversationItem = conversationItem,
                    conversationOptionNavigation = ConversationOptionNavigation.MutingNotificationOption
                )
            }
        }

        with(viewModel.conversationListState) {
            when (conversationRouterHomeState.conversationItemType) {
                ConversationItemType.ALL_CONVERSATIONS ->
                    AllConversationScreen(
                        conversations = foldersWithConversations,
                        hasNoConversations = hasNoConversations,
                        onEditConversation = onEditConversationItem,
                        onOpenConversationNotificationsSettings = onEditNotifications,
                    )

                ConversationItemType.CALLS ->
                    CallsScreen(
                        missedCalls = missedCalls,
                        callHistory = callHistory,
                        onCallItemClick = viewModel::openConversation,
                        onEditConversationItem = onEditConversationItem,
                        onOpenUserProfile = viewModel::openUserProfile,
                        openConversationNotificationsSettings = onEditNotifications,
                        onJoinCall = viewModel::joinOngoingCall
                    )

                ConversationItemType.MENTIONS ->
                    MentionScreen(
                        unreadMentions = unreadMentions,
                        allMentions = allMentions,
                        onMentionItemClick = viewModel::openConversation,
                        onEditConversationItem = onEditConversationItem,
                        onOpenUserProfile = viewModel::openUserProfile,
                        openConversationNotificationsSettings = onEditNotifications,
                        onJoinCall = viewModel::joinOngoingCall
                    )

                ConversationItemType.SEARCH -> {
                    SearchConversationScreen(
                        searchQuery = searchQuery,
                        conversationSearchResult = conversationSearchResult,
                        onOpenNewConversation = viewModel::openNewConversation,
                        onOpenConversation = viewModel::openConversation,
                        onEditConversation = onEditConversationItem,
                        onOpenUserProfile = viewModel::openUserProfile,
                        onOpenConversationNotificationsSettings = onEditNotifications,
                        onJoinCall = viewModel::joinOngoingCall,
                    )
                }
            }
        }

        BlockUserDialogContent(
            isLoading = requestInProgress,
            dialogState = blockUserDialogState,
            onBlock = viewModel::blockUser
        )

        DeleteConversationGroupDialog(
            isLoading = requestInProgress,
            dialogState = deleteGroupDialogState,
            onDeleteGroup = viewModel::deleteGroup
        )

        LeaveConversationGroupDialog(
            dialogState = leaveGroupDialogState,
            isLoading = requestInProgress,
            onLeaveGroup = viewModel::leaveGroup
        )

        UnblockUserDialogContent(
            dialogState = unblockUserDialogState,
            onUnblock = viewModel::unblockUser,
            isLoading = requestInProgress,
        )

        ClearConversationContentDialog(
            dialogState = clearContentDialogState,
            isLoading = requestInProgress,
            onClearConversationContent = viewModel::clearConversationContent
        )

        BackHandler(conversationItemType == ConversationItemType.SEARCH) {
            closeSearch()
        }
    }
}

@Suppress("LongParameterList")
class ConversationRouterState(
    private val initialItemType: ConversationItemType,
    val leaveGroupDialogState: VisibilityState<GroupDialogState>,
    val deleteGroupDialogState: VisibilityState<GroupDialogState>,
    val blockUserDialogState: VisibilityState<BlockUserDialogState>,
    val unblockUserDialogState: VisibilityState<UnblockUserDialogState>,
    val clearContentDialogState: VisibilityState<DialogState>,
    requestInProgress: Boolean
) {

    var requestInProgress: Boolean by mutableStateOf(requestInProgress)

    var conversationItemType: ConversationItemType by mutableStateOf(initialItemType)

    fun openSearch() {
        conversationItemType = ConversationItemType.SEARCH
    }

    fun closeSearch() {
        conversationItemType = initialItemType
    }
}

@Composable
fun rememberConversationRouterState(
    initialConversationItemType: ConversationItemType,
    homeSnackBarState: MutableSharedFlow<HomeSnackbarState>,
    onSnackBarStateChanged: (HomeSnackbarState) -> Unit,
    closeBottomSheetState: MutableSharedFlow<Unit>,
    onCloseBottomSheet: () -> Unit,
    requestInProgress: Boolean
): ConversationRouterState {

    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val clearContentDialogState = rememberVisibilityState<DialogState>()

    LaunchedEffect(Unit) {
        homeSnackBarState.collect { onSnackBarStateChanged(it) }
    }

    LaunchedEffect(Unit) {
        closeBottomSheetState.collect { onCloseBottomSheet() }
    }

    val conversationRouterState = remember(initialConversationItemType) {
        ConversationRouterState(
            initialConversationItemType,
            leaveGroupDialogState,
            deleteGroupDialogState,
            blockUserDialogState,
            unblockUserDialogState,
            clearContentDialogState,
            requestInProgress
        )
    }

    LaunchedEffect(requestInProgress) {
        if (!requestInProgress) {
            leaveGroupDialogState.dismiss()
            deleteGroupDialogState.dismiss()
            blockUserDialogState.dismiss()
            unblockUserDialogState.dismiss()
            clearContentDialogState.dismiss()
        }

        conversationRouterState.requestInProgress = requestInProgress
    }

    return conversationRouterState
}

enum class ConversationItemType {
    ALL_CONVERSATIONS, CALLS, MENTIONS, SEARCH;
}
