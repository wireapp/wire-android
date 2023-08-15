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
        isCameraOn = participant.isCameraOn,
        isSharingScreen = participant.isSharingScreen,
        avatar = participant.avatarAssetId?.let { ImageAsset.UserAvatarAsset(wireSessionImageLoader, it) },
        membership = userTypeMapper.toMembership(participant.userType),
        hasEstablishedAudio = participant.hasEstablishedAudio
    )
}
