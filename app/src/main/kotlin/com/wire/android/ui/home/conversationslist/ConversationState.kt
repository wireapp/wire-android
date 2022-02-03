package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
    val modalBottomSheetContentState: MutableState<ModalSheetContent>,
    private val coroutineScope: CoroutineScope
) {

    fun showModalSheet(conversationType: ConversationType) {
        when (conversationType) {
            is ConversationType.GroupConversation -> {
                with(conversationType) {
                    modalBottomSheetContentState.value = ModalSheetContent.GroupConversationEdit(
                        title = groupName,
                        groupColorValue = groupColorValue
                    )
                }
            }
            is ConversationType.PrivateConversation -> {
                with(conversationType) {
                    modalBottomSheetContentState.value = ModalSheetContent.PrivateConversationEdit(
                        title = conversationInfo.name,
                        avatarUrl = userInfo.avatarUrl
                    )
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
    modalBottomSheetContentState: MutableState<ModalSheetContent> = remember {
        mutableStateOf(ModalSheetContent.Initial)
    },
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(navHostController, modalBottomSheetState, modalBottomSheetContentState, coroutineScope) {
    ConversationState(
        navHostController,
        modalBottomSheetState,
        modalBottomSheetContentState,
        coroutineScope
    )
}
