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
package com.wire.android.ui.home.conversations.sendmessage

import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComposerLinkPreviewTargetDetectorTest {

    @Test
    fun `given text with link when detecting then returns first link and position`() {
        val result = ComposerLinkPreviewTargetDetector.detect("hello https://wire.com world")

        assertEquals(
            ComposerLinkPreviewTarget(
                url = "https://wire.com",
                position = 6
            ),
            result
        )
    }

    @Test
    fun `given markdown link when detecting then returns null`() {
        val result = ComposerLinkPreviewTargetDetector.detect("[Wire](https://wire.com)")

        assertEquals(null, result)
    }

    @Test
    fun `given mentioned url when detecting then skips excluded mention range`() {
        val text = "@wire.com hello"
        val result = ComposerLinkPreviewTargetDetector.detect(
            text = text,
            mentions = listOf(
                MessageMention(
                    start = 0,
                    length = 9,
                    userId = UserId("user", "wire.com"),
                    isSelfMention = false
                )
            )
        )

        assertEquals(null, result)
    }
}
