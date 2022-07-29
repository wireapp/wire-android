package com.wire.android.ui.calling

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.call.Participant

data class CallState(
    val conversationName: ConversationName? = null,
    val callerName: String? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val participants: List<UICallParticipant> = listOf(),
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraFlipped: Boolean = false,
    val conversationType: ConversationType = ConversationType.OneOnOne,
    val membership: Membership = Membership.None,
)
