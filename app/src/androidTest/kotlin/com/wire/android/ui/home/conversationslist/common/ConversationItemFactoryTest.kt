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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wire.android.model.BadgeEventType
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.QualifiedID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationItemFactoryTest {

    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    @Test
    fun givenDisabledSelectableConversation_whenClickingOnIt_thenItIsNotSelected() {
        var selectionCount = 0
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(
                    conversation = viewerOnlyConversation,
                    isSelectableItem = true,
                    isEnabled = false,
                    onConversationSelectedOnRadioGroup = { selectionCount++ },
                    openConversation = { selectionCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText(GROUP_NAME).performClick()

        assertEquals(0, selectionCount)
    }

    @Test
    fun givenEnabledSelectableConversation_whenClickingOnIt_thenItIsSelected() {
        var selectionCount = 0
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(
                    conversation = viewerOnlyConversation,
                    isSelectableItem = true,
                    isEnabled = true,
                    onConversationSelectedOnRadioGroup = { selectionCount++ },
                    openConversation = { selectionCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText(GROUP_NAME).performClick()

        assertEquals(1, selectionCount)
    }

    private val viewerOnlyConversation = ConversationItem.Group.Regular(
        groupName = GROUP_NAME,
        conversationId = QualifiedID("value", "domain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        lastMessageContent = null,
        badgeEventType = BadgeEventType.None,
        selfMemberRole = null,
        isFromTheSameTeam = false,
        teamId = null,
        isArchived = false,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        isFavorite = false,
        folder = null,
        isSelfUserViewerOnly = true,
    )

    private companion object {
        const val GROUP_NAME = "Viewer only conversation"
    }
}