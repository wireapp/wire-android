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

package com.wire.android.ui.calling.ongoing.participantsview.gridview

import android.view.View
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.participantsview.ParticipantTile
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCallGrid(
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    contentHeight: Dp,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    onDoubleTap: (userId: UserId, clientId: String, isSelfUser: Boolean) -> Unit
) {
    val config = LocalConfiguration.current

    LazyVerticalGrid(
        userScrollEnabled = false,
        contentPadding = PaddingValues(MaterialTheme.wireDimensions.spacing4x),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x),
        columns = GridCells.Fixed(NUMBER_OF_GRID_CELLS)
    ) {

        items(
            items = participants,
            key = { it.id.toString() + it.clientId + pageIndex },
            contentType = { getContentType(it.isCameraOn, it.isSharingScreen) }
        ) { participant ->
            // since we are getting participants by chunk of 8 items,
            // we need to check that we are on first page for self user
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

            // if we have more than 6 participants then we reduce avatar size
            val userAvatarSize =
                if (participants.size <= 6 || config.screenHeightDp > MIN_SCREEN_HEIGHT) {
                    dimensions().onGoingCallUserAvatarSize
                } else {
                    dimensions().onGoingCallUserAvatarMinimizedSize
                }

            val usernameString =
                when (val conversationName = getConversationName(participant.name)) {
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
                isSharingScreen = participant.isSharingScreen,
                avatar = participant.avatar,
                membership = participant.membership
            )
            val tileHeight = (contentHeight - dimensions().spacing4x) / numberOfTilesRows

            ParticipantTile(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onDoubleTap(
                                    participantState.id,
                                    participantState.clientId,
                                    isSelfUser
                                )
                            }
                        )
                    }
                    .height(tileHeight)
                    .animateItemPlacement(tween(durationMillis = 200)),
                participantTitleState = participantState,
                onGoingCallTileUsernameMaxWidth = dimensions().onGoingCallTileUsernameMaxWidth,
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

private fun getContentType(
    isCameraOn: Boolean,
    isSharingScreen: Boolean
) = if (isCameraOn || isSharingScreen) "videoRender" else null

private const val NUMBER_OF_GRID_CELLS = 2
private const val MIN_SCREEN_HEIGHT = 800

@Preview
@Composable
fun PreviewGroupCallGrid() {
    GroupCallGrid(
        participants = listOf(
            UICallParticipant(
                id = QualifiedID("", ""),
                clientId = "clientId",
                name = "name",
                isMuted = false,
                isSpeaking = false,
                isCameraOn = false,
                isSharingScreen = false,
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
                isSharingScreen = false,
                avatar = null,
                membership = Membership.Admin,
            )
        ),
        contentHeight = 800.dp,
        pageIndex = 0,
        isSelfUserMuted = true,
        isSelfUserCameraOn = false,
        onSelfVideoPreviewCreated = { },
        onSelfClearVideoPreview = { },
        onDoubleTap = { _, _, _ -> {} }
    )
}
