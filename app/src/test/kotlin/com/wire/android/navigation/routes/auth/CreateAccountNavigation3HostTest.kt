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

import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountNavigation3HostTest {

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
    fun givenCreateAccountContribution_whenInspectingEntries_thenAllTenRoutesAreRegistered() {
        val source = sourceFile("navigation/routes/auth/CreateAccountNavigation3Entries.kt").readText()
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
        val source = sourceFile("navigation/routes/auth/CreateAccountNavigation3Entries.kt").readText()

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
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
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

    private fun sourceFile(relativePath: String): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(projectDir, "app/src/main/kotlin/com/wire/android/$relativePath").also {
            assertTrue(it.isFile, "Missing source file $relativePath")
        }
    }
}
