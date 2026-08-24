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

import com.wire.android.feature.conversation.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageResourceProviderTest {

    @Test
    fun defaultsUseConversationFeatureResources() {
        val provider = MessageResourceProvider()

        assertEquals(R.string.member_name_deleted_label, provider.memberNameDeleted)
        assertEquals(R.string.member_name_you_label_lowercase, provider.memberNameYouLowercase)
        assertEquals(R.string.member_name_you_label_titlecase, provider.memberNameYouTitlecase)
        assertEquals(R.string.sent_a_message_with_content, provider.sentAMessageWithContent)
    }
}
