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
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

val mockConversations1 = listOf(
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest
            ),
            conversationId = ConversationId("someId", "someDomain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = true
        )
    ),
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest
            ),
            conversationId = ConversationId("someId", "someDomain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = true
        )
    ),
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.Guest
            ),
            conversationId = ConversationId("someId", "someDomain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = true
        )
    ),
)

val mockConversations2 = listOf(
    GeneralConversation(
        ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.External
            ),
            conversationId = ConversationId("someId", "someDomain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = true
        )
    ),
    ConversationMissedCall(
        callInfo = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall),
        conversationType = ConversationType.PrivateConversation(
            userInfo = UserInfo(),
            conversationInfo = ConversationInfo(
                name = "some test value",
                membership = Membership.None
            ),
            conversationId = ConversationId("someId", "someDomain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = true
        )
    ),
    ConversationUnreadMention(
        mentionInfo = MentionInfo(MentionMessage("Some mention message")),
        conversationType = ConversationType.GroupConversation(
            groupColorValue = 0xFF00FF00,
            groupName = "Some group name",
            conversationId = ConversationId("someId", "someDomain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = true
        ),
    )
)

val mockConversation = ConversationType.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some test value",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true
)

val mockGroupConversation = ConversationType.GroupConversation(
    groupColorValue = 0xFFFF0000,
    groupName = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true
)

val mockGeneralConversation = GeneralConversation(
    ConversationType.PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true
    )
)

val conversationMockData = mapOf(
    ConversationFolder.Custom("SOME TEST FOLDER") to mockConversations1,
    ConversationFolder.Custom("FOLDER NAME1") to mockConversations1,
    ConversationFolder.Custom("SOME OTHER FOLDER") to mockConversations1,
    ConversationFolder.Custom("SOME OTHER Folder1") to mockConversations1,
    ConversationFolder.Custom("THIS IS A TEST FOLDER") to mockConversations1,
    ConversationFolder.Custom(
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
