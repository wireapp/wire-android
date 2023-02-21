/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

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
