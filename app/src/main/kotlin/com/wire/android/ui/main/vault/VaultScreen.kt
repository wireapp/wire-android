package com.wire.android.ui.main.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.SearchBar
import com.wire.android.ui.common.UnderConstructionScreen

@Composable
fun VaultScreen() {
    Column {
        SearchBar(placeholderText = stringResource(R.string.hint_search_bar_vault))
        UnderConstructionScreen(screenName = "VaultScreen")
    }
}

@Preview(showBackground = false)
@Composable
fun VaultScreenPreview() {
    VaultScreen()
}
