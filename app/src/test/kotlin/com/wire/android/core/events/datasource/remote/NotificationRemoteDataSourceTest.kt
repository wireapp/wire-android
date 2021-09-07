package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.WebSocket
import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.mapper.EventMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NotificationRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var notificationApi: NotificationApi

    @MockK
    private lateinit var notificationRetrofitResponse: Response<NotificationResponse>

    @MockK
    private lateinit var notificationResponse: NotificationResponse

    @MockK
    private lateinit var webSocketService: WebSocketService

    @MockK
    private lateinit var eventMapper: EventMapper

    @MockK
    private lateinit var webSocket: WebSocket

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    private lateinit var notificationRemoteDataSource: NotificationRemoteDataSource

    @Before
    fun setUp() {
        notificationRemoteDataSource = NotificationRemoteDataSource(webSocketService, notificationApi, eventMapper, connectedNetworkHandler)

        val webSocketEvent1 = WebSocket.Event.OnConnectionOpened(webSocket)
        val webSocketEvent2 = WebSocket.Event.OnConnectionClosed(mockk())
        every { webSocketService.observeWebSocketEvent() } returns flowOf(webSocketEvent2, webSocketEvent1)
            .shareIn(testScope, SharingStarted.WhileSubscribed(), 1)
    }

    @Test
    fun `given lastNotification is called, when notificationApi returns response, then returns LastNotificationResponse`() {
        every { notificationRetrofitResponse.body() } returns notificationResponse
        every { notificationRetrofitResponse.isSuccessful } returns true
        coEvery { notificationApi.lastNotification(any()) } returns notificationRetrofitResponse

        val result = runBlocking {
            notificationRemoteDataSource.lastNotification(CLIENT_ID)
        }

        coVerify(exactly = 1) { notificationApi.lastNotification(CLIENT_ID) }
        result shouldSucceed {}
    }

    @Test
    fun `given lastNotification is called, when notificationApi returns failure, then returns the failure`() {
        every { notificationRetrofitResponse.body() } returns null
        every { notificationRetrofitResponse.isSuccessful } returns false
        coEvery { notificationApi.lastNotification(any()) } returns notificationRetrofitResponse

        val result = runBlocking {
            notificationRemoteDataSource.lastNotification(CLIENT_ID)
        }

        coVerify(exactly = 1) { notificationApi.lastNotification(CLIENT_ID) }
        result shouldFail {}
    }

    @Test
    fun `given payloads are null, when the websocket emits events, then emit null`() {
        val eventResponse = mockk<EventResponse>().also {
            every { it.payload } returns null
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse).shareIn(testScope, SharingStarted.WhileSubscribed(), 1)

        runBlocking {
            val result = notificationRemoteDataSource.receiveEvents()

            result.first() shouldBeEqualTo null
            verify(exactly = 1) { webSocketService.receiveEvent() }
        }
    }

    @Test
    fun `given the list of empty payloads, when the websocket emits events, then emit null`() {
        val eventResponse = mockk<EventResponse>().also {
            every { it.payload } returns listOf()
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse).shareIn(testScope, SharingStarted.WhileSubscribed(), 1)

        runBlocking {
            val result = notificationRemoteDataSource.receiveEvents()

            result.first() shouldBeEqualTo listOf()
            verify(exactly = 1) { webSocketService.receiveEvent() }
            verify(exactly = 0) { eventMapper.eventFromPayload(any(), any()) }
        }
    }

    @Test
    fun `given a message payload, when the websocket emits events, then emit the mapped message event`() {
        val eventId = "event-id"
        val message = mockk<Event.Conversation.MessageEvent>()
        val data = mockk<Data>()
        val payload = mockk<Payload>().also {
            every { it.data } returns data
            every { it.type } returns NEW_MESSAGE_TYPE
        }
        val eventResponse = mockk<EventResponse>().also {
            every { it.id } returns eventId
            every { it.payload } returns listOf(payload)
        }
        every { eventMapper.eventFromPayload(payload, eventId) } returns message
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse).shareIn(testScope, SharingStarted.WhileSubscribed(), 1)

        runBlocking {
            val result = notificationRemoteDataSource.receiveEvents()

            result.first()?.size shouldBeEqualTo 1
            result.first()?.get(0) shouldBeEqualTo message
        }
    }

    @Test
    fun `given an API failure response, when collecting notificationsFlow, then an empty list is emitted`() {
        every { webSocketService.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened(webSocket))
            .shareIn(testScope, SharingStarted.WhileSubscribed(), 1)
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns mockErrorResponse(400)

        runBlocking {
            val result = notificationRemoteDataSource.notificationsFlow(any(), any())
            result.first().size shouldBeEqualTo 0
        }
    }

    @Test
    fun `given an API empty notification list response, when collecting notificationsFlow, then an empty list is emitted`() {
        val response = mockSuccessResponse().also {
            every { it.body()?.notifications } returns listOf()
        }
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns response

        runBlocking {
            val result = notificationRemoteDataSource.notificationsFlow(any(), any())

            result.first() shouldBeEqualTo listOf()
        }
    }

    @Test
    fun `given an API null payload response, when collecting notificationsFlow, then an empty list is emitted`() {
        val response = mockSuccessResponse().also {
            every { it.body()?.notifications } returns listOf(notificationResponse)
        }
        every { notificationResponse.payload } returns null
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns response

        runBlocking {
            val result = notificationRemoteDataSource.notificationsFlow(any(), any())

            result.first() shouldBeEqualTo listOf()
        }
    }

    @Test
    fun `given an API empty payload list response, when collecting notificationsFlow, then an empty list is emitted`() {
        val response = mockSuccessResponse().also {
            every { it.body()?.notifications } returns listOf(notificationResponse)
        }
        every { notificationResponse.payload } returns listOf()
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns response

        runBlocking {
            val result = notificationRemoteDataSource.notificationsFlow(any(), any())

            result.first() shouldBeEqualTo listOf()
        }
    }

    @Test
    fun `given an API payload response, when collecting notificationsFlow, then a mapped list of events is emitted`() {
        val notificationId = "notification-id"
        val event = mockk<Event.Conversation.MessageEvent>()
        val payload = mockk<Payload>()
        every { notificationResponse.id } returns notificationId
        every { notificationResponse.payload } returns listOf(payload)
        val response = mockSuccessResponse().also {
            every { it.body()?.notifications } returns listOf(notificationResponse)
        }
        coEvery { notificationApi.notificationsByBatch(any(), any(), any()) } returns response
        every { eventMapper.eventFromPayload(payload, notificationId) } returns event

        runBlocking {
            val result = notificationRemoteDataSource.notificationsFlow(any(), any())

            result.first() shouldBeEqualTo listOf(event)
            verify(exactly = 1) { eventMapper.eventFromPayload(payload, notificationId) }
        }
    }


    companion object {
        private const val CLIENT_ID = "test-client-id"
        private const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"

        private fun mockSuccessResponse(): Response<NotificationPageResponse> =
            (mockkClass(Response::class) as Response<NotificationPageResponse>).apply {
                every { isSuccessful } returns true
                every { body() } returns mockk()
            }.also {
                every { it.body() } returns mockk()
                every { it.body()?.hasMore } returns false
            }

        private fun mockErrorResponse(errorCode: Int): Response<NotificationPageResponse> =
            (mockkClass(Response::class) as Response<NotificationPageResponse>).apply {
                every { isSuccessful } returns false
                every { code() } returns errorCode
            }
    }
}
