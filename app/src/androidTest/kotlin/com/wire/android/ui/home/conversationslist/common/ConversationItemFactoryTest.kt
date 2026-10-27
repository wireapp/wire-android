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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.wire.android.model.BadgeEventType
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.PlayingAudioInConversation
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
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
                    selectionMode = ConversationItemSelectionMode.SINGLE,
                    isEnabled = false,
                    onConversationSelectionChanged = { selectionCount++ },
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
                    selectionMode = ConversationItemSelectionMode.SINGLE,
                    isEnabled = true,
                    onConversationSelectionChanged = { selectionCount++ },
                    openConversation = { selectionCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText(GROUP_NAME).performClick()

        assertEquals(1, selectionCount)
    }

    @Test
    fun givenConversation_whenShiftF10IsHeld_thenOpensItsMenuOnceAndEnterStillOpensConversation() {
        val menus = mutableListOf<ConversationItem>()
        var opened = 0
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(
                    conversation = viewerOnlyConversation,
                    openMenu = { menus.add(it) },
                    openConversation = { opened++ },
                )
            }
        }
        focusRow()
        composeTestRule.onRoot().performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.F10)
            advanceEventTime(1_000)
            keyUp(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(listOf(viewerOnlyConversation), menus)
        assertEquals(0, opened)
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, opened)
    }

    @Test
    fun givenArchivedConversation_whenMenuKeyPressed_thenOpensItsMenu() {
        val archived = viewerOnlyConversation.copy(isArchived = true)
        val menus = mutableListOf<ConversationItem>()
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(conversation = archived, openMenu = { menus.add(it) })
            }
        }
        focusRow()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Menu) }
        assertEquals(listOf(archived), menus)
    }

    @Test
    fun givenSecondConversation_whenShiftF10Pressed_thenOpensOnlyItsMenu() {
        val second = viewerOnlyConversation.copy(groupName = "Second", conversationId = QualifiedID("second", "domain"))
        val menus = mutableListOf<ConversationItem>()
        composeTestRule.setContent {
            WireTestTheme {
                Column {
                    ConversationItemFactory(conversation = viewerOnlyConversation, openMenu = { menus.add(it) })
                    ConversationItemFactory(conversation = second, openMenu = { menus.add(it) })
                }
            }
        }
        focusRow("Second")
        shortcut()
        assertEquals(listOf(second), menus)
    }

    @Test
    fun givenAudioControlFocused_whenShiftF10Pressed_thenOpensOwningConversationMenu() {
        val menus = mutableListOf<ConversationItem>()
        var played = 0
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(
                    conversation = viewerOnlyConversation,
                    openMenu = { menus.add(it) },
                    playingAudio = PlayingAudioInConversation(viewerOnlyConversation.conversationId, "audio", true),
                    onPlayPauseCurrentAudio = { played++ },
                )
            }
        }
        focusRow()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Tab) }
        shortcut()
        assertEquals(listOf(viewerOnlyConversation), menus)
        assertEquals(0, played)
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(1, played)
    }

    @Test
    fun givenSelectableConversation_whenShortcutPressed_thenDoesNotOpenMenuOrChangeSelection() {
        var menus = 0
        var selections = 0
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(
                    conversation = viewerOnlyConversation,
                    selectionMode = ConversationItemSelectionMode.SINGLE,
                    openMenu = { menus++ },
                    onConversationSelectionChanged = { selections++ },
                )
            }
        }
        focusRow()
        shortcut()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Menu) }
        assertEquals(0, menus)
        assertEquals(0, selections)
    }

    @Test
    fun givenDisabledConversation_whenShortcutPressed_thenDoesNotOpenMenu() {
        var menus = 0
        composeTestRule.setContent {
            WireTestTheme {
                Column(Modifier.focusable()) {
                    ConversationItemFactory(conversation = viewerOnlyConversation, isEnabled = false, openMenu = { menus++ })
                }
            }
        }
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Tab) }
        shortcut()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Menu) }
        assertEquals(0, menus)
    }

    @Test
    fun givenConnectionRequest_whenShortcutPressed_thenDoesNotOpenMenuAndEnterStillOpensProfile() {
        var menus = 0
        var profiles = 0
        val connection = viewerOnlyConversation.copy(
            lastMessageContent = UILastMessageContent.Connection(ConnectionState.PENDING, UserId("user", "domain")),
        )
        composeTestRule.setContent {
            WireTestTheme {
                ConversationItemFactory(conversation = connection, openMenu = { menus++ }, openUserProfile = { profiles++ })
            }
        }
        focusRow()
        shortcut()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Menu) }
        assertEquals(0, menus)
        composeTestRule.onRoot().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, profiles)
    }

    private fun focusRow(name: String = GROUP_NAME) {
        composeTestRule.onNodeWithText(name).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        composeTestRule.onNodeWithText(name).assertIsFocused()
    }

    private fun shortcut() {
        composeTestRule.onRoot().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F10)
            keyUp(Key.ShiftLeft)
        }
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
