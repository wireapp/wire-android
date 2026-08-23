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
package com.wire.android.ui.home.conversations.usecase

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.DeliveryStatus
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GetUsersForMessageUseCaseTest {

    @Test
    fun givenCompleteDeliveryMessageWithoutSender_whenInvoke_thenReturnEmptyListWithoutObservingUsers() = runTest {
        val (arrangement, useCase) = Arrangement().arrange()

        val result = useCase(regularMessage())

        assertEquals(emptyList<User>(), result)
        coVerify(exactly = 0) { arrangement.observeMemberDetailsByIds(any()) }
    }

    @Test
    fun givenMemberChangeMessage_whenInvoke_thenObserveAndReturnChangedMembers() = runTest {
        val relatedUsers = relatedUsers()
        val relatedUserIds = relatedUsers.map { it.id }
        val message = systemMessage(MessageContent.MemberChange.Added(relatedUserIds))

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(relatedUsers)
            .arrange()

        val result = useCase(message)

        assertEquals(relatedUsers, result)
        coVerify(exactly = 1) { arrangement.observeMemberDetailsByIds(relatedUserIds) }
    }

    @Test
    fun givenLegalHoldForMembersMessage_whenInvoke_thenObserveAndReturnAffectedMembers() = runTest {
        val relatedUsers = relatedUsers()
        val relatedUserIds = relatedUsers.map { it.id }
        val message = systemMessage(MessageContent.LegalHold.ForMembers.Enabled(relatedUserIds))

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(relatedUsers)
            .arrange()

        val result = useCase(message)

        assertEquals(relatedUsers, result)
        coVerify(exactly = 1) { arrangement.observeMemberDetailsByIds(relatedUserIds) }
    }

    @Test
    fun givenDisabledLegalHoldForMembersMessage_whenInvoke_thenObserveAndReturnAffectedMembers() = runTest {
        val relatedUsers = relatedUsers()
        val relatedUserIds = relatedUsers.map { it.id }
        val message = systemMessage(MessageContent.LegalHold.ForMembers.Disabled(relatedUserIds))

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(relatedUsers)
            .arrange()

        val result = useCase(message)

        assertEquals(relatedUsers, result)
        coVerify(exactly = 1) { arrangement.observeMemberDetailsByIds(relatedUserIds) }
    }

    @Test
    fun givenMemberChangeWithoutMembers_whenInvoke_thenReturnOnlySenderWithoutObservingUsers() = runTest {
        val sender = TestUser.OTHER_USER
        val message = systemMessage(MessageContent.MemberChange.Added(emptyList()), sender)

        val (arrangement, useCase) = Arrangement().arrange()

        val result = useCase(message)

        assertEquals(listOf(sender), result)
        coVerify(exactly = 0) { arrangement.observeMemberDetailsByIds(any()) }
    }

    @Test
    fun givenPartialDeliveryWithDuplicateIds_whenInvoke_thenPreserveFailureOrderAndRemoveDuplicates() = runTest {
        val firstUser = relatedUser("first")
        val secondUser = relatedUser("second")
        val thirdUser = relatedUser("third")
        val expectedUserIds = listOf(firstUser.id, secondUser.id, thirdUser.id)
        val expectedUsers = listOf(firstUser, secondUser, thirdUser)
        val message = regularMessage(
            deliveryStatus = DeliveryStatus.PartialDelivery(
                recipientsFailedWithNoClients = listOf(secondUser.id, thirdUser.id, firstUser.id),
                recipientsFailedDelivery = listOf(firstUser.id, secondUser.id),
            ),
        )

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(expectedUsers)
            .arrange()

        val result = useCase(message)

        assertEquals(expectedUsers, result)
        coVerify(exactly = 1) { arrangement.observeMemberDetailsByIds(expectedUserIds) }
    }

    @Test
    fun givenSenderAndRelatedUsers_whenInvoke_thenReturnSenderFirst() = runTest {
        val sender = TestUser.OTHER_USER
        val relatedUser = relatedUser("related")
        val message = systemMessage(
            content = MessageContent.MemberChange.Removed(listOf(relatedUser.id)),
            sender = sender,
        )

        val (arrangement, useCase) = Arrangement()
            .withMemberDetails(listOf(relatedUser))
            .arrange()

        val result = useCase(message)

        assertEquals(listOf(sender, relatedUser), result)
        coVerify(exactly = 1) { arrangement.observeMemberDetailsByIds(listOf(relatedUser.id)) }
    }

    @Test
    fun givenSystemMessageWithoutRelatedUsers_whenInvoke_thenReturnOnlySenderWithoutObservingUsers() = runTest {
        val sender = TestUser.OTHER_USER
        val message = systemMessage(MessageContent.MissedCall, sender)

        val (arrangement, useCase) = Arrangement().arrange()

        val result = useCase(message)

        assertEquals(listOf(sender), result)
        coVerify(exactly = 0) { arrangement.observeMemberDetailsByIds(any()) }
    }

    @Test
    fun givenSignalingMessage_whenInvoke_thenReturnEmptyListWithoutObservingUsers() = runTest {
        val (arrangement, useCase) = Arrangement().arrange()

        val result = useCase(signalingMessage())

        assertEquals(emptyList<User>(), result)
        coVerify(exactly = 0) { arrangement.observeMemberDetailsByIds(any()) }
    }

    private fun regularMessage(
        deliveryStatus: DeliveryStatus = DeliveryStatus.CompleteDelivery,
        sender: User? = null,
    ) = Message.Regular(
        id = "message-id",
        content = MessageContent.Text("message"),
        conversationId = conversationId,
        date = messageDate,
        senderUserId = sender?.id ?: defaultSenderId,
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        editStatus = Message.EditStatus.NotEdited,
        sender = sender,
        isSelfMessage = false,
        deliveryStatus = deliveryStatus,
    )

    private fun systemMessage(
        content: MessageContent.System,
        sender: User? = null,
    ) = Message.System(
        id = "message-id",
        content = content,
        conversationId = conversationId,
        date = messageDate,
        senderUserId = sender?.id ?: defaultSenderId,
        status = Message.Status.Sent,
        expirationData = null,
        sender = sender,
    )

    private fun signalingMessage() = Message.Signaling(
        id = "message-id",
        content = MessageContent.ClientAction,
        conversationId = conversationId,
        date = messageDate,
        senderUserId = defaultSenderId,
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        isSelfMessage = false,
        expirationData = null,
    )

    private fun relatedUsers() = listOf(relatedUser("first"), relatedUser("second"))

    private fun relatedUser(id: String) = TestUser.OTHER_USER.copy(id = UserId("$id-user-id", "domain"))

    private class Arrangement {

        @MockK
        lateinit var observeMemberDetailsByIds: ObserveUserListByIdUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withMemberDetails(userList: List<User>) = apply {
            coEvery { observeMemberDetailsByIds(any()) } returns flowOf(userList)
        }

        fun arrange() = this to GetUsersForMessageUseCase(
            observeMemberDetailsByIds,
        )
    }

    private companion object {
        val conversationId = ConversationId("conversation-id", "domain")
        val defaultSenderId = UserId("sender-id", "domain")
        val messageDate = Instant.parse("2022-03-30T15:36:00.000Z")
    }
}
