package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.mapper.AssetMapper

class ContactMapper {

    fun fromContactResponseListToEntityList(contactResponseList: List<ContactResponse>, assetMapper: AssetMapper): List<ContactEntity> =
        contactResponseList.map { fromContactResponseToEntity(it, assetMapper) }

    private fun fromContactResponseToEntity(
        contactResponse: ContactResponse,
        assetMapper: AssetMapper
    ) = ContactEntity(
        id = contactResponse.id,
        name = contactResponse.name,
        assetKey = assetMapper.profilePictureAssetKey(contactResponse.assets)
    )

    fun fromContactEntityList(entityList: List<ContactEntity>): List<Contact> =
        entityList.map { fromContactEntity(it) }

    private fun fromContactEntity(entity: ContactEntity): Contact =
        Contact(
            id = entity.id,
            name = entity.name,
            profilePicture = entity.assetKey?.let { PublicAsset(it) }
        )
}
