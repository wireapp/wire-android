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

import com.wire.navigation.WireNavEntryId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AuthenticationRoutesTest {

    @Test
    fun givenNewLoginStartFactory_whenCreatingRoute_thenEntryAndFlowOwnershipMatch() {
        val route = NewLoginRoute.start()

        assertEquals(route.entryId.value, route.flowId)
    }

    @Test
    fun givenLegacyAuthenticationRoots_whenCreatingRoutes_thenEntryStartsFlowOwnership() {
        val welcome = WelcomeRoute(entryId = WireNavEntryId("welcome-entry"))
        val login = LoginRoute(entryId = WireNavEntryId("login-entry"))

        assertEquals("welcome-entry", welcome.flowId)
        assertEquals("login-entry", login.flowId)
    }

    @Test
    fun givenAuthenticationRoutes_whenReadingRouteIds_thenGeneratedBaseRoutesArePreserved() {
        assertEquals("app/welcome_chooser_screen", WelcomeChooserRoute.ROUTE_ID)
        assertEquals("app/new_welcome_empty_start_screen", NewWelcomeEmptyStartRoute.ROUTE_ID)
        assertEquals("app/welcome_screen", WelcomeRoute.ROUTE_ID)
        assertEquals("app/login_screen", LoginRoute.ROUTE_ID)
        assertEquals("app/new_login_screen", NewLoginRoute.ROUTE_ID)
        assertEquals("app/new_login_password_screen", NewLoginPasswordRoute.ROUTE_ID)
        assertEquals(
            "app/new_login_verification_code_screen",
            NewLoginVerificationCodeRoute.ROUTE_ID,
        )
    }

    @Test
    fun givenSameAuthenticationDestination_whenCreatingTwoRoutes_thenEntryIdsAreDifferent() {
        val first = LoginRoute()
        val second = LoginRoute()

        assertEquals(first.routeId, second.routeId)
        assertNotEquals(first.entryId, second.entryId)
    }

    @Test
    fun givenNewLoginRoute_whenSerializedAndRestored_thenArgumentsAndOwnershipArePreserved() {
        val route = NewLoginRoute(
            args = completeAuthenticationLoginArguments(),
            flowId = "new-login-flow",
            entryId = WireNavEntryId("new-login-entry"),
        )

        val restored = Json.decodeFromString<NewLoginRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("new-login-flow", restored.flowId)
        assertEquals("new-login-entry", restored.entryId.value)
        assertEquals("app/new_login_screen", restored.routeId)
    }

    @Test
    fun givenBlankFlowId_whenCreatingNewLoginRoute_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NewLoginRoute(flowId = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            NewLoginPasswordRoute(
                args = AuthenticationLoginArguments(),
                flowId = " ",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            NewLoginVerificationCodeRoute(
                args = AuthenticationLoginArguments(),
                flowId = "",
            )
        }
    }

    @Test
    fun givenBlankClaimedDomain_whenCreatingTypedLoginArguments_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationDomainClaim.Claimed("")
        }
    }

    private fun completeAuthenticationLoginArguments() = AuthenticationLoginArguments(
        userHandle = AuthenticationPrefilledUserIdentifier(
            value = "alice@example.com",
            editable = true,
        ),
        ssoLoginResult = AuthenticationSsoLoginResult.Success(
            cookie = "cookie",
            serverConfigId = "backend-id",
        ),
        loginPasswordPath = AuthenticationLoginPasswordPath(
            customServerConfig = SERVER_LINKS,
            isCloudAccountCreationPossible = false,
            domainClaim = AuthenticationDomainClaim.Claimed("example.com"),
        ),
        ssoCodeAutoLogin = AuthenticationSsoCodeAutoLogin(
            ssoCode = "sso-code",
            autoInitiateLogin = false,
            nomadServiceUrl = "https://nomad.example.com",
            cookieLabel = "wire-cookie",
        ),
        showBackendConfigSuccess = true,
    )

    private companion object {
        val SERVER_LINKS = AuthenticationServerLinks(
            api = "https://api.example.com",
            accounts = "https://accounts.example.com",
            webSocket = "wss://websocket.example.com",
            blackList = "https://blacklist.example.com",
            teams = "https://teams.example.com",
            website = "https://www.example.com",
            title = "Example",
            isOnPremises = true,
            apiProxy = AuthenticationApiProxy(
                needsAuthentication = true,
                host = "proxy.example.com",
                port = 8443,
            ),
            supportEmail = "support@example.com",
        )
    }
}
