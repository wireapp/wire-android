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
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModel
import com.wire.android.di.metro.sessionKeyedMetroViewModel
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
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel as metroxViewModel

interface CallingManualViewModelFactory : ManualViewModelAssistedFactory {
    fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel
    fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel
    fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel
    fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel
}

interface CallingViewModelGraph {
    val callingViewModelFactory: CallingViewModelFactory
}

@Composable
fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel =
    sessionKeyedAssistedMetroViewModel<IncomingCallViewModel, CallingManualViewModelFactory>(
        key = "incoming_$conversationId",
    ) {
        incomingCallViewModel(conversationId)
    }

@Composable
fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel =
    sessionKeyedAssistedMetroViewModel<OutgoingCallViewModel, CallingManualViewModelFactory>(
        key = "outgoing_$conversationId",
    ) {
        outgoingCallViewModel(conversationId)
    }

@Composable
fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel =
    sessionKeyedAssistedMetroViewModel<OngoingCallViewModel, CallingManualViewModelFactory>(
        key = "ongoing_$conversationId",
    ) {
        ongoingCallViewModel(conversationId)
    }

@Composable
fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel =
    sessionKeyedAssistedMetroViewModel<SharedCallingViewModel, CallingManualViewModelFactory>(
        key = "shared_$conversationId",
    ) {
        sharedCallingViewModel(conversationId)
    }

@Composable
fun conversationCallViewModel(): ConversationCallViewModel =
    metroxViewModel()

@Composable
fun conversationListCallViewModel(conversationsSource: ConversationsSource): ConversationListCallViewModel = when {
    LocalInspectionMode.current -> ConversationListCallViewModelPreview
    else -> sessionKeyedMetroViewModel<ConversationListCallViewModelImpl>(key = "call_$conversationsSource")
}
