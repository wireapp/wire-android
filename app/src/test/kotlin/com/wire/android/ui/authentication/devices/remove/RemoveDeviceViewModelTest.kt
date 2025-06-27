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
package com.wire.android.ui.authentication.devices.remove

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.TestClient.CLIENT
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class RemoveDeviceViewModelTest {

    @Test
    fun `given success, when fetching devices, then show devices list`() = runTest {
        val (_, viewModel) = Arrangement()
            .withFetchSelfClientsResult(SelfClientsResult.Success(clients = listOf(CLIENT), currentClientId = CLIENT.id))
            .arrange()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.isLoadingClientsList)
        assertEquals(RemoveDeviceError.None, viewModel.state.error)
        assertEquals(1, viewModel.state.deviceList.size)
        assertEquals(CLIENT.id, viewModel.state.deviceList.first().clientId)
    }

    @Test
    fun `given error, when fetching devices, then show init error`() = runTest {
        val (_, viewModel) = Arrangement()
            .withFetchSelfClientsResult(SelfClientsResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null)))
            .arrange()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.isLoadingClientsList)
        assertEquals(RemoveDeviceError.InitError, viewModel.state.error)
        assertEquals(emptyList(), viewModel.state.deviceList)
    }

    @Test
    fun `given error, when retrying, then execute fetch properly`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withFetchSelfClientsResult(SelfClientsResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null)))
            .arrange()
        advanceUntilIdle()

        viewModel.retryFetch()

        coVerify(exactly = 2) { // first time during init, second time when retrying
            arrangement.fetchSelfClientsFromRemote()
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase

        @MockK
        lateinit var deleteClientUseCase: DeleteClientUseCase

        @MockK
        lateinit var registerClientUseCase: GetOrRegisterClientUseCase

        @MockK
        lateinit var isPasswordRequired: IsPasswordRequiredUseCase

        @MockK
        lateinit var userDataStore: UserDataStore

        @MockK
        lateinit var getSelfUser: GetSelfUserUseCase

        @MockK
        lateinit var requestSecondFactorVerificationCodeUseCase: RequestSecondFactorVerificationCodeUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withFetchSelfClientsResult(result: SelfClientsResult) = apply {
            coEvery { fetchSelfClientsFromRemote() } returns result
        }

        fun arrange() = this to RemoveDeviceViewModel(
            fetchSelfClientsFromRemote = fetchSelfClientsFromRemote,
            deleteClientUseCase = deleteClientUseCase,
            registerClientUseCase = registerClientUseCase,
            isPasswordRequired = isPasswordRequired,
            userDataStore = userDataStore,
            getSelfUser = getSelfUser,
            requestSecondFactorVerificationCodeUseCase = requestSecondFactorVerificationCodeUseCase,
        )
    }
}
