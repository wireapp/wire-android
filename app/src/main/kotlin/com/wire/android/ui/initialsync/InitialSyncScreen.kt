package com.wire.android.ui.initialsync

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.SettingUpWireScreenContent

@Composable
fun InitialSyncScreen(viewModel: InitialSyncViewModel = hiltViewModel()) {
    SettingUpWireScreenContent()
    viewModel.waitUntilSyncIsCompleted()
}
