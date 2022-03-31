package com.wire.android.ui.home.newconversation.contacts


import android.os.Parcelable
import com.wire.android.model.UserStatus
import com.wire.kalium.logic.data.publicuser.model.PublicUser
import kotlinx.parcelize.Parcelize

data class ContactsState(val contacts: List<Contact> = emptyList(), val addToGroupContacts: List<Contact> = emptyList())

@Parcelize
data class Contact(
    val id: String,
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
) : Parcelable

fun PublicUser.toContact() =
    Contact(
        id = id.value,
        name = name ?: "",
        label = handle ?: "",
    )
