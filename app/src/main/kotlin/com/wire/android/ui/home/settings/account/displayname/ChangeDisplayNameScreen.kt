package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChangeDisplayNameScreen(viewModel: ChangeDisplayNameViewModel = hiltViewModel()) {
    with(viewModel.displayNameState) {
        Text(text = displayName.text)
    }
}
