package com.wire.android.ui.home.conversationslist.mock

import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.MentionMessage
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.ui.home.conversationslist.model.UserInfo
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

val mockShortMentionInfo = ConversationLastEvent.Mention(mentionMessage = MentionMessage("Short message"))

val mockLongMentionInfo = ConversationLastEvent.Mention(
    mentionMessage = MentionMessage(
        "THis is a very very very very very very very " +
                "very very very very very very very" +
                " very very very very very very very " +
                "very very very very very very very mention message"
    )
)

val mockConversations1 = listOf(
    ConversationItem.PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true,
        lastEvent = ConversationLastEvent.None
    ),
    ConversationItem.PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true,
        lastEvent = ConversationLastEvent.None
    ),
    ConversationItem.PrivateConversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true,
        lastEvent = ConversationLastEvent.None
    ),
)

fun mockCallConversation(
    lastEvent: ConversationLastEvent = ConversationLastEvent.Call(
        CallTime("Today", "5:34 PM"),
        CallEvent.NoAnswerCall
    )
): ConversationItem.PrivateConversation = ConversationItem.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some test value",
        membership = Membership.None
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = lastEvent,
)

fun mockCallGroupConversation(
    lastEvent: ConversationLastEvent = ConversationLastEvent.Call(
        CallTime("Today", "5:34 PM"),
        CallEvent.NoAnswerCall
    )
): ConversationItem.GroupConversation = ConversationItem.GroupConversation(
    groupColorValue = 0xFFFF0000,
    groupName = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = lastEvent,
)

val mockConversation = ConversationItem.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some test valueeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = ConversationLastEvent.None
)

val mockGroupConversation = ConversationItem.GroupConversation(
    groupColorValue = 0xFFFF0000,
    groupName = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = ConversationLastEvent.None
)

val mockMentionPrivateConversation = ConversationItem.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some very long naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaame",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = mockShortMentionInfo,
)

val mockGeneralConversation = ConversationItem.PrivateConversation(
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some very long naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaame",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = ConversationLastEvent.None
)

val mockGeneralConversationPending = ConversationItem.ConnectionConversation(
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    userInfo = UserInfo(),
    conversationInfo = ConversationInfo(
        name = "some very long teeeeeeeeeeeeeeeeeeeeeeeeest value",
        membership = Membership.Guest
    ),
    lastEvent = ConversationLastEvent.Connection(ConnectionState.PENDING, UserId("someId", "someDomain")),
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


val mockMentionShortGroupConversation = ConversationItem.GroupConversation(
    groupColorValue = 0xFF00FF00,
    groupName = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = mockShortMentionInfo,
)

val mockMentionGroupLongConversation = ConversationItem.GroupConversation(
    groupColorValue = 0xFF00FF00,
    groupName = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = mockShortMentionInfo,
)

val mockUnreadMentionList = listOf(
    mockMentionPrivateConversation,
    mockGroupConversation,
)

val mockAllMentionList = listOf(
    mockMentionGroupLongConversation,
    mockMentionShortGroupConversation,
    mockMentionPrivateConversation,
    mockMentionGroupLongConversation,
    mockMentionShortGroupConversation,
    mockMentionPrivateConversation,
    mockMentionGroupLongConversation,
    mockMentionShortGroupConversation,
    mockMentionPrivateConversation,
    mockMentionGroupLongConversation,
    mockMentionShortGroupConversation,
    mockMentionPrivateConversation,
    mockMentionGroupLongConversation,
    mockMentionShortGroupConversation,
    mockMentionPrivateConversation,
    mockMentionGroupLongConversation,
    mockMentionShortGroupConversation,
    mockMentionPrivateConversation,
)

val mockCallInfo1 = ConversationLastEvent.Call(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo2 = ConversationLastEvent.Call(CallTime("Yesterday", "1:00 PM"), CallEvent.OutgoingCall)
val mockCallInfo3 = ConversationLastEvent.Call(CallTime("Today", "2:34 PM"), CallEvent.MissedCall)
val mockCallInfo4 = ConversationLastEvent.Call(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo5 = ConversationLastEvent.Call(CallTime("Today", "6:59 PM"), CallEvent.NoAnswerCall)

val mockMissedCalls = listOf(
    mockCallConversation(),
    mockCallConversation(),
    mockCallConversation(),
)

val mockCallHistory = listOf(
    mockCallConversation(mockCallInfo1),
    mockCallGroupConversation(mockCallInfo1),
    mockCallConversation(mockCallInfo2),
    mockCallGroupConversation(mockCallInfo2),
    mockCallConversation(mockCallInfo3),
    mockCallGroupConversation(mockCallInfo3),
    mockCallConversation(mockCallInfo4),
    mockCallGroupConversation(mockCallInfo4),
    mockCallConversation(mockCallInfo5),
    mockCallGroupConversation(mockCallInfo5),
)
