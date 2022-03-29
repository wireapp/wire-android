package com.wire.android.ui.home.newconversation.contacts

import com.wire.android.model.UserStatus
import com.wire.kalium.logic.data.publicuser.model.PublicUser

data class ContactsState(val contacts: List<Contact> = emptyList())

data class Contact(
    val id: String,
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
)

fun PublicUser.toContact() =
    Contact(
        id = id.value,
        name = name ?: "",
        label = handle ?: "",
    )
