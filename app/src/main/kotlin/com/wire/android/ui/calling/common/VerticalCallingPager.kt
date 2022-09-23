package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPagerApi::class)
@Composable
fun VerticalCallingPager(
    participants: List<UICallParticipant>,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        val pagerState = rememberPagerState()
        val pagesCount = pagesCount(participants.size)
        Box(
            modifier = Modifier.weight(1f),
        ) {
            VerticalPager(
                count = pagesCount,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                if (participants.isNotEmpty()) {

                    val participantsChunkedList = participants.chunked(MAX_TILES_PER_PAGE)
                    val participantsWithCameraOn by rememberUpdatedState(participants.count { it.isCameraOn })

                    LaunchedEffect(participantsWithCameraOn) {
                        requestVideoStreams(participantsChunkedList[pagerState.currentPage])
                    }
                    LaunchedEffect(true) {
                        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
                            if (page < participantsChunkedList.size)
                                requestVideoStreams(participantsChunkedList[page])
                        }
                    }
                    if (participantsChunkedList[pageIndex].size <= MAX_ITEMS_FOR_ONE_ON_ONE_VIEW) {
                        OneOnOneCallView(
                            participants = participantsChunkedList[pageIndex],
                            pageIndex = pageIndex,
                            isSelfUserMuted = isSelfUserMuted,
                            isSelfUserCameraOn = isSelfUserCameraOn,
                            onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                            onSelfClearVideoPreview = onSelfClearVideoPreview
                        )
                    } else {
                        GroupCallGrid(
                            participants = participantsChunkedList[pageIndex],
                            pageIndex = pageIndex,
                            isSelfUserMuted = isSelfUserMuted,
                            isSelfUserCameraOn = isSelfUserCameraOn,
                            onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                            onSelfClearVideoPreview = onSelfClearVideoPreview
                        )
                    }
                }
            }
            // we don't need to display the indicator if we have one page
            if (pagesCount > 1) {
                Surface(
                    shape = RoundedCornerShape(dimensions().corner16x),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = MaterialTheme.wireDimensions.spacing12x),
                    color = colorsScheme().callingPagerIndicatorBackground,
                ) {
                    VerticalPagerIndicator(
                        modifier = Modifier.padding(dimensions().spacing4x),
                        pagerState = pagerState,
                        activeColor = colorsScheme().callingActiveIndicator,
                        inactiveColor = colorsScheme().callingInActiveIndicator,
                        inactiveBorderColor = colorsScheme().callingInActiveBorderIndicator,
                        inactiveBorderWidth = dimensions().spacing2x,
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

/**
 * Returns number of pages(with an already defined max number of tiles) needed to display x participants
 */
private fun pagesCount(size: Int): Int {
    val pages = size / MAX_TILES_PER_PAGE
    return if (size % MAX_TILES_PER_PAGE > 0) {
        pages + 1
    } else pages
}

private const val MAX_TILES_PER_PAGE = 8
private const val MAX_ITEMS_FOR_ONE_ON_ONE_VIEW = 3

@Composable
@Preview
fun SamplePreview() {
    VerticalCallingPager(
        participants = listOf(),
        isSelfUserMuted = false,
        isSelfUserCameraOn = false,
        onSelfVideoPreviewCreated = {},
        onSelfClearVideoPreview = {},
        requestVideoStreams = {}
    )
}
