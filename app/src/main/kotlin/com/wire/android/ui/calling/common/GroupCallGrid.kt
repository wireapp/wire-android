package com.wire.android.ui.calling.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.wire.android.model.ImageAsset
import com.wire.android.ui.calling.ParticipantTile
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.call.Participant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCallGrid(participants: List<Participant>) {
    val config = LocalConfiguration.current

    if (participants.isNotEmpty() && participants.size < 4) {
        Column(
            modifier = Modifier.padding(
                start = dimensions().spacing6x,
                end = dimensions().spacing6x,
                top = dimensions().spacing6x
            ), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            participants.forEach { participant ->
                //For now we are handling only self user camera state
//                val isSelfUserCameraOn = if (participants.first() == participant) callState.isCameraOn else false
                ParticipantTile(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    conversationName = getConversationName(participant.name),
                    participantAvatar = ImageAsset.UserAvatarAsset(participant.avatarAssetId!!),
                    isMuted = participant.isMuted,
                    isCameraOn = false,
                    onSelfUserVideoPreviewCreated = { },
                    onClearSelfUserVideoPreview = { }
                )
            }
        }
    } else {
        LazyVerticalGrid(
            modifier = Modifier.padding(
                start = dimensions().spacing6x,
                end = dimensions().spacing6x,
                top = dimensions().spacing6x),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), columns = GridCells.Fixed(2)
        ) {

            items(participants) {
                val numberOfGridRows = if (participants.size % 2 == 0) (participants.size / 2) else ((participants.size / 2) + 1)

                ParticipantTile(
                    modifier = Modifier
                        .height(((config.screenHeightDp - 72 - 112) / numberOfGridRows).dp)
                        .animateItemPlacement(),
                    conversationName = getConversationName(it.name),
                    participantAvatar = ImageAsset.UserAvatarAsset(it.avatarAssetId!!),
                    isMuted = it.isMuted,
                    isCameraOn = false,
                    onSelfUserVideoPreviewCreated = { },
                    onClearSelfUserVideoPreview = { }
                )
            }
        }
    }

}
