package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.wire.android.model.ImageAsset
import com.wire.android.ui.calling.ParticipantTile
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.common.dimensions
import com.wire.kalium.logic.data.call.Participant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCallGrid(
    participants: List<Participant>,
    isSelfUserCameraOn: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
) {
    val config = LocalConfiguration.current

    if (participants.isNotEmpty() && participants.size < 4) {
        Column(
            modifier = Modifier.padding(dimensions().spacing6x), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            participants.forEach { participant ->
                //For now we are handling only self user camera state
                val isCameraOn = if (participants.first() == participant) isSelfUserCameraOn else false
                ParticipantTile(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    conversationName = getConversationName(participant.name),
                    participantAvatar = ImageAsset.UserAvatarAsset(participant.avatarAssetId!!),
                    isMuted = participant.isMuted,
                    isCameraOn = isCameraOn,
                    onSelfUserVideoPreviewCreated = { onSelfVideoPreviewCreated(it) },
                    onClearSelfUserVideoPreview = onSelfClearVideoPreview
                )
            }
        }
    } else {
        LazyVerticalGrid(
            contentPadding = PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), columns = GridCells.Fixed(2)
        ) {

            items(participants) { participant ->
                val numberOfGridRows = if (participants.size % 2 == 0) (participants.size / 2) else ((participants.size / 2) + 1)
                val isCameraOn = if (participants.first() == participant) isSelfUserCameraOn else false

                ParticipantTile(
                    modifier = Modifier
                        .height(((config.screenHeightDp - 72 - 106) / numberOfGridRows).dp)
                        .animateItemPlacement(),
                    conversationName = getConversationName(participant.name),
                    participantAvatar = ImageAsset.UserAvatarAsset(participant.avatarAssetId!!),
                    isMuted = participant.isMuted,
                    isCameraOn = isCameraOn,
                    onSelfUserVideoPreviewCreated = { onSelfVideoPreviewCreated(it) },
                    onClearSelfUserVideoPreview = onSelfClearVideoPreview
                )
            }
        }
    }

}
