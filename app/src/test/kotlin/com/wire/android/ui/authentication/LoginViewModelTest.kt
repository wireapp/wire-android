package com.wire.android.ui.authentication

import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.UserSessionsUseCaseProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.util.newServerConfig
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var userSessionsUseCaseFactory: UserSessionsUseCaseProvider.Factory

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var authServerConfigProvider: AuthServerConfigProvider

    private lateinit var loginViewModel: LoginViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns ""
        every { authServerConfigProvider.authServer.value } returns newServerConfig(1).links
        loginViewModel = LoginViewModel(
            savedStateHandle,
            navigationManager,
            clientScopeProviderFactory,
            userSessionsUseCaseFactory,
            authServerConfigProvider
        )
    }

    @Test
    fun `given a navigation, when navigating back, then should delegate call to navigation manager back`() {
        coEvery { navigationManager.navigateBack() } returns Unit
        loginViewModel.navigateBack()
        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }
}
