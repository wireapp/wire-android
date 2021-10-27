package com.wire.android.feature.contact.datasources.local

import android.database.sqlite.SQLiteConstraintException
import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldThrow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ContactClientDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var contactDao: ContactDao
    private lateinit var contactClientDao: ContactClientDao

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        contactDao = userDatabase.contactDao()
        contactClientDao = userDatabase.contactClientDao()
    }

    @Test
    fun insertAll_noContactExistsWithUserId_throwException() = databaseTestRule.runTest {
        val entity1 = ContactClientEntity(userId = "id-1", id = "abc")

        val func = { runBlocking { contactClientDao.insertAll(listOf(entity1)) } }

        func shouldThrow SQLiteConstraintException::class
    }

    @Test
    fun insert_noContactExistsWithUserId_throwException() = databaseTestRule.runTest {
        val entity1 = ContactClientEntity(userId = "id-1", id = "abc")

        val func = { runBlocking { contactClientDao.insert(entity1) } }
        func shouldThrow SQLiteConstraintException::class
    }

    @Test
    fun insertAll_readAllClients_containsInsertedItems() = databaseTestRule.runTest {
        val contact1 = ContactEntity("id-1", "name 1", "key")
        val contact2 = ContactEntity("id-2", "name 2", "key")
        contactDao.insertAll(listOf(contact1, contact2))
        val entity1 = ContactClientEntity(userId = contact1.id, id = "abc")
        val entity2 = ContactClientEntity(userId = contact1.id, id = "def")
        val entity3 = ContactClientEntity(userId = contact2.id, id = "ghi")
        contactClientDao.insertAll(listOf(entity1, entity2, entity3))

        val clients = contactClientDao.clients()

        clients shouldContainSame listOf(entity1, entity2, entity3)
    }

    @Test
    fun clientsByUserId_clientsWithIdsPresent_returnsEntitiesWithIds() = databaseTestRule.runTest {
        val contact1 = ContactEntity("id-1", "name 1", "key")
        val contact2 = ContactEntity("id-2", "name 2", "key")
        val contact3 = ContactEntity("id-3", "name 3", null)
        contactDao.insertAll(listOf(contact1, contact2, contact3))
        val entity1 = ContactClientEntity(userId = contact1.id, id = "abc")
        val entity2 = ContactClientEntity(userId = contact2.id, id = "def")
        val entity3 = ContactClientEntity(userId = contact3.id, id = "ghi")
        contactClientDao.insertAll(listOf(entity1, entity2, entity3))

        val contactsByUserId = contactClientDao.clientsByUserId(setOf(contact1.id, contact2.id, "someOtherId"))

        contactsByUserId shouldContainSame listOf(entity1, entity2)
    }

    @Test
    fun clientsByUserId_noClientsWithIdsPresent_returnsEmptyList() = databaseTestRule.runTest {
        val contact1 = ContactEntity("id-1", "name 1", "key")
        val contact2 = ContactEntity("id-2", "name 2", "key")
        contactDao.insertAll(listOf(contact1, contact2))
        val entity1 = ContactClientEntity(userId = contact1.id, id = "abc")
        val entity2 = ContactClientEntity(userId = contact1.id, id = "def")
        val entity3 = ContactClientEntity(userId = contact2.id, id = "ghi")
        contactClientDao.insertAll(listOf(entity1, entity2, entity3))

        val contactsById = contactClientDao.clientsByUserId(setOf("someId", "someOtherId"))

        contactsById.isEmpty() shouldBeEqualTo true
    }

}
