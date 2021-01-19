package com.wire.android.feature.contact.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.GeneralIOFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import java.io.File

class ContactDataSource(
    private val contactRemoteDataSource: ContactRemoteDataSource,
    private val contactLocalDataSource: ContactLocalDataSource,
    private val contactMapper: ContactMapper
) : ContactRepository {

    override suspend fun contactsById(ids: Set<String>): Either<Failure, List<Contact>> = suspending {
        contactLocalDataSource.contactsById(ids).map { entities ->
            entities.map {
                val profilePicture = getProfilePictureLocally(it).fold({ null }) { it }
                contactMapper.fromContactEntity(it, profilePicture)
            }
        }.flatMap { localContacts ->
            if (localContacts.size == ids.size) Either.Right(localContacts)
            else {
                val locallyAvailableIds = localContacts.map { it.id }
                val idsForRemoteFetch = ids - locallyAvailableIds

                fetchContactsByIdAndSave(idsForRemoteFetch).map { remoteContacts ->
                    localContacts + remoteContacts
                }
            }
        }
    }

    private suspend fun fetchContactsByIdAndSave(ids: Set<String>): Either<Failure, List<Contact>> = suspending {
        contactRemoteDataSource.contactsById(ids).flatMap { responseList ->
            val entities = contactMapper.fromContactResponseListToEntityList(responseList)
            contactLocalDataSource.saveContacts(entities).map { responseList }
        }.map { responseList ->
            responseList.map {
                val profilePicture = fetchProfilePictureAndSave(it).fold({ null }) { file -> file }
                contactMapper.fromContactResponse(it, profilePicture)
            }
        }
    }

    private fun getProfilePictureLocally(contactEntity: ContactEntity): Either<Failure, File> =
        contactLocalDataSource.profilePicture(contactEntity)

    private suspend fun fetchProfilePictureAndSave(contactResponse: ContactResponse): Either<Failure, File> = suspending {
        val key = contactMapper.profilePictureAssetKey(contactResponse)
        if (key == null) Either.Left(GeneralIOFailure())
        else {
            contactRemoteDataSource.downloadProfilePicture(key).flatMap {
                contactLocalDataSource.saveProfilePicture(contactResponse.id, it.byteStream())
            }
        }
    }
}
