package com.wire.android.ui.authentication.login.sso

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.configuration.ServerConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class LoginSSOViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    @MockK
    private lateinit var navigationManager: NavigationManager
    @MockK
    private lateinit var serverConfig: ServerConfig

    private lateinit var loginViewModel: LoginSSOViewModel

    private val apiBaseUrl: String = "apiBaseUrl"

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns ""
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { serverConfig.apiBaseUrl } returns apiBaseUrl
        loginViewModel = LoginSSOViewModel(savedStateHandle, navigationManager)
    }

    @Test
    fun `given empty string, when entering code, then button is disabled`() {
        loginViewModel.onSSOCodeChange(TextFieldValue(String.EMPTY))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering code, then button is enabled`() {
        loginViewModel.onSSOCodeChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when logging in, then show loading`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))

        loginViewModel.onSSOCodeChange(TextFieldValue("abc"))
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
        loginViewModel.login(serverConfig)
        loginViewModel.loginState.loginEnabled shouldBeEqualTo false
        loginViewModel.loginState.loading shouldBeEqualTo true
        scheduler.advanceUntilIdle()
        loginViewModel.loginState.loginEnabled shouldBeEqualTo true
        loginViewModel.loginState.loading shouldBeEqualTo false
    }
}

