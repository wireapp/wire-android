/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.info

import com.wire.android.ui.home.conversations.ConversationAvatar
import com.wire.android.ui.home.conversations.ConversationDetailsData
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation

data class ConversationInfoViewState(
    val conversationName: UIText = UIText.DynamicString(""),
    val conversationDetailsData: ConversationDetailsData = ConversationDetailsData.None,
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
    val hasUserPermissionToEdit : Boolean = false,
    val conversationType: Conversation.Type = Conversation.Type.ONE_ON_ONE
)
