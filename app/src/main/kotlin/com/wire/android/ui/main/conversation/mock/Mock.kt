package com.wire.android.ui.main.conversation

import com.wire.android.ui.main.conversation.model.AvailabilityStatus
import com.wire.android.ui.main.conversation.model.Conversation
import com.wire.android.ui.main.conversation.model.ConversationFolder
import com.wire.android.ui.main.conversation.model.ConversationInfo
import com.wire.android.ui.main.conversation.all.model.EventType
import com.wire.android.ui.main.conversation.model.Membership
import com.wire.android.ui.main.conversation.all.model.NewActivity
import com.wire.android.ui.main.conversation.model.UserInfo
import com.wire.android.ui.main.conversation.call.model.Call
import com.wire.android.ui.main.conversation.call.model.CallEvent
import com.wire.android.ui.main.conversation.call.model.CallInfo
import com.wire.android.ui.main.conversation.call.model.CallTime
import com.wire.android.ui.main.conversation.mention.model.Mention
import com.wire.android.ui.main.conversation.mention.model.MentionInfo
import com.wire.android.ui.main.conversation.mention.model.MentionMessage

val mockConversations = listOf(
    Conversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest,
            isLegalHold = true
        )
    ),
    Conversation(
        userInfo = UserInfo(
            availabilityStatus = AvailabilityStatus.Available
        ),
        conversationInfo = ConversationInfo(
            name = "some other test value",
            isLegalHold = true
        )
    ),
    Conversation(
        userInfo = UserInfo(
            availabilityStatus = AvailabilityStatus.Busy
        ),
        conversationInfo = ConversationInfo(
            name = "and once more 1",
            membership = Membership.External
        )
    ),
    Conversation(
        userInfo = UserInfo(
            availabilityStatus = AvailabilityStatus.Away
        ),
        conversationInfo = ConversationInfo(
            name = "and once more 2",
            isLegalHold = true
        )
    ),
    Conversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "and once more 3",
            membership = Membership.External
        )
    ),
    Conversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "and once more 4",
            membership = Membership.External
        )
    ),
)

val mockConversation = Conversation(
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
    Call(mockCallInfo3, mockConversation),
)

val mockCallHistory = listOf(
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo5, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo5, mockConversation),
)


