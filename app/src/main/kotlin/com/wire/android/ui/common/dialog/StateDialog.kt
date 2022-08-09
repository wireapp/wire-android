package com.wire.android.ui.common.dialog

import androidx.compose.runtime.Composable
import com.wire.android.model.DialogState

@Composable
fun <T : Any> StateDialog(
    dialogState: DialogState<T>,
    visible: @Composable (T) -> Unit,
    ) {
    when(dialogState) {
        DialogState.Hidden -> {}
        is DialogState.Visible -> visible(dialogState.value)
    }
}
