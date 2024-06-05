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

package com.wire.android.ui.home.conversationslist

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.getOngoingCallIntent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout2
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionNavigation
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.dialogs.ArchiveConversationDialog
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversationslist.all.AllConversationScreenContent
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.isArchive
import com.wire.android.ui.home.conversationslist.search.SearchConversationScreen
import com.wire.android.util.permission.PermissionDenialType
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

// Since the HomeScreen is responsible for displaying the bottom sheet content,
// we create a bridge that passes the content of the BottomSheet
// also we expose the lambda which expands the BottomSheet from the HomeScreen
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComplexMethod")
@Composable
fun ConversationRouterHomeBridge(
    navigator: Navigator,
    conversationItemType: ConversationItemType,
    searchBarState: SearchBarState,
    conversationsSource: ConversationsSource = ConversationsSource.MAIN,
    conversationListViewModel: ConversationListViewModel = hiltViewModel(),
    conversationCallListViewModel: ConversationCallListViewModel = hiltViewModel(),
) {
    var currentSheetConversationItem by remember {
        mutableStateOf<ConversationItem?>(null)
    }
    var currentConversationOptionNavigation by remember {
        mutableStateOf<ConversationOptionNavigation>(ConversationOptionNavigation.Home)
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true },
    )
    val coroutineScope = rememberCoroutineScope()

    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val activity = LocalActivity.current

    LaunchedEffect(conversationsSource) {
        conversationListViewModel.updateConversationsSource(conversationsSource)
    }

    LaunchedEffect(key1 = currentSheetConversationItem) {
        if (currentSheetConversationItem != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            currentSheetConversationItem = null
        }
    }

    LaunchedEffect(Unit) {
        conversationListViewModel.infoMessage.collect {
            currentSheetConversationItem = null
        }
    }

    LaunchedEffect(Unit) {
        conversationListViewModel.closeBottomSheet.collect {
            currentSheetConversationItem = null
        }
    }

    val conversationRouterHomeState = rememberConversationRouterState(
        initialConversationItemType = conversationItemType,
        requestInProgress = conversationListViewModel.requestInProgress
    )

    with(searchBarState) {
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                conversationListViewModel.refreshMissingMetadata()
                conversationRouterHomeState.openSearch()
            } else {
                conversationRouterHomeState.closeSearch()
            }
        }

        LaunchedEffect(searchQueryTextState.text) {
            conversationListViewModel.searchQueryChanged(searchQueryTextState.text.toString())
        }
    }

    fun showConfirmationDialogOrUnarchive(): (DialogState) -> Unit {
        return { dialogState ->
            if (dialogState.isArchived) {
                conversationListViewModel.moveConversationToArchive(dialogState)
            } else {
                conversationRouterHomeState.archiveConversationDialogState.show(dialogState)
            }
        }
    }

    with(conversationRouterHomeState) {
        val onEditConversationItem: (ConversationItem) -> Unit = remember {
            {
                currentSheetConversationItem = it
                currentConversationOptionNavigation = ConversationOptionNavigation.Home
            }
        }

        val onOpenConversation: (ConversationId) -> Unit = remember(navigator) {
            { conversationId -> navigator.navigate(NavigationCommand(ConversationScreenDestination(conversationId))) }
        }
        val onOpenUserProfile: (UserId) -> Unit = remember(navigator) {
            { userId -> navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(userId))) }
        }
        val onJoinedCall: (ConversationId) -> Unit = remember(navigator) {
            {
                getOngoingCallIntent(activity, it.toString()).run {
                    activity.startActivity(this)
                }
            }
        }

        with(conversationListViewModel.conversationListState) {
            when (conversationRouterHomeState.conversationItemType) {
                ConversationItemType.ALL_CONVERSATIONS ->
                    AllConversationScreenContent(
                        conversations = foldersWithConversations,
                        isFromArchive = conversationsSource.isArchive(),
                        hasNoConversations = hasNoConversations,
                        onEditConversation = onEditConversationItem,
                        onOpenConversation = onOpenConversation,
                        onOpenUserProfile = onOpenUserProfile,
                        onJoinedCall = onJoinedCall,
                        onPermissionPermanentlyDenied = {
                            if (it == PermissionDenialType.CallingMicrophone) {
                                permissionPermanentlyDeniedDialogState.show(
                                    PermissionPermanentlyDeniedDialogState.Visible(
                                        R.string.app_permission_dialog_title,
                                        R.string.call_permission_dialog_description
                                    )
                                )
                            }
                        },
                        conversationListCallState = conversationCallListViewModel.conversationListCallState,
                        dismissJoinCallAnywayDialog = conversationCallListViewModel::dismissJoinCallAnywayDialog,
                        joinCallAnyway = conversationCallListViewModel::joinAnyway,
                        joinOngoingCall = conversationCallListViewModel::joinOngoingCall
                    )

                ConversationItemType.SEARCH -> {
                    SearchConversationScreen(
                        searchQuery = searchQuery,
                        conversationSearchResult = conversationSearchResult,
                        onOpenNewConversation = { navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination)) },
                        onOpenConversation = onOpenConversation,
                        onEditConversation = onEditConversationItem,
                        onOpenUserProfile = onOpenUserProfile,
                        onJoinCall = {
                            conversationCallListViewModel.joinOngoingCall(it, onJoinedCall)
                        },
                        onPermissionPermanentlyDenied = { }
                    )
                }
            }
        }

        PermissionPermanentlyDeniedDialog(
            dialogState = permissionPermanentlyDeniedDialogState,
            hideDialog = permissionPermanentlyDeniedDialogState::dismiss
        )

        BlockUserDialogContent(
            isLoading = requestInProgress,
            dialogState = blockUserDialogState,
            onBlock = conversationListViewModel::blockUser
        )

        DeleteConversationGroupDialog(
            isLoading = requestInProgress,
            dialogState = deleteGroupDialogState,
            onDeleteGroup = conversationListViewModel::deleteGroup
        )

        LeaveConversationGroupDialog(
            dialogState = leaveGroupDialogState,
            isLoading = requestInProgress,
            onLeaveGroup = conversationListViewModel::leaveGroup
        )

        UnblockUserDialogContent(
            dialogState = unblockUserDialogState,
            onUnblock = conversationListViewModel::unblockUser,
            isLoading = requestInProgress,
        )

        ClearConversationContentDialog(
            dialogState = clearContentDialogState,
            isLoading = requestInProgress,
            onClearConversationContent = conversationListViewModel::clearConversationContent
        )

        ArchiveConversationDialog(
            dialogState = archiveConversationDialogState,
            onArchiveButtonClicked = conversationListViewModel::moveConversationToArchive
        )

        currentSheetConversationItem?.let {
            WireModalSheetLayout2(
                sheetState = sheetState,
                coroutineScope = coroutineScope,
                sheetContent = {
                    val conversationState = rememberConversationSheetState(
                        conversationItem = it,
                        conversationOptionNavigation = currentConversationOptionNavigation
                    )

                    ConversationSheetContent(
                        conversationSheetState = conversationState,
                        onMutingConversationStatusChange = {
                            conversationListViewModel.muteConversation(
                                conversationId = conversationState.conversationId,
                                mutedConversationStatus = conversationState.conversationSheetContent!!.mutingConversationState
                            )
                        },
                        addConversationToFavourites = conversationListViewModel::addConversationToFavourites,
                        moveConversationToFolder = conversationListViewModel::moveConversationToFolder,
                        updateConversationArchiveStatus = showConfirmationDialogOrUnarchive(),
                        clearConversationContent = clearContentDialogState::show,
                        blockUser = blockUserDialogState::show,
                        unblockUser = unblockUserDialogState::show,
                        leaveGroup = leaveGroupDialogState::show,
                        deleteGroup = deleteGroupDialogState::show,
                        closeBottomSheet = {
                            currentSheetConversationItem = null
                        }
                    )
                },
                onCloseBottomSheet = {
                    currentSheetConversationItem = null
                }
            )
        }

        BackHandler(conversationItemType == ConversationItemType.SEARCH) {
            closeSearch()
        }
    }

    SnackBarMessageHandler(infoMessages = conversationListViewModel.infoMessage)
}

@Suppress("LongParameterList")
class ConversationRouterState(
    private val initialItemType: ConversationItemType,
    val leaveGroupDialogState: VisibilityState<GroupDialogState>,
    val deleteGroupDialogState: VisibilityState<GroupDialogState>,
    val blockUserDialogState: VisibilityState<BlockUserDialogState>,
    val unblockUserDialogState: VisibilityState<UnblockUserDialogState>,
    val clearContentDialogState: VisibilityState<DialogState>,
    val archiveConversationDialogState: VisibilityState<DialogState>,
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
    requestInProgress: Boolean
): ConversationRouterState {

    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val clearContentDialogState = rememberVisibilityState<DialogState>()
    val archiveConversationDialogState = rememberVisibilityState<DialogState>()

    val conversationRouterState = remember(initialConversationItemType) {
        ConversationRouterState(
            initialConversationItemType,
            leaveGroupDialogState,
            deleteGroupDialogState,
            blockUserDialogState,
            unblockUserDialogState,
            clearContentDialogState,
            archiveConversationDialogState,
            requestInProgress,
        )
    }

    LaunchedEffect(requestInProgress) {
        if (!requestInProgress) {
            leaveGroupDialogState.dismiss()
            deleteGroupDialogState.dismiss()
            blockUserDialogState.dismiss()
            unblockUserDialogState.dismiss()
            clearContentDialogState.dismiss()
            archiveConversationDialogState.dismiss()
        }

        conversationRouterState.requestInProgress = requestInProgress
    }

    return conversationRouterState
}

enum class ConversationItemType {
    ALL_CONVERSATIONS, SEARCH;
}
