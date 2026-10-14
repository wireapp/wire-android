/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.wire.android.search.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.android.search.SearchManualViewModelFactoryGroup
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationMembersQueryConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@WireAssistedViewModelBinding(SearchManualViewModelFactoryGroup::class)
class SearchGroupsToAddMembersViewModel @AssistedInject constructor(
    @Assisted private val onlySelfTeamAndDomain: Boolean,
    private val searchConversations: GetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase,
    private val contactMapper: ContactMapper,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(onlySelfTeamAndDomain: Boolean): SearchGroupsToAddMembersViewModel
    }

    private val query = MutableStateFlow("")
    val groups: Flow<PagingData<SearchGroup>> = query.debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE).flatMapLatest { term ->
        searchConversations(
            queryConfig = ConversationMembersQueryConfig(searchQuery = term, onlyInteractionEnabled = true),
            pagingConfig = PagingConfig(pageSize = 20, initialLoadSize = 40, prefetchDistance = 5, enablePlaceholders = false),
            startingOffset = 0L,
            strictMlsFilter = false,
            onlySelfTeamAndDomain = onlySelfTeamAndDomain,
        ).map { conversations ->
            conversations.map { result ->
                val details = result.conversationDetails as ConversationDetails.Group
                SearchGroup(
                    details.conversation.id,
                    details.conversation.name.orEmpty(),
                    result.members.map { it.user }.filterIsInstance<OtherUser>().map(contactMapper::fromOtherUser).toSet(),
                    isChannel = details is ConversationDetails.Group.Channel,
                    isPrivateChannel = (details as? ConversationDetails.Group.Channel)?.access ==
                            ConversationDetails.Group.Channel.ChannelAccess.PRIVATE,
                )
            }
        }
    }.cachedIn(viewModelScope)

    fun search(query: String) {
        this.query.value = query
    }
}

data class SearchGroup(
    val id: ConversationId,
    val name: String,
    val members: Set<Contact>,
    val isChannel: Boolean = false,
    val isPrivateChannel: Boolean = false,
)
