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
 */
package com.wire.android.ui.calling.ongoing.fullscreen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.ongoing.participantsview.ParticipantTile
import com.wire.android.ui.common.dimensions
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.UserId

@Composable
fun FullScreenTile(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    userId: UserId,
    clientId: String,
    isSelfUser: Boolean,
    height: Dp,
    onDoubleTap: (offset: Offset) -> Unit
) {

    sharedCallingViewModel.callState.participants.find {
        it.id == userId && it.clientId == clientId
    }?.let {
        ParticipantTile(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { /* Called when the gesture starts */ },
                        onDoubleTap = onDoubleTap,
                        onLongPress = { /* Called on Long Press */ },
                        onTap = { /* Called on Tap */ }
                    )
                }
                .height(height)
                .padding(
                    start = dimensions().spacing4x,
                    end = dimensions().spacing4x
                ),
            participantTitleState = it,
            isSelfUser = isSelfUser,
            shouldFill = false,
            onSelfUserVideoPreviewCreated = sharedCallingViewModel::setVideoPreview,
            onClearSelfUserVideoPreview = sharedCallingViewModel::clearVideoPreview
        )
    }
}

@Preview
@Composable
fun PreviewFullScreenVideoCall() {
    FullScreenTile(
        userId = UserId(String.EMPTY, String.EMPTY),
        clientId = String.EMPTY,
        isSelfUser = false,
        height = 100.dp,
        onDoubleTap = { }
    )
}
