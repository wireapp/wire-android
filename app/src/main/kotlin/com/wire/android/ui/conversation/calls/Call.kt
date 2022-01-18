package com.wire.android.ui.conversation.calls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun Call(viewModel: CallViewModel = CallViewModel()) {
    val uiState by viewModel.state.collectAsState()

    CallsScreen(uiState = uiState)
}

@Composable
private fun CallsScreen(uiState: CallState) {
    CallContent(uiState)
}

@Composable
fun CallContent(uiState: CallState) {

}

