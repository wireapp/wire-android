package com.wire.android.mapper

import com.wire.android.model.ImageAsset
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.Participant
import javax.inject.Inject

class UICallParticipantMapper @Inject constructor(
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
) {
    fun toUICallParticipant(participant: Participant) = UICallParticipant(
        id = participant.id,
        clientId = participant.clientId,
        name = participant.name,
        isMuted = participant.isMuted,
        isSpeaking = participant.isSpeaking,
        avatar = participant.avatarAssetId?.let { ImageAsset.UserAvatarAsset(wireSessionImageLoader, it) },
        membership = userTypeMapper.toMembership(participant.userType)
    )
}
