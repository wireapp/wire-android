package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.calling.ParticipantTile
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions

@Composable
fun OneOnOneCallView(
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
) {
    Column(
        modifier = Modifier.padding(dimensions().spacing6x),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing6x)
    ) {
        participants.forEach { participant ->
            // For now we are handling only self user camera state
            val isCameraOn = if (pageIndex == 0 && participants.first() == participant)
                isSelfUserCameraOn else false
            val isMuted = if (pageIndex == 0 && participants.first() == participant) isSelfUserMuted
                else participant.isMuted
            ParticipantTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                conversationName = getConversationName(participant.name),
                participantAvatar = participant.avatar,
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                isOtherCameraOn = participant.isCameraOn,
                onSelfUserVideoPreviewCreated = {
                    if (pageIndex == 0 && participants.first() == participant) onSelfVideoPreviewCreated(it)
                },
                onClearSelfUserVideoPreview = {
                    if (pageIndex == 0 && participants.first() == participant)
                        onSelfClearVideoPreview()
                },
                isActiveSpeaker = participant.isSpeaking,
                isSelfUser = participant == participants.first(),
                userIdString = participant.id.toString(),
                clientIdString = participant.clientId
            )
        }
    }
}
