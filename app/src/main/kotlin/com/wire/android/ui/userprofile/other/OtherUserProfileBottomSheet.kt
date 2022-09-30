@file:OptIn(ExperimentalMaterialApi::class)

package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationNavigationOptions
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

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
    when (val otherUserProfileSheetNavigation = otherUserBottomSheetContentState.otherUserProfileSheetNavigation) {
        is OtherUserProfileSheetNavigation.Conversation -> {
            with(otherUserProfileSheetNavigation) {
                when (conversationSheetState) {
                    is ConversationSheetContentState.Loaded -> {
                        ConversationSheetContent(
                            conversationSheetState = conversationSheetState.conversationSheetState,
                            onMutingConversationStatusChange = onMutingConversationStatusChange,
                            blockUser = blockUser,
                            leaveGroup = leaveGroup,
                            deleteGroup = deleteGroup,
                            addConversationToFavourites = { },
                            moveConversationToFolder = { },
                            moveConversationToArchive = { },
                            clearConversationContent = { }
                        )
                    }
                    ConversationSheetContentState.Loading -> {}
                }
            }
        }
        is OtherUserProfileSheetNavigation.RoleChange -> {
            with(otherUserProfileSheetNavigation) {
                if (groupInfoAvailability is GroupInfoAvailibility.Available) {
                    EditGroupRoleBottomSheet(
                        groupState = groupInfoAvailability.otherUserProfileGroupInfo,
                        changeMemberRole = changeMemberRole
                    )
                }
            }
        }
    }

    // without clearing BottomSheet after every closing there could be strange UI behaviour.
    // Example: open some big BottomSheet (ConversationBS), close it, then open small BS (ChangeRoleBS) ->
    // in that case user will see ChangeRoleBS at the center of the screen (just for few milliseconds)
    // and then it moves to the bottom.
    // It happens cause when `sheetState.show()` is called, it calculates animation offset by the old BS height (which was big)
    // To avoid such case we clear BS content on every closing
    with(otherUserBottomSheetContentState) {
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
            OtherUserProfileSheetNavigation.Conversation(
                conversationSheetState = ConversationSheetContentState.Loading
            )
        )
    }

    val otherUserBottomSheetContentState = remember(conversationSheetContent, groupInfoAvailability) {
        OtherUserBottomSheetContentState(
            groupInfoAvailability = groupInfoAvailability,
            conversationSheetContent = conversationSheetContent,
            otherUserProfileSheetNavigationState = otherUserProfileSheetNavigationState,
            modalBottomSheetState = modalBottomSheetState,
            requestConversationDetailsOnDemand = requestOnConversationDetails
        )
    }

    return otherUserBottomSheetContentState
}

class OtherUserProfileSheetNavigationState(initialValue: OtherUserProfileSheetNavigation) {

    var otherUserProfileSheetNavigation by mutableStateOf(
        initialValue
    )

}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    conversationSheetContent: ConversationSheetContent?,
    groupInfoAvailability: GroupInfoAvailibility,
    val otherUserProfileSheetNavigationState: OtherUserProfileSheetNavigationState,
    val modalBottomSheetState: ModalBottomSheetState,
    val requestConversationDetailsOnDemand: () -> Unit
) {

    private val groupInfoAvailability by mutableStateOf(
        groupInfoAvailability
    )

    private val conversationSheetContent by mutableStateOf(
        conversationSheetContent
    )

    val otherUserProfileSheetNavigation: OtherUserProfileSheetNavigation by derivedStateOf {
        when (otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation) {
            is OtherUserProfileSheetNavigation.Conversation -> {
                OtherUserProfileSheetNavigation.Conversation(
                    if (conversationSheetContent == null) {
                        ConversationSheetContentState.Loading
                    } else {
                        ConversationSheetContentState.Loaded(
                            ConversationSheetState(
                                conversationSheetContent = conversationSheetContent,
                                conversationNavigationOptions = ConversationNavigationOptions.Home
                            )
                        )
                    }
                )
            }
            is OtherUserProfileSheetNavigation.RoleChange -> {
                OtherUserProfileSheetNavigation.RoleChange(groupInfoAvailability = groupInfoAvailability)
            }
        }
    }

    suspend fun showConversationOption() {
        if (conversationSheetContent == null) {
            requestConversationDetailsOnDemand()
        }

        if (conversationSheetContent != null) {
            otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Conversation(
                conversationSheetState = ConversationSheetContentState.Loaded(
                    ConversationSheetState(
                        conversationSheetContent = conversationSheetContent,
                        conversationNavigationOptions = ConversationNavigationOptions.Home
                    )
                )
            )
        }

        modalBottomSheetState.show()
    }

    suspend fun showChangeRoleOption() {
        otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.RoleChange(
            groupInfoAvailability = groupInfoAvailability
        )

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

    fun resetState() {
        otherUserProfileSheetNavigationState.otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Conversation(
            conversationSheetState = ConversationSheetContentState.Loading
        )
    }

}

sealed class OtherUserProfileSheetNavigation {
    data class Conversation(
        val conversationSheetState: ConversationSheetContentState
    ) : OtherUserProfileSheetNavigation()

    data class RoleChange(val groupInfoAvailability: GroupInfoAvailibility) : OtherUserProfileSheetNavigation()

}

sealed class ConversationSheetContentState {
    object Loading : ConversationSheetContentState()
    data class Loaded(val conversationSheetState: ConversationSheetState) : ConversationSheetContentState()
}
