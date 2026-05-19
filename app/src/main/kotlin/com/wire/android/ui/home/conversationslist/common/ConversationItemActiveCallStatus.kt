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

package com.wire.android.ui.home.conversationslist.common

import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.id.ConversationId

internal fun ConversationItem.withActiveCallStatus(activeCallConversationIds: Set<ConversationId>): ConversationItem =
    when (this) {
        is ConversationItem.Group.Regular -> {
            val hasActiveCall = conversationId in activeCallConversationIds && isSelfUserMember
            copy(
                hasOnGoingCall = hasActiveCall,
                hasNewActivitiesToShow = hasNewActivitiesToShow || hasActiveCall
            )
        }

        is ConversationItem.Group.Channel -> {
            val hasActiveCall = conversationId in activeCallConversationIds && isSelfUserMember
            copy(
                hasOnGoingCall = hasActiveCall,
                hasNewActivitiesToShow = hasNewActivitiesToShow || hasActiveCall
            )
        }

        is ConversationItem.ConnectionConversation,
        is ConversationItem.PrivateConversation -> this
    }
