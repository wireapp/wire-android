package com.wire.android.ui.home.conversationslist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationOptionNavigation
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.rememberConversationSheetState
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.GroupDialogState

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
// Since the HomeScreen is responsible for displaying the bottom sheet content,
// we create a bridge that passes the content of the BottomSheet
// also we expose the lambda which expands the BottomSheet from the HomeScreen
@Composable
fun ConversationRouterHomeBridge(
    itemType: ConversationItemType,
    onHomeBottomSheetContentChanged: (@Composable ColumnScope.() -> Unit) -> Unit,
    onOpenBottomSheet: () -> Unit,
    onCloseBottomSheet: () -> Unit,
    onSnackBarStateChanged: (HomeSnackbarState) -> Unit
) {
    val viewModel: ConversationListViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.snackBarState.collect { onSnackBarStateChanged(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.closeBottomSheet.collect { onCloseBottomSheet() }
    }

    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()

    val requestInProgress = viewModel.requestInProgress
    if (!requestInProgress) {
        leaveGroupDialogState.dismiss()
        deleteGroupDialogState.dismiss()
        blockUserDialogState.dismiss()
        unblockUserDialogState.dismiss()
    }

    fun openConversationBottomSheet(
        conversationItem: ConversationItem,
        conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
    ) {
        onHomeBottomSheetContentChanged {
            val conversationState = rememberConversationSheetState(
                conversationItem = conversationItem,
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
                // FIXME: Compose - Find a way to not recreate this lambda
                onMutingConversationStatusChange = { mutedStatus ->
                    conversationState.muteConversation(mutedStatus)
                    viewModel.muteConversation(conversationId = conversationState.conversationId, mutedStatus)
                },
                addConversationToFavourites = viewModel::addConversationToFavourites,
                moveConversationToFolder = viewModel::moveConversationToFolder,
                moveConversationToArchive = viewModel::moveConversationToArchive,
                clearConversationContent = viewModel::clearConversationContent,
                blockUser = blockUserDialogState::show,
                unblockUser = unblockUserDialogState::show,
                leaveGroup = leaveGroupDialogState::show,
                deleteGroup = deleteGroupDialogState::show
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

    with(viewModel.state) {
        when (itemType) {
            ConversationItemType.ALL_CONVERSATIONS ->
                AllConversationScreen(
                    conversations = conversations,
                    onOpenConversation = viewModel::openConversation,
                    onEditConversation = onEditConversationItem,
                    onOpenUserProfile = viewModel::openUserProfile,
                    onOpenConversationNotificationsSettings = onEditNotifications,
                    onJoinCall = viewModel::joinOngoingCall,
                    shouldShowEmptyState= shouldShowEmptyState
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
}

enum class ConversationItemType {
    ALL_CONVERSATIONS, CALLS, MENTIONS;
}
