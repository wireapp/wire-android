package com.wire.android.feature.contact.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.core.io.FileSystem
import com.wire.android.core.storage.db.DatabaseService
import java.io.File
import java.io.InputStream

class ContactLocalDataSource(
    private val contactDao: ContactDao,
    private val fileSystem: FileSystem
) : DatabaseService {

    suspend fun contactsById(ids: Set<String>): Either<Failure, List<ContactEntity>> = request {
        contactDao.contactsById(ids)
    }

    suspend fun saveContacts(contacts: List<ContactEntity>): Either<Failure, Unit> = request {
        contactDao.insertAll(contacts)
    }

    fun profilePicture(contactEntity: ContactEntity): Either<Failure, File> =
        fileSystem.internalFile(profilePictureFilePath(contactEntity.id))

    fun saveProfilePicture(contactId: String, inputStream: InputStream): Either<Failure, File> {
        val path = profilePictureFilePath(contactId)

        return fileSystem.createInternalFile(path).flatMap { file ->
            fileSystem.writeToFile(file, inputStream)
        }
    }

    companion object {
        fun profilePictureFilePath(contactId: String) = "/contact/${contactId}/profile_picture.jpg"
    }
}
