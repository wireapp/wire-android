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
        every { authServerConfigProvider.authServer } returns MutableStateFlow(newServerConfig(1).links)
        coEvery { authServerConfigProvider.authServer.value } returns newServerConfig(1).links
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
