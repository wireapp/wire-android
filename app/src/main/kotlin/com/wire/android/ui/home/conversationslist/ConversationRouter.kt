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
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.dialog.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.dialog.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationOptionNavigation
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.rememberConversationSheetState
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.DialogState
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
    onSnackBarStateChanged: (HomeSnackbarState) -> Unit
) {
    val viewModel: ConversationListViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.snackBarState.collect { onSnackBarStateChanged(it) }
    }

    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val clearConversationContentDialogState = rememberVisibilityState<DialogState>()

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
                clearConversationContent = clearConversationContentDialogState::show,
                blockUser = viewModel::onBlockUserClicked,
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
                    newActivities = newActivities,
                    conversations = conversations,
                    onOpenConversation = viewModel::openConversation,
                    onEditConversation = onEditConversationItem,
                    onOpenUserProfile = viewModel::openUserProfile,
                    onOpenConversationNotificationsSettings = onEditNotifications,
                    onJoinCall = viewModel::joinOngoingCall
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

    //TODO: refactor to use the mechanism as DeleteConversationGroupDialog
    // for example
    BlockUserDialogContent(
        dialogState = viewModel.blockUserDialogState,
        dismiss = viewModel::onDismissBlockUserDialog,
        onBlock = viewModel::onBlockUserClicked
    )

    DeleteConversationGroupDialog(
        isLoading = viewModel.requestInProgress,
        dialogState = deleteGroupDialogState,
        onDeleteGroup = viewModel::deleteGroup
    )

    LeaveConversationGroupDialog(
        isLoading = viewModel.requestInProgress,
        dialogState = leaveGroupDialogState,
        onLeaveGroup = viewModel::leaveGroup
    )

    ClearConversationContentDialog(
        isLoading = viewModel.requestInProgress,
        dialogState = clearConversationContentDialogState,
        onClearConversationContent = viewModel::clearConversationContent
    )
}

enum class ConversationItemType {
    ALL_CONVERSATIONS, CALLS, MENTIONS;
}
