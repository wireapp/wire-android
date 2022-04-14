package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
data class MutingConversationState(
    val coroutineScope: CoroutineScope,
    val mutedStatus: MutedConversationStatus = MutedConversationStatus.AllAllowed,
    val sheetState: ModalBottomSheetState
) {
    var conversationId by mutableStateOf<ConversationId?>(null)
        private set

    fun openMutedStatusSheetContent(conversationId: ConversationId?) {
        coroutineScope.launch {
            this@MutingConversationState.conversationId = conversationId
            sheetState.animateTo(ModalBottomSheetValue.Expanded)
        }
    }

    fun closeMutedStatusSheetContent() {
        coroutineScope.launch {
            this@MutingConversationState.conversationId = null
            sheetState.animateTo(ModalBottomSheetValue.Hidden)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMutingConversationState(
    mutedStatus: MutedConversationStatus = MutedConversationStatus.AllAllowed,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
): MutingConversationState {

    val coroutineScope = rememberCoroutineScope()

    return remember {
        MutingConversationState(coroutineScope, mutedStatus, sheetState)
    }
}
