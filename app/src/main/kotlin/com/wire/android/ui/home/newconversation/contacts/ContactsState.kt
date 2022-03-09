package com.wire.android.ui.home.newconversation.contacts

import com.wire.android.model.UserStatus

data class ContactsState(val contacts: List<Contact> = emptyList())

data class Contact(
    val id: Int,
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
)


data class PublicWire(
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
)


data class FederatedBackend(
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
)
