/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.ui.settings.devices

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestClient
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserMlsClientIdentitiesUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SelfDevicesViewModelTest {

    @Test
    fun `given a self client id, when observing self clients, then returns devices list without current device`() =
        runTest {
            // given
            val currentClient = TestClient.CLIENT
            val otherClient = TestClient.CLIENT.copy(id = ClientId("anotherId"))
            val (_, viewModel) = Arrangement()
                .withCurrentClientId(currentClient.id)
                .withObserveClientsByUserIdResult(ObserveClientsByUserIdUseCase.Result.Success(listOf(currentClient, otherClient)))
                .arrange()
            val currentDevice = Device(currentClient)
            val otherDevice = Device(otherClient)

            // when
            viewModel.loadCertificates()

            // then
            assertEquals(false, viewModel.state.isLoadingClientsList)
            assertEquals(false, viewModel.state.deviceList.contains(currentDevice))
            assertEquals(true, viewModel.state.deviceList.contains(otherDevice))
        }

    @Test
    fun `given a self client id, when loadCertificates is called, then E2EI Certificates is fetched again`() =
        runTest {
            // given
            val (arragne, viewModel) = Arrangement()
                .arrange()

            // when
            viewModel.loadCertificates()
            viewModel.loadCertificates()

            // then
            coVerify(exactly = 2) { arragne.getUserMlsClientIdentities(any()) }
        }

    private class Arrangement {

        @MockK
        lateinit var observeClientsByUserId: ObserveClientsByUserIdUseCase

        @MockK
        lateinit var currentClientId: ObserveCurrentClientIdUseCase

        @MockK
        lateinit var fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase

        @MockK
        lateinit var getUserMlsClientIdentities: GetUserMlsClientIdentitiesUseCase

        @MockK
        lateinit var isE2EIEnabledUseCase: IsE2EIEnabledUseCase

        val selfId = UserId("selfId", "domain")

        private val viewModel by lazy {
            SelfDevicesViewModel(
                observeClientList = observeClientsByUserId,
                currentAccountId = selfId,
                currentClientIdUseCase = currentClientId,
                fetchSelfClientsFromRemote = fetchSelfClientsFromRemote,
                getUserMlsClientIdentities = getUserMlsClientIdentities,
                isE2EIEnabledUseCase = isE2EIEnabledUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { currentClientId.invoke() } returns flowOf(TestClient.CLIENT_ID)
            coEvery { fetchSelfClientsFromRemote.invoke() } returns SelfClientsResult.Success(listOf(), null)
            coEvery { observeClientsByUserId(any()) } returns flowOf(ObserveClientsByUserIdUseCase.Result.Success(listOf()))
            coEvery { getUserMlsClientIdentities.invoke(any()) } returns mapOf()
            coEvery { isE2EIEnabledUseCase() } returns true
        }

        fun withCurrentClientId(clientId: ClientId) = apply {
            coEvery { currentClientId.invoke() } returns flowOf(clientId)
        }

        fun withFetchSelfClientsResult(result: SelfClientsResult) = apply {
            coEvery { fetchSelfClientsFromRemote() } returns result
        }

        fun withObserveClientsByUserIdResult(result: ObserveClientsByUserIdUseCase.Result) = apply {
            coEvery { observeClientsByUserId(any()) } returns flowOf(result)
        }

        fun arrange() = this to viewModel
    }
}
