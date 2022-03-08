package com.wire.android.ui.home.newconversation.search

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

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

                RowItem(onRowItemClick = { /*TODO*/ }, onRowItemLongClick = { /*TODO*/ }) {
                    HighLightLabel(
                        contactSearchResult.name,
                        highLightIndexes = extractHighLightIndexes(
                            toHighLightText = searchQuery,
                            text = contactSearchResult.name
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HighLightLabel(
    text: String,
    highLightIndexes: List<HighLightResult> = emptyList()
) {
    if (highLightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                append(text)

                highLightIndexes
                    .forEach { highLightIndexes ->
                        Log.d("TEST", "thiss is test index ${highLightIndexes.startIndex} end : ${highLightIndexes.endIndex}")
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red),
                            highLightIndexes.endIndex - 1,
                            highLightIndexes.startIndex - 1
                        )
                    }
            }
        )
    } else {
        Text(text)
    }
}

private fun extractHighLightIndexes(
    results: List<HighLightResult> = emptyList(),
    toHighLightText: String,
    text: String
): List<HighLightResult> {
    val index = text.indexOf(toHighLightText, ignoreCase = true)

    return if (index != -1) {
        extractHighLightIndexes(
            results = results + HighLightResult(index, index + toHighLightText.length),
            toHighLightText = toHighLightText,
            text = text.removeRange(index, index + toHighLightText.length)
        )
    } else {
        results
    }
}

data class HighLightResult(val startIndex: Int, val endIndex: Int)

