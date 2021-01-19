package com.wire.android.feature.contact.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource
import com.wire.android.framework.functional.shouldFail
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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
        verify { contactRemoteDataSource wasNot Called }
        verify { contactMapper wasNot Called }
        coVerify(inverse = true) { contactLocalDataSource.saveContacts(any()) }
    }

//    @Test
//    fun `given contactsById is called, when all contacts are all locally present, then directly propagates success`() {
//        val contactEntities: List<ContactEntity> = mockk()
//        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(contactEntities)
//        every { contactMapper.fromContactEntityList(contactEntities) } returns TEST_CONTACTS
//
//        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }
//
//        result shouldSucceed { it shouldContainSame TEST_CONTACTS }
//        coVerify(exactly = 1) { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) }
//        verify(exactly = 1) { contactMapper.fromContactEntityList(contactEntities) }
//        coVerify { contactRemoteDataSource wasNot Called }
//        coVerify(inverse = true) { contactLocalDataSource.saveContacts(any()) }
//    }
//
//    @Test
//    fun `given contactsById is called, when contacts are partially locally present, then fetches the rest remotely`() {
//        val contactEntities: List<ContactEntity> = mockk()
//        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(contactEntities)
//        every { contactMapper.fromContactEntityList(contactEntities) } returns TEST_CONTACTS.take(1)
//
//        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(ServerError)
//
//        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }
//
//        val remoteContactIdsSlot = slot<Set<String>>()
//        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(capture(remoteContactIdsSlot)) }
//        remoteContactIdsSlot.captured shouldContainSame TEST_CONTACT_IDS.drop(1)
//    }
//
//    @Test
//    fun `given contactsById is called, when remote fetch of contacts fails, then propagates failure`() {
//        val contactEntities: List<ContactEntity> = mockk()
//        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(contactEntities)
//        every { contactMapper.fromContactEntityList(contactEntities) } returns TEST_CONTACTS.take(1)
//
//        val failure = mockk<Failure>()
//        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(failure)
//
//        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }
//
//        result shouldFail { it shouldBeEqualTo failure }
//    }
//
//    @Test
//    fun `given contactsById is called, when remote fetch of contacts are successful, then saves them into local storage`() {
//        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(mockk())
//        every { contactMapper.fromContactEntityList(any()) } returns TEST_CONTACTS.take(1)
//
//        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(mockk())
//        val remoteContacts: List<Contact> = mockk()
//        every { contactMapper.fromContactResponseList(any()) } returns remoteContacts
//
//        val remoteEntities: List<ContactEntity> = mockk()
//        every { contactMapper.toContactEntityList(remoteContacts) } returns remoteEntities
//        coEvery { contactLocalDataSource.saveContacts(remoteEntities) } returns Either.Left(DatabaseFailure())
//
//        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }
//
//        coVerify(exactly = 1) { contactLocalDataSource.contactsById(any()) }
//        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(any()) }
//        coVerify(exactly = 1) { contactLocalDataSource.saveContacts(remoteEntities) }
//    }
//
//    @Test
//    fun `given contactsById is called, when remotely fetched items fail to be saved, then propagates the failure`() {
//        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(mockk())
//        every { contactMapper.fromContactEntityList(any()) } returns TEST_CONTACTS.take(1)
//
//        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(mockk())
//        every { contactMapper.fromContactResponseList(any()) } returns mockk()
//        every { contactMapper.toContactEntityList(any()) } returns mockk()
//
//        val failure = mockk<Failure>()
//        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Left(failure)
//
//        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }
//
//        result shouldFail { it shouldBeEqualTo failure }
//    }
//
//    @Test
//    fun `given contactsById is called, when remotely fetched items are successfully saved, then propagates success with all contacts`() {
//        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(mockk())
//        every { contactMapper.fromContactEntityList(any()) } returns TEST_CONTACTS.take(1)
//
//        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(mockk())
//        val remoteContacts = TEST_CONTACTS.drop(1)
//        every { contactMapper.fromContactResponseList(any()) } returns remoteContacts
//        every { contactMapper.toContactEntityList(remoteContacts) } returns mockk()
//
//        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
//
//        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }
//
//        result shouldSucceed { it shouldContainSame TEST_CONTACTS }
//    }


    companion object {
        private val TEST_CONTACT_IDS = setOf("id-1", "id-2", "id-3")

        private val TEST_CONTACTS = TEST_CONTACT_IDS.map { contactId ->
            val contact: Contact = mockk()
            every { contact.id } returns contactId
            contact
        }
    }
}
