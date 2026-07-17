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

package com.wire.android.ui.home.conversations.messages.item

import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPreviewMessageBodyTest {

    @Test
    fun givenStandalonePreviewedUrl_whenCheckingVisibility_thenMessageBodyIsHidden() {
        val url = "https://wire.com"
        val messageBody = MessageBody(UIText.DynamicString("  $url  "))
        val preview = MessageLinkPreview(url = url, urlOffset = 2)

        assertTrue(messageBody.shouldHideStandalonePreviewedUrl(preview))
    }

    @Test
    fun givenTextAroundPreviewedUrl_whenCheckingVisibility_thenMessageBodyIsShown() {
        val url = "https://wire.com"
        val messageBody = MessageBody(UIText.DynamicString("see $url now"))
        val preview = MessageLinkPreview(url = url, urlOffset = 4)

        assertFalse(messageBody.shouldHideStandalonePreviewedUrl(preview))
    }

    @Test
    fun givenPreviewOffsetDoesNotMatchMessageText_whenCheckingVisibility_thenMessageBodyIsShown() {
        val url = "https://wire.com"
        val messageBody = MessageBody(UIText.DynamicString(url))
        val preview = MessageLinkPreview(url = url, urlOffset = 1)

        assertFalse(messageBody.shouldHideStandalonePreviewedUrl(preview))
    }

    @Test
    fun givenNegativePreviewOffset_whenCheckingVisibility_thenMessageBodyIsShown() {
        val url = "https://wire.com"
        val messageBody = MessageBody(UIText.DynamicString(url))
        val preview = MessageLinkPreview(url = url, urlOffset = -1)

        assertFalse(messageBody.shouldHideStandalonePreviewedUrl(preview))
    }

    @Test
    fun givenPreviewOffsetPastTextLength_whenCheckingVisibility_thenMessageBodyIsShown() {
        val url = "https://wire.com"
        val messageBody = MessageBody(UIText.DynamicString(url))
        val preview = MessageLinkPreview(url = url, urlOffset = Int.MAX_VALUE)

        assertFalse(messageBody.shouldHideStandalonePreviewedUrl(preview))
    }

    @Test
    fun givenEmptyPreviewUrl_whenCheckingVisibility_thenMessageBodyIsShown() {
        val messageBody = MessageBody(UIText.DynamicString("https://wire.com"))
        val preview = MessageLinkPreview(url = "", urlOffset = 0)

        assertFalse(messageBody.shouldHideStandalonePreviewedUrl(preview))
    }
}
