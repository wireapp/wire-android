package com.wire.android.core.events.datasource

import com.wire.android.UnitTest
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
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
}
