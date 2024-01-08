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

package com.wire.android.ui.home.conversations.details.options

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer

data class GroupConversationOptionsState(
    val conversationId: ConversationId,
    val groupName: String = "",
    val protocolInfo: Conversation.ProtocolInfo = Conversation.ProtocolInfo.Proteus,
    val areAccessOptionsAvailable: Boolean = false,
    val isGuestAllowed: Boolean = false,
    val isServicesAllowed: Boolean = false,
    val isReadReceiptAllowed: Boolean = false,
    val isUpdatingAllowed: Boolean = false,
    val isUpdatingGuestAllowed: Boolean = false,
    val isUpdatingReadReceiptAllowed: Boolean = false,
    val changeGuestOptionConfirmationRequired: Boolean = false,
    val changeServiceOptionConfirmationRequired: Boolean = false,
    val loadingServicesOption: Boolean = false,
    val loadingReadReceiptOption: Boolean = false,
    val error: Error = Error.None,
    val mlsEnabled: Boolean = false,
    val selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Disabled
) {

    sealed interface Error {
        object None : Error
        class UpdateServicesError(val cause: CoreFailure) : Error
        class UpdateReadReceiptError(val cause: CoreFailure) : Error
    }
}
