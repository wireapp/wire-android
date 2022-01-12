package com.wire.android.feature.contact.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.contact.datasources.local.ContactClientEntity
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.local.ContactWithClients
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ContactDataSourceTest : UnitTest() {

    @MockK
    private lateinit var contactLocalDataSource: ContactLocalDataSource

    @MockK
    private lateinit var contactRemoteDataSource: ContactRemoteDataSource

    @MockK
    private lateinit var contactMapper: ContactMapper

    private lateinit var contactDataSource: ContactDataSource

    @Before
    fun setUp() {
        contactDataSource = ContactDataSource(contactRemoteDataSource, contactLocalDataSource, contactMapper)
    }

    @Test
    fun `given contactsById is called, when local data source fails to query local contacts, then propagates failure`() {
        val failure: Failure = mockk()
        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) }
    }

    @Test
    fun `given contactsById is called, when local data source returns contacts, then maps local contacts and propagates result`() {
        val entities = mockk<List<ContactEntity>>()
        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(entities)

        val contacts = mockk<List<Contact>>()
        every { contactMapper.fromContactEntityList(any()) } returns contacts

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBeEqualTo contacts }
        coVerify(exactly = 1) { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) }
        verify(exactly = 1) { contactMapper.fromContactEntityList(entities) }
    }

    @Test
    fun `given fetchContactsById is called, when remote fetch of contacts fails, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchContactsById is called and remote items are fetched, when items fail to be saved, then propagates the failure`() {
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(mockk())
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()

        val failure = mockk<Failure>()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchContactsById is called and remote items are fetched, when items are saved successfully, then propagates success`() {
        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))

        val savedItems = mockk<List<ContactEntity>>()
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns savedItems
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(Unit)

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(TEST_CONTACT_IDS) }
        verify(exactly = 1) { contactMapper.fromContactResponseListToEntityList(any()) }
        coVerify(exactly = 1) { contactLocalDataSource.saveContacts(savedItems) }
    }

    @Test
    fun `given a list of contact ids, when calling cachedDetailedContactsById, then call the local data source`() = runBlockingTest {
        val ids = setOf("abc", "123")
        coEvery { contactLocalDataSource.contactClientsByContactId(any()) } returns Either.Right(listOf())

        contactDataSource.cachedDetailedContactsById(ids)

        coVerify(exactly = 1) { contactLocalDataSource.contactClientsByContactId(ids) }
    }

    @Test
    fun `given a list of contact ids, when calling cachedDetailedContactsById, then return mapped result`() = runBlockingTest {
        val ids = setOf("abc", "123")
        val contact = ContactWithClients(
                ContactEntity("id", "name", null), listOf(ContactClientEntity("id", "clientId"))
        )
        val mappedResponse = mockk<DetailedContact>()
        every { contactMapper.fromContactWithClients(any()) } returns mappedResponse
        coEvery { contactLocalDataSource.contactClientsByContactId(any()) } returns Either.Right(listOf(contact))

        val result = contactDataSource.cachedDetailedContactsById(ids)

        result.shouldSucceed {
            it.size shouldBeEqualTo 1
            it.first() shouldBeEqualTo mappedResponse
        }
    }

    @Test
    fun `given a list of contact ids, when calling cachedDetailedContactsById, then should map local data`() = runBlockingTest {
        val ids = setOf("abc", "123")
        val contact = ContactWithClients(
                ContactEntity("id", "name", null), listOf(ContactClientEntity("id", "clientId"))
        )
        val mappedResponse = mockk<DetailedContact>()
        every { contactMapper.fromContactWithClients(any()) } returns mappedResponse
        coEvery { contactLocalDataSource.contactClientsByContactId(any()) } returns Either.Right(listOf(contact))

        contactDataSource.cachedDetailedContactsById(ids)

        verify(exactly = 1) { contactMapper.fromContactWithClients(contact) }
    }

    @Test
    fun `given local data source fails, when calling cachedDetailedContactsById, then failure should be propagated`() = runBlockingTest {
        val ids = setOf("abc", "123")
        val expectedFailure = SQLiteFailure()
        coEvery { contactLocalDataSource.contactClientsByContactId(any()) } returns Either.Left(expectedFailure)

        val result = contactDataSource.cachedDetailedContactsById(ids)

        result.shouldFail { it shouldBeEqualTo expectedFailure }
    }

    @Test
    fun `given a contact + new clients + all succeeds, when calling addNewClientsToContact, then return success`() = runBlockingTest {
        val id = "abc"
        val clients = listOf(ContactClient("clientId"))
        val contactEntity = ContactEntity(id, "name", null)
        val contactClientEntity = ContactClientEntity(id, "clientId")
        val contactWithClients = ContactWithClients(contactEntity, listOf(contactClientEntity))
        val contact = Contact(contactEntity.id, contactEntity.name, null)
        coEvery { contactLocalDataSource.contactsById(setOf(id)) } returns Either.Right(listOf(contactEntity))
        coEvery { contactLocalDataSource.saveNewClients(any()) } returns Either.Right(Unit)
        every { contactMapper.fromContactEntityList(any()) } returns listOf(contact)
        every { contactMapper.fromDetailedContactToContactWithClients(any()) } returns contactWithClients

        val result = contactDataSource.addNewClientsToContact(id, clients)
        result shouldSucceed {}
    }

    @Test
    fun `given a contact and new clients, when calling addNewClientsToContact, then the mapper should be used`() = runBlockingTest {
        val id = "abc"
        val clients = listOf(ContactClient("clientId"))
        val contactEntity = ContactEntity(id, "name", null)
        val contactClientEntity = ContactClientEntity(id, "clientId")
        val contactWithClients = ContactWithClients(contactEntity, listOf(contactClientEntity))
        val contact = Contact(contactEntity.id, contactEntity.name, null)
        coEvery { contactLocalDataSource.contactsById(setOf(id)) } returns Either.Right(listOf(contactEntity))
        every { contactMapper.fromContactEntityList(any()) } returns listOf(contact)
        every { contactMapper.fromDetailedContactToContactWithClients(any()) } returns contactWithClients

        contactDataSource.addNewClientsToContact(id, clients)

        coVerify(exactly = 1) { contactMapper.fromContactEntityList(listOf(contactEntity)) }
        coVerify(exactly = 1) { contactMapper.fromDetailedContactToContactWithClients(DetailedContact(contact, clients)) }
    }

    @Test
    fun `given contactsById fails, when calling addNewClientsToContact, then the failure is propagated`() = runBlockingTest {
        val id = "abc"
        val clients = listOf(ContactClient("clientId"))
        val expectedFailure = SQLiteFailure()
        coEvery { contactLocalDataSource.contactsById(setOf(id)) } returns Either.Left(expectedFailure)

        val result = contactDataSource.addNewClientsToContact(id, clients)

        result shouldFail { it shouldBeEqualTo expectedFailure }
    }

    @Test
    fun `given saveNewClients fails, when calling addNewClientsToContact, then the failure is propagated`() = runBlockingTest {
        val id = "abc"
        val clients = listOf(ContactClient("clientId"))
        val contactEntity = ContactEntity(id, "name", null)
        val clientEntities = listOf(ContactClientEntity(id, "clientId"))
        val contact = Contact(contactEntity.id, contactEntity.name, null)
        val contactWithClients = ContactWithClients(contactEntity, clientEntities)
        val expectedFailure = SQLiteFailure()
        coEvery { contactLocalDataSource.contactsById(setOf(id)) } returns Either.Right(listOf(contactEntity))
        coEvery { contactLocalDataSource.saveNewClients(any()) } returns Either.Left(expectedFailure)
        every { contactMapper.fromContactEntityList(any()) } returns listOf(contact)
        every { contactMapper.fromDetailedContactToContactWithClients(any()) } returns contactWithClients

        val result = contactDataSource.addNewClientsToContact(id, clients)

        result shouldFail { it shouldBeEqualTo expectedFailure }
    }

    @Test
    fun `given a contact and new clients, when calling addNewClientsToContact, then should save into local data`() = runBlockingTest {
        val id = "abc"
        val clients = listOf(ContactClient("clientId"))
        val contactEntity = ContactEntity(id, "name", null)
        val clientEntities = listOf(ContactClientEntity(id, "clientId"))
        val contactWithClients = ContactWithClients(contactEntity, clientEntities)
        val contact = Contact(contactEntity.id, contactEntity.name, null)
        coEvery { contactLocalDataSource.contactsById(setOf(id)) } returns Either.Right(listOf(contactEntity))
        every { contactMapper.fromContactEntityList(any()) } returns listOf(contact)
        every { contactMapper.fromDetailedContactToContactWithClients(any()) } returns contactWithClients

        contactDataSource.addNewClientsToContact(id, clients)

        coVerify(exactly = 1) { contactLocalDataSource.saveNewClients(clientEntities) }
    }

    companion object {
        private val TEST_CONTACT_IDS = setOf("contact-id-1", "contact-id-2")
    }
}
