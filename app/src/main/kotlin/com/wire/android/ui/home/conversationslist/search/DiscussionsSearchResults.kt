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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesEmptyScreen
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNoResultsScreen

@Composable
fun DiscussionsSearchResults(
    state: DiscussionsSearchState,
    onDiscussionClick: (DiscussionClusterSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        DiscussionsSearchState.EmptyQuery -> SearchConversationMessagesEmptyScreen(modifier)
        DiscussionsSearchState.Loading -> LoadingListContent(modifier)
        DiscussionsSearchState.NoResults -> SearchConversationMessagesNoResultsScreen(modifier)
        is DiscussionsSearchState.Success -> DiscussionsSearchResultsList(
            discussions = state.discussions,
            onDiscussionClick = onDiscussionClick,
            modifier = modifier
        )
    }
}

@Composable
private fun DiscussionsSearchResultsList(
    discussions: List<DiscussionClusterSummary>,
    onDiscussionClick: (DiscussionClusterSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = dimensions().spacing16x,
            vertical = dimensions().spacing8x
        )
    ) {
        items(discussions) { discussion ->
            DiscussionSearchResultCard(
                discussion = discussion,
                onClick = { onDiscussionClick(discussion) },
                modifier = Modifier.padding(vertical = dimensions().spacing4x)
            )
        }
    }
}
