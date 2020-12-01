package com.wire.android.feature.contact.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource

class ContactDataSource(
    private val contactRemoteDataSource: ContactRemoteDataSource,
    private val contactLocalDataSource: ContactLocalDataSource,
    private val contactMapper: ContactMapper
) : ContactRepository {

    override suspend fun contactsById(ids: Set<String>): Either<Failure, List<Contact>> = suspending {
        contactLocalDataSource.contactsById(ids).map {
            contactMapper.fromContactEntityList(it)
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
        contactRemoteDataSource.contactsById(ids).map {
            contactMapper.fromContactResponseList(it)
        }.flatMap { contactList ->
            val entities = contactMapper.toContactEntityList(contactList)
            contactLocalDataSource.saveContacts(entities).map { contactList }
        }
    }
}
