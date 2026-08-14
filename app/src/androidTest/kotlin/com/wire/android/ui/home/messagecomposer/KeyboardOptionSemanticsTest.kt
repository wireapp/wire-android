/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.R
import com.wire.android.model.Contact
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.ConnectionState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KeyboardOptionSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenKeyboardAttachmentOption_whenRendered_thenItExposesOneActionableSemanticsNode() {
        var clicked = false
        val label = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.attachment_share_file)

        composeTestRule.setContent {
            WireTestTheme {
                KeyboardAttachmentOptions(
                    options = listOf(
                        AttachmentOptionItem(
                            text = R.string.attachment_share_file,
                            icon = R.drawable.ic_attach_file,
                            onClick = { clicked = true },
                        )
                    ),
                    focusRequesters = remember { listOf(FocusRequester()) },
                    columnCount = 1,
                    contentPadding = PaddingValues(0.dp),
                    labelStyle = TextStyle.Default,
                )
            }
        }

        assertSingleActionableNode(label)
        composeTestRule.onNode(hasContentDescription(label) and hasClickAction()).performClick()
        composeTestRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun givenKeyboardMentionOption_whenRendered_thenItExposesOneActionableSemanticsNode() {
        var clicked = false
        val member = Contact(
            id = "member-id",
            domain = "wire.com",
            name = "Marko Alonso",
            handle = "marko",
            label = "@marko",
            membership = Membership.Admin,
            connectionState = ConnectionState.ACCEPTED,
        )
        val description = "${member.name}, ${member.label}"

        composeTestRule.setContent {
            WireTestTheme {
                KeyboardMentionList(
                    membersToMention = listOf(member),
                    searchQuery = "",
                    onMentionPicked = { clicked = true },
                    onDismissRequest = {},
                    firstItemFocusRequester = remember { FocusRequester() },
                )
            }
        }

        assertSingleActionableNode(description)
        composeTestRule.onNode(hasContentDescription(description) and hasClickAction()).performClick()
        composeTestRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun givenKeyboardRichTextOption_whenRendered_thenItExposesOneActionableSemanticsNode() {
        var clicked = false
        val label = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.content_description_conversation_rich_text_header)

        composeTestRule.setContent {
            WireTestTheme {
                RichTextOptions(
                    onRichTextHeaderButtonClicked = { clicked = true },
                    onRichTextBoldButtonClicked = {},
                    onRichTextItalicButtonClicked = {},
                    onCloseRichTextEditingButtonClicked = {},
                    useKeyboardNavigation = true,
                )
            }
        }

        assertSingleActionableNode(label)
        composeTestRule.onNode(hasContentDescription(label) and hasClickAction()).performClick()
        composeTestRule.runOnIdle { assertTrue(clicked) }
    }

    private fun assertSingleActionableNode(contentDescription: String) {
        val matcher = hasContentDescription(contentDescription) and hasClickAction()
        composeTestRule.onAllNodes(matcher, useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onNode(matcher, useUnmergedTree = true).assertHasClickAction()
    }
}
