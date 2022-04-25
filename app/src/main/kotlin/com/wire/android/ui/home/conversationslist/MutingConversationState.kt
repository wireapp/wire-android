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

@OptIn(ExperimentalMaterialApi::class)
data class MutingConversationState(
    val coroutineScope: CoroutineScope,
    val sheetState: ModalBottomSheetState
) {
    var conversationId by mutableStateOf<ConversationId?>(null)
        private set

    var mutedStatus by mutableStateOf<MutedConversationStatus>(MutedConversationStatus.AllAllowed)
        private set

    fun openMutedStatusSheetContent(conversationId: ConversationId?, mutedStatus: MutedConversationStatus) {
        this@MutingConversationState.conversationId = conversationId
        updateMutedStatus(mutedStatus)
    }

    fun closeMutedStatusSheetContent() {
        this@MutingConversationState.conversationId = null
    }

    fun updateMutedStatus(mutedStatus: MutedConversationStatus) {
        this.mutedStatus = mutedStatus
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMutingConversationState(
    mutedStatus: MutedConversationStatus,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
): MutingConversationState {

    val coroutineScope = rememberCoroutineScope()

    return remember { MutingConversationState(coroutineScope, sheetState) }.apply { updateMutedStatus(mutedStatus) }
}
