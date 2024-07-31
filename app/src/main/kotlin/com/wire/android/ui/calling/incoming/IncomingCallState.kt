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
package com.wire.android.ui.calling.incoming

import com.wire.kalium.logic.data.id.ConversationId

data class IncomingCallState(
    val hasEstablishedCall: Boolean = false,
    val shouldShowJoinCallAnywayDialog: Boolean = false,
    val flowState: FlowState = FlowState.Default,
    val waitingUnlockToJoin: Boolean = false,
    val waitingUnlockToJoinAnyway: Boolean = false
) {
    sealed interface FlowState {
        data object Default : FlowState
        data object CallClosed : FlowState
        data class CallAccepted(val conversationId: ConversationId) : FlowState
    }
}
