package com.wire.android.feature.contact

import com.wire.android.shared.asset.Asset

class Contact(val id: String, val name: String, val profilePicture: Asset?)

data class ContactClient(val id: String)

data class DetailedContact(val contact: Contact, val clients: List<ContactClient>)
