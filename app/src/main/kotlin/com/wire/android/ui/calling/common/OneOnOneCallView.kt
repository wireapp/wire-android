package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.calling.ConversationName
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
        modifier = Modifier.padding(dimensions().spacing4x),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x)
    ) {
        participants.forEach { participant ->
            // since we are getting participants by chunk of 8 items,
            // we need to check that we are on first page for sel user
            val isSelfUser = pageIndex == 0 && participants.first() == participant

            val isCameraOn = if (isSelfUser)
                isSelfUserCameraOn else participant.isCameraOn
            val isMuted = if (isSelfUser)
                isSelfUserMuted else participant.isMuted

            val username = when (val conversationName = getConversationName(participant.name)) {
                is ConversationName.Known -> conversationName.name
                is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
            }

            val participantState = UICallParticipant(
                id = participant.id,
                clientId = participant.clientId,
                name = username,
                isMuted = isMuted,
                isSpeaking = participant.isSpeaking,
                isCameraOn = isCameraOn,
                avatar = participant.avatar,
                membership = participant.membership
            )
            ParticipantTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                participantTitleState = participantState,
                isSelfUser = isSelfUser,
                onSelfUserVideoPreviewCreated = {
                    if (isSelfUser) onSelfVideoPreviewCreated(it)
                },
                onClearSelfUserVideoPreview = {
                    if (isSelfUser)
                        onSelfClearVideoPreview()
                }
            )
        }
    }
}
