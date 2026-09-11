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

package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestMessage
import com.wire.android.model.BadgeEventType
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.message.MessagePreviewContent
import com.wire.kalium.logic.data.message.UnreadEventType
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ConversationMapperTest {

    @Test
    fun givenActiveConversationWithAdminlessDeletion_whenMapping_thenReminderOverridesDraftAndBadgeIsPreserved() {
        val item = conversation(archived = false).toConversationItem(
            userTypeMapper = mockk(relaxed = true),
            uiTextResolver = uiTextResolver,
            selfUserTeamId = null,
        ) as ConversationItem.Group.Regular

        val preview = assertInstanceOf(UILastMessageContent.TextMessage::class.java, item.lastMessageContent)
        assertEquals(R.string.last_message_adminless_delete_reminder, (preview.messageBody.message as UIText.StringResource).resId)
        assertEquals(BadgeEventType.UnreadMention, item.badgeEventType)
    }

    @Test
    fun givenArchivedConversationWithAdminlessDeletion_whenMapping_thenReminderPreviewIsNotUsed() {
        val item = conversation(archived = true).toConversationItem(
            userTypeMapper = mockk(relaxed = true),
            uiTextResolver = uiTextResolver,
            selfUserTeamId = null,
        ) as ConversationItem.Group.Regular

        val preview = assertInstanceOf(UILastMessageContent.TextMessage::class.java, item.lastMessageContent)
        val message = preview.messageBody.message as UIText.PluralResource
        assertNotEquals(R.string.last_message_adminless_delete_reminder, message.resId)
        assertEquals(BadgeEventType.UnreadMention, item.badgeEventType)
    }

    private fun conversation(archived: Boolean) = ConversationDetailsWithEvents(
        conversationDetails = TestConversationDetails.GROUP.copy(
            conversation = TestConversationDetails.GROUP.conversation.copy(
                archived = archived,
                adminlessGroupDeletionTimestamp = Instant.parse("2026-08-30T12:00:00Z"),
            )
        ),
        unreadEventCount = mapOf(UnreadEventType.MENTION to 2),
        lastMessage = TestMessage.PREVIEW.copy(content = MessagePreviewContent.Draft("draft")),
    )

    private val uiTextResolver = object : UiTextResolver {
        override fun resolve(text: UIText): String = when (text) {
            is UIText.DynamicString -> text.value
            is UIText.StringResource -> "res_${text.resId}"
            is UIText.PluralResource -> "plural_${text.resId}_${text.count}"
        }

        override fun localeTag(): String = "test-locale"
    }
}
