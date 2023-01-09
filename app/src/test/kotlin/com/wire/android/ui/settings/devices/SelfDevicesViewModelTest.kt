@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.ui.settings.devices

import com.wire.android.framework.TestClient
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.client.SelfClientsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Test

class SelfDevicesViewModelTest {

    @Test
    fun `given a self client id, when fetching self clients, then returns devices list without current device`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withSelfClientsSuccess()
            .arrange()

        // when
        val currentDevice = Device(TestClient.CLIENT)

        // then
        assert(!viewModel.state.deviceList.contains(currentDevice))
    }

    private class Arrangement {
        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var selfClientsUseCase: SelfClientsUseCase

        private val viewModel by lazy {
            SelfDevicesViewModel(
                navigationManager = navigationManager,
                selfClientsUseCase = selfClientsUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            val scheduler = TestCoroutineScheduler()
            Dispatchers.setMain(StandardTestDispatcher(scheduler))

            coEvery { navigationManager.navigate(command = any()) } returns Unit
        }

        fun withSelfClientsSuccess() = apply {
            coEvery { selfClientsUseCase() } returns SelfClientsResult.Success(
                currentClientId = TestClient.CLIENT_ID,
                clients = listOf(
                    TestClient.CLIENT,
                    TestClient.CLIENT.copy(id = ClientId("client2")),
                    TestClient.CLIENT.copy(id = ClientId("client3")),
                )
            )
        }

        fun arrange() = this to viewModel

    }

}
