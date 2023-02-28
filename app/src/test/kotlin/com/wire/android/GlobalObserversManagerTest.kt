package com.wire.android

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.notification.NotificationChannelsManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
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
    fun `given few valid accounts, then create user-specific notification channels`() {
        val accs = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, manager) = Arrangement()
            .withValidAccounts(accs.map { it to null })
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createUserNotificationChannels(accs) }
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var notificationChannelsManager: NotificationChannelsManager

        private val manager by lazy {
            GlobalObserversManager(
                dispatcherProvider = TestDispatcherProvider(),
                coreLogic = coreLogic,
                notificationChannelsManager = notificationChannelsManager
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

        fun arrange() = this to manager
    }
}
