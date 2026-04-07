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

package com.wire.android.ui.authentication.welcome

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.mockUri
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class WelcomeViewModelTest {

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var getSessions: GetSessionsUseCase

    @MockK
    lateinit var doesValidNomadAccountExist: DoesValidNomadAccountExistUseCase

    private lateinit var welcomeViewModel: WelcomeViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        val authServer = newServerConfig(1)
        every { savedStateHandle.navArgs<WelcomeNavArgs>() } returns WelcomeNavArgs(authServer.links)
        coEvery { getSessions() } returns GetAllSessionsResult.Success(listOf())
        coEvery { doesValidNomadAccountExist() } returns false
    }

    @Test
    fun `given no nomad account exists, when checking sessions, then nomadAccountBlocksLogin is false`() = runTest {
        welcomeViewModel = WelcomeViewModel(savedStateHandle, getSessions, doesValidNomadAccountExist, ServerConfig.STAGING)
        advanceUntilIdle()

        assertEquals(false, welcomeViewModel.state.nomadAccountBlocksLogin)
    }

    @Test
    fun `given nomad account exists, when checking sessions, then nomadAccountBlocksLogin is true`() = runTest {
        coEvery { doesValidNomadAccountExist() } returns true

        welcomeViewModel = WelcomeViewModel(savedStateHandle, getSessions, doesValidNomadAccountExist, ServerConfig.STAGING)
        advanceUntilIdle()

        assertEquals(true, welcomeViewModel.state.nomadAccountBlocksLogin)
    }
}
