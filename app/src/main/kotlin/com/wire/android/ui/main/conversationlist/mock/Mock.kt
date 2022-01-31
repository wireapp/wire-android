package com.wire.android.ui.main.conversationlist.mock

import com.wire.android.ui.main.conversationlist.model.AvailabilityStatus
import com.wire.android.ui.main.conversationlist.model.Call
import com.wire.android.ui.main.conversationlist.model.CallEvent
import com.wire.android.ui.main.conversationlist.model.CallInfo
import com.wire.android.ui.main.conversationlist.model.CallTime
import com.wire.android.ui.main.conversationlist.model.Conversation
import com.wire.android.ui.main.conversationlist.model.Conversation.GroupConversation
import com.wire.android.ui.main.conversationlist.model.Conversation.PrivateConversation
import com.wire.android.ui.main.conversationlist.model.ConversationFolder
import com.wire.android.ui.main.conversationlist.model.ConversationInfo
import com.wire.android.ui.main.conversationlist.model.EventType
import com.wire.android.ui.main.conversationlist.model.Membership
import com.wire.android.ui.main.conversationlist.model.Mention
import com.wire.android.ui.main.conversationlist.model.MentionInfo
import com.wire.android.ui.main.conversationlist.model.MentionMessage
import com.wire.android.ui.main.conversationlist.model.NewActivity
import com.wire.android.ui.main.conversationlist.model.UserInfo

val mockConversations = listOf(
    PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest,
            isLegalHold = true
        )
    ),
    PrivateConversation(
        userInfo = UserInfo(
            availabilityStatus = AvailabilityStatus.Available
        ),
        conversationInfo = ConversationInfo(
            name = "some other test value",
            isLegalHold = true
        )
    ),
    PrivateConversation(
        userInfo = UserInfo(
            availabilityStatus = AvailabilityStatus.Busy
        ),
        conversationInfo = ConversationInfo(
            name = "and once more 1",
            membership = Membership.External
        )
    ),
    PrivateConversation(
        userInfo = UserInfo(
            availabilityStatus = AvailabilityStatus.Away
        ),
        conversationInfo = ConversationInfo(
            name = "and once more 2",
            isLegalHold = true
        )
    ),
    GroupConversation(
        0xFF0000FF, "Some group"
    ),
    PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "and once more 3",
            membership = Membership.External
        )
    ),
    PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "and once more 4",
            membership = Membership.External
        )
    ),
)

val mockConversation = PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "and once more 4",
        membership = Membership.External
    )
)

val conversationMockData = mapOf(
    ConversationFolder("SOME TEST FOLDER") to mockConversations,
    ConversationFolder("FOLDER NAME1") to mockConversations,
    ConversationFolder("SOME OTHER FOLDER") to mockConversations,
    ConversationFolder("SOME OTHER Folder1") to mockConversations,
    ConversationFolder("THIS IS A TEST FOLDER") to mockConversations,
    ConversationFolder(
        "THIS IS A TEST FOLDER WITH A VERY VERY VERY VERY" +
                " VERY VERY VERY VERY VERY VERY VERY " +
                "VERY VERY VERY VERY VERY LONG NAME"
    ) to mockConversations
)

val conversationMockData1 = mapOf(
    ConversationFolder("THIS IS A TEST FOLDER after deletin the first one") to mockConversations,
    ConversationFolder(
        "THIS IS A TEST FOLDER WITH A VERY VERY VERY VERY" +
                " VERY VERY VERY VERY VERY VERY VERY " +
                "VERY VERY VERY VERY VERY LONG NAME"
    ) to mockConversations
)


val mockGroupConversation = GroupConversation(
    0xFF00FF00, "Some group"
)

@Suppress("MagicNumber")
val newActivitiesMockData = listOf(
    NewActivity(EventType.MissedCall, mockConversation),
    NewActivity(EventType.UnreadMention, mockConversation),
    NewActivity(EventType.UnreadReply, mockConversation),
    NewActivity(EventType.UnreadMessage(2), mockConversation),
    NewActivity(EventType.UnreadMessage(1000000), mockConversation),
    NewActivity(EventType.UnreadMessage(0), mockConversation),
    NewActivity(EventType.UnreadMessage(50), mockConversation),
    NewActivity(EventType.UnreadMessage(99), mockConversation),
    NewActivity(EventType.UnreadMention, mockConversation),
    NewActivity(EventType.UnreadReply, mockConversation)
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
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
    Mention(mentionInfo = mockShortMentionInfo, mockConversation),
    Mention(mentionInfo = mockLongMentionInfo, mockConversation),
)

val mockCallInfo1 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo2 = CallInfo(CallTime("Yesterday", "1:00 PM"), CallEvent.OutgoingCall)
val mockCallInfo3 = CallInfo(CallTime("Today", "2:34 PM"), CallEvent.MissedCall)
val mockCallInfo4 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo5 = CallInfo(CallTime("Today", "6:59 PM"), CallEvent.NoAnswerCall)

val mockMissedCalls = listOf(
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo4, mockGroupConversation),
    Call(mockCallInfo3, mockConversation),
)

val mockCallHistory = listOf(
    Call(mockCallInfo4, mockGroupConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo4, mockGroupConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo4, mockGroupConversation),
    Call(mockCallInfo5, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo5, mockConversation),
)


