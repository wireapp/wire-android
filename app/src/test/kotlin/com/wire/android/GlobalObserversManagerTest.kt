package com.wire.android

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.framework.TestUser
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GlobalObserversManagerTest {

    @Test
    fun `given few valid accounts, when starting observing accounts, then create user-specific notification channels`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), false)
        )
        val accounts = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, manager) = Arrangement()
            .withValidAccounts(accounts.map { it to null })
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createUserNotificationChannels(accounts) }
        coVerify(exactly = 1) { arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(any(), any()) }
    }

    @Test
    fun `given valid accounts, at least one with persistent socket enabled, and socket service running, then do not start service again`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
        )
        val accounts = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, manager) = Arrangement()
            .withValidAccounts(accounts.map { it to null })
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()

        manager.observe()

        coVerify(exactly = 1) {
            arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(
                listOf(TestUser.SELF_USER.id),
                any()
            )
        }
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var notificationChannelsManager: NotificationChannelsManager

        @MockK
        lateinit var notificationManager: WireNotificationManager

        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider

        private val manager by lazy {
            GlobalObserversManager(
                dispatcherProvider = TestDispatcherProvider(),
                coreLogic = coreLogic,
                notificationChannelsManager = notificationChannelsManager,
                notificationManager = notificationManager,
                userDataStoreProvider = userDataStoreProvider,
            )
        }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()
            every { notificationChannelsManager.createUserNotificationChannels(any()) } returns Unit
        }

        fun withValidAccounts(list: List<Pair<SelfUser, Team?>>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observeValidAccounts() } returns flowOf(list)
        }

        fun withPersistentWebSocketConnectionStatuses(list: List<PersistentWebSocketStatus>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(flowOf(list))
        }

        fun arrange() = this to manager
    }
}
