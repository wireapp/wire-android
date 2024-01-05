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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.newconversation.model.Contact

@Composable
fun SearchAllServicesScreen(
    searchQuery: String,
    searchResult: SearchResultState,
    initialServices: SearchResultState,
    onServiceClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    SearchAllServicesContent(
        searchQuery = searchQuery,
        onServiceClicked = onServiceClicked,
        result = if (searchQuery.isEmpty()) initialServices else searchResult,
        lazyListState = lazyListState
    )
}

@Composable
private fun SearchAllServicesContent(
    searchQuery: String,
    result: SearchResultState,
    onServiceClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    when (result) {
        SearchResultState.Initial, SearchResultState.InProgress -> {
            CenteredCircularProgressBarIndicator()
        }

        is SearchResultState.Failure -> {
            SearchFailureBox(failureMessage = result.failureString)
        }

        // TODO: what to do when user team has no services?
        SearchResultState.EmptyResult -> {
            EmptySearchQueryScreen()
        }

        is SearchResultState.Success -> {
            SuccessServicesList(
                searchQuery = searchQuery,
                onServiceClicked = onServiceClicked,
                services = result.result,
                lazyListState = lazyListState
            )
        }
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
            services
                .forEach {
                    item {
                        RowItemTemplate(
                            leadingIcon = {
                                Row {
                                    UserProfileAvatar(it.avatarData)
                                }
                            },
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
                            clickable = remember { Clickable(enabled = true) { onServiceClicked(it) } }
                        )
                    }
                }
        }
    }
}
