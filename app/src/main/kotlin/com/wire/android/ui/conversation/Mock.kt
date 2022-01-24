package com.wire.android.ui.conversation

import com.wire.android.ui.conversation.all.model.AvailabilityStatus
import com.wire.android.ui.conversation.all.model.Conversation
import com.wire.android.ui.conversation.all.model.ConversationFolder
import com.wire.android.ui.conversation.all.model.ConversationInfo
import com.wire.android.ui.conversation.all.model.EventType
import com.wire.android.ui.conversation.all.model.Membership
import com.wire.android.ui.conversation.all.model.NewActivity
import com.wire.android.ui.conversation.all.model.UserInfo
import com.wire.android.ui.conversation.mention.model.Mention
import com.wire.android.ui.conversation.mention.model.MentionInfo
import com.wire.android.ui.conversation.mention.model.MentionMessage

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
    ConversationFolder("NEW ACTIVITY") to mockConversations,
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
