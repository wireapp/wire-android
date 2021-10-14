package com.wire.android.shared.prekey.data.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkError
import com.wire.android.framework.network.mockNetworkResponse
import com.wire.android.shared.prekey.data.QualifiedUserPreKeyInfo
import com.wire.android.shared.prekey.data.UserPreKeyInfo
import com.wire.android.shared.user.QualifiedId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class PreKeyRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var preKeyAPI: PreKeyAPI

    @MockK
    private lateinit var remotePreKeyListMapper: RemotePreKeyListMapper

    private lateinit var subject: PreKeyRemoteDataSource

    @Before
    fun setUp() {
        subject = PreKeyRemoteDataSource(connectedNetworkHandler, preKeyAPI, remotePreKeyListMapper)
    }

    @Test
    fun `given the API is failing, when fetching pre keys of qualified users, the fail should be propagated`() {
        coEvery { preKeyAPI.preKeysByClientsOfQualifiedUsers(any()) } returns mockNetworkError()

        val result = runBlocking { subject.preKeysForMultipleQualifiedUsers(mapOf()) }

        result.shouldFail {}
    }

    @Test
    fun `given the API is failing, when fetching pre keys of qualified users, the API should be called only once`() {
        coEvery { preKeyAPI.preKeysByClientsOfQualifiedUsers(any()) } returns mockNetworkError()

        runBlocking { subject.preKeysForMultipleQualifiedUsers(mapOf()) }

        coVerify(exactly = 1) { preKeyAPI.preKeysByClientsOfQualifiedUsers(any()) }
    }

    @Test
    fun `given the API is returning successfully, when fetching pre keys of qualified users, the result should be passed to the mapper`() {
        val response = mockk<QualifiedPreKeyListResponse>()
        val mappedResult = mockk<List<QualifiedUserPreKeyInfo>>()

        coEvery { preKeyAPI.preKeysByClientsOfQualifiedUsers(any()) } returns mockNetworkResponse(response)
        every { remotePreKeyListMapper.fromRemoteQualifiedPreKeyInfoMap(any()) } returns mappedResult

        runBlocking { subject.preKeysForMultipleQualifiedUsers(mapOf()) }

        coVerify(exactly = 1) { remotePreKeyListMapper.fromRemoteQualifiedPreKeyInfoMap(response) }
    }

    @Test
    fun `given the API is returning successfully, when fetching pre keys of qualified users, the mapped result should be returned`() {
        val response = mockk<QualifiedPreKeyListResponse>()
        val mappedResult = mockk<List<QualifiedUserPreKeyInfo>>()

        coEvery { preKeyAPI.preKeysByClientsOfQualifiedUsers(any()) } returns mockNetworkResponse(response)
        every { remotePreKeyListMapper.fromRemoteQualifiedPreKeyInfoMap(any()) } returns mappedResult

        val result = runBlocking { subject.preKeysForMultipleQualifiedUsers(mapOf()) }

        result.shouldSucceed { it shouldBeEqualTo mappedResult }
    }

    @Test
    fun `given a map of IDs, when fetching pre keys of qualified users, the correct parameters should be used`() {
        val idMap = mapOf(QualifiedId("a", "A") to listOf("1", "2"))
        val mappedId = mapOf("a" to mapOf("A" to listOf("1", "2")))

        coEvery { preKeyAPI.preKeysByClientsOfQualifiedUsers(any()) } returns mockNetworkResponse()
        every { remotePreKeyListMapper.fromRemoteQualifiedPreKeyInfoMap(any()) } returns mockk()

        runBlocking { subject.preKeysForMultipleQualifiedUsers(idMap) }

        coVerify(exactly = 1) { preKeyAPI.preKeysByClientsOfQualifiedUsers(mappedId) }
    }

    @Test
    fun `given the API is failing, when fetching pre keys of users, the fail should be propagated`() {
        coEvery { preKeyAPI.preKeysByClientsOfUsers(any()) } returns mockNetworkError()

        val result = runBlocking { subject.preKeysForMultipleQualifiedUsers(mapOf()) }

        result.shouldFail {}
    }

    @Test
    fun `given the API is failing, when fetching pre keys of users, the API should be called only once`() {
        coEvery { preKeyAPI.preKeysByClientsOfUsers(any()) } returns mockNetworkError()

        runBlocking { subject.preKeysForMultipleUsers(mapOf()) }

        coVerify(exactly = 1) { preKeyAPI.preKeysByClientsOfUsers(any()) }
    }

    @Test
    fun `given the API is returning successfully, when fetching pre keys of users, the result should be passed to the mapper`() {
        val response = mockk<PreKeyListResponse>()
        val mappedResult = mockk<List<UserPreKeyInfo>>()

        coEvery { preKeyAPI.preKeysByClientsOfUsers(any()) } returns mockNetworkResponse(response)
        every { remotePreKeyListMapper.fromRemotePreKeyInfoMap(any()) } returns mappedResult

        runBlocking { subject.preKeysForMultipleUsers(mapOf()) }

        coVerify(exactly = 1) { remotePreKeyListMapper.fromRemotePreKeyInfoMap(response) }
    }

    @Test
    fun `given the API is returning successfully, when fetching pre keys of users, the mapped result should be returned`() {
        val response = mockk<PreKeyListResponse>()
        val mappedResult = mockk<List<UserPreKeyInfo>>()

        coEvery { preKeyAPI.preKeysByClientsOfUsers(any()) } returns mockNetworkResponse(response)
        every { remotePreKeyListMapper.fromRemotePreKeyInfoMap(any()) } returns mappedResult

        val result = runBlocking { subject.preKeysForMultipleUsers(mapOf()) }

        result.shouldSucceed { it shouldBeEqualTo mappedResult }
    }

    @Test
    fun `given a map of IDs, when fetching pre keys of users, the correct parameters should be used`() {
        val idMap = mapOf("A" to listOf("1", "2"))
        val mappedId = mapOf("A" to listOf("1", "2"))

        coEvery { preKeyAPI.preKeysByClientsOfUsers(any()) } returns mockNetworkResponse()
        every { remotePreKeyListMapper.fromRemotePreKeyInfoMap(any()) } returns mockk()

        runBlocking { subject.preKeysForMultipleUsers(idMap) }

        coVerify(exactly = 1) { preKeyAPI.preKeysByClientsOfUsers(mappedId) }
    }
}
