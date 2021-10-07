package com.wire.android.feature.contact.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ContactDaoTest : InstrumentationTest() {

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
    fun insertEntity_readContacts_containsInsertedItem() = databaseTestRule.runTest {
        contactDao.insert(TEST_CONTACT_ENTITY)
        val contacts = contactDao.contacts()

        contacts shouldContainSame listOf(TEST_CONTACT_ENTITY)
    }

    @Test
    fun insertAll_readContacts_containsInsertedItems() = databaseTestRule.runTest {
        val entity1 = ContactEntity(id = "id-1", name = "Contact #1", assetKey = "asset-key-1")
        val entity2 = ContactEntity(id = "id-2", name = "Contact #2", assetKey = "asset-key-2")
        val entity3 = ContactEntity(id = "id-3", name = "Contact #3", assetKey = null)
        val entities = listOf(entity1, entity2, entity3)

        contactDao.insertAll(entities)
        val contacts = contactDao.contacts()

        contacts shouldContainSame listOf(entity1, entity2, entity3)
    }

    @Test
    fun contactsById_contactsWithIdsPresent_returnsEntitiesWithIds() = databaseTestRule.runTest {
        val id1 = "id-1"
        val id2 = "id-2"
        val entity1 = ContactEntity(id = id1, name = "Contact #1", assetKey = "asset-key-1")
        val entity2 = ContactEntity(id = id2, name = "Contact #2", assetKey = null)
        val entity3 = ContactEntity(id = "id-3", name = "Contact #3", assetKey = "asset-key-3")

        contactDao.insertAll(listOf(entity1, entity2, entity3))

        val contactsById = contactDao.contactsById(setOf(id1, id2, "someOtherId"))

        contactsById shouldContainSame listOf(entity1, entity2)
    }

    @Test
    fun contactsById_noContactsWithIdsPresent_returnsEmptyList() = databaseTestRule.runTest {
        val entity1 = ContactEntity(id = "id-1", name = "Contact #1", assetKey = "asset-key-1")
        val entity2 = ContactEntity(id = "id-2", name = "Contact #2", assetKey = "asset-key-2")
        val entity3 = ContactEntity(id = "id-3", name = "Contact #3", assetKey = null)

        contactDao.insertAll(listOf(entity1, entity2, entity3))

        val contactsById = contactDao.contactsById(setOf("someId", "someOtherId"))

        contactsById.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun contactsWithClients_contactClientPresent_returnsClientsOfAllContacts() = databaseTestRule.runTest {
        val entity1 = ContactEntity(id = "id-1", name = "Contact #1", assetKey = "asset-key-1")
        val entity2 = ContactEntity(id = "id-2", name = "Contact #2", assetKey = null)
        contactDao.insertAll(listOf(entity1, entity2))

        val contactClientEntity1 = ContactClientEntity(entity1.id, "first client")
        val contactClientEntity2 = ContactClientEntity(entity1.id, "second client")
        val contactClientEntity3 = ContactClientEntity(entity2.id, "third client")
        contactClientDao.insertAll(listOf(contactClientEntity1, contactClientEntity2, contactClientEntity3))

        val result = contactDao.contactsWithClients()

        result shouldContainSame listOf(
                ContactWithClients(entity1, listOf(contactClientEntity1, contactClientEntity2)),
                ContactWithClients(entity2, listOf(contactClientEntity3))
        )
    }

    @Test
    fun contactsByIdWithClients_contactClientPresent_returnsClientsOfSpecificContact() = databaseTestRule.runTest {
        val entity1 = ContactEntity(id = "id-1", name = "Contact #1", assetKey = "asset-key-1")
        val entity2 = ContactEntity(id = "id-2", name = "Contact #2", assetKey = null)
        val entity3 = ContactEntity(id = "id-3", name = "Contact #3", assetKey = null)
        contactDao.insertAll(listOf(entity1, entity2, entity3))

        val contactClientEntity1 = ContactClientEntity(entity1.id, "first client")
        val contactClientEntity2 = ContactClientEntity(entity1.id, "second client")
        val contactClientEntity3 = ContactClientEntity(entity2.id, "third client")
        contactClientDao.insertAll(listOf(contactClientEntity1, contactClientEntity2, contactClientEntity3))

        val result = contactDao.contactsByIdWithClients(setOf(entity1.id, entity3.id))

        result shouldContainSame listOf(
                ContactWithClients(entity1, listOf(contactClientEntity1, contactClientEntity2)),
                ContactWithClients(entity3, listOf())
        )
    }

    companion object {
        private val TEST_CONTACT_ENTITY = ContactEntity("id-123", "Edgar Allan Poe", "asset-key-5093")
    }
}
