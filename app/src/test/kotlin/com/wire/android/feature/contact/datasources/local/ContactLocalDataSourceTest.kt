package com.wire.android.feature.contact.datasources.local

import android.database.sqlite.SQLiteException
import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ContactLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var contactDao: ContactDao

    private lateinit var contactLocalDataSource: ContactLocalDataSource

    @Before
    fun setUp() {
        contactLocalDataSource = ContactLocalDataSource(contactDao)
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
}
