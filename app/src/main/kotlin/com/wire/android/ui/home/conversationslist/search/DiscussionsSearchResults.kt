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

package com.wire.android.ui.home.conversationslist.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesEmptyScreen
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNoResultsScreen
import com.wire.android.ui.theme.wireTypography

@Composable
fun DiscussionsSearchResults(
    state: DiscussionsSearchState,
    modifier: Modifier = Modifier
) {
    when (state) {
        DiscussionsSearchState.EmptyQuery -> SearchConversationMessagesEmptyScreen(modifier)
        DiscussionsSearchState.Loading -> LoadingListContent(modifier)
        DiscussionsSearchState.NoResults -> SearchConversationMessagesNoResultsScreen(modifier)
        is DiscussionsSearchState.Success -> DiscussionsSearchResultsList(
            discussions = state.discussions,
            modifier = modifier
        )
    }
}

@Composable
private fun DiscussionsSearchResultsList(
    discussions: List<DiscussionClusterSummary>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(discussions) { discussion ->
            DiscussionSearchResultItem(discussion)
        }
    }
}

@Composable
private fun DiscussionSearchResultItem(discussion: DiscussionClusterSummary) {
    RowItemTemplate(
        titleStartPadding = dimensions().spacing16x,
        title = {
            Text(
                text = discussion.topic,
                style = MaterialTheme.wireTypography.body01
            )
        },
        subtitle = {
            Column {
                Text(
                    text = discussion.conversationName,
                    style = MaterialTheme.wireTypography.body02
                )
                Text(
                    text = stringResource(
                        R.string.search_results_discussions_participants,
                        discussion.participants.joinToString(separator = ", ")
                    ),
                    style = MaterialTheme.wireTypography.body02
                )
            }
        }
    )
}
