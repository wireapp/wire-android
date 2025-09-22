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
package com.wire.android.ui.userprofile.other

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

/**
 * @param userId the id of the user whose profile is accessed
 * @param groupConversationId the id of the group conversation from which the user profile is accessed, to show and edit the member role
 * for that user, if the profile is not accessed from a conversation members list or mention then this parameter should be null
 */
data class OtherUserProfileNavArgs(
    val userId: UserId,
    val groupConversationId: ConversationId? = null
)
