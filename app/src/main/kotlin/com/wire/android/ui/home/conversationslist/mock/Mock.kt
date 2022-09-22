package com.wire.android.ui.home.conversationslist.mock

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.MentionMessage
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
        name = "test name",
        userAvatarData = UserAvatarData(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true,
        lastEvent = ConversationLastEvent.None,
        userId = UserId("someUserId", "someDomain"),
        blockingState = BlockingState.NOT_BLOCKED,
        badgeEventType = BadgeEventType.UnreadMessage(1)
    ),
    ConversationItem.PrivateConversation(
        name = "test name",
        userAvatarData = UserAvatarData(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true,
        lastEvent = ConversationLastEvent.None,
        userId = UserId("someUserId", "someDomain"),
        blockingState = BlockingState.NOT_BLOCKED,
        badgeEventType = BadgeEventType.None
    ),
    ConversationItem.PrivateConversation(
        name = "test name",
        userAvatarData = UserAvatarData(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            membership = Membership.Guest
        ),
        conversationId = ConversationId("someId", "someDomain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        isLegalHold = true,
        lastEvent = ConversationLastEvent.None,
        userId = UserId("someUserId", "someDomain"),
        blockingState = BlockingState.NOT_BLOCKED,
        badgeEventType = BadgeEventType.None
    ),
)

fun mockCallConversation(
    lastEvent: ConversationLastEvent = ConversationLastEvent.Call(
        CallTime("Today", "5:34 PM"),
        CallEvent.NoAnswerCall
    )
): ConversationItem.PrivateConversation = ConversationItem.PrivateConversation(
    name = "test name",
    userAvatarData = UserAvatarData(),
    conversationInfo = ConversationInfo(
        name = "some test value",
        membership = Membership.None
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = lastEvent,
    userId = UserId("someUserId", "someDomain"),
    blockingState = BlockingState.NOT_BLOCKED,
    badgeEventType = BadgeEventType.MissedCall
)

fun mockCallGroupConversation(
    lastEvent: ConversationLastEvent = ConversationLastEvent.Call(
        CallTime("Today", "5:34 PM"),
        CallEvent.NoAnswerCall
    )
): ConversationItem.GroupConversation = ConversationItem.GroupConversation(
    name = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = lastEvent,
    badgeEventType = BadgeEventType.MissedCall
)

val mockConversation = ConversationItem.PrivateConversation(
    name = "test name",
    userAvatarData = UserAvatarData(),
    conversationInfo = ConversationInfo(
        name = "some test valueeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = ConversationLastEvent.None,
    userId = UserId("someUserId", "someDomain"),
    blockingState = BlockingState.NOT_BLOCKED,
    badgeEventType = BadgeEventType.None
)

val mockGroupConversation = ConversationItem.GroupConversation(
    name = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = ConversationLastEvent.None,
    badgeEventType = BadgeEventType.None
)

val mockMentionPrivateConversation = ConversationItem.PrivateConversation(
    name = "test name",
    userAvatarData = UserAvatarData(),
    conversationInfo = ConversationInfo(
        name = "some very long naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaame",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = mockShortMentionInfo,
    userId = UserId("someUserId", "someDomain"),
    blockingState = BlockingState.NOT_BLOCKED,
    badgeEventType = BadgeEventType.UnreadMention
)

val mockGeneralConversation = ConversationItem.PrivateConversation(
    name = "test name",
    userAvatarData = UserAvatarData(),
    conversationInfo = ConversationInfo(
        name = "some very long naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaame",
        membership = Membership.Guest
    ),
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = ConversationLastEvent.None,
    userId = UserId("someUserId", "someDomain"),
    blockingState = BlockingState.NOT_BLOCKED,
    badgeEventType = BadgeEventType.None
)

val mockGeneralConversationPending = ConversationItem.ConnectionConversation(
    name = "test name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    userAvatarData = UserAvatarData(),
    conversationInfo = ConversationInfo(
        name = "some very long teeeeeeeeeeeeeeeeeeeeeeeeest value",
        membership = Membership.Guest
    ),
    lastEvent = ConversationLastEvent.Connection(ConnectionState.PENDING, UserId("someId", "someDomain")),
    badgeEventType = BadgeEventType.ReceivedConnectionRequest
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

val mockMentionShortGroupConversation = ConversationItem.GroupConversation(
    name = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = mockShortMentionInfo,
    badgeEventType = BadgeEventType.UnreadMention
)

val mockMentionGroupLongConversation = ConversationItem.GroupConversation(
    name = "Some group name",
    conversationId = ConversationId("someId", "someDomain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    isLegalHold = true,
    lastEvent = mockShortMentionInfo,
    badgeEventType = BadgeEventType.UnreadMention
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
