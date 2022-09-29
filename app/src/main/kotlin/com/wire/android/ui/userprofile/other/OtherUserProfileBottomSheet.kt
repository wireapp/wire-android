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
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@Composable
fun OtherUserProfileBottomSheet(otherUserBottomSheetContentState: OtherUserBottomSheetContentState) {
    when (val test = otherUserBottomSheetContentState.test) {
        is Test.Conversation -> {
            when (test.conversationSheetState) {
                is Dupa.Loaded -> {
                    ConversationSheetContent(conversationSheetState = test.conversationSheetState.conversationSheetContent)
                }
                Dupa.Loading -> {}
            }
        }
        is Test.RoleChange -> {
            if (test.groupInfoAvailibility is GroupInfoAvailibility.Available) {
                EditGroupRoleBottomSheet(
                    groupState = test.groupInfoAvailibility.otherUserProfileGroupInfo,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberOtherUserBottomSheetContentState(
    requestOnDemand: () -> Unit,
    conversationSheetContent: ConversationSheetContent?,
    groupInfoAvailibility: GroupInfoAvailibility
): OtherUserBottomSheetContentState {
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    val test = remember(conversationSheetContent, groupInfoAvailibility) {
        OtherUserBottomSheetContentState(
            modalBottomSheetState,
            requestOnDemand,
            if (conversationSheetContent == null) Dupa.Loading else Dupa.Loaded(ConversationSheetState(conversationSheetContent)),
            groupInfoAvailibility
        )
    }

    LaunchedEffect(test.conversationSheetContent) {
        if (test.conversationSheetContent is Dupa.Loaded) {
            val currentTest = test.test

            if (currentTest is Test.Conversation) {
                test.test =
                    Test.Conversation(Dupa.Loaded(ConversationSheetState(test.conversationSheetContent.conversationSheetContent.conversationSheetContent)))
            }
        }
    }

    return test
}

sealed class Dupa {
    object Loading : Dupa()
    data class Loaded(val conversationSheetContent: ConversationSheetState) : Dupa()
}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    val modalBottomSheetState: ModalBottomSheetState,
    val requestOnDemand: () -> Unit,
    val conversationSheetContent: Dupa = Dupa.Loading,
    val groupInfoAvailibility: GroupInfoAvailibility = GroupInfoAvailibility.NotAvailable
) {

    var test: Test by mutableStateOf(Test.Conversation(conversationSheetContent))

    suspend fun showConversationOption() {
        show()

        test = Test.Conversation(conversationSheetContent)
    }

    suspend fun showChangeRoleOption() {
        show()

        test = Test.RoleChange(groupInfoAvailibility)
    }

    suspend fun show() {
        if (conversationSheetContent is Dupa.Loading) {
            requestOnDemand()
        }

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        val currentTest = test

        if (currentTest is Test.Conversation) {
            if (currentTest.conversationSheetState is Dupa.Loaded) {
                currentTest.conversationSheetState.conversationSheetContent.muteConversation(mutedConversationStatus)
            }
        }

    }

    fun toMutingNotificationOption() {
        val currentTest = test

        if (currentTest is Test.Conversation) {
            if (currentTest.conversationSheetState is Dupa.Loaded) {
                currentTest.conversationSheetState.conversationSheetContent.toMutingNotificationOption()
            }
        }
    }

    fun toHome() {
        val currentTest = test

        if (currentTest is Test.Conversation) {
            if (currentTest.conversationSheetState is Dupa.Loaded) {
                currentTest.conversationSheetState.conversationSheetContent.toHome()
            }
        }
    }

}

sealed class Test {
    data class Conversation(val conversationSheetState: Dupa) : Test()
    data class RoleChange(val groupInfoAvailibility: GroupInfoAvailibility) : Test()

}

//@Composable
//fun OtherUSerProfileBottomSheetContent(
//    otherUserNavigationOptions: OtherUserNavigationOptions,
//) {
////    val otherUserProfileScreenViewModel: OtherUserProfileScreenViewModel = hiltViewModel()
////
////    LaunchedEffect(Unit) {
////        otherUserProfileScreenViewModel.getAdditionalConversationDetails()
////    }
//
//    when (otherUserNavigationOptions) {
//        is ConversationNavigationOptions.Home, ConversationNavigationOptions.MutingOptionsNotification -> {
//            ConversationSheetState(conversationNavigationOptions = otherUserNavigationOptions)
//        }
//        is OtherUserNavigationOption.ChangeRole -> {
//            EditGroupRoleBottomSheet(
//                groupState = bottomSheetState.otherUserProfileGroupInfo,
//                changeMemberRole = eventsHandler::onChangeMemberRole,
//                closeChangeRoleBottomSheet = closeBottomSheet
////            )
//        }
////        is OtherUserBottomSheetContent.Conversation -> {
////            val conversationId = bottomSheetState.conversationData.conversationId
////            ConversationMainSheetContent(
////                conversationSheetContent = bottomSheetState.conversationData,
////// TODO(profile): enable when implemented
//////
//////                addConversationToFavourites = { eventsHandler.onAddConversationToFavourites(conversationId) },
//////                moveConversationToFolder = { eventsHandler.onMoveConversationToFolder(conversationId) },
//////                moveConversationToArchive = { eventsHandler.onMoveConversationToArchive(conversationId) },
//////                clearConversationContent = { eventsHandler.onClearConversationContent(conversationId) },
////                blockUserClick = blockUser,
////                leaveGroup = { },
////                deleteGroup = { },
////                navigateToNotification = eventsHandler::setBottomSheetStateToMuteOptions
////            )
////        }
////        is OtherUserBottomSheetContent.Mute ->
////            MutingOptionsSheetContent(
////                mutingConversationState = bottomSheetState.conversationData.mutingConversationState,
////                onMuteConversation = {
////                    eventsHandler.onMutingConversationStatusChange(bottomSheetState.conversationData.conversationId, it)
////                },
////                onBackClick = eventsHandler::setBottomSheetStateToConversation
////            )
////        is OtherUserBottomSheetContent.ChangeRole ->
////            EditGroupRoleBottomSheet(
////                groupState = bottomSheetState.otherUserProfileGroupInfo,
////                changeMemberRole = eventsHandler::onChangeMemberRole,
////                closeChangeRoleBottomSheet = closeBottomSheet
////            )
//
//    }
//
//    BackHandler(bottomSheetState != null) {
//        if (bottomSheetState is OtherUserBottomSheetContent.Mute) eventsHandler.setBottomSheetStateToConversation()
//        else closeBottomSheet()
//    }
//
//}

