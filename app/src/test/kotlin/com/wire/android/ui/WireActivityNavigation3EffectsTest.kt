/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui

import com.wire.android.navigation.routes.auth.AuthenticationPrefilledUserIdentifier
import com.wire.android.navigation.routes.auth.AuthenticationSsoLoginResult
import com.wire.android.navigation.routes.auth.LoginRoute
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.android.navigation.routes.media.AuthenticatedImportMediaRoute
import com.wire.android.navigation.routes.media.LoggedOutImportMediaRoute
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireActivityNavigation3EffectsTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun givenAuthorizationNeededOnEmptyWelcome_whenResolving_thenLoginClearsStackAndToastIsShown() {
        val result = resolve(OnAuthorizationNeeded, listOf(NewWelcomeEmptyStartRoute()))
        val command = result.singleCommand()

        assertInstanceOf(NewLoginRoute::class.java, command.destination)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
        assertEquals(com.wire.android.R.string.deeplink_authorization_needed, result.toastMessageResId)
    }

    @Test
    fun givenMigrationLogin_whenNewLoginIsEnabled_thenHandleAndReplacementModeAreTyped() {
        val result = resolve(
            OnMigrationLogin(DeepLinkResult.MigrationLogin("alice")),
            listOf(WelcomeRoute()),
        )
        val command = result.singleCommand()
        val route = assertInstanceOf(NewLoginRoute::class.java, command.destination)

        assertEquals(
            AuthenticationPrefilledUserIdentifier("alice"),
            route.args.userHandle,
        )
        assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
    }

    @Test
    fun givenAutomaticLegacyLogin_whenResolving_thenSsoAndUpdateModeArePreserved() {
        val result = resolve(
            OnAutomaticLogin(
                serverLinks = null,
                ssoCode = "code",
                nomadServiceUrl = "https://nomad.example",
                cookieLabel = "cookie",
                useNewLogin = false,
            ),
            listOf(HomeRoute(session)),
        )
        val command = result.singleCommand()
        val route = assertInstanceOf(LoginRoute::class.java, command.destination)

        assertEquals("code", route.args.ssoCodeAutoLogin?.ssoCode)
        assertTrue(route.args.ssoCodeAutoLogin?.autoInitiateLogin == true)
        assertEquals(WireBackStackMode.UPDATE_EXISTING, command.backStackMode)
    }

    @Test
    fun givenCustomBackendNewLogin_whenResolving_thenConfigAndSuccessFlagArePreserved() {
        val result = resolve(
            OnCustomBackendLogin(serverLinks(), useNewLogin = true),
            listOf(HomeRoute(session)),
        )
        val route = assertInstanceOf(
            NewLoginRoute::class.java,
            result.singleCommand().destination,
        )

        assertEquals("https://api.example", route.args.loginPasswordPath?.customServerConfig?.api)
        assertTrue(route.args.showBackendConfigSuccess)
    }

    @Test
    fun givenCustomBackendResultOnLoginRoot_whenResolving_thenEntryAndFlowArePreserved() {
        val current = NewLoginRoute(
            flowId = "custom-backend:owner",
            entryId = WireNavEntryId("custom-backend-entry"),
        )

        val result = resolve(
            OnCustomBackendLogin(serverLinks(), useNewLogin = true),
            listOf(current),
        )
        val replacement = assertInstanceOf(
            WireActivityNavigation3Mutation.ReplaceCurrent::class.java,
            result.mutations.single(),
        ).route as NewLoginRoute

        assertEquals(current.entryId, replacement.entryId)
        assertEquals(current.flowId, replacement.flowId)
        assertEquals("https://api.example", replacement.args.loginPasswordPath?.customServerConfig?.api)
        assertTrue(replacement.args.showBackendConfigSuccess)
    }

    @Test
    fun givenSsoResultOnNewLogin_whenResolving_thenCurrentEntryAndFlowOwnersArePreserved() {
        val current = NewLoginRoute(
            flowId = "new-login:owner",
            entryId = WireNavEntryId("owner"),
        )
        val result = resolve(
            OnSSOLogin(DeepLinkResult.SSOLogin.Success("cookie", "server")),
            listOf(current),
        )
        val replacement = assertInstanceOf(
            WireActivityNavigation3Mutation.ReplaceCurrent::class.java,
            result.mutations.single(),
        ).route as NewLoginRoute

        assertEquals(current.entryId, replacement.entryId)
        assertEquals(current.flowId, replacement.flowId)
        assertEquals(
            AuthenticationSsoLoginResult.Success("cookie", "server"),
            replacement.args.ssoLoginResult,
        )
    }

    @Test
    fun givenConversationAfterAccountSwitch_whenResolving_thenHomeIsClearedBeforeTypedConversationUpdate() {
        val targetSession = QualifiedID("switched-user", "wire.example")
        val result = resolve(
            OpenConversation(
                DeepLinkResult.OpenConversation(
                    conversationId = QualifiedID("conversation", "wire.example"),
                    switchedAccount = true,
                    targetSessionId = targetSession,
                )
            ),
            listOf(HomeRoute(session)),
        )
        val commands = result.mutations.map {
            (it as WireActivityNavigation3Mutation.Navigate).command
        }

        val home = assertInstanceOf(HomeRoute::class.java, commands[0].destination)
        assertEquals(WireSessionId(targetSession.value, targetSession.domain), home.sessionId)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, commands[0].backStackMode)
        val conversation = assertInstanceOf(ConversationRoute::class.java, commands[1].destination)
        assertEquals(WireSessionId(targetSession.value, targetSession.domain), conversation.sessionId)
        assertEquals(WireBackStackMode.UPDATE_EXISTING, commands[1].backStackMode)
    }

    @Test
    fun givenProfileAfterAccountSwitch_whenResolving_thenTypedProfileUsesCurrentSession() {
        val targetSession = QualifiedID("switched-user", "wire.example")
        val result = resolve(
            OnOpenUserProfile(
                DeepLinkResult.OpenOtherUserProfile(
                    userId = QualifiedID("other", "wire.example"),
                    switchedAccount = true,
                    targetSessionId = targetSession,
                )
            ),
            listOf(HomeRoute(session)),
        )
        val commands = result.mutations.map {
            (it as WireActivityNavigation3Mutation.Navigate).command
        }

        assertInstanceOf(HomeRoute::class.java, commands[0].destination)
        val profile = assertInstanceOf(OtherUserProfileRoute::class.java, commands[1].destination)
        assertEquals(WireSessionId(targetSession.value, targetSession.domain), profile.sessionId)
        assertEquals("other", profile.targetUserId.value)
    }

    @Test
    fun givenImportMediaWithAndWithoutSession_whenResolving_thenScopeOwnerIsUnambiguous() {
        val authenticated = resolve(OnShowImportMediaScreen, listOf(HomeRoute(session)))
        val loggedOut = WireActivityNavigation3EffectResolver.resolve(
            OnShowImportMediaScreen,
            routes = listOf(NewWelcomeEmptyStartRoute()),
            currentSessionId = null,
            canUseNewLogin = true,
        )

        assertInstanceOf(AuthenticatedImportMediaRoute::class.java, authenticated.singleCommand().destination)
        assertInstanceOf(LoggedOutImportMediaRoute::class.java, loggedOut.singleCommand().destination)
    }

    @Test
    fun givenSsoFailureOutsideNewLogin_whenResolving_thenLegacyLoginIsUpdated() {
        val result = resolve(
            OnSSOLogin(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Forbidden)),
            listOf(HomeRoute(session)),
        )
        val command = result.singleCommand()
        val route = assertInstanceOf(LoginRoute::class.java, command.destination)

        assertEquals(
            AuthenticationSsoLoginResult.Failure(
                com.wire.android.navigation.routes.auth.AuthenticationSsoFailureCode.FORBIDDEN
            ),
            route.args.ssoLoginResult,
        )
        assertEquals(WireBackStackMode.UPDATE_EXISTING, command.backStackMode)
    }

    private fun resolve(
        action: WireActivityViewAction,
        routes: List<com.wire.navigation.WireRoute>,
    ) = WireActivityNavigation3EffectResolver.resolve(
        action = action,
        routes = routes,
        currentSessionId = session,
        canUseNewLogin = true,
    )

    private fun WireActivityNavigation3EffectResolution.singleCommand() =
        (mutations.single() as WireActivityNavigation3Mutation.Navigate).command

    private fun serverLinks() = ServerConfig.Links(
        api = "https://api.example",
        accounts = "https://accounts.example",
        webSocket = "wss://socket.example",
        blackList = "https://blacklist.example",
        teams = "https://teams.example",
        website = "https://wire.example",
        title = "Example",
        isOnPremises = true,
        apiProxy = null,
    )
}
