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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversations.common.ConversationItemTemplate
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
                ContactSearchResultItem(contactSearchResult, searchQuery)
            }
        }
    }
}

@Composable
private fun ContactSearchResultItem(
    contactSearchResult: Contact,
    searchQuery: String
) {
    ConversationItemTemplate(
        leadingIcon = { UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) },
        title = {
            HighLightName(
                name = contactSearchResult.name,
                searchQuery = searchQuery
            )
        },
        subTitle = {
            HighLightSubTitle(
                subTitle = contactSearchResult.label,
                searchQuery = searchQuery
            )
        },
        onConversationItemClick = {},
        onConversationItemLongClick = {}
    )
}

@Composable
private fun HighLightSubTitle(
    subTitle: String,
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
                text = subTitle
            )
        }
    }

    if (highlightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.wireColorScheme.secondaryText,
                        fontWeight = MaterialTheme.wireTypography.subline01.fontWeight,
                        fontSize = MaterialTheme.wireTypography.subline01.fontSize,
                        fontFamily = MaterialTheme.wireTypography.subline01.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.subline01.fontStyle
                    )
                ) {
                    append(subTitle)
                }

                highlightIndexes
                    .forEach { highLightIndexes ->
                        addStyle(
                            style = SpanStyle(
                                background = MaterialTheme.wireColorScheme.highLight.copy(0.5f),
                            ),
                            start = highLightIndexes.startIndex,
                            end = highLightIndexes.endIndex
                        )
                    }
            }
        )
    } else {
        Text(
            text = subTitle,
            style = MaterialTheme.wireTypography.subline01,
            color = MaterialTheme.wireColorScheme.secondaryText
        )
    }
}


@Composable
private fun HighLightName(
    name: String,
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
                text = name
            )
        }
    }

    if (highlightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = MaterialTheme.wireTypography.title02.fontWeight,
                        fontSize = MaterialTheme.wireTypography.title02.fontSize,
                        fontFamily = MaterialTheme.wireTypography.title02.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.title02.fontStyle
                    )
                ) {
                    append(name)
                }

                highlightIndexes
                    .forEach { highLightIndexes ->
                        addStyle(
                            style = SpanStyle(background = MaterialTheme.wireColorScheme.highLight.copy(0.5f)),
                            start = highLightIndexes.startIndex,
                            end = highLightIndexes.endIndex
                        )
                    }
            }
        )
    } else {
        Text(
            text = name,
            style = MaterialTheme.wireTypography.title02
        )
    }
}
