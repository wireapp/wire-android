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

package com.wire.android.ui.calling.ongoing.participantsview.horizentalview

import android.view.View
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.participantsview.ParticipantTile
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.QualifiedID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallingHorizontalView(
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    contentHeight: Dp,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    onDoubleTap: (selectedParticipant: SelectedParticipant) -> Unit
) {

    LazyColumn(
        modifier = Modifier.padding(
            start = dimensions().spacing4x,
            end = dimensions().spacing4x
        ),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x)
    ) {
        items(items = participants, key = { it.id.toString() + it.clientId }) { participant ->
            // since we are getting participants by chunk of 8 items,
            // we need to check that we are on first page for self user
            val isSelfUser = remember(pageIndex, participants.first()) {
                pageIndex == 0 && participants.first() == participant
            }
            val spacing4x = dimensions().spacing4x
            val tileHeight = remember(participants.size) {
                (contentHeight - spacing4x) / participants.size
            }

            ParticipantTile(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onDoubleTap(
                                    SelectedParticipant(
                                        userId = participant.id,
                                        clientId = participant.clientId,
                                        isSelfUser = isSelfUser
                                    )
                                )
                            }
                        )
                    }
                    .fillMaxWidth()
                    .height(tileHeight)
                    .animateItemPlacement(tween(durationMillis = 200)),
                participantTitleState = participant,
                isSelfUser = isSelfUser,
                isSelfUserMuted = isSelfUserMuted,
                isSelfUserCameraOn = isSelfUserCameraOn,
                onSelfUserVideoPreviewCreated = onSelfVideoPreviewCreated,
                onClearSelfUserVideoPreview = onSelfClearVideoPreview
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCallingHorizontalView() {
    val participant1 = UICallParticipant(
        id = QualifiedID("", ""),
        clientId = "client-id",
        name = "user name",
        isMuted = true,
        isSpeaking = false,
        isCameraOn = false,
        isSharingScreen = false,
        avatar = null,
        membership = Membership.Admin,
        hasEstablishedAudio = true
    )
    val participant2 = UICallParticipant(
        id = QualifiedID("", ""),
        clientId = "client-id",
        name = "user name 2",
        isMuted = true,
        isSpeaking = false,
        isCameraOn = false,
        isSharingScreen = false,
        avatar = null,
        membership = Membership.Admin,
        hasEstablishedAudio = true
    )
    CallingHorizontalView(
        participants = listOf(participant1, participant2),
        pageIndex = 0,
        isSelfUserMuted = true,
        isSelfUserCameraOn = false,
        contentHeight = 500.dp,
        onSelfVideoPreviewCreated = {},
        onSelfClearVideoPreview = {},
        onDoubleTap = { }
    )
}
