package com.wire.android.feature.contact.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.GeneralIOFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import okhttp3.ResponseBody
import java.io.File

class ContactDataSource(
    private val contactRemoteDataSource: ContactRemoteDataSource,
    private val contactLocalDataSource: ContactLocalDataSource,
    private val contactMapper: ContactMapper
) : ContactRepository {

    override suspend fun fetchContactsById(ids: Set<String>): Either<Failure, Unit> =
        remoteContacts(ids).map { Unit }

    override suspend fun contactsById(ids: Set<String>): Either<Failure, List<Contact>> =
        readContacts(ids).map { entities ->
            entities.map { contactInfo(it) }
        }

    private suspend fun remoteContacts(ids: Set<String>): Either<Failure, List<Contact>> = suspending {
        fetchContacts(ids).flatMap { contactResponseList ->
            saveRemoteContacts(contactResponseList).map { contactResponseList }
        }.map { responseList ->
            responseList.map { contactInfo(it) }
        }
    }

    private suspend fun readContacts(ids: Set<String>): Either<Failure, List<ContactEntity>> =
        contactLocalDataSource.contactsById(ids)

    private suspend fun fetchContacts(ids: Set<String>): Either<Failure, List<ContactResponse>> =
        contactRemoteDataSource.contactsById(ids)

    private suspend fun saveRemoteContacts(remoteContacts: List<ContactResponse>): Either<Failure, Unit> {
        val entities = contactMapper.fromContactResponseListToEntityList(remoteContacts)
        return contactLocalDataSource.saveContacts(entities)
    }

    private fun contactInfo(contactEntity: ContactEntity): Contact {
        val profilePicture = localProfilePicture(contactEntity).fold({ null }) { it }
        return contactMapper.fromContactEntity(contactEntity, profilePicture)
    }

    private suspend fun contactInfo(contactResponse: ContactResponse): Contact {
        val profilePicture = remoteProfilePicture(contactResponse).fold({ null }) { it }
        return contactMapper.fromContactResponse(contactResponse, profilePicture)
    }

    private fun localProfilePicture(contactEntity: ContactEntity): Either<Failure, File> =
        contactLocalDataSource.profilePicture(contactEntity)

    private suspend fun remoteProfilePicture(contactResponse: ContactResponse): Either<Failure, File> = suspending {
        fetchProfilePicture(contactResponse).flatMap {
            saveProfilePicture(contactResponse.id, it)
        }
    }

    private suspend fun fetchProfilePicture(contactResponse: ContactResponse): Either<Failure, ResponseBody> {
        val key = contactMapper.profilePictureAssetKey(contactResponse)

        return if (key == null) Either.Left(GeneralIOFailure())
        else contactRemoteDataSource.downloadProfilePicture(key)
    }

    private fun saveProfilePicture(contactId: String, responseBody: ResponseBody): Either<Failure, File> =
        contactLocalDataSource.saveProfilePicture(contactId, responseBody.byteStream())

}
