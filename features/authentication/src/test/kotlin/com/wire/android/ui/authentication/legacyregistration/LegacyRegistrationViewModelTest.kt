package com.wire.android.ui.authentication.legacyregistration

import com.wire.android.ui.authentication.legacyregistration.code.LegacyCodeActivationResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegisterClientResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeGateway
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeInput
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeState
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeViewModel
import com.wire.android.ui.authentication.legacyregistration.code.LegacyPersonalRegistrationRequest
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyStoreSessionResult
import com.wire.android.ui.authentication.legacyregistration.details.LegacyActivationCodeResult
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsGateway
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsInput
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsState
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsViewModel
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyRegistrationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `details validates passwords then requests an activation code`() = runTest(dispatcher) {
        val gateway = DetailsGateway()
        val viewModel = LegacyRegistrationDetailsViewModel(
            LegacyRegistrationDetailsInput<String>(null, "user@wire.test"),
            "default",
            gateway,
            analyticsWarmupMillis = 0,
        )
        fillDetails(viewModel, "bad", "different")
        advanceUntilIdle()
        viewModel.onDetailsContinue()
        advanceUntilIdle()
        assertEquals(LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError, viewModel.state.error)

        fillDetails(viewModel, "valid-password", "valid-password")
        viewModel.onDetailsContinue()
        advanceUntilIdle()
        assertTrue(viewModel.state.termsDialogVisible)
        viewModel.onTermsAccept()
        advanceUntilIdle()
        assertTrue(viewModel.state.success)
        assertEquals("user@wire.test", gateway.activationEmail)
    }

    @Test
    fun `code registration stores session before terminal client outcomes`() = runTest(dispatcher) {
        val gateway = CodeGateway(clientResult = LegacyRegisterClientResult.TooManyDevices)
        val viewModel = LegacyRegistrationCodeViewModel(
            LegacyRegistrationCodeInput<String>(null, "user@wire.test", "Jane", "password"),
            "default",
            gateway,
        )
        advanceUntilIdle()
        viewModel.codeTextState.edit { append("123456") }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
        assertEquals(LegacyRegistrationCodeState.Result.TooManyDevices("user-id"), viewModel.state.result)
        assertEquals(listOf("register", "store", "client"), gateway.calls)
    }

    @Test
    fun `code registration treats e2ei certificate requirement as successful`() = runTest(dispatcher) {
        val gateway = CodeGateway(clientResult = LegacyRegisterClientResult.E2EICertificateRequired)
        val viewModel = LegacyRegistrationCodeViewModel(
            LegacyRegistrationCodeInput<String>(null, "user@wire.test", "Jane", "password"),
            "default",
            gateway,
        )
        advanceUntilIdle()
        viewModel.codeTextState.edit { append("123456") }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
        assertEquals(LegacyRegistrationCodeState.Result.Success("user-id"), viewModel.state.result)
    }

    private fun fillDetails(viewModel: LegacyRegistrationDetailsViewModel<String, String>, password: String, confirm: String) {
        viewModel.nameTextState.edit { append("Jane") }
        viewModel.passwordTextState.edit { replace(0, length, password) }
        viewModel.confirmPasswordTextState.edit { replace(0, length, confirm) }
    }

    private class DetailsGateway : LegacyRegistrationDetailsGateway<String, String> {
        var activationEmail: String? = null
        override fun isPasswordValid(password: String) = password == "valid-password"
        override fun isEmailValid(email: String) = email.contains('@')
        override suspend fun requestActivationCode(serverConfig: String, email: String): LegacyActivationCodeResult<String> {
            activationEmail = email
            return LegacyActivationCodeResult.Sent
        }
        override suspend fun setAnonymousRegistrationEnabled(enabled: Boolean) = Unit
        override suspend fun onAccountSetup(withPasswordTries: Boolean) = Unit
        override suspend fun onTermsOfUseDialog() = Unit
    }

    private class CodeGateway(
        private val clientResult: LegacyRegisterClientResult<String>,
    ) : LegacyRegistrationCodeGateway<String, String, String, String> {
        val calls = mutableListOf<String>()
        override suspend fun requestActivationCode(serverConfig: String, email: String) = LegacyCodeActivationResult.Sent
        override suspend fun register(
            serverConfig: String,
            request: LegacyPersonalRegistrationRequest,
        ): LegacyRegistrationResult<String, String> {
            calls += "register"
            return LegacyRegistrationResult.Success("credentials")
        }
        override suspend fun storeSession(credentials: String): LegacyStoreSessionResult<String, String> {
            calls += "store"
            return LegacyStoreSessionResult.Success("user-id")
        }
        override suspend fun registerClient(userId: String, password: String): LegacyRegisterClientResult<String> {
            calls += "client"
            return clientResult
        }
        override suspend fun onCodeVerificationShown() = Unit
        override suspend fun onCodeVerificationFailed() = Unit
    }
}
