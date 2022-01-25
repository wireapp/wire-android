package com.wire.android.ui.main.archive

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.SearchBar
import com.wire.android.ui.common.UnderConstructionScreen

@Composable
fun ArchiveScreen() {
    SearchBar(placeholderText = stringResource(R.string.hint_search_bar_archive))
    UnderConstructionScreen(screenName = "ArchiveScreen")
}

@Preview(showBackground = false)
@Composable
fun ArchiveScreenPreview() {
    ArchiveScreen()
}
