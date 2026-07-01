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
package com.wire.android.ui.home.conversations.model

import android.content.res.Resources
import com.wire.android.framework.TestUser
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.withoutPreviewedLink
import com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview
import com.wire.kalium.logic.data.message.mention.MessageMention
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageTypesTest {

    @MockK
    lateinit var mockResources: Resources

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockResources.getString(any()) } returns "mockedString"
    }

    @Test
    fun `Given message with no mentions when mapping then no mentions should be returned`() {
        val uiText = UIText.DynamicString("Hello world")
        val result = mapToDisplayMentions(uiText, mockResources)
        assertTrue(result.first.isEmpty())
        assertEquals("Hello world", result.second)
    }

    @Test
    fun `Given message with a valid mention when mapping then the mention should be returned with markers`() {
        val mention = MessageMention(0, 6, TestUser.USER_ID, isSelfMention = false)
        val uiText = UIText.DynamicString("@Hello world", listOf(mention))
        val result = mapToDisplayMentions(uiText, mockResources)
        assertEquals(1, result.first.size)
        assertEquals("@Hello", result.first.first().mentionUserName)
        assertEquals("${MarkdownConstants.MENTION_MARK}@Hello${MarkdownConstants.MENTION_MARK} world", result.second)
    }

    @Test
    fun `Given message with a negative start mention when mapping then no mentions should be returned`() {
        val mention = MessageMention(-1, 5, TestUser.USER_ID, isSelfMention = false)
        val uiText = UIText.DynamicString("Hello world", listOf(mention))
        val result = mapToDisplayMentions(uiText, mockResources)
        assertTrue(result.first.isEmpty())
        assertEquals("Hello world", result.second)
    }

    @Test
    fun `Given message with a mention exceeding string length when mapping then no mentions should be returned`() {
        val mention = MessageMention(10, 20, TestUser.USER_ID, isSelfMention = false)
        val uiText = UIText.DynamicString("Hello world", listOf(mention))
        val result = mapToDisplayMentions(uiText, mockResources)
        assertTrue(result.first.isEmpty())
        assertEquals("Hello world", result.second)
    }

    @Test
    fun `Given message where mention's start position doesn't have '@' character when mapping then no mentions should be returned`() {
        val mention = MessageMention(6, 5, TestUser.USER_ID, isSelfMention = false)
        val uiText = UIText.DynamicString("Hello world", listOf(mention))
        val result = mapToDisplayMentions(uiText, mockResources)
        assertTrue(result.first.isEmpty())
        assertEquals("Hello world", result.second)
    }

    @Test
    fun `Given message with multiple mentions when mapping then only valid mentions should be returned`() {
        val validMention = MessageMention(6, 5, TestUser.USER_ID, isSelfMention = false)
        val invalidMention = MessageMention(50, 5, TestUser.OTHER_USER.id, isSelfMention = false)
        val uiText = UIText.DynamicString("Hello @world", listOf(validMention, invalidMention))
        val result = mapToDisplayMentions(uiText, mockResources)
        assertEquals(1, result.first.size)
    }

    @Test
    fun `Given text containing previewed url when deriving display text then the raw link is removed`() {
        val uiText = UIText.DynamicString("hello https://wire.com world")

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 6
                )
            )
        )

        assertEquals(UIText.DynamicString("hello world"), result)
    }

    @Test
    fun `Given text with mentions after previewed url when deriving display text then mention offsets are shifted`() {
        val mention = MessageMention(
            start = 19,
            length = 5,
            userId = TestUser.USER_ID,
            isSelfMention = false
        )
        val uiText = UIText.DynamicString(
            "https://wire.com @john",
            listOf(mention)
        )

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 0
                )
            )
        ) as UIText.DynamicString

        assertEquals("@john", result.value)
        assertEquals(0, result.mentions.single().start)
    }

    @Test
    fun `Given text with mention before previewed url when deriving display text then earlier mention stays unchanged`() {
        val mention = MessageMention(
            start = 0,
            length = 5,
            userId = TestUser.USER_ID,
            isSelfMention = false
        )
        val uiText = UIText.DynamicString(
            "@john https://wire.com",
            listOf(mention)
        )

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 6
                )
            )
        ) as UIText.DynamicString

        assertEquals("@john", result.value)
        assertEquals(0, result.mentions.single().start)
        assertEquals(5, result.mentions.single().length)
    }

    @Test
    fun `Given markdown around previewed url when deriving display text then markdown is preserved`() {
        val uiText = UIText.DynamicString("**hello** https://wire.com _world_")

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 10
                )
            )
        )

        assertEquals(UIText.DynamicString("**hello** _world_"), result)
    }

    @Test
    fun `Given mismatched preview url offset when deriving display text then original text is returned unchanged`() {
        val uiText = UIText.DynamicString("hello https://wire.com world")

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 7
                )
            )
        )

        assertSame(uiText, result)
    }

    @Test
    fun `Given out of bounds preview url offset when deriving display text then original text is returned unchanged`() {
        val uiText = UIText.DynamicString("hello https://wire.com world")

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 200
                )
            )
        )

        assertSame(uiText, result)
    }

    @Test
    fun `Given non dynamic text when deriving display text then original text is returned unchanged`() {
        val uiText: UIText = UIText.StringResource(android.R.string.copy)

        val result = uiText.withoutPreviewedLink(
            listOf(
                MessageLinkPreview(
                    url = "https://wire.com",
                    urlOffset = 0
                )
            )
        )

        assertSame(uiText, result)
    }
}
