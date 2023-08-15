/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.mapper

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageEditStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent.TextMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.android.util.uiMessageDateTime
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageMapperTest {

    @Test
    fun givenMessagesList_whenGettingMemberIdList_thenReturnCorrectList() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val clientMessageAuthor = UserId("client-id", "client-domain")
        val serverMessageAuthor = UserId("server-id", "server-domain")
        val messages = listOf(
            TestMessage.TEXT_MESSAGE.copy(senderUserId = clientMessageAuthor),
            TestMessage.MEMBER_REMOVED_MESSAGE.copy(
                senderUserId = serverMessageAuthor,
                content = MessageContent.MemberChange.Removed(listOf(serverMessageAuthor))
            )
        )
        val expected = listOf(clientMessageAuthor, serverMessageAuthor)
        // When
        val list = mapper.memberIdList(messages)
        // Then
        list shouldBeEqualTo expected
    }

    @Test
    @Suppress("LongMethod")
    fun givenMessageList_whenMappingToUIMessages_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val serverDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

        val now = serverDateFormatter.format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -1)
        val yesterday = serverDateFormatter.format(calendar.time)

        val (arrangement, mapper) = Arrangement().arrange()
        val userId1 = UserId("user-id1", "user-domain")
        val userId2 = UserId("user-id2", "user-domain")
        val message1 = arrangement.testMessage(senderUserId = userId1, date = now)
        val message2 = arrangement.testMessage(senderUserId = userId2, status = Message.Status.Failed, date = yesterday)
        val message3 = arrangement.testMessage(senderUserId = userId1, editStatus = Message.EditStatus.Edited(now), date = now)
        val message4 = arrangement.testMessage(senderUserId = userId1, visibility = Message.Visibility.DELETED, date = now)
        val message5 = arrangement.testMessage(senderUserId = userId1, date = now).failureToDecrypt(false)
        val message6 = arrangement.testMessage(senderUserId = userId1, date = now).failureToDecrypt(true)

        val member1 = TestUser.MEMBER_SELF.copy(TestUser.SELF_USER.copy(id = userId1))
        val member2 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId2))
        val members = listOf(member1.user, member2.user)
        // When
        val uiMessage1 = mapper.toUIMessage(members, message1)
        val uiMessage2 = mapper.toUIMessage(members, message2)
        val uiMessage3 = mapper.toUIMessage(members, message3)
        val uiMessage4 = mapper.toUIMessage(members, message4)
        val uiMessage5 = mapper.toUIMessage(members, message5)
        val uiMessage6 = mapper.toUIMessage(members, message6)

        // Then
        assert(
            checkMessageData(
                uiMessage = uiMessage1,
                time = message1.date.uiMessageDateTime()
            )
        )
        assert(
            checkMessageData(
                uiMessage = uiMessage2,
                time = message2.date.uiMessageDateTime(),
                source = MessageSource.OtherUser,
                membership = Membership.Guest,
                status = MessageStatus(
                    flowStatus = MessageFlowStatus.Failure.Send.Locally(false),
                    expirationStatus = ExpirationStatus.NotExpirable
                )
            )
        )
        assert(
            checkMessageData(
                uiMessage = uiMessage3,
                time = message3.date.uiMessageDateTime(),
                status = MessageStatus(
                    flowStatus = MessageFlowStatus.Sent,
                    editStatus = MessageEditStatus.Edited(now.uiMessageDateTime() ?: ""),
                    expirationStatus = ExpirationStatus.NotExpirable
                )
            )
        )
        assert(
            checkMessageData(
                uiMessage = uiMessage4,
                time = message4.date.uiMessageDateTime(),
                status = MessageStatus(
                    flowStatus = MessageFlowStatus.Sent,
                    isDeleted = true,
                    expirationStatus = ExpirationStatus.NotExpirable
                )
            )
        )

        assert(
            checkMessageData(
                uiMessage = uiMessage5,
                time = message5.date.uiMessageDateTime(),
                status = MessageStatus(
                    flowStatus = MessageFlowStatus.Failure.Decryption(false),
                    isDeleted = false,
                    expirationStatus = ExpirationStatus.NotExpirable
                )
            )
        )

        assert(
            checkMessageData(
                uiMessage = uiMessage6,
                time = message6.date.uiMessageDateTime(),
                status = MessageStatus(
                    flowStatus = MessageFlowStatus.Failure.Decryption(true),
                    isDeleted = false,
                    expirationStatus = ExpirationStatus.NotExpirable
                )
            )
        )
    }

    @Test
    fun givenMessageHasReadStatus_whenMappingToUiMessage_theCorrectValueShouldBeReturned() = runTest {
        // given
        val (arrangement, mapper) = Arrangement().arrange()

        val userId1 = UserId("user-id1", "user-domain")
        val userId2 = UserId("user-id2", "user-domain")
        val member1 = TestUser.MEMBER_SELF.copy(TestUser.SELF_USER.copy(id = userId1))
        val member2 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId2))
        val members = listOf(member1.user, member2.user)

        val message = arrangement.testMessage(
            status = Message.Status.Read(10)
        )

        // when
        val result = mapper.toUIMessage(members, message)?.header?.messageStatus?.flowStatus

        // then
        assert(result != null)
        assert(result!! is MessageFlowStatus.Read)
        assert((result as MessageFlowStatus.Read).count == 10L)
    }

    private fun checkMessageData(
        uiMessage: UIMessage?,
        time: String?,
        source: MessageSource = MessageSource.Self,
        membership: Membership = Membership.None,
        status: MessageStatus = MessageStatus(
            flowStatus = MessageFlowStatus.Sent,
            expirationStatus = ExpirationStatus.NotExpirable
        )
    ): Boolean {
        return (uiMessage?.source == source && uiMessage.header.membership == membership
                && uiMessage.header.messageTime.formattedDate == time
                && uiMessage.header.messageStatus.flowStatus == status.flowStatus
                && uiMessage.header.messageStatus.isDeleted == status.isDeleted
                && uiMessage.header.messageStatus.editStatus == status.editStatus
                && uiMessage.header.messageStatus.expirationStatus == status.expirationStatus
                )
    }

    private class Arrangement {
        @MockK
        lateinit var userTypeMapper: UserTypeMapper

        @MockK
        lateinit var messageContentMapper: MessageContentMapper

        @MockK
        lateinit var isoFormatter: ISOFormatter

        @MockK
        private lateinit var wireSessionImageLoader: WireSessionImageLoader

        private val messageMapper by lazy {
            MessageMapper(userTypeMapper, messageContentMapper, isoFormatter, wireSessionImageLoader)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { userTypeMapper.toMembership(any()) } returns Membership.Guest
            coEvery { messageContentMapper.fromMessage(any(), any()) } returns TextMessage(
                MessageBody(UIText.DynamicString("some message text"))
            )
            every { isoFormatter.fromISO8601ToTimeFormat(any()) } answers { firstArg<String>().uiMessageDateTime() ?: "" }
        }

        fun arrange() = this to messageMapper

        fun testMessage(
            senderUserId: UserId = UserId("someValue", "someDomain"),
            status: Message.Status = Message.Status.Sent,
            visibility: Message.Visibility = Message.Visibility.VISIBLE,
            editStatus: Message.EditStatus = Message.EditStatus.NotEdited,
            date: String = "2016-09-18T17:34:02.666Z"
        ): Message.Regular = TestMessage.TEXT_MESSAGE.copy(
            senderUserId = senderUserId,
            status = status,
            date = date,
            visibility = visibility,
            editStatus = editStatus
        )
    }
}

private fun Message.Regular.failureToDecrypt(isDecryptionResolved: Boolean) =
    this
        .copy(
            content = MessageContent.FailedDecryption(
                encodedData = null,
                senderUserId = this.senderUserId,
                isDecryptionResolved = isDecryptionResolved
            )
        )
