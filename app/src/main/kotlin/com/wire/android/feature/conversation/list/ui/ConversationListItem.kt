package com.wire.android.feature.conversation.list.ui

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.Conversation

data class ConversationListItem(val conversation: Conversation, val members: List<Contact>)
