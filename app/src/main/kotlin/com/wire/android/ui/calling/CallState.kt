package com.wire.android.ui.calling

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

data class CallState(
    val conversationName: ConversationName? = null,
    val callerName: String? = null,
    val callStatus: CallStatus = CallStatus.CLOSED,
    val avatarAssetId: UserAvatarAsset? = null,
    val participants: List<UICallParticipant> = listOf(),
    val isMuted: Boolean? = null,
    val isCameraOn: Boolean? = null,
    val isSpeakerOn: Boolean = false,
    val isCameraFlipped: Boolean = false,
    val conversationType: ConversationType = ConversationType.OneOnOne,
    val membership: Membership = Membership.None,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
)
