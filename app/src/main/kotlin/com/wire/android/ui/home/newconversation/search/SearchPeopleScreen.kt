package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun SearchPeopleScreen(
    searchPeopleViewModel: SearchPeopleViewModel = hiltViewModel(),
    searchQuery: String,
    onScrollPositionChanged: (Int) -> Unit
) {
    val state = searchPeopleViewModel.state

    LaunchedEffect(searchQuery) {
        searchPeopleViewModel.search(searchQuery)
    }

    SearchPeopleScreenContent(
        searchQuery = state.searchQuery,
        onScrollPositionChanged = onScrollPositionChanged
    )
}

@Composable
private fun SearchPeopleScreenContent(
    searchQuery: String,
    onScrollPositionChanged: (Int) -> Unit
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        SearchResult(
            searchQuery = searchQuery,
            onScrollPositionChanged = onScrollPositionChanged
        )
    }
}

@Composable
private fun EmptySearchQueryScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Search for user with their display name of their @username",
                style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    onScrollPositionChanged: (Int) -> Unit
) {


}
