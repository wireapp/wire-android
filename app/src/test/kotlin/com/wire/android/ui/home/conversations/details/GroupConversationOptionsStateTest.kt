/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details

import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupConversationOptionsStateTest {

    @Test
    fun givenTitleIsEmptyAndTheGroupSizeIsOne_whenCallingIsTheGroupAbandoned_returnsTrue() = runTest {
        val givenData = createData(title = "")
        val givenParticipantsCount = 1

        assertEquals(true, givenData.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenTitleIsEmptyAndTheGroupSizeIsGtOne_whenCallingIsTheGroupAbandoned_returnsFalse() = runTest {
        val givenData = createData(title = "")
        val givenParticipantsCount = 3

        assertEquals(false, givenData.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenTitleIsNotEmptyAndTheGroupSizeIsOne_whenCallingIsTheGroupAbandoned_returnsFalse() = runTest {
        val givenData = createData(title = "non-empty")
        val givenParticipantsCount = 3

        assertEquals(false, givenData.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    private fun createData(title: String = "") = GroupConversationOptionsState(
        conversationId = ConversationId("id", "domain"),
        groupName = title,
    )
}
