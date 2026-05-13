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
package com.wire.android.ui.home.conversations.search.adddembertoconversation

import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import dev.zacsweers.metro.Inject

@Inject
class AddMembersToConversationViewModelFactory(
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val dispatchers: DispatcherProvider,
) {
    fun create(args: AddMembersSearchNavArgs): AddMembersToConversationViewModel = AddMembersToConversationViewModel(
        addMembersSearchNavArgs = args,
        addMemberToConversation = addMemberToConversation,
        dispatchers = dispatchers,
    )
}
