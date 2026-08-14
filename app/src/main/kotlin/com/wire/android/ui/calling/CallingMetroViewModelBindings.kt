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

import androidx.lifecycle.ViewModel
import com.wire.android.ui.CallFeedbackViewModel
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.incoming.IncomingCallViewModel
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelImpl
import com.wire.android.ui.home.meetings.MeetingsCallViewModel
import com.wire.kalium.logic.data.id.ConversationId
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object CallingMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(CallFeedbackViewModel::class)
    fun callFeedbackViewModel(viewModel: CallFeedbackViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(MeetingsCallViewModel::class)
    fun meetingsCallViewModel(viewModel: MeetingsCallViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ConversationListCallViewModelImpl::class)
    fun conversationListCallViewModel(viewModel: ConversationListCallViewModelImpl): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CallingManualViewModelFactory::class)
    fun callingManualViewModelFactory(
        incomingCallFactory: IncomingCallViewModel.Factory,
        outgoingCallFactory: OutgoingCallViewModel.Factory,
        ongoingCallFactory: OngoingCallViewModel.Factory,
        sharedCallingFactory: SharedCallingViewModel.Factory,
        conversationCallFactory: ConversationCallViewModel.Factory,
    ): ManualViewModelAssistedFactory = object : CallingManualViewModelFactory {
        override fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel =
            incomingCallFactory.create(conversationId)

        override fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel =
            outgoingCallFactory.create(conversationId)

        override fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel =
            ongoingCallFactory.create(conversationId)

        override fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel =
            sharedCallingFactory.create(conversationId)

        override fun conversationCallViewModel(args: ConversationNavArgs): ConversationCallViewModel =
            conversationCallFactory.create(args)
    }
}
