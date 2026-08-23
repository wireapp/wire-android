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
package com.wire.android.ui.home.conversations.call

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId

sealed interface JoinOrStartCallScreenDialogType {
    data class JoinAnyway(val conversationId: ConversationId) : JoinOrStartCallScreenDialogType
    data class VerificationDegraded(
        val conversationId: ConversationId,
        val conversationType: Conversation.Type
    ) : JoinOrStartCallScreenDialogType

    data class OngoingActiveCall(val conversationId: ConversationId) : JoinOrStartCallScreenDialogType
    data object NoConnectivity : JoinOrStartCallScreenDialogType
    data class CallConfirmation(
        val conversationId: ConversationId,
        val participantsCount: Int,
        val conversationType: Conversation.Type
    ) : JoinOrStartCallScreenDialogType

    sealed interface CallingFeatureUnavailable : JoinOrStartCallScreenDialogType {
        data object TeamMember : CallingFeatureUnavailable
        data object TeamAdmin : CallingFeatureUnavailable
        data object Other : CallingFeatureUnavailable
    }

    data object None : JoinOrStartCallScreenDialogType
}
