package com.wire.android.feature.conversation.list.ui

import com.wire.android.feature.contact.Contact

data class ConversationListItem(val id: String, val name: String?, val members: List<Contact>)
