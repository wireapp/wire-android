/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.calling.ongoing.participantsview.gridview

import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.participantsview.CallingGridParams
import com.wire.android.ui.calling.ongoing.participantsview.ParticipantTile
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemesForLandscape
import com.wire.android.util.ui.PreviewMultipleThemesForPortrait
import com.wire.android.util.ui.PreviewMultipleThemesForSquare
import com.wire.kalium.logic.data.user.UserId

@Composable
fun GroupCallGrid(
    gridParams: CallingGridParams,
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    contentHeight: Dp,
    isOnFrontCamera: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    onDoubleTap: (selectedParticipant: SelectedParticipant) -> Unit,
    flipCamera: () -> Unit,
    isInPictureInPictureMode: Boolean,
    recentReactions: Map<UserId, String>,
    modifier: Modifier = Modifier,
    contentPadding: Dp = dimensions().spacing4x,
    spacedBy: Dp = dimensions().spacing2x,
) {
    val (columns, rows) = remember(gridParams, participants.size) {
        gridParams.calculateColumnsAndRows(participants.size)
    }
    val tileHeight = remember(participants.size, contentHeight, contentPadding, spacedBy) {
        val heightAvailableForItems = contentHeight - 2 * contentPadding - (rows - 1) * spacedBy
        heightAvailableForItems / rows
    }
    LazyVerticalGrid(
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = PaddingValues(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        verticalArrangement = Arrangement.spacedBy(spacedBy),
        columns = GridCells.Fixed(columns)
    ) {

        items(
            items = participants,
            key = { it.id.value + it.clientId },
            contentType = { getContentType(it.isCameraOn, it.isSharingScreen) }
        ) { participant ->
            Box(
                modifier = Modifier
                    .height(tileHeight)
                    .animateItem()
            ) {
                ParticipantTile(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onDoubleTap(
                                        SelectedParticipant(
                                            userId = participant.id,
                                            clientId = participant.clientId,
                                            isSelfUser = participant.isSelfUser,
                                        )
                                    )
                                }
                            )
                        },
                    participantTitleState = participant,
                    isOnPiPMode = isInPictureInPictureMode,
                    isSelfUserMuted = isSelfUserMuted,
                    isSelfUserCameraOn = isSelfUserCameraOn,
                    onSelfUserVideoPreviewCreated = onSelfVideoPreviewCreated,
                    onClearSelfUserVideoPreview = onSelfClearVideoPreview,
                    recentReaction = recentReactions[participant.id],
                    isOnFrontCamera = isOnFrontCamera,
                    flipCamera = flipCamera,
                )
            }
        }
    }
}

private fun getContentType(
    isCameraOn: Boolean,
    isSharingScreen: Boolean
) = if (isCameraOn || isSharingScreen) "videoRender" else null

@Composable
private fun PreviewGroupCallGrid(participants: List<UICallParticipant>, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(top = 60.dp, bottom = 80.dp)) { // paddings to simulate top and bottom bars
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            GroupCallGrid(
                gridParams = CallingGridParams.fromScreenDimensions(maxWidth, maxHeight),
                participants = participants,
                pageIndex = 0,
                isSelfUserMuted = false,
                isSelfUserCameraOn = false,
                contentHeight = maxHeight,
                onSelfVideoPreviewCreated = {},
                onSelfClearVideoPreview = {},
                onDoubleTap = { },
                isInPictureInPictureMode = false,
                recentReactions = emptyMap(),
                isOnFrontCamera = false,
                flipCamera = {},
            )
        }
    }
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewGroupCallGrid_1Participant() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(1))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewGroupCallGrid_2Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(2))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewGroupCallGrid_3Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(3))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewGroupCallGrid_4Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(4))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewGroupCallGrid_6Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(6))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@Composable
fun PreviewGroupCallGrid_8Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(8))
}

@PreviewMultipleThemesForSquare
@Composable
fun PreviewGroupCallGrid_9Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(9))
}
