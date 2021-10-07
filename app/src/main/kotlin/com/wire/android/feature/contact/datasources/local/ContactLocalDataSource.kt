package com.wire.android.feature.contact.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class ContactLocalDataSource(private val contactDao: ContactDao,
                             private val contactClientDao: ContactClientDao) : DatabaseService {

    suspend fun contactsById(ids: Set<String>): Either<Failure, List<ContactEntity>> = request {
        contactDao.contactsById(ids)
    }

    suspend fun saveContacts(contacts: List<ContactEntity>): Either<Failure, Unit> = request {
        contactDao.insertAll(contacts)
    }

    suspend fun contactClientsByContactId(contactIds: Set<String>): Either<Failure, List<ContactWithClients>> = request {
        contactDao.contactsByIdWithClients(contactIds)
    }

    suspend fun saveNewClients(clients: List<ContactClientEntity>) = request {
        contactClientDao.insertAll(clients)
    }
}
