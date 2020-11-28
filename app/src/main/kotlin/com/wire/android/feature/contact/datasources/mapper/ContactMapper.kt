package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.remote.ContactResponse

class ContactMapper {

    fun fromContactResponseList(contactResponseList: List<ContactResponse>): List<Contact> =
        contactResponseList.map { fromContactResponse(it) }

    //TODO: map other fields as well
    private fun fromContactResponse(contactResponse: ContactResponse): Contact =
        Contact(id = contactResponse.id, name = contactResponse.name)

    fun toContactEntityList(contacts: List<Contact>) : List<ContactEntity> = contacts.map { toContactEntity(it) }

    private fun toContactEntity(contact: Contact): ContactEntity =
        ContactEntity(id = contact.id, name = contact.name)

    fun fromContactEntityList(entityList: List<ContactEntity>): List<Contact> = entityList.map { fromContactEntity(it) }

    private fun fromContactEntity(entity: ContactEntity): Contact =
        Contact(id = entity.id, name = entity.name)
}
