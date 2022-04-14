package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
data class MutingConversationState(
    val coroutineScope: CoroutineScope,
    val mutedStatus: MutedConversationStatus = MutedConversationStatus.AllAllowed,
    val sheetState: ModalBottomSheetState
) {

    fun toggleSheetState() {
        coroutineScope.launch {
            if (sheetState.isVisible) sheetState.animateTo(ModalBottomSheetValue.Hidden)
            else sheetState.animateTo(ModalBottomSheetValue.Expanded)
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
