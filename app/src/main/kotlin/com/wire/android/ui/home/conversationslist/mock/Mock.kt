package com.wire.android.ui.home.conversationslist.mock

import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallInfo
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.MentionInfo
import com.wire.android.ui.home.conversationslist.model.MentionMessage
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.ui.home.conversationslist.model.UserInfo
import com.wire.kalium.logic.data.conversation.ConversationId

val mockConversations1 = listOf(
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest,
                isLegalHold = true
            ),
            conversationsId = ConversationId("someId", "someDomain")
        )
    ),
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest,
                isLegalHold = true
            ),
            conversationsId = ConversationId("someId", "someDomain")
        )
    ),
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest,
                isLegalHold = true
            ),
            conversationsId = ConversationId("someId", "someDomain")
        )
    ),
)

val mockConversations2 = listOf(
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.External,
                isLegalHold = true
            ),
            conversationsId = ConversationId("someId", "someDomain")
        )
    ),
    ConversationMissedCall(
        callInfo = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall),
        conversationType = ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.None,
                isLegalHold = true
            ),
            conversationsId = ConversationId("someId", "someDomain")
        )
    ),
    ConversationUnreadMention(
        mentionInfo = MentionInfo(MentionMessage("Some mention message")),
        conversationType = ConversationType.GroupConversation(
            groupColorValue = 0xFF00FF00,
            groupName = "Some group name",
            conversationsId = ConversationId("someId", "someDomain")
        ),
    )
)

val mockConversation = ConversationType.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some test value",
        membership = Membership.Guest,
        isLegalHold = true
    ),
    conversationsId = ConversationId("someId", "someDomain")
)

val mockGroupConversation = ConversationType.GroupConversation(
    groupColorValue = 0xFFFF0000,
    groupName = "Some group name",
    conversationsId = ConversationId("someId", "someDomain")
)

val mockGeneralConversation = GeneralConversation(
    ConversationType.PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest,
            isLegalHold = true
        ),
        conversationsId = ConversationId("someId", "someDomain")
    )
)

val conversationMockData = mapOf(
    ConversationFolder("SOME TEST FOLDER") to mockConversations1,
    ConversationFolder("FOLDER NAME1") to mockConversations1,
    ConversationFolder("SOME OTHER FOLDER") to mockConversations1,
    ConversationFolder("SOME OTHER Folder1") to mockConversations1,
    ConversationFolder("THIS IS A TEST FOLDER") to mockConversations1,
    ConversationFolder(
        "THIS IS A TEST FOLDER WITH A VERY VERY VERY VERY" +
                " VERY VERY VERY VERY VERY VERY VERY " +
                "VERY VERY VERY VERY VERY LONG NAME"
    ) to mockConversations1
)

fun conversationMockData(conversations: List<GeneralConversation>) = mapOf(ConversationFolder("REAL CONVERSATIONS HERE") to conversations)

@Suppress("MagicNumber")
val newActivitiesMockData = listOf(
    NewActivity(EventType.MissedCall, mockGeneralConversation),
    NewActivity(EventType.UnreadMention, mockGeneralConversation),
    NewActivity(EventType.UnreadReply, mockGeneralConversation),
    NewActivity(EventType.UnreadMessage(2), mockGeneralConversation),
    NewActivity(EventType.UnreadMessage(1000000), mockGeneralConversation),
    NewActivity(EventType.UnreadMessage(0), mockGeneralConversation),
    NewActivity(EventType.UnreadMessage(50), mockGeneralConversation),
    NewActivity(EventType.UnreadMessage(99), mockGeneralConversation),
    NewActivity(EventType.UnreadMention, mockGeneralConversation),
    NewActivity(EventType.UnreadReply, mockGeneralConversation)
)

val mockShortMentionInfo = MentionInfo(mentionMessage = MentionMessage("Short message"))

val mockLongMentionInfo = MentionInfo(
    mentionMessage = MentionMessage(
        "THis is a very very very very very very very " +
                "very very very very very very very" +
                " very very very very very very very " +
                "very very very very very very very mention message"
    )
)

val mockUnreadMentionList = listOf(
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockConversation),
)

val mockAllMentionList = listOf(
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockGroupConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockGroupConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockGroupConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockShortMentionInfo, mockConversation),
    ConversationUnreadMention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
)

val mockCallInfo1 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo2 = CallInfo(CallTime("Yesterday", "1:00 PM"), CallEvent.OutgoingCall)
val mockCallInfo3 = CallInfo(CallTime("Today", "2:34 PM"), CallEvent.MissedCall)
val mockCallInfo4 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo5 = CallInfo(CallTime("Today", "6:59 PM"), CallEvent.NoAnswerCall)

val mockMissedCalls = listOf(
    ConversationMissedCall(mockCallInfo1, mockConversation),
    ConversationMissedCall(mockCallInfo2, mockConversation),
    ConversationMissedCall(mockCallInfo3, mockConversation),
)

val mockCallHistory = listOf(
    ConversationMissedCall(mockCallInfo1, mockGroupConversation),
    ConversationMissedCall(mockCallInfo2, mockConversation),
    ConversationMissedCall(mockCallInfo3, mockConversation),
    ConversationMissedCall(mockCallInfo1, mockGroupConversation),
    ConversationMissedCall(mockCallInfo2, mockGroupConversation),
    ConversationMissedCall(mockCallInfo3, mockConversation),
    ConversationMissedCall(mockCallInfo4, mockGroupConversation),
    ConversationMissedCall(mockCallInfo2, mockConversation),
    ConversationMissedCall(mockCallInfo3, mockGroupConversation),
    ConversationMissedCall(mockCallInfo1, mockGroupConversation),
    ConversationMissedCall(mockCallInfo5, mockConversation),
    ConversationMissedCall(mockCallInfo3, mockGroupConversation),
    ConversationMissedCall(mockCallInfo1, mockConversation),
    ConversationMissedCall(mockCallInfo2, mockConversation),
    ConversationMissedCall(mockCallInfo3, mockConversation),
    ConversationMissedCall(mockCallInfo4, mockConversation),
    ConversationMissedCall(mockCallInfo2, mockConversation),
    ConversationMissedCall(mockCallInfo5, mockConversation),
)
