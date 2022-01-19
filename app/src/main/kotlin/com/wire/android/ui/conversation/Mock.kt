package com.wire.android.ui.conversation

import com.wire.android.ui.conversation.model.AvailabilityStatus
import com.wire.android.ui.conversation.model.Conversation
import com.wire.android.ui.conversation.model.ConversationFolder
import com.wire.android.ui.conversation.model.ConversationInfo
import com.wire.android.ui.conversation.model.Membership
import com.wire.android.ui.conversation.model.UserInfo

val mockConversations = listOf(
    Conversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "some test value",
            memberShip = Membership.Guest,
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
            memberShip = Membership.External
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
            memberShip = Membership.External
        )
    ),
    Conversation(
        userInfo = UserInfo(),
        conversationInfo = ConversationInfo(
            name = "and once more 4",
            memberShip = Membership.External
        )
    ),
)

val mockData = mapOf(
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


