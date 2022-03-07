package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
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
    }
}

@OptIn(ExperimentalMaterialApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun rememberConversationState(
    navHostController: NavHostController = rememberAnimatedNavController(),
    modalBottomSheetContentState: MutableState<ModalSheetContent> = remember {
        mutableStateOf(ModalSheetContent.Initial)
    },
) = remember(navHostController, modalBottomSheetContentState) {
    ConversationState(
        navHostController,
        modalBottomSheetContentState,
    )
}
