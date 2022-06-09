package com.wire.android.mapper

import android.content.res.Resources
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent.TextMessage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
                content = MessageContent.MemberChange.Removed(listOf(Member(serverMessageAuthor)))
            )
        )
        val expected = listOf(clientMessageAuthor, serverMessageAuthor)
        // When
        val list = mapper.memberIdList(messages)
        // Then
        list shouldBeEqualTo expected
    }

    @Test
    fun givenMessageList_whenMappingToUIMessages_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val userId1 = UserId("user-id1", "user-domain")
        val userId2 = UserId("user-id2", "user-domain")
        val message1 = TestMessage.TEXT_MESSAGE.copy(senderUserId = userId1, status = Message.Status.READ, date = "date1")
        val message2 = TestMessage.TEXT_MESSAGE.copy(senderUserId = userId2, status = Message.Status.FAILED, date = "date2")
        val messages = listOf(message1, message2)
        val member1 = TestUser.MEMBER_SELF.copy(TestUser.SELF_USER.copy(id = userId1))
        val member2 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId2))
        val members = listOf(member1, member2)
        // When
        val result = mapper.toUIMessages(messages, members)
        // Then
        assert(
            result.size == 2
                    && result[0].messageSource == MessageSource.Self
                    && result[0].messageHeader.membership == Membership.None
                    && result[0].messageHeader.time == message1.date
                    && result[0].messageHeader.messageStatus == MessageStatus.Untouched
                    && result[1].messageSource == MessageSource.OtherUser
                    && result[1].messageHeader.membership == Membership.Guest
                    && result[1].messageHeader.time == message2.date
                    && result[1].messageHeader.messageStatus == MessageStatus.SendFailure
        )
    }

    private class Arrangement {
        @MockK
        lateinit var userTypeMapper: UserTypeMapper

        @MockK
        lateinit var messageContentMapper: MessageContentMapper

        @MockK
        lateinit var resources: Resources

        private val messageMapper by lazy { MessageMapper(TestDispatcherProvider(), userTypeMapper, messageContentMapper) }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { userTypeMapper.toMembership(any()) } returns Membership.Guest
            coEvery { messageContentMapper.fromMessage(any(), any()) } returns TextMessage(
                MessageBody(UIText.DynamicString("some message text"))
            )
            coEvery { messageContentMapper.toSystemMessageMemberName(any(), any()) } returns UIText.DynamicString("username")
        }

        fun arrange() = this to messageMapper
    }
}
