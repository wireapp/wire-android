package com.wire.android.feature.auth.client.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ClientDataSourceTest : UnitTest() {

    @MockK
    private lateinit var clientRemoteDataSource: ClientRemoteDataSource

    private lateinit var clientDataSource: ClientDataSource


    @Before
    fun setUp() {
        clientDataSource = ClientDataSource(clientRemoteDataSource)
    }

    @Test
    fun `given registerNewClient is called, when remote registration fails, then propagates failure`(){
        val failure = mockk<Failure>()
        val client = mockk<Client>()
        val authorizationToken = "authorizationKey"
        coEvery { clientRemoteDataSource.registerNewClient(authorizationToken, client) } returns Either.Left(failure)

        val result = runBlocking { clientDataSource.registerNewClient(authorizationToken, client) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given registerNewClient is called, when remote registration is successfully done, then return client response`(){
        val client = mockk<Client>()
        val clientResponse = mockk<ClientResponse>()
        val authorizationToken = "authorizationKey"
        coEvery { clientRemoteDataSource.registerNewClient(authorizationToken, client) } returns Either.Right(clientResponse)

        val result = runBlocking { clientDataSource.registerNewClient(authorizationToken, client) }

        result shouldSucceed {it shouldBeEqualTo clientResponse}
    }
}

