package com.wire.android.ui.home.conversationslist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.home.conversationslist.bottomsheet.ModalSheetContent
import com.wire.android.ui.home.conversationslist.model.ConversationType

@ExperimentalMaterialApi
class ConversationState(
    val navHostController: NavHostController,
    val modalBottomSheetContentState: MutableState<ModalSheetContent>,
) {

    fun changeModalSheetContentState(conversationType: ConversationType) {
        when (conversationType) {
            is ConversationType.GroupConversation -> {
                with(conversationType) {
                    modalBottomSheetContentState.value = ModalSheetContent.GroupConversationEdit(
                        title = groupName,
                        groupColorValue = groupColorValue,
                        conversationId = this.conversationId
                    )
                }
            }
            is ConversationType.PrivateConversation -> {
                with(conversationType) {
                    modalBottomSheetContentState.value = ModalSheetContent.PrivateConversationEdit(
                        title = conversationInfo.name,
                        avatarUrl = userInfo.avatarUrl,
                        conversationId = this.conversationId
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun rememberConversationState(
    navHostController: NavHostController = rememberNavController(),
    modalBottomSheetContentState: MutableState<ModalSheetContent> = remember {
        mutableStateOf(ModalSheetContent.Initial)
    },
) = remember(navHostController, modalBottomSheetContentState) {
    ConversationState(
        navHostController,
        modalBottomSheetContentState,
    )
}
