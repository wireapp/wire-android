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
package com.wire.android.search

import com.wire.android.search.apps.SearchAppsViewModel
import com.wire.android.search.users.SearchUserViewModel
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey

@BindingContainer
object SearchMetroViewModelBindings {

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(SearchManualViewModelFactory::class)
    fun searchManualViewModelFactory(factory: SearchViewModelFactory): ManualViewModelAssistedFactory =
        object : SearchManualViewModelFactory {
            override fun searchUserViewModel(conversationId: ConversationId?): SearchUserViewModel =
                factory.searchUserViewModel(conversationId)

            override fun searchAppsViewModel(protocolInfo: Conversation.ProtocolInfo?): SearchAppsViewModel =
                factory.searchAppsViewModel(protocolInfo)
        }
}
