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
package com.wire.android.ui.debug.conversation

import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCUseCase
import dev.zacsweers.metro.Inject

@Inject
class DebugConversationViewModelFactory(
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val resetMLSConversation: ResetMLSConversationUseCase,
    private val fetchConversation: FetchConversationUseCase,
    private val feedConversation: DebugFeedConversationUseCase,
    private val getConversationEpochFromCC: GetConversationEpochFromCCUseCase,
) {
    fun create(args: DebugConversationScreenNavArgs): DebugConversationViewModel = DebugConversationViewModel(
        conversationDetails = conversationDetails,
        resetMLSConversation = resetMLSConversation,
        fetchConversation = fetchConversation,
        feedConversation = feedConversation,
        getConversationEpochFromCC = getConversationEpochFromCC,
        args = args,
    )
}
