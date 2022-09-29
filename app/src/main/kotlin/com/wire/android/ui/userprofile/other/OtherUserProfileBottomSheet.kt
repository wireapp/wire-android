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
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationNavigationOptions
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState

@Composable
fun OtherUserProfileBottomSheet(
    otherUserBottomSheetContentState: OtherUserBottomSheetContentState
) {
    when (val otherUserProfileSheetNavigation = otherUserBottomSheetContentState.otherUserProfileSheetNavigation) {
        is OtherUserProfileSheetNavigation.Conversation -> {
            with(otherUserProfileSheetNavigation) {
                when (conversationSheetState) {
                    is ConversationSheetContentState.Loaded -> {
                        ConversationSheetContent(
                            conversationSheetState = conversationSheetState.conversationSheetState,
                            onMutingConversationStatusChange = { },
                            addConversationToFavourites = { },
                            moveConversationToFolder = { },
                            moveConversationToArchive = { },
                            clearConversationContent = { },
                            blockUser = { },
                            leaveGroup = { },
                            deleteGroup = { }
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
    LaunchedEffect(otherUserBottomSheetContentState.modalBottomSheetState.isVisible) {
        if (!otherUserBottomSheetContentState.modalBottomSheetState.isVisible
            && !otherUserBottomSheetContentState.modalBottomSheetState.isAnimationRunning
        ) {
            otherUserBottomSheetContentState.resetState()
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
            conversationSheetContent = conversationSheetContent,
            requestConversationDetailsOnDemand = requestOnConversationDetails
        )
    }

    return otherUserBottomSheetContentState
}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    val modalBottomSheetState: ModalBottomSheetState,
    val groupInfoAvailability: GroupInfoAvailibility,
    private val conversationSheetContent: ConversationSheetContent?,
    val requestConversationDetailsOnDemand: () -> Unit
) {

    var otherUserProfileSheetNavigation: OtherUserProfileSheetNavigation by mutableStateOf(
        OtherUserProfileSheetNavigation.Conversation(
            if (conversationSheetContent == null) {
                ConversationSheetContentState.Loading
            } else {
                ConversationSheetContentState.Loaded(
                    ConversationSheetState(conversationSheetContent, ConversationNavigationOptions.Home)
                )
            }
        )
    )

    suspend fun showConversationOption() {
        if (conversationSheetContent == null) {
            requestConversationDetailsOnDemand()
        }

        if (conversationSheetContent != null) {
            otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Conversation(
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
        otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.RoleChange(
            groupInfoAvailability = groupInfoAvailability
        )

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

    fun resetState() {
        otherUserProfileSheetNavigation = OtherUserProfileSheetNavigation.Conversation(
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
