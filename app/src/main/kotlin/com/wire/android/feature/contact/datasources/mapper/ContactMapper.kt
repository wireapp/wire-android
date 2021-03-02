package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.shared.asset.PublicAsset

class ContactMapper {

    fun fromContactResponseListToEntityList(contactResponseList: List<ContactResponse>): List<ContactEntity> =
        contactResponseList.map { fromContactResponseToEntity(it) }

    private fun fromContactResponseToEntity(contactResponse: ContactResponse): ContactEntity =
        ContactEntity(
            id = contactResponse.id,
            name = contactResponse.name,
            assetKey = profilePictureAssetKey(contactResponse)
        )

    private fun profilePictureAssetKey(contactResponse: ContactResponse): String? =
        contactResponse.assets.find { it.size == ASSET_SIZE_PROFILE_PICTURE }?.key

    fun fromContactEntityList(entityList: List<ContactEntity>): List<Contact> =
        entityList.map { fromContactEntity(it) }

    private fun fromContactEntity(entity: ContactEntity): Contact =
        Contact(
            id = entity.id,
            name = entity.name,
            profilePicture = entity.assetKey?.let { PublicAsset(it) }
        )

    companion object {
        private const val ASSET_SIZE_PROFILE_PICTURE = "complete"
    }
}
