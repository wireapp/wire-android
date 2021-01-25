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
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

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

    @Test
    fun `given contactsById is called, when all contacts are all locally present, then directly propagates success`() {
        val contactEntities: List<ContactEntity> = listOf(mockk(), mockk())
        coEvery { contactLocalDataSource.profilePicture(any()) } returns Either.Left(mockk())
        coEvery { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) } returns Either.Right(contactEntities)

        val contacts = listOf<Contact>(mockk(), mockk())
        every { contactMapper.fromContactEntity(any(), any()) } returnsMany contacts

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldContainSame contacts }
        coVerify(exactly = 1) { contactLocalDataSource.contactsById(TEST_CONTACT_IDS) }
        coVerify { contactRemoteDataSource wasNot Called }
    }

    @Test
    fun `given contactsById is called and a local contact found, when contact has profile picture, then returns mapping with picture`() {
        val entity = mockk<ContactEntity>()
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(listOf(entity))

        val profilePicture = mockk<File>()
        coEvery { contactLocalDataSource.profilePicture(entity) } returns Either.Right(profilePicture)

        val contact = mockk<Contact>()
        every { contactMapper.fromContactEntity(entity, any()) } returns contact

        val result = runBlocking { contactDataSource.contactsById(setOf(TEST_CONTACT_ID_1)) }

        result shouldSucceed { it shouldContainSame listOf(contact) }
        verify(exactly = 1) { contactMapper.fromContactEntity(entity, profilePicture) }
    }

    @Test
    fun `given contactsById is called & a local contact found, when contact doesn't have a picture, then returns mapping w null picture`() {
        val entity = mockk<ContactEntity>()
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(listOf(entity))

        coEvery { contactLocalDataSource.profilePicture(entity) } returns Either.Left(mockk())

        val contact = mockk<Contact>()
        every { contactMapper.fromContactEntity(entity, null) } returns contact

        val result = runBlocking { contactDataSource.contactsById(setOf(TEST_CONTACT_ID_1)) }

        result shouldSucceed { it shouldContainSame listOf(contact) }
        verify(exactly = 1) { contactMapper.fromContactEntity(entity, null) }
    }

    @Test
    fun `given contactsById is called, when no contacts are locally present, then fetches all remotely`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(mockk())

        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        val idSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactLocalDataSource.contactsById(capture(idSlot)) }
        idSlot.captured shouldContainSame TEST_CONTACT_IDS
        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(capture(idSlot)) }
        idSlot.captured shouldContainSame TEST_CONTACT_IDS
    }

    @Test
    fun `given contactsById is called, when contacts are partially locally present, then fetches the rest remotely`() {
        val entity = mockk<ContactEntity>()
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(listOf(entity))
        coEvery { contactLocalDataSource.profilePicture(entity) } returns Either.Left(mockk())

        val contact1 = mockk<Contact>()
        every { contact1.id } returns TEST_CONTACT_ID_1
        every { contactMapper.fromContactEntity(entity, any()) } returns contact1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(mockk())

        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        val idSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactLocalDataSource.contactsById(capture(idSlot)) }
        idSlot.captured shouldContainSame TEST_CONTACT_IDS
        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(capture(idSlot)) }
        idSlot.captured shouldContainSame setOf(TEST_CONTACT_ID_2)
    }


    @Test
    fun `given contactsById is called, when remote fetch of contacts fails, then propagates failure`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val failure = mockk<Failure>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given contactsById is called, when remote fetch of contacts are successful, then saves them into local storage`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val response = mockk<List<ContactResponse>>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(response)

        val entities = mockk<List<ContactEntity>>()
        every { contactMapper.fromContactResponseListToEntityList(response) } returns entities

        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Left(mockk())

        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(any()) }
        coVerify(exactly = 1) { contactMapper.fromContactResponseListToEntityList(response) }
        coVerify(exactly = 1) { contactLocalDataSource.saveContacts(entities) }
    }

    @Test
    fun `given contactsById is called, when remotely fetched items fail to be saved, then propagates the failure`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(mockk())
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()

        val failure = mockk<Failure>()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given contactsById is called & remote items are saved, when item doesn't have asset key, then propagates contact without pictr`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))

        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())

        every { contactMapper.profilePictureAssetKey(contactResponse) } returns null

        val contact = mockk<Contact>()
        every { contactMapper.fromContactResponse(contactResponse, any()) } returns contact

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldContainSame listOf(contact) }
        verify(exactly = 1) { contactMapper.fromContactResponse(contactResponse, null) }
    }

    @Test
    fun `given contactsById is called & remote items are saved, when item has an asset key, then downloads profile picture`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())

        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY

        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Left(mockk())

        every { contactMapper.fromContactResponse(any(), any()) } returns mockk()

        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        verify(exactly = 1) { contactMapper.profilePictureAssetKey(contactResponse) }
        coVerify(exactly = 1) { contactRemoteDataSource.downloadProfilePicture(TEST_ASSET_KEY) }
    }

    @Test
    fun `given contactsById is called & remote item has asset key, when profile pic download fails, then propagates contact with no pic`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY

        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Left(mockk())

        val contact = mockk<Contact>()
        every { contactMapper.fromContactResponse(contactResponse, any()) } returns contact

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldContainSame listOf(contact) }
        verify(exactly = 1) { contactMapper.fromContactResponse(contactResponse, null) }
    }

    @Test
    fun `given contactsById is called, when profile pic download is successful, then saves picture into local storage`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY

        val downloadResponse = mockk<ResponseBody>()
        val downloadStream = mockk<InputStream>()
        every { downloadResponse.byteStream() } returns downloadStream
        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Right(downloadResponse)

        every { contactLocalDataSource.saveProfilePicture(any(), any()) } returns Either.Left(mockk())
        every { contactMapper.fromContactResponse(contactResponse, any()) } returns mockk()

        runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        coVerify(exactly = 1) { contactLocalDataSource.saveProfilePicture(TEST_CONTACT_ID_1, downloadStream) }
    }

    @Test
    fun `given contactsById is called, when profile pic cannot be saved into local storage, then propagates contact with no picture`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY
        val downloadResponse = mockk<ResponseBody>()
        every { downloadResponse.byteStream() } returns mockk()
        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Right(downloadResponse)

        every { contactLocalDataSource.saveProfilePicture(any(), any()) } returns Either.Left(mockk())

        val contact = mockk<Contact>()
        every { contactMapper.fromContactResponse(contactResponse, null) } returns contact

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldContainSame listOf(contact) }
        verify { contactMapper.fromContactResponse(contactResponse, null) }
    }

    @Test
    fun `given contactsById is called, when profile pic is saved into local storage, then propagates contact with saved picture`() {
        coEvery { contactLocalDataSource.contactsById(any()) } returns Either.Right(emptyList())

        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY
        val downloadResponse = mockk<ResponseBody>()
        every { downloadResponse.byteStream() } returns mockk()
        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Right(downloadResponse)

        val savedPicture = mockk<File>()
        every { contactLocalDataSource.saveProfilePicture(any(), any()) } returns Either.Right(savedPicture)

        val contact = mockk<Contact>()
        every { contactMapper.fromContactResponse(contactResponse, savedPicture) } returns contact

        val result = runBlocking { contactDataSource.contactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldContainSame listOf(contact) }
        verify { contactMapper.fromContactResponse(contactResponse, savedPicture) }
    }

    @Test
    fun `given fetchContactsById is called, when remote fetch of contacts fails, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchContactsById is called, when remote fetch of contacts are successful, then saves them into local storage`() {
        val response = mockk<List<ContactResponse>>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(response)

        val entities = mockk<List<ContactEntity>>()
        every { contactMapper.fromContactResponseListToEntityList(response) } returns entities

        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Left(mockk())

        runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        coVerify(exactly = 1) { contactRemoteDataSource.contactsById(any()) }
        coVerify(exactly = 1) { contactMapper.fromContactResponseListToEntityList(response) }
        coVerify(exactly = 1) { contactLocalDataSource.saveContacts(entities) }
    }

    @Test
    fun `given fetchContactsById is called, when remotely fetched items fail to be saved, then propagates the failure`() {
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(mockk())
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()

        val failure = mockk<Failure>()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Left(failure)

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchContactsById called & remote items saved, when item doesn't have asset key, then propagate contact without picture`() {
        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))

        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())

        every { contactMapper.profilePictureAssetKey(contactResponse) } returns null

        every { contactMapper.fromContactResponse(contactResponse, any()) } returns mockk()

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBe Unit }
        verify(exactly = 1) { contactMapper.fromContactResponse(contactResponse, null) }
    }

    @Test
    fun `given fetchContactsById is called & remote items are saved, when item has an asset key, then downloads profile picture`() {
        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())

        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY

        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Left(mockk())

        every { contactMapper.fromContactResponse(any(), any()) } returns mockk()

        runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        verify(exactly = 1) { contactMapper.profilePictureAssetKey(contactResponse) }
        coVerify(exactly = 1) { contactRemoteDataSource.downloadProfilePicture(TEST_ASSET_KEY) }
    }

    @Test
    fun `given fetchContactsById called & remote item has asset key, when prof pic download fails, then propagates contact with no pic`() {
        val contactResponse = mockk<ContactResponse>()
        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY

        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Left(mockk())

        every { contactMapper.fromContactResponse(contactResponse, any()) } returns mockk()

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBe Unit }
        verify(exactly = 1) { contactMapper.fromContactResponse(contactResponse, null) }
    }

    @Test
    fun `given fetchContactsById is called, when profile pic download is successful, then saves picture into local storage`() {
        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY

        val downloadResponse = mockk<ResponseBody>()
        val downloadStream = mockk<InputStream>()
        every { downloadResponse.byteStream() } returns downloadStream
        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Right(downloadResponse)

        every { contactLocalDataSource.saveProfilePicture(any(), any()) } returns Either.Left(mockk())
        every { contactMapper.fromContactResponse(contactResponse, any()) } returns mockk()

        runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        coVerify(exactly = 1) { contactLocalDataSource.saveProfilePicture(TEST_CONTACT_ID_1, downloadStream) }
    }

    @Test
    fun `given fetchContactsById is called, when profile pic cannot be saved into local storage, then propagates contact with no pic`() {
        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY
        val downloadResponse = mockk<ResponseBody>()
        every { downloadResponse.byteStream() } returns mockk()
        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Right(downloadResponse)

        every { contactLocalDataSource.saveProfilePicture(any(), any()) } returns Either.Left(mockk())

        every { contactMapper.fromContactResponse(contactResponse, null) } returns mockk()

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBe Unit }
        verify { contactMapper.fromContactResponse(contactResponse, null) }
    }

    @Test
    fun `given fetchContactsById is called, when profile pic is saved into local storage, then propagates contact with saved picture`() {
        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_1

        coEvery { contactRemoteDataSource.contactsById(any()) } returns Either.Right(listOf(contactResponse))
        every { contactMapper.fromContactResponseListToEntityList(any()) } returns mockk()
        coEvery { contactLocalDataSource.saveContacts(any()) } returns Either.Right(mockk())
        every { contactMapper.profilePictureAssetKey(contactResponse) } returns TEST_ASSET_KEY
        val downloadResponse = mockk<ResponseBody>()
        every { downloadResponse.byteStream() } returns mockk()
        coEvery { contactRemoteDataSource.downloadProfilePicture(any()) } returns Either.Right(downloadResponse)

        val savedPicture = mockk<File>()
        every { contactLocalDataSource.saveProfilePicture(any(), any()) } returns Either.Right(savedPicture)

        val contact = mockk<Contact>()
        every { contactMapper.fromContactResponse(contactResponse, savedPicture) } returns contact

        val result = runBlocking { contactDataSource.fetchContactsById(TEST_CONTACT_IDS) }

        result shouldSucceed { it shouldBe Unit }
        verify { contactMapper.fromContactResponse(contactResponse, savedPicture) }
    }

    companion object {
        private const val TEST_CONTACT_ID_1 = "contact-id-1"
        private const val TEST_CONTACT_ID_2 = "contact-id-2"
        private val TEST_CONTACT_IDS = setOf(TEST_CONTACT_ID_1, TEST_CONTACT_ID_2)

        private const val TEST_ASSET_KEY = "asset-key-3459"
    }
}
