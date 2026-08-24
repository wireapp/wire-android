/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.auth

import com.wire.android.ui.newauthentication.login.NewLoginAction
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationNavigation3EntriesTest {

    @Test
    fun givenAuthenticationContribution_whenInspectingEntries_thenAllSevenRoutesAreRegistered() {
        val source = sourceFile("AuthenticationNavigation3Entries.kt").readText()

        listOf(
            "wireEntry<WelcomeChooserRoute>",
            "wireEntry<NewWelcomeEmptyStartRoute>",
            "wireEntry<WelcomeRoute>",
            "wireEntry<LoginRoute>",
            "wireEntry<NewLoginRoute>",
            "wireEntry<NewLoginPasswordRoute>",
            "wireEntry<NewLoginVerificationCodeRoute>",
        ).forEach { registration ->
            assertTrue(source.contains(registration), "Missing $registration")
        }
    }

    @Test
    fun givenAuthenticationEntries_whenInspectingSource_thenTheyDoNotDependOnLegacyNavigationRuntime() {
        val source = sourceFile("AuthenticationNavigation3Entries.kt").readText()

        assertFalse(source.contains("ScreenDestination"))
        assertFalse(source.contains("com.ramcosta.composedestinations"))
        assertFalse(source.contains("NavController"))
        assertFalse(source.contains("SavedStateHandle"))
        assertFalse(source.contains("Bundle"))
        assertFalse(source.contains("navArgs()"))
    }

    @Test
    fun givenArgumentBackedAuthenticationViewModels_whenInspectingSources_thenTypedArgsUseFocusedHostFactories() {
        val graph = sourceFile(
            "../../../ui/authentication/AuthenticationViewModelGraph.kt"
        ).readText()
        val bindings = sourceFile(
            "../../../di/metro/AuthenticationMetroViewModelBindings.kt"
        ).readText()
        val welcomeViewModel = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/welcome/WelcomeViewModel.kt",
        ).readText()
        val welcomeHostFactory = sourceFile(
            "../../../ui/authentication/welcome/WelcomeViewModelHostFactory.kt"
        ).readText()
        val newLoginViewModel = sourceFile(
            "../../../ui/newauthentication/login/NewLoginViewModel.kt"
        ).readText()
        val loginSsoViewModel = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/login/sso/LoginSSOViewModel.kt",
        ).readText()
        val loginSsoHostFactory = sourceFile(
            "../../../ui/authentication/login/sso/LoginSSOViewModelHostFactory.kt"
        ).readText()
        val loginEmailViewModel = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/login/email/LoginEmailViewModel.kt",
        ).readText()
        val loginEmailHostFactory = sourceFile(
            "../../../ui/authentication/login/email/LoginEmailViewModelHostFactory.kt"
        ).readText()
        val entries = sourceFile("AuthenticationNavigation3Entries.kt").readText()

        assertTrue(graph.contains("fun welcomeViewModel(navArgs: WelcomeNavArgs): WelcomeViewModel<ServerConfig.Links>"))
        assertTrue(graph.contains("fun newLoginViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras)"))
        assertTrue(bindings.contains("WelcomeViewModelHostFactory"))
        assertTrue(bindings.contains("NewLoginViewModel.Factory"))
        assertTrue(bindings.contains("LoginSSOViewModelHostFactory"))
        assertFalse(bindings.contains("LoginSSOViewModel.Factory"))
        assertTrue(bindings.contains("LoginEmailViewModelHostFactory"))
        assertFalse(bindings.contains("LoginEmailViewModel.Factory"))
        assertTrue(bindings.contains("welcomeFactory.create(navArgs)"))
        assertTrue(bindings.contains("newLoginFactory.create(loginNavArgs, extras.createSavedStateHandle())"))
        assertTrue(welcomeViewModel.contains("class WelcomeViewModel<LinksT>"))
        assertFalse(welcomeViewModel.contains("AssistedInject"))
        assertTrue(welcomeHostFactory.contains("fun create(navArgs: WelcomeNavArgs): WelcomeViewModel<ServerConfig.Links>"))
        assertTrue(newLoginViewModel.contains("fun create(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle)"))
        assertTrue(loginSsoViewModel.contains("class LoginSSOViewModel<LinksT, FailureT, UserT, SsoFailureT, SessionT>"))
        assertTrue(loginSsoHostFactory.contains("fun create(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle)"))
        assertTrue(loginEmailViewModel.contains("class LoginEmailViewModel<LinksT, FailureT, UserT, ScopeT, SessionT, BackendRequestT, DomainClaimT>"))
        assertFalse(loginEmailViewModel.contains("LoginNavArgs"))
        assertFalse(loginEmailViewModel.contains("ServerConfig"))
        assertTrue(loginEmailHostFactory.contains("fun create(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle)"))
        assertTrue(entries.contains("welcomeViewModel(route.toLegacyNavArgs(), flowOwner)"))
        assertTrue(entries.contains("newLoginViewModel(legacyArgs, flowOwner)"))
        assertTrue(entries.contains("WireViewModelOwner.Flow(route.flowId)"))
        assertTrue(entries.contains("NewLoginVerificationCodeRouteScreen"))
        assertTrue(Regex("WireViewModelOwner\\.Flow\\(route\\.flowId\\)").findAll(entries).count() >= 5)
        assertFalse(entries.contains("openVerificationCode"))
    }

    @Test
    fun givenWelcomeChooserEntry_whenBuildingNewLoginFlowId_thenItIsStableAndEntrySpecific() {
        val route = WelcomeChooserRoute(entryId = WireNavEntryId("chooser-entry"))

        assertEquals("new-login:chooser-entry", route.newLoginFlowId())
        assertEquals(route.newLoginFlowId(), route.newLoginFlowId())
    }

    @Test
    fun givenNewLoginCompletionSteps_whenMappingToHostBoundary_thenTheirMeaningIsPreserved() {
        val userId = com.wire.kalium.logic.data.user.UserId("user", "wire.test")
        val sessionId = WireSessionId("user", "wire.test")
        assertEquals(
            AuthenticationLoginCompletion.Home(sessionId),
            NewLoginAction.Success.NextStep.None(userId).toAuthenticationLoginCompletion(),
        )
        assertEquals(
            AuthenticationLoginCompletion.InitialSync(sessionId),
            NewLoginAction.Success.NextStep.InitialSync(userId).toAuthenticationLoginCompletion(),
        )
    }

    private fun sourceFile(name: String): File {
        return File(
            repositoryRoot(),
            "app/src/main/kotlin/com/wire/android/navigation/routes/auth/$name",
        ).also {
            assertTrue(it.isFile, "Missing source file $name")
        }
    }

    private fun repositoryRoot(): File =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
}
