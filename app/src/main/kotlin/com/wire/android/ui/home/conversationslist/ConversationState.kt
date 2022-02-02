package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.home.conversationslist.model.ConversationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
class ConversationState(
    val navHostController: NavHostController,
    val modalBottomSheetState: ModalBottomSheetState,
    private val modalSheetContentState: ModalSheetContentState,
    private val coroutineScope: CoroutineScope
) {

    val modalSheetTitle = modalSheetContentState.title

    val modalSheetAvatar = modalSheetContentState.avatar

    fun showModalSheet(conversationType: ConversationType) {
        when (conversationType) {
            is ConversationType.GroupConversation -> {
                with(conversationType) {
                    modalSheetContentState.avatar.value = ModalSheetAvatar.GroupAvatar(groupColorValue)
                    modalSheetContentState.title.value = groupName
                }
            }
            is ConversationType.PrivateConversation -> {
                with(conversationType) {
                    modalSheetContentState.avatar.value = ModalSheetAvatar.UserAvatar(userInfo.avatarUrl)
                    modalSheetContentState.title.value = conversationInfo.name
                }
            }
        }
        coroutineScope.launch { modalBottomSheetState.show() }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberConversationState(
    navHostController: NavHostController = rememberNavController(),
    modalBottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    modalSheetContentState: ModalSheetContentState = rememberModalSheetContentState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(navHostController, modalBottomSheetState, modalSheetContentState, coroutineScope) {
    ConversationState(
        navHostController,
        modalBottomSheetState,
        modalSheetContentState,
        coroutineScope
    )
}
