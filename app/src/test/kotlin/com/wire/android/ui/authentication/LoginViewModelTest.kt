package com.wire.android.ui.authentication

import androidx.compose.material.ExperimentalMaterialApi
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

    @MockK
    private lateinit var authServerConfigProvider: AuthServerConfigProvider

    private lateinit var loginViewModel: LoginViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { authServerConfigProvider.authServer.value } returns newServerConfig(1).links
        loginViewModel = LoginViewModel(navigationManager, clientScopeProviderFactory, authServerConfigProvider)
    }

    @Test
    fun `given a navigation, when navigating back, then should delegate call to navigation manager back`() {
        coEvery { navigationManager.navigateBack() } returns Unit
        loginViewModel.navigateBack()
        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }
}
