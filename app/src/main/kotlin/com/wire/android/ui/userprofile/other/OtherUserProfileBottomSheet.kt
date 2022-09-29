package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState

@Composable
fun OtherUserProfileBottomSheet(
    otherUserBottomSheetContentState: OtherUserBottomSheetContentState
) {
    when (val otherUserProfileSheetNavigation = otherUserBottomSheetContentState.otherUserProfileSheetNavigation) {
        is OtherUserProfileSheetNavigation.Conversation -> {
            when (otherUserProfileSheetNavigation.conversationSheetState) {
                is ConversationSheetContentState.Loaded -> {
                    ConversationSheetContent(conversationSheetState = otherUserProfileSheetNavigation.conversationSheetState.conversationSheetState)
                }
                ConversationSheetContentState.Loading -> {}
            }
        }
        is OtherUserProfileSheetNavigation.RoleChange -> {
            if (otherUserProfileSheetNavigation.groupInfoAvailability is GroupInfoAvailibility.Available) {
                EditGroupRoleBottomSheet(
                    groupState = otherUserProfileSheetNavigation.groupInfoAvailability.otherUserProfileGroupInfo,
                )
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

    val otherUserBottomSheetContentState = remember(conversationSheetContent, groupInfoAvailability) {
        OtherUserBottomSheetContentState(
            modalBottomSheetState = modalBottomSheetState,
            groupInfoAvailability = groupInfoAvailability,
            conversationSheetContent = if (conversationSheetContent == null) {
                ConversationSheetContentState.Loading
            } else {
                ConversationSheetContentState.Loaded(
                    conversationSheetState = ConversationSheetState(
                        conversationSheetContent = conversationSheetContent
                    )
                )
            },
            requestConversationDetailsOnDemand = requestOnConversationDetails
        )
    }

    return otherUserBottomSheetContentState
}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    val modalBottomSheetState: ModalBottomSheetState,
    val groupInfoAvailability: GroupInfoAvailibility,
    val conversationSheetContent: ConversationSheetContentState,
    val requestConversationDetailsOnDemand: () -> Unit
) {

    var otherUserProfileSheetNavigation: OtherUserProfileSheetNavigation by mutableStateOf(
        OtherUserProfileSheetNavigation.Conversation(
            conversationSheetContent
        )
    )

    suspend fun showConversationOption() {
        if (conversationSheetContent is ConversationSheetContentState.Loading) {
            requestConversationDetailsOnDemand()
        }

        otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Conversation(conversationSheetContent)

        modalBottomSheetState.show()
    }

    suspend fun showChangeRoleOption() {
        otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.RoleChange(groupInfoAvailability)

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

}

sealed class OtherUserProfileSheetNavigation {
    data class Conversation(val conversationSheetState: ConversationSheetContentState) : OtherUserProfileSheetNavigation()
    data class RoleChange(val groupInfoAvailability: GroupInfoAvailibility) : OtherUserProfileSheetNavigation()

}

sealed class ConversationSheetContentState {
    object Loading : ConversationSheetContentState()
    data class Loaded(val conversationSheetState: ConversationSheetState) : ConversationSheetContentState()
}
