package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.ParticipantTile
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.QualifiedID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCallGrid(
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
) {
    val config = LocalConfiguration.current

    LazyVerticalGrid(
        userScrollEnabled = false,
        contentPadding = PaddingValues(MaterialTheme.wireDimensions.spacing4x),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x),
        columns = GridCells.Fixed(NUMBER_OF_GRID_CELLS)
    ) {

        items(items = participants, key = { it.id.toString() + it.clientId }) { participant ->
            // since we are getting participants by chunk of 8 items,
            // we need to check that we are on first page for sel user
            val isSelfUser = pageIndex == 0 && participants.first() == participant

            // We need the number of tiles rows needed to calculate their height
            val numberOfTilesRows = tilesRowsCount(participants.size)
            val isCameraOn = if (isSelfUser) {
                isSelfUserCameraOn
            } else participant.isCameraOn
            // for self user we don't need to get the muted value from participants list
            // if we do, this will show visuals with some delay
            val isMuted = if (isSelfUser) isSelfUserMuted
            else participant.isMuted

            // if we have more than 4 participants then we reduce avatar size
            val userAvatarSize = if (participants.size > 4) dimensions().onGoingCallUserAvatarSize
            else dimensions().onGoingCallUserAvatarMinimizedSize
            val usernameString = when (val conversationName = getConversationName(participant.name)) {
                is ConversationName.Known -> conversationName.name
                is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
            }

            val participantState = UICallParticipant(
                id = participant.id,
                clientId = participant.clientId,
                name = usernameString,
                isMuted = isMuted,
                isSpeaking = participant.isSpeaking,
                isCameraOn = isCameraOn,
                avatar = participant.avatar,
                membership = participant.membership
            )

            ParticipantTile(
                modifier = Modifier
                    .height(((config.screenHeightDp - TOP_APP_BAR_AND_BOTTOM_SHEET_HEIGHT) / numberOfTilesRows).dp)
                    .animateItemPlacement(tween(durationMillis = 200)),
                participantTitleState = participantState,
                onGoingCallTileUsernameMaxWidth = MaterialTheme.wireDimensions.onGoingCallTileUsernameMaxWidth,
                avatarSize = userAvatarSize,
                isSelfUser = isSelfUser,
                onSelfUserVideoPreviewCreated = {
                    if (isSelfUser) {
                        onSelfVideoPreviewCreated(it)
                    }
                },
                onClearSelfUserVideoPreview = {
                    if (isSelfUser) {
                        onSelfClearVideoPreview()
                    }
                }
            )
        }
    }
}

/**
 * Returns the number of lines needed to display x participants in a page
 */
private fun tilesRowsCount(participantsSize: Int): Int = with(participantsSize) {
    return@with if (this % 2 == 0) (this / 2) else ((this / 2) + 1)
}

private const val NUMBER_OF_GRID_CELLS = 2
private const val TOP_APP_BAR_AND_BOTTOM_SHEET_HEIGHT = 170

@Preview
@Composable
fun GroupCallGridPreview() {
    GroupCallGrid(
        participants = listOf(
            UICallParticipant(
                id = QualifiedID("", ""),
                clientId = "clientId",
                name = "name",
                isMuted = false,
                isSpeaking = false,
                isCameraOn = false,
                avatar = null,
                membership = Membership.Admin,
            ),
            UICallParticipant(
                id = QualifiedID("", ""),
                clientId = "clientId",
                name = "name",
                isMuted = false,
                isSpeaking = false,
                isCameraOn = false,
                avatar = null,
                membership = Membership.Admin,
            )
        ),
        pageIndex = 0,
        isSelfUserMuted = true,
        isSelfUserCameraOn = false,
        onSelfVideoPreviewCreated = { },
        onSelfClearVideoPreview = { }
    )
}
