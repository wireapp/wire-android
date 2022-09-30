@file:OptIn(ExperimentalMaterialApi::class)

package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtherUserProfileBottomSheet(
    otherUserBottomSheetContentState: OtherUserBottomSheetContentState,
    onMutingConversationStatusChange: (MutedConversationStatus) -> Unit,
    changeMemberRole: (Conversation.Member.Role) -> Unit,
    blockUser: (BlockUserDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    deleteGroup: (GroupDialogState) -> Unit,
    addConversationToFavourites: () -> Unit = { },
    moveConversationToFolder: () -> Unit = { },
    moveConversationToArchive: () -> Unit = { },
    clearConversationContent: () -> Unit = { }
) {
    with(otherUserBottomSheetContentState) {
        with(otherUserProfileSheetNavigationState) {
            when (otherUserProfileSheetNavigation) {
                is OtherUserProfileSheetNavigation.Conversation -> {
                    val currentConversationSheetContentState = conversationSheetContentState

                    if (currentConversationSheetContentState is ConversationSheetContentState.Loaded) {
                        ConversationSheetContent(
                            conversationSheetState = ConversationSheetState(currentConversationSheetContentState.conversationSheetContent),
                            onMutingConversationStatusChange = onMutingConversationStatusChange,
                            blockUser = blockUser,
                            leaveGroup = leaveGroup,
                            deleteGroup = deleteGroup,
                            addConversationToFavourites = { },
                            moveConversationToFolder = { },
                            moveConversationToArchive = { },
                            clearConversationContent = { }
                        )
                    } else {
                        { }
                    }
                }
                is OtherUserProfileSheetNavigation.RoleChange -> {
                    val currentGroupInfoAvailability = groupInfoAvailability

                    if (currentGroupInfoAvailability is GroupInfoAvailibility.Available) {
                        EditGroupRoleBottomSheet(
                            groupState = currentGroupInfoAvailability.otherUserProfileGroupInfo,
                            changeMemberRole = changeMemberRole
                        )
                    } else {
                        { }
                    }
                }
                is OtherUserProfileSheetNavigation.Empty -> {

                }
            }
        }

        // without clearing BottomSheet after every closing there could be strange UI behaviour.
        // Example: open some big BottomSheet (ConversationBS), close it, then open small BS (ChangeRoleBS) ->
        // in that case user will see ChangeRoleBS at the center of the screen (just for few milliseconds)
        // and then it moves to the bottom.
        // It happens cause when `sheetState.show()` is called, it calculates animation offset by the old BS height (which was big)
        // To avoid such case we clear BS content on every closing
        LaunchedEffect(modalBottomSheetState.isVisible) {
            if (!modalBottomSheetState.isVisible
                && !modalBottomSheetState.isAnimationRunning
            ) {
                resetState()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberOtherUserBottomSheetContentState(
    requestOnConversationDetails: () -> Unit,
    conversationSheetContent: ConversationSheetContent?,
    groupInfoAvailability: GroupInfoAvailibility
): OtherUserBottomSheetContentState {
    val modalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    val otherUserProfileSheetNavigationState = remember {
        OtherUserProfileSheetNavigationState(
            OtherUserProfileSheetNavigation.Conversation
        )
    }

    val otherUserBottomSheetContentState = remember {
        OtherUserBottomSheetContentState(
            otherUserProfileSheetNavigationState = otherUserProfileSheetNavigationState,
            modalBottomSheetState = modalBottomSheetState,
            requestConversationDetailsOnDemand = requestOnConversationDetails
        )
    }

    LaunchedEffect(conversationSheetContent) {
        if (conversationSheetContent != null) {
            otherUserProfileSheetNavigationState.updateConversationSheetContent(conversationSheetContent)
        }
    }

    LaunchedEffect(groupInfoAvailability) {
        otherUserProfileSheetNavigationState.updateGroupInfoAvailability(groupInfoAvailability)
    }

    return otherUserBottomSheetContentState
}

class OtherUserProfileSheetNavigationState(initialValue: OtherUserProfileSheetNavigation) {

    var otherUserProfileSheetNavigation by mutableStateOf(
        initialValue
    )

    var conversationSheetContentState: ConversationSheetContentState by mutableStateOf(
        ConversationSheetContentState.Loading
    )

    var groupInfoAvailability: GroupInfoAvailibility by mutableStateOf(
        GroupInfoAvailibility.NotAvailable
    )

    fun updateConversationSheetContent(conversationSheetContent: ConversationSheetContent) {
        this.conversationSheetContentState = ConversationSheetContentState.Loaded(conversationSheetContent)
    }

    fun updateGroupInfoAvailability(groupInfoAvailability: GroupInfoAvailibility) {
        this.groupInfoAvailability = groupInfoAvailability
    }

}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    val otherUserProfileSheetNavigationState: OtherUserProfileSheetNavigationState,
    val modalBottomSheetState: ModalBottomSheetState,
    val requestConversationDetailsOnDemand: () -> Unit
) {
    suspend fun showConversationOption() {
        coroutineScope {
            if (otherUserProfileSheetNavigationState.conversationSheetContentState is ConversationSheetContentState.Loading) {
                launch {
                    delay(3000)
                    requestConversationDetailsOnDemand()
                }
            }

            otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Conversation

            modalBottomSheetState.show()
        }
    }

    suspend fun showChangeRoleOption() {
        otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.RoleChange

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

    fun resetState() {
        otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Empty
    }

}

sealed class OtherUserProfileSheetNavigation {
    object Conversation : OtherUserProfileSheetNavigation()
    object RoleChange : OtherUserProfileSheetNavigation()
    object Empty : OtherUserProfileSheetNavigation()

}

sealed class ConversationSheetContentState {
    object Loading : ConversationSheetContentState()
    data class Loaded(val conversationSheetContent: ConversationSheetContent) : ConversationSheetContentState()
}
