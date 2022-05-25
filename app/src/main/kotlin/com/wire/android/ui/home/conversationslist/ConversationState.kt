package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.model.ConversationType

@ExperimentalMaterialApi
class ConversationState(
    val modalBottomSheetContentState: MutableState<ConversationSheetContent>,
    val isEditingMutedSetting: MutableState<Boolean>
) {

    fun toggleEditMutedSetting(enable: Boolean) {
        isEditingMutedSetting.value = enable
    }

    fun changeModalSheetContentState(conversationType: ConversationType) {
        when (conversationType) {
            is ConversationType.GroupConversation -> {
                with(conversationType) {
                    modalBottomSheetContentState.value = ConversationSheetContent.GroupConversation(
                        title = groupName,
                        groupColorValue = groupColorValue,
                        conversationId = this.conversationId,
                        mutedStatus = this.mutedStatus
                    )
                }
            }
            is ConversationType.PrivateConversation -> {
                with(conversationType) {
                    modalBottomSheetContentState.value = ConversationSheetContent.PrivateConversation(
                        title = conversationInfo.name,
                        avatarAsset = userInfo.avatarAsset,
                        conversationId = this.conversationId,
                        mutedStatus = this.mutedStatus
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberConversationState(
    modalBottomSheetContentState: MutableState<ConversationSheetContent> = remember {
        mutableStateOf(ConversationSheetContent.Initial)
    },
    isEditingMutedSetting: MutableState<Boolean> = remember { mutableStateOf(false) },
) = remember(modalBottomSheetContentState) {
    ConversationState(
        modalBottomSheetContentState,
        isEditingMutedSetting
    )
}
