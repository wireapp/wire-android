/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.wire.android.ui.home.messagecomposer.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageCompositionTest {
    private val everyone = UIMention(0, 4, null, "@all")
    private val member = UIMention(5, 4, UserId("member", "example.com"), "@all")

    @Test
    fun `deleting everyone before an identical member preserves the member target`() {
        assertEquals(listOf(member.copy(start = 0)), reconcile(listOf(everyone, member)) { delete(0, 5) })
    }

    @Test
    fun `deleting everyone before identical plain text leaves no mention`() {
        assertEquals(emptyList<UIMention>(), reconcile(listOf(everyone)) { delete(0, 5) })
    }

    @Test
    fun `deleting a member before everyone preserves only everyone`() {
        assertEquals(
            listOf(everyone),
            reconcile(listOf(member.copy(start = 0), everyone.copy(start = 5))) { delete(0, 5) }
        )
    }

    @Test
    fun `inserting before a mention shifts its UTF16 range`() {
        assertEquals(listOf(everyone.copy(start = 3)), reconcile(listOf(everyone)) { insert(0, "👋 ") })
    }

    @Test
    fun `editing inside a token drops its target`() {
        assertEquals(emptyList<UIMention>(), reconcile(listOf(everyone)) { insert(2, "x") })
    }

    @Test
    fun `replacing a token with identical text does not transfer its target`() {
        assertEquals(emptyList<UIMention>(), reconcile(listOf(everyone)) { replace(0, 4, "@all") })
    }

    @Test
    fun `independent formatting insertions preserve both identical targets`() {
        assertEquals(
            listOf(everyone.copy(start = 2), member.copy(start = 9)),
            reconcile(listOf(everyone, member)) {
                insert(4, "**")
                insert(0, "**")
            }
        )
    }

    private fun reconcile(mentions: List<UIMention>, edit: TextFieldBuffer.() -> Unit): List<UIMention> {
        val composition = MessageComposition(ConversationId("group", "example.com"), selectedMentions = mentions)
        var result = mentions
        TextFieldState("@all @all ").edit {
            edit()
            result = composition.getSelectedMentions(this)
        }
        return result
    }
}
