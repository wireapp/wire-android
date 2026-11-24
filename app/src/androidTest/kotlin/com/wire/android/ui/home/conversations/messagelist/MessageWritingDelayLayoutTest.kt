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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.RegularMessageItem
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.util.ui.UIText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MessageWritingDelayLayoutTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun groupedBubbleShowsWritingDelayNextToTimestamp() = assertInline(bubbles = true, grouped = true)

    @Test
    fun legacyHeaderShowsWritingDelayNextToTimestamp() = assertInline(bubbles = false, grouped = false)

    @Test
    fun groupedLegacyMessageShowsWritingDelayNextToTimestamp() = assertInline(bubbles = false, grouped = true)

    private fun assertInline(bubbles: Boolean, grouped: Boolean) {
        val label = "Written 5 min earlier"
        val message = mockMessageWithText.copy(writingDelayLabel = UIText.DynamicString(label))
        rule.setContent {
            WireTestTheme {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    RegularMessageItem(
                        clickActions = MessageClickActions.Content(),
                        message = message,
                        conversationDetailsData = ConversationDetailsData.None(null),
                        showAuthor = !grouped,
                        useSmallBottomPadding = grouped,
                        isBubbleUiEnabled = bubbles
                    )
                }
            }
        }

        val timestamp = rule.onNodeWithText(message.header.messageTime.formattedDate, useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val writingDelay = rule.onNodeWithText(label, useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(timestamp.center.y, writingDelay.center.y, 1f)
    }
}
