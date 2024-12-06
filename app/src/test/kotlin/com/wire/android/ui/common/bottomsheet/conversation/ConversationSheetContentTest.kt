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
package com.wire.android.ui.common.bottomsheet.conversation

import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModelTest.Companion.testGroup
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.TeamId
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

class ConversationSheetContentTest {

    @Test
    fun givenTitleIsEmptyAndTheGroupSizeIsOne_whenCallingIsTheGroupAbandoned_returnsTrue() = runTest {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))

        val givenConversationSheetContent = ConversationSheetContent(
            title = "",
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, false),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false
        )
        val givenParticipantsCount = 1

        assertEquals(true, givenConversationSheetContent.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenTitleIsEmptyAndTheGroupSizeIsGtOne_whenCallingIsTheGroupAbandoned_returnsFalse() = runTest {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))

        val givenConversationSheetContent = ConversationSheetContent(
            title = "",
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, false),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false
        )
        val givenParticipantsCount = 3

        assertEquals(false, givenConversationSheetContent.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenTitleIsNotEmptyAndTheGroupSizeIsOne_whenCallingIsTheGroupAbandoned_returnsFalse() = runTest {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))

        val givenConversationSheetContent = ConversationSheetContent(
            title = "notEmpty",
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, false),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false
        )
        val givenParticipantsCount = 3

        assertEquals(false, givenConversationSheetContent.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }
}
