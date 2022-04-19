package com.wire.android.ui.authentication.login.sso

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    private lateinit var serverConfig: ServerConfig
    @MockK
    private lateinit var ssoInitiateLoginUseCase: SSOInitiateLoginUseCase

    private lateinit var loginViewModel: LoginSSOViewModel

    private val apiBaseUrl: String = "apiBaseUrl"

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns ""
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        every { serverConfig.apiBaseUrl } returns apiBaseUrl
        every { serverConfig.id } returns "0"
        loginViewModel = LoginSSOViewModel(savedStateHandle, ssoInitiateLoginUseCase)
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
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Success("")

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

    @Test
    fun `given button is clicked, when login returns Success, then open the web url from the response`() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val ssoCode = "wire-fd994b20-b9af-11ec-ae36-00163e9b33ca"
        val param = SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode, serverConfig)
        val url = "https://wire.com/sso"
        coEvery { ssoInitiateLoginUseCase.invoke(param) } returns SSOInitiateLoginResult.Success(url)
        loginViewModel.onSSOCodeChange(TextFieldValue(ssoCode))
        runTest {
            loginViewModel.login(serverConfig)
            loginViewModel.openWebUrl.first() shouldBe url
        }
        coVerify(exactly = 1) { ssoInitiateLoginUseCase.invoke(param) }
    }

    @Test
    fun `given button is clicked, when login returns InvalidCode error, then InvalidCodeError is passed`() {
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.InvalidCode
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginSSOError.TextFieldError.InvalidCodeError::class
    }

    @Test
    fun `given button is clicked, when login returns InvalidRequest error, then GenericError IllegalArgument is passed`() {
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.InvalidRedirect
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginSSOError.DialogError.GenericError::class
        with(loginViewModel.loginState.loginSSOError as LoginSSOError.DialogError.GenericError) {
            coreFailure shouldBeInstanceOf CoreFailure.Unknown::class
            with(coreFailure as CoreFailure.Unknown) {
                this.rootCause shouldBeInstanceOf IllegalArgumentException::class
            }
        }
    }

    @Test
    fun `given button is clicked, when login returns Generic error, then GenericError is passed`() {
        coEvery { ssoInitiateLoginUseCase.invoke(any()) } returns SSOInitiateLoginResult.Failure.Generic(NetworkFailure.NoNetworkConnection)
        runTest { loginViewModel.login(serverConfig) }
        loginViewModel.loginState.loginSSOError shouldBeInstanceOf LoginSSOError.DialogError.GenericError::class
        (loginViewModel.loginState.loginSSOError as LoginSSOError.DialogError.GenericError).coreFailure shouldBe
                NetworkFailure.NoNetworkConnection
    }
}

