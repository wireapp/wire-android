package com.wire.android.feature.contact.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ContactRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var contactsApi: ContactsApi

    private lateinit var contactRemoteDataSource: ContactRemoteDataSource

    @Before
    fun setUp() {
        contactRemoteDataSource = ContactRemoteDataSource(contactsApi, connectedNetworkHandler)
    }

    @Test
    fun `given contactsById is called, when contactsApi successfully returns a list of contacts, then propagates them as success`() {
        val ids = mockk<Set<String>>()
        val contactList = mockk<List<ContactResponse>>()
        val contactListResponse = mockk<Response<List<ContactResponse>>>().also {
            every { it.isSuccessful } returns true
            every { it.body() } returns contactList
        }
        coEvery { contactsApi.contactsById(ids) } returns contactListResponse

        val result = runBlocking { contactRemoteDataSource.contactsById(ids) }

        result shouldSucceed { it shouldBeEqualTo contactList }
    }

    @Test
    fun `given contactsById is called, when contactsApi returns an error, then propagates a failure`() {
        val ids = mockk<Set<String>>()
        val contactListResponse = mockk<Response<List<ContactResponse>>>().also {
            every { it.isSuccessful } returns false
            every { it.code() } returns 404
        }
        coEvery { contactsApi.contactsById(ids) } returns contactListResponse

        val result = runBlocking { contactRemoteDataSource.contactsById(ids) }

        result shouldFail { }
    }
}
