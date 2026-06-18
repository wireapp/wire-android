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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesEmptyScreen
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNoResultsScreen
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesResultsScreen
import com.wire.android.ui.theme.wireTypography

@Composable
fun MessagesSearchResults(
    state: MessagesSearchState,
    onMessageClick: (UIMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    when (state) {
        MessagesSearchState.EmptyQuery -> SearchConversationMessagesEmptyScreen(modifier)
        MessagesSearchState.Loading -> LoadingListContent(modifier)
        MessagesSearchState.NoResults -> SearchConversationMessagesNoResultsScreen(modifier)
        MessagesSearchState.Failure -> MessagesSearchFailure(modifier)
        is MessagesSearchState.Success -> SearchConversationMessagesResultsScreen(
            messages = state.messages,
            lazyListState = lazyListState,
            onMessageClick = onMessageClick,
            modifier = modifier
        )
    }
}

@Composable
private fun MessagesSearchFailure(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.label_search_messages_no_results),
            style = MaterialTheme.wireTypography.body01,
        )
    }
}
