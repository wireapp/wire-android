package com.wire.android.ui.migration

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.SettingUpWireScreenContent

@Composable
fun MigrationScreen(viewModel: MigrationViewModel = hiltViewModel()) {
    viewModel.init()
    SettingUpWireScreenContent()
}
