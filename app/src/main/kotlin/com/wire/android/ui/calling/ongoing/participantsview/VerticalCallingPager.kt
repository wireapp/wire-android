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

package com.wire.android.ui.calling.ongoing.participantsview

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.participantsview.gridview.GroupCallGrid
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId

@Composable
fun VerticalCallingPager(
    participants: List<UICallParticipant>,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    isInPictureInPictureMode: Boolean,
    isOnFrontCamera: Boolean,
    contentHeight: Dp,
    contentWidth: Dp,
    recentReactions: Map<UserId, String>,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit,
    onDoubleTap: (selectedParticipant: SelectedParticipant) -> Unit,
    flipCamera: () -> Unit,
    gridParams: CallingGridParams = CallingGridParams.fromScreenDimensions(width = contentWidth, height = contentHeight),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(width = contentWidth, height = contentHeight)
    ) {
        // if PiP is enabled and more than one participant is present,
        // we need to remove the first participant(self user) from the list
        val participantsWithoutPip =
            if (BuildConfig.PICTURE_IN_PICTURE_ENABLED && participants.size > 1) {
                participants.subList(1, participants.size)
            } else {
                participants
            }
        val participantsPages = remember(participantsWithoutPip, gridParams.maxItemsPerPage) {
            participantsWithoutPip.chunked(gridParams.maxItemsPerPage)
        }

        val pagerState = rememberPagerState(pageCount = { participantsPages.size })

        Box {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                if (participants.isNotEmpty()) {
                    val participantsWithCameraOn by rememberUpdatedState(participantsWithoutPip.count { it.isCameraOn })
                    val participantsWithScreenShareOn by rememberUpdatedState(participantsWithoutPip.count { it.isSharingScreen })
                    GroupCallGrid(
                        gridParams = gridParams,
                        participants = participantsPages[pageIndex],
                        isSelfUserMuted = isSelfUserMuted,
                        isSelfUserCameraOn = isSelfUserCameraOn,
                        contentHeight = contentHeight,
                        onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                        onSelfClearVideoPreview = onSelfClearVideoPreview,
                        onDoubleTap = onDoubleTap,
                        isInPictureInPictureMode = isInPictureInPictureMode,
                        recentReactions = recentReactions,
                        isOnFrontCamera = isOnFrontCamera,
                        flipCamera = flipCamera,
                    )

                    LaunchedEffect(
                        participantsWithCameraOn, // Request video stream when someone turns camera on/off
                        participantsWithScreenShareOn, // Request video stream when someone starts sharing screen
                        pagerState.currentPage // Request video stream when swiping to a different page on the grid
                    ) {
                        requestVideoStreams(participantsPages[pagerState.currentPage])
                    }
                }
            }
            // we don't need to display the indicator if we have one page and when it's in PiP mode
            if (participantsPages.size > 1 && !isInPictureInPictureMode) {
                Surface(
                    shape = RoundedCornerShape(dimensions().corner16x),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = MaterialTheme.wireDimensions.spacing12x),
                ) {
                    VerticalPagerIndicator(
                        modifier = Modifier.padding(dimensions().spacing4x),
                        pagerState = pagerState,
                        indicatorHeight = dimensions().spacing12x,
                        indicatorWidth = dimensions().spacing12x,
                        spacing = dimensions().spacing6x,
                        indicatorShape = CircleShape
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewVerticalCallingPager(participants: List<UICallParticipant>) {
    VerticalCallingPager(
        participants = participants,
        isSelfUserMuted = false,
        isSelfUserCameraOn = false,
        contentHeight = 800.dp,
        contentWidth = 480.dp,
        onSelfVideoPreviewCreated = {},
        onSelfClearVideoPreview = {},
        requestVideoStreams = {},
        onDoubleTap = { },
        flipCamera = { },
        isInPictureInPictureMode = false,
        recentReactions = emptyMap(),
        isOnFrontCamera = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewVerticalCallingPagerHorizontalView() = WireTheme {
    PreviewVerticalCallingPager(
        participants = buildPreviewParticipantsList(CallingGridParams.Portrait.maxItemsPerPage)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewVerticalCallingPagerGrid() = WireTheme {
    PreviewVerticalCallingPager(
        participants = buildPreviewParticipantsList(CallingGridParams.Portrait.maxItemsPerPage + 1)
    )
}
