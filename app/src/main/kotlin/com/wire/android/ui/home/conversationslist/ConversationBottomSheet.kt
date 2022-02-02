package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.CircularProgressIndicator
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar


@ExperimentalMaterialApi
@Composable
fun ConversationModalBottomSheet(
    conversationViewState: ConversationState,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = conversationViewState.modalBottomSheetState,
        sheetContent = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (val avatar = conversationViewState.modalSheetAvatar.value) {
                        is ModalSheetAvatar.GroupAvatar -> GroupConversationAvatar(colorValue = avatar.groupColor)
                        is ModalSheetAvatar.UserAvatar -> UserProfileAvatar()
                        ModalSheetAvatar.None -> CircularProgressIndicator(progressColor = Color.Blue)
                    }
                    Text(conversationViewState.modalSheetTitle.value)
                }
            }
        }
    ) {
        content()
    }
}

class ModalSheetContentState {
    val title: MutableState<String> = mutableStateOf("")
    val avatar: MutableState<ModalSheetAvatar> = mutableStateOf(ModalSheetAvatar.None)
}

sealed class ModalSheetAvatar {
    data class UserAvatar(val avatarUrl: String) : ModalSheetAvatar()
    data class GroupAvatar(val groupColor: Long) : ModalSheetAvatar()
    object None : ModalSheetAvatar()
}

@Composable
fun rememberModalSheetContentState(): ModalSheetContentState {
    return remember {
        ModalSheetContentState()
    }
}
