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
package com.wire.android.ui.home.conversations.call

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

data class JoinOrStartCallViewState(
    val hasEstablishedCall: Boolean = false,
    val dialogType: JoinOrStartCallScreenDialogType = JoinOrStartCallScreenDialogType.None
)

sealed interface JoinOrStartCallViewActions {
    data class JoinedCall(val conversationId: ConversationId, val userId: UserId) : JoinOrStartCallViewActions
    data class InitiatedCall(val conversationId: ConversationId, val userId: UserId) : JoinOrStartCallViewActions
}
