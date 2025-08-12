/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SearchAllServicesScreen(
    searchQuery: String,
    onServiceClicked: (Contact) -> Unit,
    searchServicesViewModel: SearchServicesViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LaunchedEffect(key1 = searchQuery) {
        searchServicesViewModel.searchQueryChanged(searchQuery)
    }

    SearchAllServicesContent(
        searchQuery = searchServicesViewModel.state.searchQuery,
        onServiceClicked = onServiceClicked,
        result = searchServicesViewModel.state.result,
        lazyListState = lazyListState,
        isLoading = searchServicesViewModel.state.isLoading
    )
}

@Composable
private fun SearchAllServicesContent(
    searchQuery: String,
    result: ImmutableList<Contact>,
    isLoading: Boolean,
    onServiceClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    when {
        isLoading -> CenteredCircularProgressBarIndicator()

        searchQuery.isBlank() && result.isEmpty() -> EmptySearchQueryScreen(
            text = stringResource(R.string.label_search_apps_instruction),
            learnMoreTextToLink = stringResource(R.string.label_learn_more_searching_app) to stringResource(R.string.url_wire_plans)
        )

        searchQuery.isNotBlank() && result.isEmpty() -> SearchFailureBox(R.string.label_no_results_found)

        else -> SuccessServicesList(
            searchQuery = searchQuery,
            onServiceClicked = onServiceClicked,
            services = result,
            lazyListState = lazyListState
        )
    }
}

@Composable
private fun SuccessServicesList(
    searchQuery: String,
    services: List<Contact>,
    onServiceClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
        ) {
            folderWithElements(
                items = services.associateBy { it.id }
            ) {
                val clickDescription = stringResource(id = R.string.content_description_open_service_label)
                RowItemTemplate(
                    leadingIcon = {
                        Row {
                            UserProfileAvatar(it.avatarData)
                        }
                    },
                    titleStartPadding = dimensions().spacing0x,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HighlightName(
                                name = it.name,
                                searchQuery = searchQuery,
                                modifier = Modifier.weight(weight = 1f, fill = false)
                            )
                            UserBadge(
                                membership = it.membership,
                                connectionState = it.connectionState,
                                startPadding = dimensions().spacing8x
                            )
                        }
                    },
                    actions = {},
                    clickable = remember(it) { Clickable(onClickDescription = clickDescription) { onServiceClicked(it) } },
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_Loading() = WireTheme {
    SearchAllServicesContent("", persistentListOf(), true, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_InitialResults() = WireTheme {
    SearchAllServicesContent("", previewServiceList(count = 10).toPersistentList(), false, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_EmptyInitialResults() = WireTheme {
    SearchAllServicesContent("", persistentListOf(), false, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_SearchResults() = WireTheme {
    SearchAllServicesContent("Serv", previewServiceList(count = 10).toPersistentList(), false, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_EmptySearchResults() = WireTheme {
    SearchAllServicesContent("Serv", persistentListOf(), false, {})
}

private fun previewService(index: Int) = Contact(
    id = index.toString(),
    domain = "wire.com",
    name = "Service nr $index",
    handle = "service_$index",
    connectionState = ConnectionState.NOT_CONNECTED,
    membership = Membership.Service,
)

private fun previewServiceList(count: Int): List<Contact> = buildList {
    repeat(count) { index -> add(previewService(index)) }
}
