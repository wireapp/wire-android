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
            "fun createAccountDetailsViewModel(\n        navArgs: CreateAccountNavArgs,",
            "createAccountCodeViewModel(navArgs: CreateAccountNavArgs)",
            "createAccountSelectorViewModel(navArgs: CreateAccountSelectorNavArgs)",
            "createAccountDataDetailViewModel(navArgs: CreateAccountDataNavArgs)",
        ).forEach {
            assertTrue(graph.contains(it), "Missing manual assisted method $it")
        }
        assertTrue(graph.contains("fun createAccountVerificationCodeViewModel("))
    }

    @Test
    fun givenOverviewViewModel_whenInspectingHost_thenGenericFeatureBoundaryPreservesOptionalCustomConfig() {
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
        val metro = sourceFile("di/metro/AuthenticationMetroViewModelBindings.kt").readText()
        val screen = sourceFile(
            "ui/authentication/create/overview/CreatePersonalAccountOverviewScreen.kt"
        ).readText()
        val hostFactory = sourceFile(
            "ui/authentication/create/overview/CreateAccountOverviewViewModelHostFactory.kt"
        ).readText()
        val featureViewModel = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/overview/" +
                "CreateAccountOverviewViewModel.kt",
        ).readText()

        assertTrue(
            graph.contains(
                "createAccountOverviewViewModel(navArgs: CreateAccountOverviewNavArgs): " +
                    "CreateAccountOverviewViewModel<ServerConfig.Links>"
            )
        )
        assertTrue(metro.contains("CreateAccountOverviewViewModelHostFactory"))
        assertFalse(metro.contains("CreateAccountOverviewViewModel.Factory"))
        assertTrue(hostFactory.contains("pricingUrl = { it.pricing }"))
        assertTrue(screen.contains("customServerConfig = viewModel.customServerConfig"))
        assertFalse(screen.contains("viewModel.navArgs.customServerConfig"))
        assertTrue(featureViewModel.contains("class CreateAccountOverviewViewModel<LinksT>"))
        assertFalse(featureViewModel.contains("CreateAccountOverviewNavArgs"))
        assertFalse(featureViewModel.contains("com.wire.kalium.logic.configuration.server.ServerConfig"))
        assertFalse(featureViewModel.contains("AssistedInject"))
    }

    @Test
    fun givenDetailsViewModel_whenInspectingHost_thenFeatureBoundaryKeepsNavArgsAndKaliumInApp() {
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
        val metro = sourceFile("di/metro/AuthenticationMetroViewModelBindings.kt").readText()
        val entries = sourceFile("navigation/routes/auth/CreateAccountNavigation3Entries.kt").readText()
        val screen = sourceFile("ui/authentication/create/details/CreateAccountDetailsScreen.kt").readText()
        val hostFactory = sourceFile(
            "ui/authentication/create/details/CreateAccountDetailsViewModelHostFactory.kt"
        ).readText()
        val featureRoot = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/details",
        )
        val featureSources = featureRoot.listFiles().orEmpty().joinToString("\n") { it.readText() }
        val detailsEntry = entries
            .substringAfter("private fun CreateAccountDetailsNavigation3Entry(")
            .substringBefore("private fun CreateAccountCodeNavigation3Entry(")

        assertTrue(graph.contains("CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure>"))
        assertTrue(graph.contains("fun createAccountDetailsViewModel():"))
        assertTrue(metro.contains("CreateAccountDetailsViewModelHostFactory"))
        assertFalse(metro.contains("CreateAccountDetailsViewModel.Factory"))
        assertTrue(hostFactory.contains("validatePassword(password).isValid"))
        assertTrue(hostFactory.contains("navArgs.flowType == CreateAccountFlowType.CreateTeam"))
        assertTrue(detailsEntry.contains("val navArgs = route.toLegacyNavArgs()"))
        assertTrue(detailsEntry.contains("navArgs = navArgs"))
        assertTrue(detailsEntry.contains("createAccountDetailsViewModel(navArgs, owner)"))
        assertTrue(screen.contains("navArgs.userRegistrationInfo.copy("))
        assertTrue(screen.contains("flowType = navArgs.flowType"))
        assertTrue(featureSources.contains("class CreateAccountDetailsViewModel<LinksT, FailureT>"))
        listOf(
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "com.wire.kalium.logic.configuration.server.ServerConfig",
            "AssistedInject",
        )
            .forEach { assertFalse(featureSources.contains(it), "Feature details source contains $it") }
    }

    @Test
    fun givenSummaryRoute_whenInspectingHost_thenStatelessFeatureUiPreservesTransitionAndRouteIdentity() {
        val entries = sourceFile("navigation/routes/auth/CreateAccountNavigation3Entries.kt").readText()
        val summaryEntry = entries
            .substringAfter("private fun CreateAccountSummaryNavigation3Entry(")
            .substringBefore("private fun CreateAccountUsernameNavigation3Entry(")

        assertTrue(summaryEntry.contains("CreateAccountSummaryRouteScreen("))
        assertTrue(summaryEntry.contains("type = route.type"))
        assertTrue(summaryEntry.contains("AuthenticationNavigationTransition.ACCOUNT_SUMMARY_TO_USERNAME"))
        assertTrue(summaryEntry.contains("CreateAccountUsernameRoute(route.sessionId, route.flowId)"))
        assertTrue(summaryEntry.contains("WireBackStackMode.CLEAR_WHOLE"))
        assertFalse(summaryEntry.contains("viewModel"))
        assertFalse(summaryEntry.contains("owner"))
        assertFalse(summaryEntry.contains("toLegacyNavArgs"))
    }

    @Test
    fun givenStatelessSummaryScreen_whenInspectingHostComposition_thenSummaryViewModelPlumbingIsAbsent() {
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
        val metro = sourceFile("di/metro/AuthenticationMetroViewModelBindings.kt").readText()
        val mapper = sourceFile("navigation/routes/auth/CreateAccountLegacyMappers.kt").readText()
        val flowType = sourceFile("ui/authentication/create/common/CreateAccountFlowType.kt").readText()

        listOf(graph, metro).forEach { source ->
            assertFalse(source.contains("CreateAccountSummaryViewModel"))
            assertFalse(source.contains("createAccountSummaryViewModel"))
        }
        assertFalse(mapper.contains("CreateAccountSummaryRoute.toLegacyNavArgs"))
        assertTrue(mapper.contains("CreateAccountSummaryNavArgs.toSummaryRoute"))
        assertFalse(flowType.contains("SummaryResources"))
        assertFalse(flowType.contains("summaryResources"))
    }

    private fun sourceFile(relativePath: String): File {
        return File(repositoryRoot(), "app/src/main/kotlin/com/wire/android/$relativePath").also {
            assertTrue(it.isFile, "Missing source file $relativePath")
        }
    }

    private fun repositoryRoot(): File =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
}
