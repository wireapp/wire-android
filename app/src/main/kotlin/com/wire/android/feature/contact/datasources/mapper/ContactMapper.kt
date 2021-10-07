package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.contact.datasources.local.ContactClientEntity
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactWithClients
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.mapper.AssetMapper

class ContactMapper(private val assetMapper: AssetMapper) {

    fun fromContactResponseListToEntityList(contactResponseList: List<ContactResponse>): List<ContactEntity> =
            contactResponseList.map { fromContactResponseToEntity(it) }

    private fun fromContactResponseToEntity(
            contactResponse: ContactResponse,
    ) = ContactEntity(
            id = contactResponse.id,
            name = contactResponse.name,
            assetKey = assetMapper.profilePictureAssetKey(contactResponse.assets)
    )

    fun fromContactEntityList(entityList: List<ContactEntity>): List<Contact> =
            entityList.map { fromContactEntity(it) }

    fun fromContactEntity(entity: ContactEntity): Contact =
            Contact(
                    id = entity.id,
                    name = entity.name,
                    profilePicture = entity.assetKey?.let { PublicAsset(it) }
            )

    fun fromContactWithClients(contactWithClients: ContactWithClients): DetailedContact =
            DetailedContact(fromContactEntity(
                    contactWithClients.contact),
                    contactWithClients.clients.map(::fromContactClientEntityToContactClient)
            )

    fun fromContactClientEntityToContactClient(contactClientEntity: ContactClientEntity): ContactClient =
            ContactClient(contactClientEntity.id)

    fun fromContactToEntity(contact: Contact): ContactEntity =
            ContactEntity(contact.id, contact.name, (contact.profilePicture as? PublicAsset)?.key)

    fun fromDetailedContactToContactWithClients(detailedContact: DetailedContact): ContactWithClients =
            ContactWithClients(
                    fromContactToEntity(detailedContact.contact),
                    detailedContact.clients.map { ContactClientEntity(detailedContact.contact.id, it.id) }
            )
}
