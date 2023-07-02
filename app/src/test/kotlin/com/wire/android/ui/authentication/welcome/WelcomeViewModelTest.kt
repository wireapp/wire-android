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

package com.wire.android.ui.authentication.welcome

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WelcomeViewModelTest {

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var authServerConfigProvider: AuthServerConfigProvider

    @MockK
    lateinit var getSessions: GetSessionsUseCase

    private lateinit var welcomeViewModel: WelcomeViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        val authServer = newServerConfig(1)
        every { authServerConfigProvider.authServer } returns MutableStateFlow(authServer.links)
        coEvery { authServerConfigProvider.authServer } returns MutableStateFlow(authServer.links)
        coEvery { getSessions() } returns GetAllSessionsResult.Success(listOf())
        welcomeViewModel = WelcomeViewModel(navigationManager, authServerConfigProvider, getSessions)
    }

    @Test
    fun `given a navigation, when it's go to login, then should emit NavigationCommand login`() = runTest {
        welcomeViewModel.goToLogin()

        coVerify(exactly = 1) { navigationManager.navigate(NavigationCommand(NavigationItem.Login.getRouteWithArgs())) }
    }

    @Test
    fun `given a navigation, when it's go to create private account, then should emit NavigationCommand create personal account`() =
        runTest {
            welcomeViewModel.goToCreatePrivateAccount()

            coVerify(exactly = 1) { navigationManager.navigate(NavigationCommand(NavigationItem.CreatePersonalAccount.getRouteWithArgs())) }
        }

    @Test
    fun `given a navigation, when it's go to create enterprise account, then should emit NavigationCommand create team`() = runTest {
        welcomeViewModel.goToCreateEnterpriseAccount()

        coVerify(exactly = 1) { navigationManager.navigate(NavigationCommand(NavigationItem.CreateTeam.getRouteWithArgs())) }
    }

    @Test
    fun `given a navigation, when navigating back, then should delegate call to navigation manager back`() = runTest {
        welcomeViewModel.navigateBack()

        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }
}
