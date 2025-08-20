/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.calling.model

import androidx.compose.runtime.Stable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.ConversationTypeForCall
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId

@Stable
data class CallState(
    val conversationId: ConversationId,
    val conversationName: ConversationName? = null,
    val callerName: String? = null,
    val accentId: Int = -1,
    val callStatus: CallStatus = CallStatus.CLOSED,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMuted: Boolean? = null,
    val isCameraOn: Boolean = false,
    val isOnFrontCamera: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val isCbrEnabled: Boolean = false,
    val conversationTypeForCall: ConversationTypeForCall = ConversationTypeForCall.OneOnOne,
    val membership: Membership = Membership.None,
    val protocolInfo: Conversation.ProtocolInfo? = null,
    val mlsVerificationStatus: Conversation.VerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    val proteusVerificationStatus: Conversation.VerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
)
