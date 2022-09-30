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
    onMutingConversationStatusChange: (MutedConversationStatus) -> Unit = {},
    changeMemberRole: (Conversation.Member.Role) -> Unit = {},
    blockUser: (BlockUserDialogState) -> Unit = {},
    leaveGroup: (GroupDialogState) -> Unit = {},
    deleteGroup: (GroupDialogState) -> Unit = {},
    addConversationToFavourites: () -> Unit = {},
    moveConversationToFolder: () -> Unit = {},
    moveConversationToArchive: () -> Unit = {},
    clearConversationContent: () -> Unit = {}
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

    val dupa = remember {
        Dupa(
            OtherUserProfileSheetNavigation.Conversation(
                ConversationSheetContentState.Loading
            )
        )
    }

    val otherUserBottomSheetContentState = remember(conversationSheetContent, groupInfoAvailability) {
        OtherUserBottomSheetContentState(
            dupa,
            initialGroupInfoAvailability = groupInfoAvailability,
            initialConversationSheetContent = conversationSheetContent,
//            otherUserProfileSheetNavigation = otherUserProfileSheetNavigation.test,
            modalBottomSheetState = modalBottomSheetState,
            requestConversationDetailsOnDemand = requestOnConversationDetails
        )
    }

    return otherUserBottomSheetContentState
}

class Dupa(initialValue: OtherUserProfileSheetNavigation) {

    var test by mutableStateOf(
        initialValue
    )

}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    val dupa: Dupa,
    initialConversationSheetContent: ConversationSheetContent?,
    initialGroupInfoAvailability: GroupInfoAvailibility,
//    otherUserProfileSheetNavigation: OtherUserProfileSheetNavigation,
    val modalBottomSheetState: ModalBottomSheetState,
    val requestConversationDetailsOnDemand: () -> Unit
) {

    private val cipeczka by mutableStateOf(
        initialGroupInfoAvailability
    )

    private val cipa by mutableStateOf(
        initialConversationSheetContent
    )

    val otherUserProfileSheetNavigation: OtherUserProfileSheetNavigation by derivedStateOf {
        when (dupa.test) {
            is OtherUserProfileSheetNavigation.Conversation -> {
                OtherUserProfileSheetNavigation.Conversation(
                    if (initialConversationSheetContent == null) {
                        ConversationSheetContentState.Loading
                    } else {
                        ConversationSheetContentState.Loaded(
                            ConversationSheetState(cipa, ConversationNavigationOptions.Home)
                        )
                    }
                )
            }
            is OtherUserProfileSheetNavigation.RoleChange -> {
                OtherUserProfileSheetNavigation.RoleChange(cipeczka)
            }
        }
    }

//    private var _otherUserProfileSheetNavigation: OtherUserProfileSheetNavigation by mutableStateOf(
//        otherUserProfileSheetNavigation
//    )

    suspend fun showConversationOption() {
        if (cipa == null) {
            requestConversationDetailsOnDemand()
        }

        if (cipa != null) {
            dupa.test = OtherUserProfileSheetNavigation.Conversation(
                conversationSheetState = ConversationSheetContentState.Loaded(
                    ConversationSheetState(
                        conversationSheetContent = cipa,
                        conversationNavigationOptions = ConversationNavigationOptions.Home
                    )
                )
            )
        }

        modalBottomSheetState.show()
    }

    suspend fun showChangeRoleOption() {
        dupa.test = OtherUserProfileSheetNavigation.RoleChange(
            groupInfoAvailability = cipeczka
        )

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

    fun resetState() {
        dupa.test = OtherUserProfileSheetNavigation.Conversation(
            ConversationSheetContentState.Loading
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
