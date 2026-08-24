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

package com.wire.android.ui.authentication.devices.register

import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.navigation.WireSessionId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterDeviceGatewayAdapterTest {

    @Test
    fun `register request preserves exact Kalium parameters and private model postfix`() = runTest {
        val arrangement = Arrangement(
            buildFlags = RegisterDeviceBuildFlags(privateBuild = true, flavor = "internal", buildType = "debug")
        )
        val parameter = slot<RegisterClientParam>()
        coEvery { arrangement.registerClient(capture(parameter)) } returns RegisterClientResult.Failure.PasswordAuthRequired

        arrangement.gateway.registerClient(RegisterDeviceRequest("password", "123456"))

        assertEquals(
            RegisterClientParam(
                password = "password",
                capabilities = null,
                secondFactorVerificationCode = "123456",
                modelPostfix = " [internal_debug]",
            ),
            parameter.captured,
        )
    }

    @Test
    fun `public build omits model postfix`() = runTest {
        val arrangement = Arrangement(
            buildFlags = RegisterDeviceBuildFlags(privateBuild = false, flavor = "prod", buildType = "release")
        )
        val parameter = slot<RegisterClientParam>()
        coEvery { arrangement.registerClient(capture(parameter)) } returns RegisterClientResult.Failure.PasswordAuthRequired

        arrangement.gateway.registerClient(RegisterDeviceRequest(null, null))

        assertNull(parameter.captured.modelPostfix)
    }

    @Test
    fun `password requirement maps required not-required and failure`() = runTest {
        val arrangement = Arrangement()

        coEvery { arrangement.isPasswordRequired() } returns IsPasswordRequiredUseCase.Result.Success(true)
        assertEquals(PasswordRequirement.Required, arrangement.gateway.passwordRequirement())

        coEvery { arrangement.isPasswordRequired() } returns IsPasswordRequiredUseCase.Result.Success(false)
        assertEquals(PasswordRequirement.NotRequired, arrangement.gateway.passwordRequirement())

        coEvery { arrangement.isPasswordRequired() } returns
            IsPasswordRequiredUseCase.Result.Failure(StorageFailure.DataNotFound)
        assertEquals(
            PasswordRequirement.Failure(AuthenticationFailure.Unknown),
            arrangement.gateway.passwordRequirement(),
        )
    }

    @Test
    fun `register success mappings include initial sync and E2EI session identity`() = runTest {
        val arrangement = Arrangement(initialSyncCompleted = true)
        coEvery { arrangement.registerClient(any()) } returns RegisterClientResult.Success(TestClient.CLIENT)
        assertEquals(
            RegisterDeviceResult.Success<WireSessionId>(initialSyncCompleted = true, isE2EIRequired = false),
            arrangement.gateway.registerClient(RegisterDeviceRequest(null, null)),
        )

        coEvery { arrangement.registerClient(any()) } returns RegisterClientResult.E2EICertificateRequired(
            TestClient.CLIENT,
            TestUser.SELF_USER_ID,
        )
        assertEquals(
            RegisterDeviceResult.Success<WireSessionId>(
                initialSyncCompleted = true,
                isE2EIRequired = true,
                e2eiSessionId = WireSessionId(TestUser.SELF_USER_ID.value, TestUser.SELF_USER_ID.domain),
            ),
            arrangement.gateway.registerClient(RegisterDeviceRequest(null, null)),
        )
    }

    @Test
    fun `every structured Kalium register failure maps to its feature result`() = runTest {
        val arrangement = Arrangement()
        val mappings = listOf(
            RegisterClientResult.Failure.TooManyClients to RegisterDeviceResult.TooManyDevices,
            RegisterClientResult.Failure.InvalidCredentials.Missing2FA to RegisterDeviceResult.MissingSecondFactor,
            RegisterClientResult.Failure.InvalidCredentials.Invalid2FA to RegisterDeviceResult.InvalidSecondFactor,
            RegisterClientResult.Failure.InvalidCredentials.InvalidPassword to RegisterDeviceResult.InvalidCredentials,
            RegisterClientResult.Failure.PasswordAuthRequired to RegisterDeviceResult.PasswordRequired,
        )

        mappings.forEach { (kaliumResult, featureResult) ->
            coEvery { arrangement.registerClient(any()) } returns kaliumResult
            assertEquals(
                featureResult,
                arrangement.gateway.registerClient(RegisterDeviceRequest("password", null)),
            )
        }
    }

    @Test
    fun `generic Kalium register failures map every dialog classification`() = runTest {
        val arrangement = Arrangement()

        failureMappings().forEach { (coreFailure, authenticationFailure) ->
            coEvery { arrangement.registerClient(any()) } returns RegisterClientResult.Failure.Generic(coreFailure)
            assertEquals(
                RegisterDeviceResult.Failure(authenticationFailure),
                arrangement.gateway.registerClient(RegisterDeviceRequest(null, null)),
            )
        }
    }

    @Test
    fun `verification success and too-many preserve email and login-registration action`() = runTest {
        val arrangement = Arrangement()
        coEvery { arrangement.getSelfUser() } returns TestUser.SELF_USER.copy(email = "member@example.com")

        coEvery { arrangement.requestVerificationCode(any(), any()) } returns
            RequestSecondFactorVerificationCodeUseCase.Result.Success
        assertEquals(
            RequestVerificationCodeResult.Sent("member@example.com"),
            arrangement.gateway.requestVerificationCode(),
        )

        coEvery { arrangement.requestVerificationCode(any(), any()) } returns
            RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests
        assertEquals(
            RequestVerificationCodeResult.TooManyRequests("member@example.com"),
            arrangement.gateway.requestVerificationCode(),
        )

        coVerify(exactly = 2) {
            arrangement.requestVerificationCode(
                "member@example.com",
                VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION,
            )
        }
    }

    @Test
    fun `verification generic failures map every dialog classification`() = runTest {
        val arrangement = Arrangement()

        failureMappings().forEach { (coreFailure, authenticationFailure) ->
            coEvery { arrangement.requestVerificationCode(any(), any()) } returns
                RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic(coreFailure)
            assertEquals(
                RequestVerificationCodeResult.Failure(authenticationFailure),
                arrangement.gateway.requestVerificationCode(),
            )
        }
    }

    @Test
    fun `missing self or missing email maps without making a verification request`() = runTest {
        val arrangement = Arrangement()

        coEvery { arrangement.getSelfUser() } returns null
        assertEquals(RequestVerificationCodeResult.MissingEmail, arrangement.gateway.requestVerificationCode())

        coEvery { arrangement.getSelfUser() } returns TestUser.SELF_USER.copy(email = null)
        assertEquals(RequestVerificationCodeResult.MissingEmail, arrangement.gateway.requestVerificationCode())

        coVerify(exactly = 0) { arrangement.requestVerificationCode(any(), any()) }
    }

    @Test
    fun `Android resend timer delegates duration updates and finish`() = runTest {
        val countdownTimer = mockk<CountdownTimer>()
        val updates = mutableListOf<String>()
        var finished = false
        coEvery { countdownTimer.start(300L, any(), any()) } coAnswers {
            secondArg<(String) -> Unit>().invoke("04:59")
            thirdArg<() -> Unit>().invoke()
        }

        AndroidRegisterDeviceResendTimer(countdownTimer).start(
            seconds = 300L,
            onUpdate = updates::add,
            onFinish = { finished = true },
        )

        assertEquals(listOf("04:59"), updates)
        assertTrue(finished)
    }

    private fun failureMappings(): List<Pair<CoreFailure, AuthenticationFailure>> = listOf(
        NetworkFailure.NoNetworkConnection(null) to AuthenticationFailure.NoNetwork,
        NetworkFailure.ServerMiscommunication(IllegalStateException("server")) to
            AuthenticationFailure.ServerMiscommunication,
        CoreFailure.Unknown(IllegalStateException("unknown")) to AuthenticationFailure.Unknown,
    )

    private class Arrangement(
        initialSyncCompleted: Boolean = false,
        buildFlags: RegisterDeviceBuildFlags = RegisterDeviceBuildFlags(false, "prod", "release"),
    ) {
        val registerClient = mockk<GetOrRegisterClientUseCase>()
        val isPasswordRequired = mockk<IsPasswordRequiredUseCase>()
        val userDataStore = mockk<UserDataStore>()
        val getSelfUser = mockk<GetSelfUserUseCase>()
        val requestVerificationCode = mockk<RequestSecondFactorVerificationCodeUseCase>()

        val gateway = KaliumRegisterDeviceGateway(
            registerClient = registerClient,
            isPasswordRequired = isPasswordRequired,
            userDataStore = userDataStore,
            getSelfUser = getSelfUser,
            requestSecondFactorVerificationCode = requestVerificationCode,
            buildFlags = buildFlags,
        )

        init {
            coEvery { isPasswordRequired() } returns IsPasswordRequiredUseCase.Result.Success(true)
            coEvery { registerClient(any()) } returns RegisterClientResult.Failure.PasswordAuthRequired
            coEvery { getSelfUser() } returns TestUser.SELF_USER
            coEvery { requestVerificationCode(any(), any()) } returns
                RequestSecondFactorVerificationCodeUseCase.Result.Success
            io.mockk.every { userDataStore.initialSyncCompleted } returns flowOf(initialSyncCompleted)
        }
    }
}
