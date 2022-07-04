package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.model.ImageAsset
import com.wire.android.ui.calling.ParticipantTile
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.call.Participant

@Composable
fun OneOnOneCallView(
    participants: List<UICallParticipant>,
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
            val isCameraOn = if (participants.first() == participant) isSelfUserCameraOn else false
            ParticipantTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                conversationName = getConversationName(participant.name),
                participantAvatar = participant.avatar,
                isMuted = participant.isMuted,
                isCameraOn = isCameraOn,
                onSelfUserVideoPreviewCreated = {
                    if (participants.first() == participant) onSelfVideoPreviewCreated(it)
                },
                onClearSelfUserVideoPreview = {
                    if (participants.first() == participant)
                        onSelfClearVideoPreview()
                }
            )
        }
    }
}
