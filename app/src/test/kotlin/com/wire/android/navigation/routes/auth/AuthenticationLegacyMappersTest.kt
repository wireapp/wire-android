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

package com.wire.android.navigation.routes.auth

import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.SSOCodeAutoLogin
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.navigation.WireNavEntryId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class AuthenticationLegacyMappersTest {

    @Test
    fun givenLegacyLoginArguments_whenMappedToTypedAndBack_thenEveryValueIsPreserved() {
        val legacy = completeLegacyLoginArguments()

        val restored = legacy.toAuthenticationArguments().toLegacy()

        assertEquals(legacy, restored)
    }

    @Test
    fun givenEveryLegacySsoFailure_whenMappedToTypedAndBack_thenFailureIsPreserved() {
        SSOFailureCodes.values().forEach { failure ->
            val legacy = LoginNavArgs(
                ssoLoginResult = DeepLinkResult.SSOLogin.Failure(failure)
            )

            assertEquals(legacy, legacy.toAuthenticationArguments().toLegacy())
        }
    }

    @Test
    fun givenLegacyLoginArguments_whenCreatingEachLoginRoute_thenRouteMappersRoundTrip() {
        val legacy = completeLegacyLoginArguments()

        assertEquals(
            legacy,
            legacy.toLoginRoute(WireNavEntryId("legacy-login-entry")).toLegacyNavArgs(),
        )
        assertEquals(
            legacy,
            legacy.toNewLoginRoute(
                flowId = "new-login-flow",
                entryId = WireNavEntryId("new-login-entry"),
            ).toLegacyNavArgs(),
        )
        assertEquals(
            legacy,
            legacy.toNewLoginVerificationCodeRoute(
                flowId = "new-login-flow",
                entryId = WireNavEntryId("verification-entry"),
            ).toLegacyNavArgs(),
        )
        assertEquals(
            "shared-legacy-flow",
            legacy.toLoginRoute(
                entryId = WireNavEntryId("legacy-login-entry"),
                flowId = "shared-legacy-flow",
            ).flowId,
        )
    }

    @Test
    fun givenRepeatedPasswordAttempts_whenCreatingRoutes_thenEachAttemptGetsAnIsolatedFlowOwner() {
        val legacy = completeLegacyLoginArguments()

        val firstAttempt = legacy.toNewLoginPasswordAttemptRoute(
            entryId = WireNavEntryId("password-attempt-one"),
        )
        val secondAttempt = legacy.toNewLoginPasswordAttemptRoute(
            entryId = WireNavEntryId("password-attempt-two"),
        )

        assertEquals("new-login-password:password-attempt-one", firstAttempt.flowId)
        assertEquals("new-login-password:password-attempt-two", secondAttempt.flowId)
        assertNotEquals(firstAttempt.flowId, secondAttempt.flowId)
    }

    @Test
    fun givenPasswordAttempt_whenOpeningVerification_thenVerificationKeepsAttemptOwner() {
        val passwordAttempt = completeLegacyLoginArguments().toNewLoginPasswordAttemptRoute(
            entryId = WireNavEntryId("password-attempt"),
        )

        val verification = passwordAttempt.toLegacyNavArgs()
            .toNewLoginVerificationCodeRoute(passwordAttempt.flowId)

        assertEquals(passwordAttempt.flowId, verification.flowId)
    }

    private fun completeLegacyLoginArguments() = LoginNavArgs(
        userHandle = PreFilledUserIdentifierType.PreFilled(
            userIdentifier = "alice@example.com",
            editable = true,
        ),
        ssoLoginResult = DeepLinkResult.SSOLogin.Success(
            cookie = "cookie",
            serverConfigId = "backend-id",
        ),
        loginPasswordPath = LoginPasswordPath(
            customServerConfig = SERVER_LINKS,
            isCloudAccountCreationPossible = false,
            isDomainClaimedByOrg = DomainClaimedByOrg.Claimed("example.com"),
        ),
        ssoCodeAutoLogin = SSOCodeAutoLogin(
            ssoCode = "sso-code",
            autoInitiateLogin = false,
            nomadServiceUrl = "https://nomad.example.com",
            cookieLabel = "wire-cookie",
        ),
        showBackendConfigSuccess = true,
    )

    private companion object {
        val SERVER_LINKS = ServerConfig.Links(
            api = "https://api.example.com",
            accounts = "https://accounts.example.com",
            webSocket = "wss://websocket.example.com",
            blackList = "https://blacklist.example.com",
            teams = "https://teams.example.com",
            website = "https://www.example.com",
            title = "Example",
            isOnPremises = true,
            apiProxy = ServerConfig.ApiProxy(
                needsAuthentication = true,
                host = "proxy.example.com",
                port = 8443,
            ),
            supportEmail = "support@example.com",
        )
    }
}
