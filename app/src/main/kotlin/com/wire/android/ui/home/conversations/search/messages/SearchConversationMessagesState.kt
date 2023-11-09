/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.search.messages

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SearchConversationMessagesState(
    val conversationId: ConversationId,
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchResult: ImmutableList<UIMessage> = persistentListOf(),
    val isEmptyResult: Boolean = false,
    val isLoading: Boolean = false
)
