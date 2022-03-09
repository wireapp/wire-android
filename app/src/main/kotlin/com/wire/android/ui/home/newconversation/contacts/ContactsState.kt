package com.wire.android.ui.home.newconversation.contacts

import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversationslist.model.EventType

data class ContactsState(val contacts: List<Contact> = emptyList())

data class Contact(
    val id: Int,
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
    val eventType: EventType? = null
)

data class ExternalContact(
    val id: Int,
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
)
