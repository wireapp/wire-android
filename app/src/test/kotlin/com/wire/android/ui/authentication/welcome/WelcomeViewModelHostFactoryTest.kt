/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WelcomeViewModelHostFactoryTest {

    @Test
    fun `nomad account takes precedence without loading sessions`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.doesValidNomadAccountExist() } returns true

        assertEquals(
            WelcomeSessionResult.NomadAccountBlocksLogin,
            arrangement.gateway.loadSessions(),
        )
        coVerify(exactly = 0) { arrangement.getSessions() }
    }

    @Test
    fun `successful sessions count only valid accounts and preserve injected maximum`() = runTest {
        val arrangement = Arrangement(maxAccounts = 2)
        coEvery { arrangement.getSessions() } returns GetAllSessionsResult.Success(
            listOf(
                AccountInfo.Valid(UserId("valid-1", "wire.test")),
                AccountInfo.Invalid(UserId("invalid", "wire.test"), LogoutReason.SELF_SOFT_LOGOUT),
                AccountInfo.Valid(UserId("valid-2", "wire.test")),
            )
        )

        assertEquals(
            WelcomeSessionResult.Sessions(validSessionCount = 2, maxAccounts = 2),
            arrangement.gateway.loadSessions(),
        )
    }

    @Test
    fun `no session and generic failures map to feature results`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.getSessions() } returns GetAllSessionsResult.Failure.NoSessionFound
        assertEquals(WelcomeSessionResult.NoSessionFound, arrangement.gateway.loadSessions())

        coEvery { arrangement.getSessions() } returns
            GetAllSessionsResult.Failure.Generic(CoreFailure.Unknown(null))
        assertEquals(WelcomeSessionResult.Unavailable, arrangement.gateway.loadSessions())
    }

    @Test
    fun `host factory selects custom links`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.getSessions() } returns GetAllSessionsResult.Success(
            listOf(AccountInfo.Valid(UserId("valid", "wire.test")))
        )

        val viewModel = arrangement.hostFactory.create(WelcomeNavArgs(ServerConfig.PRODUCTION))
        advanceUntilIdle()

        assertEquals(ServerConfig.PRODUCTION, viewModel.state.links)
        assertTrue(viewModel.state.isThereActiveSession)
    }

    @Test
    fun `host factory selects default links when custom links are absent`() = runTest {
        val arrangement = Arrangement(defaultServerConfig = ServerConfig.STAGING)

        val viewModel = arrangement.hostFactory.create(WelcomeNavArgs())
        advanceUntilIdle()

        assertEquals(ServerConfig.STAGING, viewModel.state.links)
        assertFalse(viewModel.state.isThereActiveSession)
        assertFalse(viewModel.state.maxAccountsReached)
    }

    private class Arrangement(
        defaultServerConfig: ServerConfig.Links = ServerConfig.STAGING,
        maxAccounts: Int = 4,
    ) {
        val getSessions = mockk<GetSessionsUseCase>()
        val doesValidNomadAccountExist = mockk<DoesValidNomadAccountExistUseCase>()
        val gateway = KaliumWelcomeSessionGateway(
            getSessions = getSessions,
            doesValidNomadAccountExist = doesValidNomadAccountExist,
            maxAccounts = maxAccounts,
        )
        val hostFactory = WelcomeViewModelHostFactory(
            getSessions = getSessions,
            doesValidNomadAccountExist = doesValidNomadAccountExist,
            defaultServerConfig = defaultServerConfig,
        )

        init {
            coEvery { doesValidNomadAccountExist() } returns false
            coEvery { getSessions() } returns GetAllSessionsResult.Failure.NoSessionFound
        }
    }
}
