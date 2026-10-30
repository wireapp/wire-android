/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.messagelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversations.messages.ReplyHighlightRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReplyHighlightComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var request by mutableStateOf<ReplyHighlightRequest?>(null)
    private var listGeneration by mutableIntStateOf(0)
    private lateinit var listState: LazyListState
    private lateinit var highlight: ReplyHighlightState
    private val finishedTokens = mutableListOf<Long>()

    @Before
    fun setUp() {
        composeTestRule.mainClock.autoAdvance = false
    }

    @Test
    fun givenVisibleTarget_whenRequested_thenPulsesHoldsAndFadesThenFinishes() {
        setContent()

        request(VISIBLE_ID, token = 1)
        advanceBy(REPLY_HIGHLIGHT_PULSE_MS / 2)
        assertTrue(intensity(VISIBLE_ID) > REPLY_HIGHLIGHT_HOLD_INTENSITY)
        assertEquals(0f, intensity(OTHER_VISIBLE_ID))

        advanceBy(1_000)
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(VISIBLE_ID))
        assertTrue(finishedTokens.isEmpty())

        advanceBy(REPLY_HIGHLIGHT_TOTAL_MS)
        assertEquals(0f, intensity(VISIBLE_ID))
        assertEquals(listOf(1L), finishedTokens)
    }

    @Test
    fun givenAnimationsDisabled_whenRequested_thenStaticTintForHoldDuration() {
        setContent(animationsEnabled = false)

        request(VISIBLE_ID, token = 1)
        advanceBy(FRAMES_TO_ARRIVE)
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(VISIBLE_ID))

        advanceBy(REPLY_HIGHLIGHT_HOLD_UNTIL_MS - FRAMES_TO_ARRIVE - 200)
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(VISIBLE_ID))

        advanceBy(400)
        assertEquals(0f, intensity(VISIBLE_ID))
        assertEquals(listOf(1L), finishedTokens)
    }

    @Test
    fun givenTargetNotYetVisible_whenItArrives_thenHighlightStartsOnArrival() {
        setContent()

        request(OFF_SCREEN_ID, token = 1)
        advanceBy(1_000)
        assertEquals(0f, intensity(OFF_SCREEN_ID))

        composeTestRule.runOnUiThread { listState.requestScrollToItem(OFF_SCREEN_INDEX) }
        advanceBy(REPLY_HIGHLIGHT_PULSE_MS / 2)
        assertTrue(intensity(OFF_SCREEN_ID) > 0f)
        assertTrue(finishedTokens.isEmpty())
    }

    @Test
    fun givenMissingTarget_whenWaitingTimesOut_thenFinishesWithoutHighlighting() {
        setContent()

        request(MISSING_ID, token = 1)
        repeat(8) {
            advanceBy(500)
            assertEquals(0f, intensity(MISSING_ID))
            assertEquals(0f, intensity(VISIBLE_ID))
        }
        assertEquals(listOf(1L), finishedTokens)
    }

    @Test
    fun givenActiveHighlight_whenUserScrolls_thenHighlightIsCancelledAndFinishes() {
        setContent()
        request(VISIBLE_ID, token = 1)
        advanceBy(1_000)
        assertTrue(intensity(VISIBLE_ID) > 0f)

        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        advanceBy(100)

        assertEquals(0f, intensity(VISIBLE_ID))
        assertEquals(listOf(1L), finishedTokens)
    }

    @Test
    fun givenPendingHighlight_whenUserDrags_thenRequestIsCancelledBeforeArrival() {
        setContent()
        request(OFF_SCREEN_ID, token = 1)
        advanceBy(200)

        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        advanceBy(100)

        assertEquals(listOf(1L), finishedTokens)
    }

    @Test
    fun givenActiveHighlight_whenNewerRequestArrives_thenEffectRestartsWithoutFinishingTheOldOne() {
        setContent()
        request(VISIBLE_ID, token = 1)
        advanceBy(1_500)
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(VISIBLE_ID))

        request(VISIBLE_ID, token = 2)
        advanceBy(REPLY_HIGHLIGHT_PULSE_MS / 2)
        assertTrue(intensity(VISIBLE_ID) > REPLY_HIGHLIGHT_HOLD_INTENSITY)

        advanceBy(REPLY_HIGHLIGHT_TOTAL_MS)
        assertEquals(listOf(2L), finishedTokens)
    }

    @Test
    fun givenActiveHighlight_whenListStateIsRecreated_thenEffectFollowsTheNewList() {
        setContent()
        request(VISIBLE_ID, token = 1)
        advanceBy(1_500)

        composeTestRule.runOnUiThread { listGeneration++ }
        advanceBy(FRAMES_TO_ARRIVE)
        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        advanceBy(100)

        assertEquals(0f, intensity(VISIBLE_ID))
        assertEquals(listOf(1L), finishedTokens)
    }

    private fun setContent(animationsEnabled: Boolean = true) {
        composeTestRule.setContent {
            // like ConversationMessageComposer, a new list state for every new list, while the highlight state survives
            listState = remember(listGeneration) { LazyListState() }
            highlight = rememberReplyHighlightState(
                request = request,
                lazyListState = listState,
                onFinished = { finishedTokens += it },
                animationsEnabled = animationsEnabled,
            )
            key(listGeneration) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .height(400.dp)
                        .testTag(LIST_TAG)
                ) {
                    items(items = (0 until ITEM_COUNT).map(::messageId), key = { it }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }
        }
        advanceBy(FRAMES_TO_ARRIVE)
    }

    private fun request(messageId: String, token: Long) {
        composeTestRule.runOnUiThread { request = ReplyHighlightRequest(messageId, token) }
    }

    private fun advanceBy(millis: Long) {
        composeTestRule.mainClock.advanceTimeBy(millis)
    }

    private fun intensity(messageId: String): Float {
        var value = 0f
        composeTestRule.runOnUiThread { value = highlight.intensityFor(messageId) }
        return value
    }

    private companion object {
        const val LIST_TAG = "list"
        const val ITEM_COUNT = 100
        const val OFF_SCREEN_INDEX = 80
        const val FRAMES_TO_ARRIVE = 100L
        val VISIBLE_ID = messageId(2)
        val OTHER_VISIBLE_ID = messageId(3)
        val OFF_SCREEN_ID = messageId(OFF_SCREEN_INDEX)
        const val MISSING_ID = "missing"

        fun messageId(index: Int) = "message_$index"
    }
}
