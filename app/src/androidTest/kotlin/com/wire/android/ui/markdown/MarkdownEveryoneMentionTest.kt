/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MarkdownEveryoneMentionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun targetOnlyMessageUpdateRefreshesItsProfileAction() {
        val memberId = UserId("member-id", "wire.com")
        val mention = MessageMention(0, 4, memberId, false)
        val body = mutableStateOf(MessageBody(UIText.DynamicString("@all", mentions = listOf(mention))))
        composeTestRule.setContent {
            WireTestTheme {
                MessageBody(
                    messageId = "same-message",
                    messageBody = body.value,
                    isAvailable = true,
                    accent = Accent.Blue,
                    onOpenProfile = {},
                    buttonList = null,
                    onLinkClick = {},
                    clickable = false,
                )
            }
        }
        fun profiles(): List<AnnotatedString.Range<String>> {
            val text = composeTestRule.onNodeWithText("@all", useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsProperties.Text].single()
            return text.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, text.length)
        }
        assertEquals(memberId.toString(), profiles().single().item)
        composeTestRule.runOnIdle {
            body.value = body.value.copy(message = UIText.DynamicString("@all", listOf(mention.copy(userId = null))))
        }
        assertEquals(0, profiles().size)
        composeTestRule.runOnIdle { body.value = body.value.copy(message = UIText.DynamicString("@all", listOf(mention))) }
        assertEquals(memberId.toString(), profiles().single().item)
    }

    @Test
    fun everyoneAfterIdenticalMemberInAnotherParagraphHasNoProfileAction() {
        showDocument("@all\n\n@all", everyoneStart = 6)

        val paragraphs = composeTestRule.onAllNodesWithText("@all", useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(2, paragraphs.size)
        val member = paragraphs[0].config[SemanticsProperties.Text].single()
        val everyone = paragraphs[1].config[SemanticsProperties.Text].single()
        assertEquals(1, member.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, member.length).size)
        assertEquals(0, everyone.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, everyone.length).size)
    }

    @Test
    fun boldEveryoneAfterIdenticalMemberHasNoProfileAction() {
        showDocument("@all **@all**", everyoneStart = 7)

        val text = composeTestRule.onNodeWithText("@all @all", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        val profiles = text.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, text.length)
        assertEquals(1, profiles.size)
        assertEquals(0, profiles.single().start)
        assertEquals(4, profiles.single().end)
    }

    @Test
    fun searchExcerptDoesNotGiveEveryoneTheOmittedMembersProfileAction() {
        showDocument("@all\n\n@all 42", everyoneStart = 6, searchQuery = "42")

        val text = composeTestRule.onNodeWithText("@all 42", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals(0, text.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, text.length).size)
    }

    @Test
    fun entityEncodedMarkerCannotGiveEveryoneAMembersProfileAction() {
        showDocument("&&&#xE000;&#xE001;&#xE00B;@all&&\n\n@all", everyoneStart = 26, memberStart = 34)

        val paragraphs = composeTestRule.onAllNodesWithText("@all", substring = true, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(2, paragraphs.size)
        val everyone = paragraphs[0].config[SemanticsProperties.Text].single()
        val profiles = everyone.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, everyone.length)
        assertEquals(0, profiles.size)
        val member = paragraphs[1].config[SemanticsProperties.Text].single()
        val memberProfiles = member.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, member.length)
        assertEquals(UserId("member-id", "wire.com").toString(), memberProfiles.single().item)
    }

    private fun showDocument(source: String, everyoneStart: Int, searchQuery: String = "", memberStart: Int = 0) {
        val (mentions, markedText) = source.toMarkdownTextWithMentions(
            listOf(
                MessageMention(memberStart, 4, UserId("member-id", "wire.com"), false),
                MessageMention(everyoneStart, 4, null, true),
            )
        )
        composeTestRule.setContent {
            WireTestTheme {
                MarkdownDocument(
                    document = markedText.toMarkdownDocument(),
                    nodeData = NodeData(
                        style = MaterialTheme.wireTypography.body01,
                        colorScheme = MaterialTheme.wireColorScheme,
                        typography = MaterialTheme.wireTypography,
                        mentions = mentions,
                        searchQuery = searchQuery,
                        messageColors = MessageColors(MaterialTheme.wireColorScheme.primary),
                        accent = Accent.Blue,
                    ),
                    clickable = false,
                )
            }
        }
    }

    @Test
    fun everyoneHasNoProfileActionWhileMemberKeepsProfileAction() {
        var rendered = AnnotatedString("")
        val memberId = UserId("member-id", "wire.com")
        val (mentions, markedText) = "Hi @all and @Ada".toMarkdownTextWithMentions(
            listOf(
                MessageMention(start = 3, length = 4, userId = null, isSelfMention = true),
                MessageMention(start = 12, length = 4, userId = memberId, isSelfMention = false),
            )
        )
        composeTestRule.setContent {
            WireTestTheme {
                val data = NodeData(
                    style = MaterialTheme.wireTypography.body01,
                    colorScheme = MaterialTheme.wireColorScheme,
                    typography = MaterialTheme.wireTypography,
                    mentions = mentions,
                    searchQuery = "",
                    messageColors = MessageColors(MaterialTheme.wireColorScheme.primary),
                    accent = Accent.Blue,
                )
                rendered = buildAnnotatedString { appendLinksAndMentions(this, markedText, data) }
            }
        }

        composeTestRule.runOnIdle {
            assertEquals("Hi @all and @Ada", rendered.text)
            val profileActions = rendered.getStringAnnotations(MarkdownConstants.TAG_MENTION, 0, rendered.length)
            assertEquals(1, profileActions.size)
            assertEquals(memberId.toString(), profileActions.single().item)
            assertEquals(12, profileActions.single().start)
        }
    }
}
