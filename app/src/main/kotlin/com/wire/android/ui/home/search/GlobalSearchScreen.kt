/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.globalSearchViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.QueryMatchExtractor
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId

@WireRootDestination(style = PopUpNavigationAnimation::class)
@Composable
fun GlobalSearchScreen(
    navigator: Navigator,
    viewModel: GlobalSearchViewModel = globalSearchViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GlobalSearchContent(
        searchQueryTextState = viewModel.searchQueryTextState,
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onResultClick = { result ->
            navigator.navigate(
                NavigationCommand(
                    ConversationScreenDestination(
                        navArgs = ConversationNavArgs(
                            conversationId = result.conversationId,
                            searchedMessageId = result.messageId,
                        )
                    ),
                    BackStackMode.UPDATE_EXISTED
                )
            )
        },
        onCloseSearchClicked = navigator::navigateBack,
    )
}

@Composable
fun GlobalSearchContent(
    searchQueryTextState: TextFieldState,
    state: GlobalSearchState,
    onFilterSelected: (GlobalSearchFilter) -> Unit,
    onResultClick: (GlobalSearchResultItem) -> Unit,
    onCloseSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation

    WireScaffold(
        modifier = modifier,
        topBar = {
            Column {
                Surface(
                    shadowElevation = lazyListState.topBarElevation(maxAppBarElevation),
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    SearchTopBar(
                        isSearchActive = true,
                        searchBarHint = stringResource(R.string.global_search_hint),
                        searchQueryTextState = searchQueryTextState,
                        onCloseSearchClicked = onCloseSearchClicked,
                        isLoading = state.isLoading,
                    )
                }
                GlobalSearchFilters(
                    selectedFilter = state.selectedFilter,
                    onFilterSelected = onFilterSelected,
                )
            }
        },
        content = { internalPadding ->
            Box(
                modifier = Modifier
                    .padding(internalPadding)
                    .fillMaxSize()
            ) {
                when {
                    state.searchQuery.isBlank() -> GlobalSearchEmptyContent()
                    state.hasError -> GlobalSearchMessage(text = stringResource(R.string.global_search_error))
                    !state.isLoading && state.visibleResults.isEmpty() ->
                        GlobalSearchMessage(text = stringResource(R.string.label_search_messages_no_results))

                    else -> GlobalSearchResults(
                        results = state.visibleResults,
                        searchQuery = state.searchQuery,
                        onResultClick = onResultClick,
                        lazyListState = lazyListState,
                    )
                }

                if (state.isLoading && state.visibleResults.isEmpty() && state.searchQuery.isNotBlank()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    )
}

@Composable
private fun GlobalSearchFilters(
    selectedFilter: GlobalSearchFilter,
    onFilterSelected: (GlobalSearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing8x,
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
    ) {
        GlobalSearchFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                enabled = filter.isEnabled,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.label()) },
            )
        }
    }
}

@Composable
private fun GlobalSearchFilter.label(): String = when (this) {
    GlobalSearchFilter.All -> stringResource(R.string.global_search_filter_all)
    GlobalSearchFilter.Messages -> stringResource(R.string.global_search_filter_messages)
    GlobalSearchFilter.People -> stringResource(R.string.global_search_filter_people)
    GlobalSearchFilter.Files -> stringResource(R.string.global_search_filter_files)
    GlobalSearchFilter.Links -> stringResource(R.string.global_search_filter_links)
    GlobalSearchFilter.Media -> stringResource(R.string.global_search_filter_media)
}

@Composable
private fun GlobalSearchResults(
    results: List<GlobalSearchResultItem>,
    searchQuery: String,
    onResultClick: (GlobalSearchResultItem) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = results,
            key = { "${it.conversationId}:${it.messageId}" }
        ) { item ->
            GlobalSearchResultRow(
                item = item,
                searchQuery = searchQuery,
                onClick = { onResultClick(item) },
            )
            HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        }
    }
}

@Composable
private fun GlobalSearchResultRow(
    item: GlobalSearchResultItem,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.content_description_open_label),
                onClick = onClick,
            )
            .padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing12x,
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_conversation),
            contentDescription = null,
            tint = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier
                .padding(top = dimensions().spacing4x)
                .size(dimensions().spacing16x)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        ) {
            Text(
                text = highlightedSnippet(item.preview, searchQuery),
                style = MaterialTheme.wireTypography.body01,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.senderName} - ${item.conversationName}",
                style = MaterialTheme.wireTypography.body02.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun highlightedSnippet(text: String, searchQuery: String) = buildAnnotatedString {
    append(text)
    QueryMatchExtractor.extractQueryMatchIndexes(
        matchText = searchQuery,
        text = text,
    ).forEach { match ->
        addStyle(
            style = SpanStyle(
                background = MaterialTheme.wireColorScheme.highlight,
                color = MaterialTheme.wireColorScheme.onHighlight,
            ),
            start = match.startIndex,
            end = match.endIndex,
        )
    }
}

@Composable
private fun GlobalSearchEmptyContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions().spacing24x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText),
            modifier = Modifier.size(dimensions().spacing32x)
        )
        Text(
            text = stringResource(R.string.global_search_empty_title),
            style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimensions().spacing16x)
        )
    }
}

@Composable
private fun GlobalSearchMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions().spacing24x),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewGlobalSearchContent() = WireTheme {
    GlobalSearchContent(
        searchQueryTextState = TextFieldState(initialText = "release"),
        state = GlobalSearchState(
            searchQuery = "release",
            results = listOf(
                GlobalSearchResultItem(
                    messageId = "messageId",
                    conversationId = ConversationId("conversationId", "domain"),
                    senderName = "Alice",
                    conversationName = "Product Planning",
                    preview = "We agreed to move the release to Friday."
                )
            )
        ),
        onFilterSelected = {},
        onResultClick = {},
        onCloseSearchClicked = {},
    )
}
