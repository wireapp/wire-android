/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.wire.android.ui.home.conversations.model

import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UIMentionTest {
    @Test
    fun everyoneSurvivesMappingForSendAndEdit() {
        val mention = UIMention(start = 3, length = 4, userId = null, handler = "@all")

        val messageMention = mention.intoMessageMention()

        assertTrue(messageMention.isEveryone)
        assertFalse(messageMention.isSelfMention)
        assertEquals(mention, messageMention.toUiMention("Hi @all!"))
    }

    @Test
    fun userMentionKeepsItsTarget() {
        val userId = UserId("user", "example.com")
        val mention = UIMention(start = 0, length = 4, userId = userId, handler = "@Sam")

        assertFalse(mention.isEveryone)
        assertEquals(mention, mention.intoMessageMention().toUiMention("@Sam"))
    }

    @Test
    fun invalidRangesDoNotCrashDraftOrEditRestoration() {
        for ((start, length) in listOf(-1 to 4, 0 to 0, 4 to 1, Int.MAX_VALUE to 4, 0 to Int.MAX_VALUE)) {
            assertNull(MessageMention(start, length, null, false).toUiMention("@all"))
        }
    }
}
