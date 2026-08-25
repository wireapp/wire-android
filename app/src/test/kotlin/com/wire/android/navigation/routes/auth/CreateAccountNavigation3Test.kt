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

import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountNavigation3Test {

    @Test
    fun givenUsernameStartFactory_whenCreatingRoute_thenEntryAndFlowOwnershipMatch() {
        val route = CreateAccountUsernameRoute.start(WireSessionId("user", "wire.test"))

        assertEquals(route.entryId.value, route.flowId)
    }

    @Test
    fun givenCreateAccountRoutes_whenReadingRouteIds_thenGeneratedBaseRoutesArePreserved() {
        assertEquals("app/create_account_selector_screen", CreateAccountSelectorRoute.ROUTE_ID)
        assertEquals("app/create_account_data_detail_screen", CreateAccountDataDetailRoute.ROUTE_ID)
        assertEquals(
            "app/create_account_verification_code_screen",
            CreateAccountVerificationCodeRoute.ROUTE_ID,
        )
        assertEquals(
            "app/create_personal_account_overview_screen",
            CreatePersonalAccountOverviewRoute.ROUTE_ID,
        )
        assertEquals(
            "app/create_team_account_overview_screen",
            CreateTeamAccountOverviewRoute.ROUTE_ID,
        )
        assertEquals("app/create_account_email_screen", CreateAccountEmailRoute.ROUTE_ID)
        assertEquals("app/create_account_details_screen", CreateAccountDetailsRoute.ROUTE_ID)
        assertEquals("app/create_account_code_screen", CreateAccountCodeRoute.ROUTE_ID)
        assertEquals("app/create_account_summary_screen", CreateAccountSummaryRoute.ROUTE_ID)
        assertEquals("app/create_account_username_screen", CreateAccountUsernameRoute.ROUTE_ID)
    }

    @Test
    fun givenRegistrationRoute_whenSerializedAndRestored_thenSensitiveFlowStateAndOwnershipArePreserved() {
        val route = CreateAccountVerificationCodeRoute(
            registrationInfo = CreateAccountRegistrationInfo(
                email = "alice@example.com",
                name = "Alice",
                password = "secret",
            ),
            flowId = "registration-flow",
            entryId = WireNavEntryId("verification-entry"),
        )

        val restored = Json.decodeFromString<CreateAccountVerificationCodeRoute>(
            Json.encodeToString(route)
        )

        assertEquals(route, restored)
    }

    @Test
    fun givenLegacyRegistrationArguments_whenMappedToTypedAndBack_thenEveryValueIsPreserved() {
        val legacy = CreateAccountDataNavArgs(
            userRegistrationInfo = UserRegistrationInfo(
                email = "alice@example.com",
                name = "Alice",
                password = "secret",
            )
        )

        val restored = legacy.toDataDetailRoute("registration-flow").toLegacyNavArgs()

        assertEquals(legacy, restored)
    }

    @Test
    fun givenBlankFlowId_whenCreatingAnyCreateAccountRoute_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CreateAccountSelectorRoute(flowId = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CreateAccountUsernameRoute(
                sessionId = WireSessionId("user", "wire.test"),
                flowId = "",
            )
        }
    }

    @Test
    fun givenCreateAccountContribution_whenInspectingEntries_thenAllTenRoutesAreRegistered() {
        val source = sourceFile("CreateAccountNavigation3Entries.kt").readText()
        listOf(
            "wireEntry<CreateAccountSelectorRoute>",
            "wireEntry<CreateAccountDataDetailRoute>",
            "wireEntry<CreateAccountVerificationCodeRoute>",
            "wireEntry<CreatePersonalAccountOverviewRoute>",
            "wireEntry<CreateTeamAccountOverviewRoute>",
            "wireEntry<CreateAccountEmailRoute>",
            "wireEntry<CreateAccountDetailsRoute>",
            "wireEntry<CreateAccountCodeRoute>",
            "wireEntry<CreateAccountSummaryRoute>",
            "wireEntry<CreateAccountUsernameRoute>",
        ).forEach {
            assertTrue(source.contains(it), "Missing $it")
        }
    }

    @Test
    fun givenCreateAccountEntries_whenInspectingSource_thenScreenViewModelsUseEntryOwnershipWithoutNav2Bridges() {
        val source = sourceFile("CreateAccountNavigation3Entries.kt").readText()

        assertTrue(source.contains("wireViewModelStoreOwner(WireViewModelOwner.Entry(entryId))"))
        assertTrue(source.contains("route.flowId"))
        assertFalse(source.contains("WireViewModelOwner.Flow"))
        assertFalse(source.contains("ScreenDestination"))
        assertFalse(source.contains("com.ramcosta.composedestinations"))
        assertFalse(source.contains("NavController"))
        assertFalse(source.contains("SavedStateHandle"))
        assertFalse(source.contains("Bundle"))
        assertFalse(source.contains("DEFAULT_ARGS_KEY"))
    }

    @Test
    fun givenArgumentBackedRegistrationViewModels_whenInspectingFactory_thenEveryTypedArgumentHasManualBinding() {
        val graph = sourceFile("../../../ui/authentication/AuthenticationViewModelGraph.kt").readText()
        listOf(
            "createAccountOverviewViewModel(navArgs: CreateAccountOverviewNavArgs)",
            "createAccountEmailViewModel(navArgs: CreateAccountNavArgs)",
            "createAccountDetailsViewModel(navArgs: CreateAccountNavArgs)",
            "createAccountCodeViewModel(navArgs: CreateAccountNavArgs)",
            "createAccountSummaryViewModel(navArgs: CreateAccountSummaryNavArgs)",
            "createAccountSelectorViewModel(navArgs: CreateAccountSelectorNavArgs)",
            "createAccountDataDetailViewModel(navArgs: CreateAccountDataNavArgs)",
        ).forEach {
            assertTrue(graph.contains(it), "Missing manual assisted method $it")
        }
        assertTrue(graph.contains("fun createAccountVerificationCodeViewModel("))
    }

    private fun sourceFile(name: String): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            projectDir,
            "app/src/main/kotlin/com/wire/android/navigation/routes/auth/$name",
        ).also {
            assertTrue(it.isFile, "Missing source file $name")
        }
    }
}
