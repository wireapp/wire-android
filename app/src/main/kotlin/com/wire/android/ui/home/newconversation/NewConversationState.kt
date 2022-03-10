package com.wire.android.ui.home.newconversation

import com.wire.android.model.UserStatus

data class NewConversationState(val contacts: List<Contact> = emptyList())

data class Contact(
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
)
