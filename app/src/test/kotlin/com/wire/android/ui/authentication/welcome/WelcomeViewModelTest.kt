package com.wire.android.ui.authentication.welcome

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WelcomeViewModelTest {

    @MockK
    lateinit var navigationManager: NavigationManager

    private lateinit var welcomeViewModel: WelcomeViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        welcomeViewModel = WelcomeViewModel(navigationManager)
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
