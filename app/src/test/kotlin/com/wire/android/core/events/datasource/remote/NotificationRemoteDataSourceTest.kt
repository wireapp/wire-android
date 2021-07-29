package com.wire.android.core.events.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
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

class NotificationRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var notificationApi: NotificationApi

    @MockK
    private lateinit var notificationRetrofitResponse: Response<NotificationResponse>

    @MockK
    private lateinit var lastNotificationRetrofitResponse: Response<LastNotificationResponse>

    @MockK
    private lateinit var notificationResponse: NotificationResponse

    @MockK
    private lateinit var lastNotificationResponse: LastNotificationResponse

    private lateinit var notificationRemoteDataSource: NotificationRemoteDataSource

    @Before
    fun setUp() {
        notificationRemoteDataSource =
            NotificationRemoteDataSource(notificationApi, connectedNetworkHandler)
    }

    @Test
    fun `given lastNotification is called, when notificationApi returns response, then return LastNotificationResponse`() {
        every { lastNotificationRetrofitResponse.body() } returns lastNotificationResponse
        every { lastNotificationRetrofitResponse.isSuccessful } returns true
        coEvery { notificationApi.lastNotification(any()) } returns lastNotificationRetrofitResponse

        val result = runBlocking {
            notificationRemoteDataSource.lastNotification(CLIENT_ID)
        }

        coVerify(exactly = 1) { notificationApi.lastNotification(CLIENT_ID) }
        result shouldSucceed {}
    }

    @Test
    fun `given lastNotification is called, when notificationApi returns failure, then the failure`() {
        every { lastNotificationRetrofitResponse.body() } returns null
        every { lastNotificationRetrofitResponse.isSuccessful } returns false
        coEvery { notificationApi.lastNotification(any()) } returns lastNotificationRetrofitResponse

        val result = runBlocking {
            notificationRemoteDataSource.lastNotification(CLIENT_ID)
        }

        coVerify(exactly = 1) { notificationApi.lastNotification(CLIENT_ID) }
        result shouldFail {}
    }

    @Test
    fun `given notificationsByBatch is called, when notificationApi returns response, then return NotificationResponse`() {
        val size = 100
        val since = "15"
        every { notificationRetrofitResponse.body() } returns notificationResponse
        every { notificationRetrofitResponse.isSuccessful } returns true
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns notificationRetrofitResponse

        val result = runBlocking {
            notificationRemoteDataSource.notificationsByBatch(size, CLIENT_ID, since)
        }

        coVerify(exactly = 1) { notificationApi.notificationsByBatch(size, CLIENT_ID, since) }
        result shouldSucceed {}
    }

    @Test
    fun `given notificationsByBatch is called, when notificationApi returns failure, then the failure`() {
        every { notificationRetrofitResponse.body() } returns null
        every { notificationRetrofitResponse.isSuccessful } returns false
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns notificationRetrofitResponse

        val result = runBlocking { notificationRemoteDataSource.notificationsByBatch(any(), any(), any()) }

        coVerify(exactly = 1) { notificationApi.notificationsByBatch(any(), any(), any()) }
        result shouldFail {}
    }

    companion object {
        private const val CLIENT_ID = "test-client-id"
    }
}
