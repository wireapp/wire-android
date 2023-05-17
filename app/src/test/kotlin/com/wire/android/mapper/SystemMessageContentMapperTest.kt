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

import android.content.res.Resources
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.UIMessageContent.SystemMessage
import com.wire.android.ui.home.conversations.name
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SystemMessageContentMapperTest {

    @Test
    fun givenConversationTimerEnforced_whenMappingToSystemMessage_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val content = MessageContent.ConversationMessageTimerChanged(10000)

        // When
        val uiContent = mapper.mapMessage(TestMessage.SYSTEM_MESSAGE.copy(content = content), listOf())

        // Then
        assertTrue(uiContent is SystemMessage.ConversationMessageTimerActivated)
    }

    @Test
    fun givenConversationTimerDisabled_whenMappingToSystemMessage_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val content = MessageContent.ConversationMessageTimerChanged(null)

        // When
        val uiContent = mapper.mapMessage(TestMessage.SYSTEM_MESSAGE.copy(content = content), listOf())

        // Then
        assertTrue(uiContent is SystemMessage.ConversationMessageTimerDeactivated)
    }
    @Test
    fun givenMemberDetails_whenMappingToSystemMessageMemberName_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val selfMemberDetails = TestUser.MEMBER_SELF
        val deletedMemberDetails = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(name = null))
        val otherMemberDetails = TestUser.MEMBER_OTHER
        // When
        val selfName = mapper.mapMemberName(selfMemberDetails.user, SystemMessageContentMapper.SelfNameType.NameOrDeleted)
        val selfResLower = mapper.mapMemberName(selfMemberDetails.user, SystemMessageContentMapper.SelfNameType.ResourceLowercase)
        val selfResTitle = mapper.mapMemberName(selfMemberDetails.user, SystemMessageContentMapper.SelfNameType.ResourceTitleCase)
        val deleted = mapper.mapMemberName(deletedMemberDetails.user, SystemMessageContentMapper.SelfNameType.NameOrDeleted)
        val otherName = mapper.mapMemberName(otherMemberDetails.user, SystemMessageContentMapper.SelfNameType.NameOrDeleted)
        // Then
        assertTrue(selfName is UIText.DynamicString && selfName.value == selfMemberDetails.name)
        assertTrue(
            selfResLower is UIText.StringResource
                    && selfResLower.resId == arrangement.messageResourceProvider.memberNameYouLowercase
        )
        assertTrue(
            selfResTitle is UIText.StringResource
                    && selfResTitle.resId == arrangement.messageResourceProvider.memberNameYouTitlecase
        )
        assertTrue(deleted is UIText.StringResource && deleted.resId == arrangement.messageResourceProvider.memberNameDeleted)
        assertTrue(otherName is UIText.DynamicString && otherName.value == otherMemberDetails.name)
    }

    @Test
    fun givenServerContent_whenMappingToUIMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val userId1 = UserId("user-id1", "user-domain")
        val userId2 = UserId("user-id2", "user-domain")
        val userId3 = UserId("user-id3", "user-domain")
        val contentLeft = MessageContent.MemberChange.Removed(listOf(userId1))
        val contentRemoved = MessageContent.MemberChange.Removed(listOf(userId2))
        val contentAdded = MessageContent.MemberChange.Added(listOf(userId2, userId3))
        val contentAddedSelf = MessageContent.MemberChange.Added(listOf(userId1))
        val member1 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId1))
        val member2 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId2))
        val member3 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId3))
        val missedCallMessage = TestMessage.MISSED_CALL_MESSAGE
        val selfCaller = MemberDetails(TestUser.SELF_USER.copy(id = missedCallMessage.senderUserId), Member.Role.Admin)
        val otherCallerInfo = (member1.user as OtherUser).copy(id = missedCallMessage.senderUserId)
        val otherCaller = member1.copy(user = otherCallerInfo)
        // When
        val resultContentLeft = mapper.mapMemberChangeMessage(contentLeft, userId1, listOf(member1.user))
        val resultContentRemoved = mapper.mapMemberChangeMessage(contentRemoved, userId1, listOf(member1.user, member2.user))
        val resultContentAdded = mapper.mapMemberChangeMessage(contentAdded, userId1, listOf(member1.user, member2.user, member3.user))
        val resultContentAddedSelf = mapper.mapMemberChangeMessage(contentAddedSelf, userId1, listOf(member1.user))
        val resultMyMissedCall = mapper.mapMessage(missedCallMessage, listOf(selfCaller.user))
        val resultOtherMissedCall = mapper.mapMessage(missedCallMessage, listOf(otherCaller.user))
        // Then
        assertTrue(
            resultContentLeft is SystemMessage.MemberLeft &&
                    resultContentLeft.author.asString(arrangement.resources) == member1.name
        )
        assertTrue(
            resultContentRemoved is SystemMessage.MemberRemoved &&
                    resultContentRemoved.author.asString(arrangement.resources) == member1.name &&
                    resultContentRemoved.memberNames.size == 1 &&
                    resultContentRemoved.memberNames[0].asString(arrangement.resources) == member2.name

        )
        assertTrue(
            resultContentAdded is SystemMessage.MemberAdded &&
                    resultContentAdded.author.asString(arrangement.resources) == member1.name &&
                    resultContentAdded.memberNames.size == 2 &&
                    resultContentAdded.memberNames[0].asString(arrangement.resources) == member2.name &&
                    resultContentAdded.memberNames[1].asString(arrangement.resources) == member3.name
        )
        assertTrue(
            resultContentAddedSelf is SystemMessage.MemberJoined &&
                    resultContentAddedSelf.author.asString(arrangement.resources) == member1.name
        )
        assertTrue(
            resultOtherMissedCall is SystemMessage.MissedCall &&
                    resultOtherMissedCall.author.asString(arrangement.resources) == TestUser.OTHER_USER.name
        )
        assertTrue(
            resultMyMissedCall is SystemMessage.MissedCall &&
                    (resultMyMissedCall.author as UIText.StringResource).resId == arrangement.messageResourceProvider.memberNameYouTitlecase
        )
    }

    private class Arrangement {

        @MockK
        lateinit var messageResourceProvider: MessageResourceProvider

        @MockK
        lateinit var resources: Resources

        private val messageContentMapper by lazy {
            SystemMessageContentMapper(messageResourceProvider)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { messageResourceProvider.memberNameDeleted } returns 10584735
            every { messageResourceProvider.memberNameYouLowercase } returns 24153498
            every { messageResourceProvider.memberNameYouTitlecase } returns 38946214
            every { messageResourceProvider.sentAMessageWithContent } returns 45407124
        }

        fun arrange() = this to messageContentMapper
    }
}
