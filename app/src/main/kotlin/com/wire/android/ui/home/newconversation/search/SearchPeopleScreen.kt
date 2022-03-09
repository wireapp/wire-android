package com.wire.android.ui.home.newconversation.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversations.common.ConversationItemTemplate
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.home.newconversation.contacts.FederatedBackend
import com.wire.android.ui.home.newconversation.contacts.PublicWire
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
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        SearchResult(
            searchQuery = searchQuery,
            contactSearchResult = contactSearchResult,
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
    contactSearchResult: List<Contact>,
    publicWire: List<PublicWire> = emptyList(),
    federatedBackend: List<FederatedBackend> = emptyList(),
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    val lazyListState = rememberLazyListState { scrollPosition -> onScrollPositionChanged(scrollPosition) }

    val searchPeopleScreenState = remember {
        SearchPeopleScreenState()
    }

    BoxWithConstraints {
        val fullHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SearchResultContent(
                    headerTitle = stringResource(id = R.string.label_contacts),
                    totalSearchResultCount = contactSearchResult.size.toString(),
                    searchResult = {
                        LazyColumn(
                            Modifier.fillMaxSize()
                        ) {
                            items(items = contactSearchResult) { contact ->
                                ContactSearchResultItem(
                                    contactSearchResult = contact,
                                    searchQuery = searchQuery,
                                )
                            }
                        }
                    },
                    onShowAllClicked = { searchPeopleScreenState.contactsAllResultsCollapsed = true },
                    onShowLessClicked = { searchPeopleScreenState.contactsAllResultsCollapsed = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (searchPeopleScreenState.contactsAllResultsCollapsed) fullHeight else 320.dp)
                )
            }
            item {
                SearchResultContent(
                    headerTitle = stringResource(R.string.label_public_wire),
                    totalSearchResultCount = publicWire.size.toString(),
                    searchResult = {
                        LazyColumn(
                            Modifier.fillMaxSize()
                        ) {
                            items(items = contactSearchResult) { contact ->
                                ContactSearchResultItem(
                                    contactSearchResult = contact,
                                    searchQuery = searchQuery,
                                )
                            }
                        }
                    },
                    onShowAllClicked = { searchPeopleScreenState.publicResultsCollapsed = true },
                    onShowLessClicked = { searchPeopleScreenState.publicResultsCollapsed = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (searchPeopleScreenState.publicResultsCollapsed) fullHeight else 320.dp)
                )
            }
            item {
                SearchResultContent(
                    headerTitle = stringResource(R.string.label_federated_backends),
                    totalSearchResultCount = federatedBackend.size.toString(),
                    searchResult = {
                        LazyColumn(
                            Modifier.fillMaxSize()
                        ) {
                            items(items = contactSearchResult) { contact ->
                                ContactSearchResultItem(
                                    contactSearchResult = contact,
                                    searchQuery = searchQuery,
                                )
                            }
                        }
                    },
                    onShowAllClicked = { searchPeopleScreenState.federatedBackendResultsCollapsed = true },
                    onShowLessClicked = { searchPeopleScreenState.federatedBackendResultsCollapsed = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (searchPeopleScreenState.federatedBackendResultsCollapsed) fullHeight else 320.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultContent(
    headerTitle: String,
    totalSearchResultCount: String,
    searchResult: @Composable () -> Unit,
    onShowAllClicked: () -> Unit,
    onShowLessClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier) {
        val (headerRef, columnRef, buttonRef) = createRefs()

        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .constrainAs(headerRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(columnRef.top)
                }
        ) {
            FolderHeader(name = headerTitle)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .constrainAs(columnRef) {
                    top.linkTo(headerRef.bottom)
                    bottom.linkTo(buttonRef.top)

                    height = Dimension.fillToConstraints
                }) {
            searchResult()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .constrainAs(buttonRef) {
                    top.linkTo(columnRef.bottom)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            ShowButton(
                totalSearchResultCount = totalSearchResultCount,
                onShowAllClicked = onShowAllClicked,
                onShowLessClicked = onShowLessClicked,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ShowButton(
    totalSearchResultCount: String,
    onShowAllClicked: () -> Unit,
    onShowLessClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isShowAll by remember { mutableStateOf(true) }

    Box(modifier) {
        AnimatedContent(isShowAll) { showAll ->
            WireSecondaryButton(
                text = if (showAll) "Show All ($totalSearchResultCount)" else "Show Less",
                onClick = {
                    if (isShowAll) onShowLessClicked() else onShowAllClicked()

                    isShowAll = !isShowAll
                },
                minHeight = 32.dp,
                fillMaxWidth = false,
            )
        }
    }
}

@Composable
private fun ContactSearchResultItem(
    contactSearchResult: Contact,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    with(contactSearchResult) {
        ConversationItemTemplate(
            leadingIcon = {
                UserProfileAvatar(
                    avatarUrl = "",
                    status = UserStatus.AVAILABLE
                )
            },
            title = {
                HighLightName(
                    name = name,
                    searchQuery = searchQuery
                )
            },
            subTitle = {
                HighLightSubTitle(
                    subTitle = label,
                    searchQuery = searchQuery
                )
            },
            onConversationItemClick = {},
            onConversationItemLongClick = {},
            modifier = modifier
        )
    }
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

class SearchPeopleScreenState {

    var contactsAllResultsCollapsed: Boolean by mutableStateOf(false)

    var publicResultsCollapsed: Boolean by mutableStateOf(false)

    var federatedBackendResultsCollapsed: Boolean by mutableStateOf(false)

}
