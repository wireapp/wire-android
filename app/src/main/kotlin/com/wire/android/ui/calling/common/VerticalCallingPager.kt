package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.VerticalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.call.Participant

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VerticalCallingPager(
    participants: List<Participant>,
    isSelfUserCameraOn: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
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
                modifier = Modifier
                    .fillMaxSize()
            ) { pageIndex ->
                if (participants.isNotEmpty()) {
                    val participantsChunkedList = participants.chunked(MAX_TILES_PER_PAGE)
                    if (participantsChunkedList[pageIndex].size <= MAX_ITEMS_FOR_ONE_ON_ONE_VIEW) {
                        OneOnOneCallView(
                            participants = participantsChunkedList[pageIndex],
                            isSelfUserCameraOn = isSelfUserCameraOn,
                            onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                            onSelfClearVideoPreview = onSelfClearVideoPreview
                        )
                    } else {
                        GroupCallGrid(
                            participants = participantsChunkedList[pageIndex],
                            isSelfUserCameraOn = isSelfUserCameraOn,
                            onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                            onSelfClearVideoPreview = onSelfClearVideoPreview
                        )
                    }
                }
            }
            // we don't need to display the indicator if we have one page
            if (pagesCount > 1) {
                VerticalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = MaterialTheme.wireDimensions.spacing8x)
                )
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
    VerticalCallingPager(listOf(), false, {}, {})
}
