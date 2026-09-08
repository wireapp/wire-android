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
import com.wire.android.di.metro.WireAssistedViewModelFactoryGroup
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.incoming.IncomingCallViewModel
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModel
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelImpl
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelPreview
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.meetings.MeetingsCallViewModel
import com.wire.kalium.logic.data.id.ConversationId

@WireAssistedViewModelFactoryGroup
object CallingManualViewModelFactoryGroup

@Composable
fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel =
    wireAssistedMetroViewModel<IncomingCallViewModel, CallingManualViewModelFactory>(
        instanceKey = "incoming_$conversationId",
    ) { _ ->
        incomingCallViewModel(conversationId)
    }

@Composable
fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel =
    wireAssistedMetroViewModel<OutgoingCallViewModel, CallingManualViewModelFactory>(
        instanceKey = "outgoing_$conversationId",
    ) { _ ->
        outgoingCallViewModel(conversationId)
    }

@Composable
fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel =
    wireAssistedMetroViewModel<OngoingCallViewModel, CallingManualViewModelFactory>(
        instanceKey = "ongoing_$conversationId",
    ) { _ ->
        ongoingCallViewModel(conversationId)
    }

@Composable
fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel =
    wireAssistedMetroViewModel<SharedCallingViewModel, CallingManualViewModelFactory>(
        instanceKey = "shared_$conversationId",
    ) { _ ->
        sharedCallingViewModel(conversationId)
    }

@Composable
fun conversationCallViewModel(): ConversationCallViewModel =
    wireMetroViewModel()

@Composable
fun conversationCallViewModel(args: ConversationNavArgs): ConversationCallViewModel =
    wireAssistedMetroViewModel<ConversationCallViewModel, CallingManualViewModelFactory> { _ ->
        conversationCallViewModel(args)
    }

@Composable
fun conversationListCallViewModel(conversationsSource: ConversationsSource): ConversationListCallViewModel = when {
    LocalInspectionMode.current -> ConversationListCallViewModelPreview
    else -> wireMetroViewModel<ConversationListCallViewModelImpl>(instanceKey = "call_$conversationsSource")
}

@Composable
fun meetingsCallViewModel(): MeetingsCallViewModel = wireMetroViewModel()
