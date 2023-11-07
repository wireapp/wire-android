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

package com.wire.android.ui.calling

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.call.CallStatus

data class CallState(
    val conversationId: ConversationId,
    val conversationName: ConversationName? = null,
    val callerName: String? = null,
    val callStatus: CallStatus = CallStatus.CLOSED,
    val avatarAssetId: UserAvatarAsset? = null,
    val participants: List<UICallParticipant> = listOf(),
    val isMuted: Boolean? = null,
    val isCameraOn: Boolean = false,
    val isOnFrontCamera: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val isCbrEnabled: Boolean = false,
    val conversationType: ConversationType = ConversationType.OneOnOne,
    val membership: Membership = Membership.None
)
