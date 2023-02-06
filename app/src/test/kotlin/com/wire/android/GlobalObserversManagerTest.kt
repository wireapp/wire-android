package com.wire.android

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.PersistentWebSocketStatus
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
    fun `given few valid accounts, then notificationChannels creating is called`() {
        val accs = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, manager) = Arrangement()
            .withValidAccounts(accs.map { it to null })
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createNotificationChannels(listOf()) }
        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createNotificationChannels(accs) }
    }

    @Test
    fun `given a valid account with persistent socket disabled, then listen for notifications and calls for it`() {
        val statuses = listOf(PersistentWebSocketStatus(TestUser.SELF_USER.id, false))
        val expectedUserIds = listOf(TestUser.SELF_USER.id)
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(expectedUserIds, any(), any()) }
    }

    @Test
    fun `given a valid account with persistent socket enabled, then do not listen for notifications and calls for it`() {
        val statuses = listOf(PersistentWebSocketStatus(TestUser.SELF_USER.id, true))
        val expectedUserIds = listOf<UserId>()
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(expectedUserIds, any(), any()) }
    }

    @Test
    fun `given valid accounts, then listen for notifications and calls only for those that have persistent socket disabled`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
        )
        val expectedUserIds = listOf(TestUser.SELF_USER.id)
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(expectedUserIds, any(), any()) }
    }

    @Test
    fun `given valid accounts, all with persistent socket disabled, then stop socket service`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), false)
        )
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()
        manager.observe()
        coVerify(exactly = 0) { arrangement.servicesManager.startPersistentWebSocketService() }
        coVerify(exactly = 1) { arrangement.servicesManager.stopPersistentWebSocketService() }
    }

    @Test
    fun `given valid accounts, at least one with persistent socket enabled, and socket service not running, then start service`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
        )
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .withIsPersistentWebSocketServiceRunning(false)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.servicesManager.startPersistentWebSocketService() }
        coVerify(exactly = 0) { arrangement.servicesManager.stopPersistentWebSocketService() }
    }

    @Test
    fun `given valid accounts, at least one with persistent socket enabled, and socket service running, then do not start service again`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
        )
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .withIsPersistentWebSocketServiceRunning(true)
            .arrange()
        manager.observe()
        coVerify(exactly = 0) { arrangement.servicesManager.startPersistentWebSocketService() }
        coVerify(exactly = 0) { arrangement.servicesManager.stopPersistentWebSocketService() }
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var notificationChannelsManager: NotificationChannelsManager

        @MockK
        lateinit var notificationManager: WireNotificationManager

        @MockK
        lateinit var servicesManager: ServicesManager

        @MockK
        lateinit var navigationManager: NavigationManager


        private val manager by lazy {
            GlobalObserversManager(
                dispatcherProvider = TestDispatcherProvider(),
                coreLogic = coreLogic,
                notificationChannelsManager = notificationChannelsManager,
                notificationManager = notificationManager,
                servicesManager = servicesManager,
                navigationManager = navigationManager
            )
        }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()

            coEvery { notificationManager.observeNotificationsAndCallsWhileRunning(any(), any(), any()) } returns Unit
            coEvery { coreLogic.getGlobalScope().observeValidAccounts() } returns flowOf(listOf())
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(flowOf(listOf()))
            every { notificationChannelsManager.createNotificationChannels(any()) } returns Unit
            every { servicesManager.isPersistentWebSocketServiceRunning() } returns false
            coEvery { navigationManager.navigate(any()) } returns Unit
        }

        fun withValidAccounts(list: List<Pair<SelfUser, Team?>>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observeValidAccounts() } returns flowOf(list)
        }

        fun withPersistentWebSocketConnectionStatuses(list: List<PersistentWebSocketStatus>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(flowOf(list))
        }

        fun withIsPersistentWebSocketServiceRunning(isRunning: Boolean): Arrangement = apply {
            every { servicesManager.isPersistentWebSocketServiceRunning() } returns isRunning
        }

        fun arrange() = this to manager
    }
}
