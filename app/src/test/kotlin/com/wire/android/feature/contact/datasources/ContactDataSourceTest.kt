package com.wire.android.feature.contact.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.contact.datasources.remote.ContactRemoteDataSource
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.asset.mapper.AssetMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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

    @MockK
    private lateinit var assetMapper: AssetMapper

    private lateinit var contactDataSource: ContactDataSource

    @Before
    fun setUp() {
        contactDataSource = ContactDataSource(contactRemoteDataSource, contactLocalDataSource, contactMapper, assetMapper)
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
        every { contactMapper.fromContactResponseListToEntityList(any(), assetMapper) } returns mockk()

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
        every { contactMapper.fromContactResponseListToEntityList(any(), assetMapper) } returns savedItems
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(Unit)

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(TEST_CONTACT_IDS) }
        verify(exactly = 1) { contactMapper.fromContactResponseListToEntityList(any(), assetMapper) }
        coVerify(exactly = 1) { contactLocalDataSource.saveContacts(savedItems) }
    }

    companion object {
        private val TEST_CONTACT_IDS = setOf("contact-id-1", "contact-id-2")
    }
}
