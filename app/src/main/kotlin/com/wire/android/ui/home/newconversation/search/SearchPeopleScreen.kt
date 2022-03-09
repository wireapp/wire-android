package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.MatchQueryResult
import com.wire.android.util.QueryMatchExtractor
import kotlinx.coroutines.launch

@Composable
fun SearchPeopleScreen(
    searchPeopleState: SearchPeopleState,
    onScrollPositionChanged: (Int) -> Unit
) {
    SearchPeopleScreenContent(
        searchQuery = searchPeopleState.searchQuery,
        contactSearchResult = searchPeopleState.contactSearchResult,
        onScrollPositionChanged = onScrollPositionChanged
    )
}

@Composable
private fun SearchPeopleScreenContent(
    searchQuery: String,
    contactSearchResult: List<Contact>,
    onScrollPositionChanged: (Int) -> Unit
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        SearchResult(
            searchQuery = searchQuery,
            searchResult = contactSearchResult,
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
    searchResult: List<Contact>,
    onScrollPositionChanged: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState {
        onScrollPositionChanged(it)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentSize()
    ) {
        LazyColumn(
            state = lazyListState,
        ) {
            folderWithElements(
                header = { stringResource(R.string.label_contacts) },
                items = searchResult
            ) { contactSearchResult ->
                RowItem(
                    onRowItemClick = { /*TODO*/ },
                    onRowItemLongClick = { /*TODO*/ }
                ) {
                    HighLightLabel(
                        text = contactSearchResult.name,
                        searchQuery = searchQuery
                    )
                }
            }
        }
    }
}

@Composable
private fun HighLightLabel(
    text: String,
    searchQuery: String,
) {
    val scope = rememberCoroutineScope()

    var highlightIndexes by remember {
        mutableStateOf(emptyList<MatchQueryResult>())
    }

    SideEffect {
        scope.launch {
            highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
                matchText = searchQuery,
                text = text
            )
        }
    }

    if (highlightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                append(text)

                highlightIndexes
                    .forEach { highLightIndexes ->
                        addStyle(
                            SpanStyle(background = MaterialTheme.wireColorScheme.highLight.copy(0.5f)),
                            highLightIndexes.startIndex,
                            highLightIndexes.endIndex
                        )
                    }
            }
        )
    } else {
        Text(text)
    }
}
