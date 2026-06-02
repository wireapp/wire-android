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
package com.wire.android.ui.calling

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.incoming.IncomingCallViewModel
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModel
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelImpl
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelPreview
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.kalium.logic.data.id.ConversationId

interface CallingViewModelGraph : MetroViewModelGraph {
    val callingViewModelFactory: CallingViewModelFactory
}

@Composable
fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel =
    metroViewModel<CallingViewModelGraph, IncomingCallViewModel>(key = "incoming_$conversationId") {
        callingViewModelFactory.incomingCallViewModel(conversationId)
    }

@Composable
fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel =
    metroViewModel<CallingViewModelGraph, OutgoingCallViewModel>(key = "outgoing_$conversationId") {
        callingViewModelFactory.outgoingCallViewModel(conversationId)
    }

@Composable
fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel =
    metroViewModel<CallingViewModelGraph, OngoingCallViewModel>(key = "ongoing_$conversationId") {
        callingViewModelFactory.ongoingCallViewModel(conversationId)
    }

@Composable
fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel =
    metroViewModel<CallingViewModelGraph, SharedCallingViewModel>(key = "shared_$conversationId") {
        callingViewModelFactory.sharedCallingViewModel(conversationId)
    }

@Composable
fun conversationCallViewModel(): ConversationCallViewModel =
    metroSavedStateViewModel<CallingViewModelGraph, ConversationCallViewModel> {
        callingViewModelFactory.conversationCallViewModel(it)
    }

@Composable
fun conversationListCallViewModel(conversationsSource: ConversationsSource): ConversationListCallViewModel = when {
    LocalInspectionMode.current -> ConversationListCallViewModelPreview
    else -> metroViewModel<CallingViewModelGraph, ConversationListCallViewModelImpl>(key = "call_$conversationsSource") {
        callingViewModelFactory.conversationListCallViewModel()
    }
}
