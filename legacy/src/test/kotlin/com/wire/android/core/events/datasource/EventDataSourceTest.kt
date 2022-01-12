package com.wire.android.core.events.datasource

import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class EventDataSourceTest : UnitTest() {

    @MockK
    private lateinit var notificationLocalDataSource: NotificationLocalDataSource

    @MockK
    private lateinit var notificationRemoteDataSource: NotificationRemoteDataSource

    @MockK
    private lateinit var sessionRepository: SessionRepository

    private lateinit var subject: EventDataSource

    @Before
    fun setUp() {
        subject = EventDataSource(notificationLocalDataSource, notificationRemoteDataSource, sessionRepository)
    }

    @Test
    fun `given the current session id, when collecting events, then the current client ID should be used in remoteDataSource`() {
        runBlockingTest {
            val clientId = "CLIENT_ID"
            val lastNotificationId = "ABC"
            coEvery { sessionRepository.currentClientId() } returns Either.Right(clientId)
            coEvery { notificationLocalDataSource.lastNotificationId() } returns lastNotificationId
            coEvery { notificationRemoteDataSource.receiveEvents(clientId) } returns emptyFlow()
            coEvery { notificationRemoteDataSource.notificationsFlow(clientId, lastNotificationId) } returns emptyFlow()

            subject.events().collect()

            coVerify(exactly = 1) { notificationRemoteDataSource.receiveEvents(clientId) }
            coVerify(exactly = 1) { notificationRemoteDataSource.notificationsFlow(clientId, lastNotificationId) }
        }
    }

    @Test
    fun `given sessionRepository returns failure, when collecting events, then return an empty flow`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.currentClientId() } returns Either.Left(failure)

        runBlocking {
            val result = subject.events()

            result.count() shouldBeEqualTo 0
        }
    }

    @Test
    fun `given repository fails to return lastNotificationId, when collecting events, then emits only live events`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.currentClientId() } returns Either.Right(CLIENT_ID)
        coEvery { notificationLocalDataSource.lastNotificationId() } returns null
        coEvery { notificationRemoteDataSource.lastNotification(CLIENT_ID) } returns Either.Left(failure)
        coEvery { notificationRemoteDataSource.receiveEvents(CLIENT_ID) } returns flowOf(LIVE_EVENTS)

        runBlocking {
            val result = subject.events()

            result.count() shouldBeEqualTo 2
            result.first() shouldBeEqualTo Either.Right(LIVE_EVENTS[0])
        }
    }

    @Test
    fun `given liveEvents repository returns null events flow, when collecting events, then emits only pending events`() {
        coEvery { sessionRepository.currentClientId() } returns Either.Right(CLIENT_ID)
        coEvery { notificationLocalDataSource.lastNotificationId() } returns LAST_NOTIFICATION_ID
        coEvery { notificationRemoteDataSource.notificationsFlow(CLIENT_ID, any()) } returns flowOf(PENDING_EVENTS)
        coEvery { notificationRemoteDataSource.receiveEvents(CLIENT_ID) } returns flowOf(null)

        runBlocking {
            val result = subject.events()

            result.count() shouldBeEqualTo 2
            result.first() shouldBeEqualTo Either.Right(PENDING_EVENTS[0])
        }
    }

    @Test
    fun `given repository live events and pending events, when collecting events, then emits events in order`() {
        coEvery { sessionRepository.currentClientId() } returns Either.Right(CLIENT_ID)
        coEvery { notificationLocalDataSource.lastNotificationId() } returns LAST_NOTIFICATION_ID
        coEvery { notificationRemoteDataSource.notificationsFlow(CLIENT_ID, any()) } returns flowOf(PENDING_EVENTS)
        coEvery { notificationRemoteDataSource.receiveEvents(CLIENT_ID) } returns flowOf(LIVE_EVENTS)

        runBlocking {
            val result = subject.events()

            result.count() shouldBeEqualTo 4
            result.first() shouldBeEqualTo Either.Right(PENDING_EVENTS[0])
            result.last() shouldBeEqualTo Either.Right(LIVE_EVENTS[1])
        }
    }

    companion object {
        private const val CLIENT_ID = "current-client-id"
        private const val LAST_NOTIFICATION_ID = "last_notification_id"
        private val LIVE_EVENTS : List<Event> = listOf(
            Event.Conversation.MessageEvent("id-live1", "convId-live1", "ClientId-live1", "UserId-live1", "content", "time"),
            Event.Conversation.MessageEvent("id-live2", "convId-live2", "ClientId-live2", "UserId-live2", "content", "time")
        )

        private val PENDING_EVENTS : List<Event> = listOf(
            Event.Conversation.MessageEvent("id-pending1", "convId-pending1", "clientId-pending1", "UserId-pending1", "content", "time"),
            Event.Conversation.MessageEvent("id-pending2", "convId-pending2", "clientId-pending2", "UserId-pending2", "content", "time")
        )
    }

}
