package com.wire.android.feature.contact.datasources.local

import android.database.sqlite.SQLiteException
import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ContactLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var contactDao: ContactDao

    @MockK
    private lateinit var contactClientDao: ContactClientDao

    private lateinit var contactLocalDataSource: ContactLocalDataSource

    @Before
    fun setUp() {
        contactLocalDataSource = ContactLocalDataSource(contactDao, contactClientDao)
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
    fun `given a list of contact IDs, when calling contactClientsByContactId, then call use dao function`() = runBlockingTest {
        val ids = setOf("abc", "dfg")
        coEvery { contactDao.contactsByIdWithClients(any()) } returns mockk()

        contactLocalDataSource.contactClientsByContactId(ids)

        coVerify(exactly = 1) { contactDao.contactsByIdWithClients(ids) }
    }

    @Test
    fun `given a list of contact IDs, when calling contactClientsByContactId, then return data from dao`() = runBlockingTest {
        val ids = setOf("abc", "dfg")
        val daoResult = mockk<List<ContactWithClients>>()
        coEvery { contactDao.contactsByIdWithClients(any()) } returns daoResult

        val result = contactLocalDataSource.contactClientsByContactId(ids)

        result.shouldSucceed {
            it shouldBeEqualTo daoResult
        }
    }

    @Test
    fun `given the dao fails, when calling contactClientsByContactId, then propagate the failure`() = runBlockingTest {
        val ids = setOf("abc", "dfg")
        coEvery { contactDao.contactsByIdWithClients(any()) } answers { throw SQLiteException() }

        val result = contactLocalDataSource.contactClientsByContactId(ids)

        result.shouldFail { }
    }

    @Test
    fun `given a list of clients, when calling saveNewClients, then it should use dao`() = runBlockingTest {
        val clients = listOf(
                ContactClientEntity("user1", "client1"),
                ContactClientEntity("user2", "client2")
        )

        contactLocalDataSource.saveNewClients(clients)

        coVerify(exactly = 1) { contactClientDao.insertAll(clients) }
    }
}
