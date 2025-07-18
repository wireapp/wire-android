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

import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.GroupDetailsViewModelTest.Companion.testGroup
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.TeamId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationOptionsDataTest {

    @Test
    fun givenPrivateConversationWithoutBlockedAndNotDeletedUser_whenCanDeleteUserIsInvoked_thenReturnsTrue() = runTest {
        // given
        val conversationSheetContent = createDataPrivate(blockingState = BlockingState.NOT_BLOCKED, isUserDeleted = false)

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertTrue(canBlockUser)
    }

    @Test
    fun givenGroupConversation_whenCanDeleteUserIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent = createDataGroup("")

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertFalse(canBlockUser)
    }

    @Test
    fun givenPrivateConversationWithBlockedAndNotDeletedUser_whenCanDeleteUserIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent = createDataPrivate(blockingState = BlockingState.BLOCKED, isUserDeleted = false)

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertFalse(canBlockUser)
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndDeletedUser_whenCanDeleteUserIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent = createDataPrivate(blockingState = BlockingState.NOT_BLOCKED, isUserDeleted = true)

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertFalse(canBlockUser)
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndNotDeletedUser_whenCanEditNotificationsIsInvoked_thenReturnsTrue() = runTest {
        // given
        val conversationSheetContent = createDataPrivate(blockingState = BlockingState.NOT_BLOCKED, isUserDeleted = false)

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertTrue(canEditNotifications)
    }

    @Test
    fun givenGroupConversation_whenCanEditNotificationsIsInvoked_thenReturnsTrue() = runTest {
        // given
        val conversationSheetContent = createDataGroup("")

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertTrue(canEditNotifications)
    }

    @Test
    fun givenPrivateConversationWithBlockedAndNotDeletedUser_whenCanEditNotificationsIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent = createDataPrivate(blockingState = BlockingState.BLOCKED, isUserDeleted = false)

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertFalse(canEditNotifications)
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndDeletedUser_whenCanEditNotificationsIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent = createDataPrivate(blockingState = BlockingState.BLOCKED, isUserDeleted = false)

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertFalse(canEditNotifications)
    }

    @Test
    fun givenGroupConversation_whenMemberOfTheConversation_thenDeleteConversationLocallyIsNotVisible() = runTest {
        // given
        val conversationSheetContent = createDataGroup("Title")

        // when
        val canDeleteGroupLocally = conversationSheetContent.canDeleteGroupLocally()

        // then
        assertFalse(canDeleteGroupLocally)
    }

    @Test
    fun givenGroupConversation_whenNotMemberOfTheConversation_thenDeleteConversationLocallyIsVisible() = runTest {
        // given
        val conversationSheetContent = createDataGroup(title = "Title", selfRole = null)

        // when
        val canDeleteGroupLocally = conversationSheetContent.canDeleteGroupLocally()

        // then
        assertTrue(canDeleteGroupLocally)
    }

    @Test
    fun givenGroupConversation_whenNotMemberOfTheConversationAndDeletionRunning_thenDeleteConversationLocallyIsNotVisible() = runTest {
        // given
        val conversationSheetContent = createDataGroup(title = "Title", selfRole = null, conversationDeletionLocallyRunning = true)

        // when
        val canDeleteGroupLocally = conversationSheetContent.canDeleteGroupLocally()

        // then
        assertFalse(canDeleteGroupLocally)
    }

    private fun createDataPrivate(
        blockingState: BlockingState = BlockingState.NOT_BLOCKED,
        isUserDeleted: Boolean = false,
    ): ConversationOptionsData {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        return ConversationOptionsData(
            title = UIText.DynamicString("notEmpty"),
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Private(
                avatarAsset = null,
                userId = TestUser.USER_ID,
                blockingState = blockingState,
                isUserDeleted = isUserDeleted
            ),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false,
            isDeletingConversationLocallyRunning = false,
            folder = null
        )
    }

    private fun createDataGroup(
        title: String = "Conversation Name",
        selfRole: Conversation.Member.Role? = Conversation.Member.Role.Member,
        conversationDeletionLocallyRunning: Boolean = false,
    ): ConversationOptionsData {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        return ConversationOptionsData(
            title = UIText.DynamicString(title),
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group.Regular(details.conversation.id, false),
            selfRole = selfRole,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false,
            isDeletingConversationLocallyRunning = conversationDeletionLocallyRunning,
            folder = null
        )
    }
}
