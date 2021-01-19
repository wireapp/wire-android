package com.wire.android.feature.contact.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.asset.datasources.remote.AssetApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ContactRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var contactsApi: ContactsApi

    @MockK
    private lateinit var assetApi: AssetApi

    private lateinit var contactRemoteDataSource: ContactRemoteDataSource

    @Before
    fun setUp() {
        contactRemoteDataSource = ContactRemoteDataSource(
            contactsApi, assetApi, connectedNetworkHandler, TEST_CONTACT_COUNT_THRESHOLD)
    }

    @Test
    fun `given contactsById is called, when number of ids is less than threshold, then sends a single request`() {
        val contactList: List<ContactResponse> = mockContacts(2)
        coEvery { contactsApi.contactsById(any()) } returns mockContactsResponseSuccess(contactList)
        val ids: Set<String> = setOf("a", "b")

        val result = runBlocking { contactRemoteDataSource.contactsById(ids) }

        result shouldSucceed { it shouldContainSame contactList }
        val idQuerySlot = slot<String>()
        coVerify(exactly = 1) { contactsApi.contactsById(capture(idQuerySlot)) }
        idQuerySlot.captured shouldBeEqualTo "a,b"
    }

    @Test
    fun `given contactsById is called, when number of ids is greater than threshold, then sends separate requests for each chunk`() {
        val ids: Set<String> = setOf("a", "b", "c", "d", "e", "f", "g") // [a, b, c], [d, e, f], [g]
        coEvery { contactsApi.contactsById(any()) } returnsMany listOf(
            mockContactsResponseSuccess(mockContacts(3)),
            mockContactsResponseSuccess(mockContacts(3)),
            mockContactsResponseSuccess(mockContacts(1))
        )

        runBlocking { contactRemoteDataSource.contactsById(ids) }

        coVerify(exactly = 1) { contactsApi.contactsById("a,b,c") }
        coVerify(exactly = 1) { contactsApi.contactsById("d,e,f") }
        coVerify(exactly = 1) { contactsApi.contactsById("g") }
    }

    @Test
    fun `given contactsById is called, when contactsApi returns an error for some chunk, then does not fail immediately`() {
        val ids: Set<String> = setOf("a", "b", "c", "d", "e", "f", "g") // [a, b, c], [d, e, f], [g]

        val contactList1 = mockContacts(3)
        val contactList3 = mockContacts(1)
        coEvery { contactsApi.contactsById(any()) } returnsMany listOf(
            mockContactsResponseSuccess(contactList1),
            mockContactsResponseError(),
            mockContactsResponseSuccess(contactList3)
        )

        val result = runBlocking { contactRemoteDataSource.contactsById(ids) }

        result shouldSucceed { it shouldContainSame contactList1 + contactList3 }
        coVerify(exactly = 1) { contactsApi.contactsById("a,b,c") }
        coVerify(exactly = 1) { contactsApi.contactsById("d,e,f") }
        coVerify(exactly = 1) { contactsApi.contactsById("g") }
    }

    @Test
    fun `given contactsById is called, when contactsApi returns an error for all chunks, then propagates failure`() {
        val ids: Set<String> = setOf("a", "b", "c", "d", "e", "f", "g") // [a, b, c], [d, e, f], [g]

        coEvery { contactsApi.contactsById(any()) } returnsMany listOf(
            mockContactsResponseError(),
            mockContactsResponseError(),
            mockContactsResponseError()
        )

        val result = runBlocking { contactRemoteDataSource.contactsById(ids) }

        result shouldFail { }
        coVerify(exactly = 1) { contactsApi.contactsById("a,b,c") }
        coVerify(exactly = 1) { contactsApi.contactsById("d,e,f") }
        coVerify(exactly = 1) { contactsApi.contactsById("g") }
    }

    @Test
    fun `given contactsById is called, when contactsApi successfully returns contacts for all chunks, then propagates them as success`() {
        val ids: Set<String> = setOf("a", "b", "c", "d", "e", "f", "g") // [a, b, c], [d, e, f], [g]

        val contactList1: List<ContactResponse> = mockContacts(3)
        val contactList2: List<ContactResponse> = mockContacts(3)
        val contactList3: List<ContactResponse> = mockContacts(1)

        coEvery { contactsApi.contactsById(any()) } returnsMany listOf(
            mockContactsResponseSuccess(contactList1),
            mockContactsResponseSuccess(contactList2),
            mockContactsResponseSuccess(contactList3)
        )

        val result = runBlocking { contactRemoteDataSource.contactsById(ids) }

        result shouldSucceed { it shouldContainSame contactList1 + contactList2 + contactList3 }
        coVerify(exactly = 1) { contactsApi.contactsById("a,b,c") }
        coVerify(exactly = 1) { contactsApi.contactsById("d,e,f") }
        coVerify(exactly = 1) { contactsApi.contactsById("g") }
    }

    companion object {
        private const val TEST_CONTACT_COUNT_THRESHOLD = 3

        private fun mockContactsResponseSuccess(contactList: List<ContactResponse>): Response<List<ContactResponse>> =
            mockk<Response<List<ContactResponse>>>().also {
                every { it.isSuccessful } returns true
                every { it.body() } returns contactList
            }

        private fun mockContactsResponseError(errorCode: Int = 404): Response<List<ContactResponse>> =
            mockk<Response<List<ContactResponse>>>().also {
                every { it.isSuccessful } returns false
                every { it.code() } returns errorCode
            }

        private fun mockContacts(size: Int): List<ContactResponse> = (0 until size).map { mockk() }
    }
}
