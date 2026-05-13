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
package com.wire.android.ui.connection

import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ConnectionActionButtonViewModelFactory(
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
) {
    fun create(args: ConnectionActionButtonArgs): ConnectionActionButtonViewModel = ConnectionActionButtonViewModelImpl(
        dispatchers = dispatchers,
        sendConnectionRequest = sendConnectionRequest,
        cancelConnectionRequest = cancelConnectionRequest,
        acceptConnectionRequest = acceptConnectionRequest,
        ignoreConnectionRequest = ignoreConnectionRequest,
        unblockUser = unblockUser,
        getOrCreateOneToOneConversation = getOrCreateOneToOneConversation,
        args = args,
    )
}
