package com.wire.android.feature.conversation.data.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.any
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ConversationRemoteDataSourceTest : UnitTest() {

    private lateinit var remoteDataSource: ConversationsRemoteDataSource

    @MockK
    private lateinit var conversationsApi: ConversationsApi

    @MockK
    private lateinit var response: Response<ConversationsResponse>

    @MockK
    private lateinit var conversationsResponse: ConversationsResponse

    @Before
    fun setup() {
        remoteDataSource = ConversationsRemoteDataSource(connectedNetworkHandler, conversationsApi)
    }

    @Test
    fun `given any size, when conversationsByBatch is requested and response is success, propagate successful ConversationsResponse`() {
        every { response.body() } returns conversationsResponse
        every { response.isSuccessful } returns true
        coEvery { conversationsApi.conversationsByBatch(any()) } returns response

        val result = runBlocking { remoteDataSource.conversationsByBatch(any(), any(), any()) }

        coVerify(exactly = 1) { conversationsApi.conversationsByBatch(any()) }
        result shouldSucceed {}
    }
}
