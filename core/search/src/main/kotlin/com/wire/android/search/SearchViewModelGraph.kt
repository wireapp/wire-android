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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.search

import androidx.compose.runtime.Composable
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModel
import com.wire.android.search.apps.SearchAppsViewModel
import com.wire.android.search.users.SearchUserViewModel
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface SearchManualViewModelFactory : ManualViewModelAssistedFactory {
    fun searchUserViewModel(
        conversationId: ConversationId? = null,
        onlyConnectedContacts: Boolean = false,
    ): SearchUserViewModel

    fun searchAppsViewModel(protocolInfo: Conversation.ProtocolInfo? = null): SearchAppsViewModel
}

@Composable
fun searchUserViewModel(
    conversationId: ConversationId? = null,
    onlyConnectedContacts: Boolean = false,
): SearchUserViewModel =
    sessionKeyedAssistedMetroViewModel<SearchUserViewModel, SearchManualViewModelFactory>(
        key = listOfNotNull(
            "search_user",
            if (onlyConnectedContacts) "only_connected_contacts" else null,
            if (conversationId != null) "conversation_id_${conversationId.value}" else null
        ).joinToString("_")
    ) {
        searchUserViewModel(conversationId, onlyConnectedContacts)
    }

@Composable
fun searchAppsViewModel(protocolInfo: Conversation.ProtocolInfo?): SearchAppsViewModel =
    sessionKeyedAssistedMetroViewModel<SearchAppsViewModel, SearchManualViewModelFactory>(
        key = "search_apps_protocol_info_${protocolInfo?.name()}"
    ) {
        searchAppsViewModel(protocolInfo)
    }
