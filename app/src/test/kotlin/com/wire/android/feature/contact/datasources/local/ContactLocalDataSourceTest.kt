package com.wire.android.feature.contact.datasources.local

import android.database.sqlite.SQLiteException
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.io.FileSystem
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

class ContactLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var contactDao: ContactDao

    @MockK
    private lateinit var fileSystem: FileSystem

    private lateinit var contactLocalDataSource: ContactLocalDataSource

    @Before
    fun setUp() {
        contactLocalDataSource = ContactLocalDataSource(contactDao, fileSystem)
    }

    @Test
    fun `given contactsById is called, when contactDao returns a list of entities, then propagates entities`() {
        val contactIds: Set<String> = mockk()
        val entityList: List<ContactEntity> = mockk()
        coEvery { contactDao.contactsById(contactIds) } returns entityList

        val result = runBlocking { contactLocalDataSource.contactsById(contactIds) }

        result shouldSucceed { it shouldBeEqualTo entityList }
    }

    @Test
    fun `given contactsById is called, when contactDao fails, then propagates failure`() {
        val contactIds: Set<String> = mockk()
        coEvery { contactDao.contactsById(contactIds) } answers { throw SQLiteException() }

        val result = runBlocking { contactLocalDataSource.contactsById(contactIds) }

        result shouldFail { }
    }

    @Test
    fun `given saveContacts is called, when contactDao successfully saves entities, then propagates entities`() {
        val entityList: List<ContactEntity> = mockk()
        coEvery { contactDao.insertAll(entityList) } returns Unit

        val result = runBlocking { contactLocalDataSource.saveContacts(entityList) }

        result shouldSucceed { it shouldBeEqualTo Unit }
    }

    @Test
    fun `given saveContacts is called, when contactDao fails to save, then propagates failure`() {
        val entityList: List<ContactEntity> = mockk()
        coEvery { contactDao.insertAll(entityList) } answers { throw SQLiteException() }

        val result = runBlocking { contactLocalDataSource.saveContacts(entityList) }

        result shouldFail { }
    }

    @Test
    fun `given profilePicture is called, when fileSystem successfully finds the file, then propagates the file`() {
        val file = mockk<File>()
        every { fileSystem.internalFile(any()) } returns Either.Right(file)

        val entity = mockk<ContactEntity>()
        every { entity.id } returns TEST_CONTACT_ID

        val result = contactLocalDataSource.profilePicture(entity)

        result shouldSucceed { it shouldBeEqualTo file }
        verify(exactly = 1) { fileSystem.internalFile(TEST_PROFILE_PICTURE_PATH) }
    }

    @Test
    fun `given profilePicture is called, when fileSystem fails to find the file, then propagates the failure`() {
        val failure = mockk<Failure>()
        every { fileSystem.internalFile(any()) } returns Either.Left(failure)

        val entity = mockk<ContactEntity>()
        every { entity.id } returns TEST_CONTACT_ID

        val result = contactLocalDataSource.profilePicture(entity)

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.internalFile(TEST_PROFILE_PICTURE_PATH) }
    }

    @Test
    fun `given saveProfilePicture is called, when fileSystem fails to create the file, then propagates the failure`() {
        val failure = mockk<Failure>()
        every { fileSystem.createInternalFile(any()) } returns Either.Left(failure)

        val result = contactLocalDataSource.saveProfilePicture(TEST_CONTACT_ID, mockk())

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_PROFILE_PICTURE_PATH) }
    }

    @Test
    fun `given saveProfilePicture is called and file is created, when fileSystem fails to write to file, then propagates the failure`() {
        val file = mockk<File>()
        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)

        val failure = mockk<Failure>()
        every { fileSystem.writeToFile(file, any()) } returns Either.Left(failure)

        val inputStream = mockk<InputStream>()

        val result = contactLocalDataSource.saveProfilePicture(TEST_CONTACT_ID, inputStream)

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.writeToFile(file, inputStream) }
    }

    @Test
    fun `given saveProfilePicture is called and file is created, when fileSystem writes to file, then propagates the written file`() {
        every { fileSystem.createInternalFile(any()) } returns Either.Right(mockk())

        val file = mockk<File>()
        every { fileSystem.writeToFile(any(), any()) } returns Either.Right(file)

        val inputStream = mockk<InputStream>()

        val result = contactLocalDataSource.saveProfilePicture(TEST_CONTACT_ID, inputStream)

        result shouldSucceed { it shouldBeEqualTo file }
        verify(exactly = 1) { fileSystem.writeToFile(any(), inputStream) }
    }

    companion object {
        private const val TEST_CONTACT_ID = "contact-id-1"
        private const val TEST_PROFILE_PICTURE_PATH = "/contact/$TEST_CONTACT_ID/profile_picture.jpg"
    }
}
