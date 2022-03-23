package com.wire.android.ui.home.newconversation.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.home.newconversation.contacts.ExternalContact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.MatchQueryResult
import com.wire.android.util.QueryMatchExtractor
import kotlinx.coroutines.launch

@Composable
fun SearchPeopleScreen(
    searchPeopleState: SearchPeopleState,
) {
    SearchPeopleScreenContent(
        searchQuery = searchPeopleState.searchQuery,
        contactSearchResult = searchPeopleState.contactSearchResult,
        publicSearchResult = searchPeopleState.publicContactSearchResult,
        federatedBackendResult = searchPeopleState.federatedContactSearchResult
    )
}

@Composable
private fun SearchPeopleScreenContent(
    searchQuery: String,
    contactSearchResult: List<Contact>,
    publicSearchResult: List<ExternalContact>,
    federatedBackendResult: List<ExternalContact>,
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        SearchResult(
            searchQuery = searchQuery,
            contactSearchResult = contactSearchResult,
            publicSearchResult = publicSearchResult,
            federatedBackendResult = federatedBackendResult
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResult(
    searchQuery: String,
    contactSearchResult: List<Contact>,
    publicSearchResult: List<ExternalContact>,
    federatedBackendResult: List<ExternalContact>,
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()

    BoxWithConstraints {
        val fullHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }

        LazyColumn(
            state = searchPeopleScreenState.lazyListState,
            modifier = Modifier
                .fillMaxSize()
        ) {

            if (contactSearchResult.isNotEmpty()) {
                folderWithElements(
                    header = {  stringResource(id = R.string.label_contacts) },
                    items = contactSearchResult.take(4),
                    bottomAction = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()) {
                            ShowButton(
                                totalSearchResultCount = "4",
                                onShowAllClicked = { },
                                onShowLessClicked = { },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = dimensions().spacing8x)
                            )
                        }
                    }) { contact ->
                    with(contact) {
                        ContactSearchResultItem(
                            avatarUrl = avatarUrl,
                            userStatus = userStatus,
                            name = name,
                            label = label,
                            searchQuery = searchQuery,
                            source = Source.Internal(eventType),
                            onRowItemClicked = {},
                            onRowItemLongClicked = {}
                        )
                    }
                }
            }

            if (publicSearchResult.isNotEmpty()) {
                folderWithElements(
                    header = {   stringResource(R.string.label_public_wire) },
                    items = publicSearchResult.take(4),
                    bottomAction = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()) {
                            ShowButton(
                                totalSearchResultCount = "4",
                                onShowAllClicked = { },
                                onShowLessClicked = { },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = dimensions().spacing8x)
                            )
                        }
                    }) { contact ->
                    with(contact) {
                        ContactSearchResultItem(
                            avatarUrl = avatarUrl,
                            userStatus = userStatus,
                            name = name,
                            label = label,
                            searchQuery = searchQuery,
                            source = Source.External,
                            onRowItemClicked = {},
                            onRowItemLongClicked = {}
                        )
                    }
                }
            }

            if (federatedBackendResult.isNotEmpty()) {
                folderWithElements(
                    header = {   stringResource(R.string.label_public_wire) },
                    items = federatedBackendResult.take(4),
                    bottomAction = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()) {
                            ShowButton(
                                totalSearchResultCount = "4",
                                onShowAllClicked = { },
                                onShowLessClicked = { },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = dimensions().spacing8x)
                            )
                        }
                    }) { contact ->
                    with(contact) {
                        ContactSearchResultItem(
                            avatarUrl = avatarUrl,
                            userStatus = userStatus,
                            name = name,
                            label = label,
                            searchQuery = searchQuery,
                            source = Source.External,
                            onRowItemClicked = {},
                            onRowItemLongClicked = {}
                        )
                    }
                }
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
                    .padding(end = dimensions().spacing8x)
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
                    if (isShowAll) onShowAllClicked() else onShowLessClicked()

                    isShowAll = !isShowAll
                },
                minHeight = dimensions().showAllCollapseButtonMinHeight,
                fillMaxWidth = false,
            )
        }
    }
}

@Composable
fun HighLightSubTitle(
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
                    append("@$subTitle")
                }

                highlightIndexes
                    .forEach { highLightIndexes ->
                        addStyle(
                            style = SpanStyle(
                                background = MaterialTheme.wireColorScheme.highLight.copy(alpha = 0.5f),
                            ),
                            // add 1 because of the "@" prefix
                            start = highLightIndexes.startIndex + 1,
                            end = highLightIndexes.endIndex + 1
                        )
                    }
            }
        )
    } else {
        Text(
            text = "@$subTitle",
            style = MaterialTheme.wireTypography.subline01,
            color = MaterialTheme.wireColorScheme.secondaryText
        )
    }
}

@Composable
fun HighLightName(
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
                            style = SpanStyle(background = MaterialTheme.wireColorScheme.highLight.copy(alpha = 0.5f)),
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

@Composable
private fun ExternalSearchResultItem(
    searchQuery: String,
    externalContact: ExternalContact,
    onRowItemClicked: () -> Unit,
    oRowItemLongClicked: () -> Unit
) {
    with(externalContact) {
        ContactSearchResultItem(
            avatarUrl = "",
            userStatus = userStatus,
            name = name,
            label = label,
            searchQuery = searchQuery,
            source = Source.External,
            onRowItemClicked = onRowItemClicked,
            onRowItemLongClicked = oRowItemLongClicked
        )
    }
}
