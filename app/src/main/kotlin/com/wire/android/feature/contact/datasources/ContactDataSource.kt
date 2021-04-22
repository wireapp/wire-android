package com.wire.android.feature.contact.datasources

import com.wire.android.core.exception.Failure
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

class ContactDataSource(
    private val contactRemoteDataSource: ContactRemoteDataSource,
    private val contactLocalDataSource: ContactLocalDataSource,
    private val contactMapper: ContactMapper
) : ContactRepository {

    override suspend fun fetchContactsById(ids: Set<String>): Either<Failure, Unit> = suspending {
        remoteContacts(ids)
            .flatMap { saveRemoteContacts(it) }
            .map { Unit }
    }

    override suspend fun contactsById(ids: Set<String>): Either<Failure, List<Contact>> =
        localContacts(ids).map { contactMapper.fromContactEntityList(it) }

    private suspend fun remoteContacts(ids: Set<String>): Either<Failure, List<ContactResponse>> =
        contactRemoteDataSource.contactsById(ids)

    private suspend fun localContacts(ids: Set<String>): Either<Failure, List<ContactEntity>> =
        contactLocalDataSource.contactsById(ids)

    private suspend fun saveRemoteContacts(remoteContacts: List<ContactResponse>): Either<Failure, List<ContactEntity>> {
        val entities = contactMapper.fromContactResponseListToEntityList(remoteContacts)
        return contactLocalDataSource.saveContacts(entities).map { entities }
    }
}
