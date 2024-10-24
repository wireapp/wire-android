/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.common.topappbar

import androidx.compose.runtime.Stable
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import kotlin.time.Duration

@Stable
sealed interface ConnectivityUIState {
    data object Connecting : ConnectivityUIState

    data class WaitingConnection(val cause: CoreFailure?, val retryDelay: Duration?) : ConnectivityUIState

    data object None : ConnectivityUIState

    data class EstablishedCall(
        val conversationId: ConversationId,
        val isMuted: Boolean
    ) : ConnectivityUIState

    data class IncomingCall(
        val conversationId: ConversationId,
        val callerName: String?
    ) : ConnectivityUIState

    data class OutgoingCall(
        val conversationId: ConversationId,
        val conversationName: String?
    ) : ConnectivityUIState
}
