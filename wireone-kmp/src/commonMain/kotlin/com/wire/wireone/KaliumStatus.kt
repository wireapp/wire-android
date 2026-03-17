package com.wire.wireone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberKaliumStatusLine(): String {
    val viewModel = remember { KaliumStatusViewModel() }
    return viewModel.statusLine()
}
