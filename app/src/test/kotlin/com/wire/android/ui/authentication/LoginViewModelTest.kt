package com.wire.android.ui.authentication

import androidx.compose.material.ExperimentalMaterialApi
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.configuration.ServerConfig
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class LoginViewModelTest {

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

    private lateinit var loginViewModel: LoginViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        loginViewModel = LoginViewModel(navigationManager, clientScopeProviderFactory)
    }

    @Test
    fun `given a navigation, when navigating back, then should delegate call to navigation manager back`() {
        coEvery { navigationManager.navigateBack() } returns Unit
        loginViewModel.navigateBack()
        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }

    @Test
    fun `given updateServerConfig called, when ssoLoginResult is null & server config has a value, then result is same serverConfig`(){
        loginViewModel.serverConfig = ServerConfig.PRODUCTION
        loginViewModel.updateServerConfig(null, ServerConfig.STAGING)
        assertEquals(loginViewModel.serverConfig, ServerConfig.STAGING)
    }

    @Test
    fun `given updateServerConfig called, when ssoLoginResult & server config have values, then result is same staging`(){
        loginViewModel.updateServerConfig(ssoLoginResult = DeepLinkResult.SSOLogin.Success("",""), ServerConfig.STAGING)
        assertEquals(loginViewModel.serverConfig, ServerConfig.STAGING)

        loginViewModel.updateServerConfig(ssoLoginResult = DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.ServerError), ServerConfig.STAGING)
        assertEquals(loginViewModel.serverConfig, ServerConfig.STAGING)
    }

}
