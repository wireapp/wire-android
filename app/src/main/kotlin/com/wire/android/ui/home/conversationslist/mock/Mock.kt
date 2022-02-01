package com.wire.android.ui.home.conversationslist.mock

import androidx.compose.ui.graphics.Color
import com.wire.android.ui.home.conversationslist.model.Call
import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallInfo
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.Conversation
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.Mention
import com.wire.android.ui.home.conversationslist.model.MentionInfo
import com.wire.android.ui.home.conversationslist.model.MentionMessage
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.ui.home.conversationslist.model.UserInfo

val mockConversations1 = listOf(
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest,
                isLegalHold = true
            )
        )
    ),
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest,
                isLegalHold = true
            )
        )
    ),
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest,
                isLegalHold = true
            )
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
            )
        )
    ),
    Call(
        callInfo = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall),
        conversationType = ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.None,
                isLegalHold = true
            )
        )
    ),
    Mention(
        mentionInfo = MentionInfo(MentionMessage("Some mention message")),
        conversationType = ConversationType.GroupConversation(
            groupColorValue = 0xFF00FF00,
            groupName = "Some group name"
        ),
    )
)


val mockConversation = ConversationType.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some test value",
        membership = Membership.Guest,
        isLegalHold = true
    )
)

val mockGroupConversation = ConversationType.GroupConversation(
    groupColorValue = 0xFFFF0000,
    groupName = "Some group name"
)

val mockGeneralConversation = GeneralConversation(
    ConversationType.PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest,
            isLegalHold = true
        )
    )
)

val conversationMockData = mapOf(
    ConversationFolder("SOME TEST FOLDER") to mockConversations1,
    ConversationFolder("FOLDER NAME1") to mockConversations2,
    ConversationFolder("SOME OTHER FOLDER") to mockConversations1,
    ConversationFolder("SOME OTHER Folder1") to mockConversations2,
    ConversationFolder("THIS IS A TEST FOLDER") to mockConversations1,
    ConversationFolder(
        "THIS IS A TEST FOLDER WITH A VERY VERY VERY VERY" +
                " VERY VERY VERY VERY VERY VERY VERY " +
                "VERY VERY VERY VERY VERY LONG NAME"
    ) to mockConversations1
)

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
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
)

val mockAllMentionList = listOf(
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockGroupConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockGroupConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockGroupConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockGroupConversation),
)

val mockCallInfo1 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo2 = CallInfo(CallTime("Yesterday", "1:00 PM"), CallEvent.OutgoingCall)
val mockCallInfo3 = CallInfo(CallTime("Today", "2:34 PM"), CallEvent.MissedCall)
val mockCallInfo4 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo5 = CallInfo(CallTime("Today", "6:59 PM"), CallEvent.NoAnswerCall)

val mockMissedCalls = listOf(
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
)

val mockCallHistory = listOf(
    Call(mockCallInfo1, mockGroupConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockGroupConversation),
    Call(mockCallInfo2, mockGroupConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockGroupConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockGroupConversation),
    Call(mockCallInfo1, mockGroupConversation),
    Call(mockCallInfo5, mockConversation),
    Call(mockCallInfo3, mockGroupConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo5, mockConversation),
)
