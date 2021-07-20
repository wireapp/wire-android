package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
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
}
