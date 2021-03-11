package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import java.io.File

class ContactMapper {

    fun profilePictureAssetKey(contactResponse: ContactResponse): String? =
        contactResponse.assets.find { it.size == ASSET_SIZE_PROFILE_PICTURE }?.key

    fun fromContactResponse(contactResponse: ContactResponse, profilePicture: File?): Contact =
        Contact(id = contactResponse.id, name = contactResponse.name, profilePicturePath = profilePicture?.absolutePath)

    fun fromContactResponseListToEntityList(contactResponseList: List<ContactResponse>): List<ContactEntity> =
        contactResponseList.map { fromContactResponseToEntity(it) }

    private fun fromContactResponseToEntity(contactResponse: ContactResponse): ContactEntity =
        ContactEntity(
            id = contactResponse.id,
            name = contactResponse.name,
            assetKey = profilePictureAssetKey(contactResponse)
        )

    fun fromContactEntity(entity: ContactEntity, profilePicture: File?): Contact =
        Contact(id = entity.id, name = entity.name, profilePicturePath = profilePicture?.absolutePath)

    companion object {
        private const val ASSET_SIZE_PROFILE_PICTURE = "complete"
    }
}
