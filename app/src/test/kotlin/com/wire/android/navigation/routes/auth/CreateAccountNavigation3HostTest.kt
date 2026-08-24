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
            "fun createAccountEmailViewModel(\n        navArgs: CreateAccountNavArgs,",
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
        assertTrue(screen.contains("CreateAccountDetailsContent("))
        assertTrue(screen.contains("showTeamName = navArgs.flowType == CreateAccountFlowType.CreateTeam"))
        assertTrue(screen.contains("ServerTitle("))
        assertTrue(screen.contains("CoreFailureErrorDialog(failure, onDismiss)"))
        assertTrue(screen.contains("firstName = firstNameTextState.text.toString().trim()"))
        assertTrue(screen.contains("lastName = lastNameTextState.text.toString().trim()"))
        assertTrue(screen.contains("password = passwordTextState.text.toString()"))
        assertTrue(screen.contains("teamName = teamNameTextState.text.toString().trim()"))
        assertFalse(screen.contains("WireScaffold("))
        assertFalse(screen.contains("WireTextField("))
        assertTrue(featureSources.contains("class CreateAccountDetailsViewModel<LinksT, FailureT>"))
        assertTrue(featureSources.contains("fun <FailureT> CreateAccountDetailsContent("))
        listOf(
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "com.wire.kalium.logic.configuration.server.ServerConfig",
            "AssistedInject",
        )
            .forEach { assertFalse(featureSources.contains(it), "Feature details source contains $it") }
    }

    @Test
    fun givenEmailViewModel_whenInspectingHost_thenFeatureBoundaryPreservesContinuationAndHostTypes() {
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
        val metro = sourceFile("di/metro/AuthenticationMetroViewModelBindings.kt").readText()
        val entries = sourceFile("navigation/routes/auth/CreateAccountNavigation3Entries.kt").readText()
        val screen = sourceFile("ui/authentication/create/email/CreateAccountEmailScreen.kt").readText()
        val hostFactory = sourceFile("ui/authentication/create/email/CreateAccountEmailViewModelHostFactory.kt").readText()
        val featureRoot = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/email",
        )
        val featureSources = featureRoot.listFiles().orEmpty().joinToString("\n") { it.readText() }
        val emailEntry = entries
            .substringAfter("private fun CreateAccountEmailNavigation3Entry(")
            .substringBefore("private fun CreateAccountDetailsNavigation3Entry(")

        assertTrue(graph.contains("CreateAccountEmailViewModel<CreateAccountFlowType, ServerConfig.Links, CoreFailure>"))
        assertTrue(graph.contains("fun createAccountEmailViewModel():"))
        assertTrue(metro.contains("CreateAccountEmailViewModelHostFactory"))
        assertFalse(metro.contains("CreateAccountEmailViewModel.Factory"))
        assertTrue(hostFactory.contains("tosUrlFor = { it.tos }"))
        assertTrue(emailEntry.contains("val navArgs = route.toLegacyNavArgs()"))
        assertTrue(emailEntry.contains("navArgs = navArgs"))
        assertTrue(emailEntry.contains("createAccountEmailViewModel(navArgs, owner)"))
        assertTrue(screen.contains("navArgs.copy("))
        assertTrue(screen.contains("UserRegistrationInfo("))
        assertTrue(screen.contains("email = emailTextState.text.trim().toString().lowercase()"))
        assertTrue(featureSources.contains("class CreateAccountEmailViewModel<FlowT, LinksT, FailureT>"))
        listOf(
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "com.wire.kalium.logic.configuration.server.ServerConfig",
            "CoreLogic",
            "CoreFailure",
            "dev.zacsweers.metro",
        )
            .forEach { assertFalse(featureSources.contains(it), "Feature email source contains $it") }
    }

    @Test
    fun givenCodeViewModel_whenInspectingHost_thenFeatureOwnsStateAndAppKeepsKaliumAndNavigation() {
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
        val metro = sourceFile("di/metro/AuthenticationMetroViewModelBindings.kt").readText()
        val entries = sourceFile("navigation/routes/auth/CreateAccountNavigation3Entries.kt").readText()
        val screen = sourceFile("ui/authentication/create/code/CreateAccountCodeScreen.kt").readText()
        val hostFactory = sourceFile("ui/authentication/create/code/CreateAccountCodeViewModelHostFactory.kt").readText()
        val featureRoot = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/code",
        )
        val featureSources = featureRoot.listFiles().orEmpty().joinToString("\n") { it.readText() }
        val codeEntry = entries
            .substringAfter("private fun CreateAccountCodeNavigation3Entry(")
            .substringBefore("private fun CreateAccountSummaryNavigation3Entry(")

        assertTrue(graph.contains("AppCreateAccountCodeViewModel"))
        assertTrue(graph.contains("fun createAccountCodeViewModel():"))
        assertTrue(metro.contains("CreateAccountCodeViewModelHostFactory"))
        assertFalse(metro.contains("CreateAccountCodeViewModel.Factory"))
        assertTrue(hostFactory.contains("StoreSessionParam("))
        assertTrue(hostFactory.contains("replace = false"))
        assertTrue(hostFactory.contains("AndroidCreateAccountCodeResendTimer(CountdownTimer())"))
        assertTrue(screen.contains("CreateAccountSummaryNavArgs(flowType)"))
        assertTrue(codeEntry.contains("AuthenticationNavigationTransition.ACCOUNT_CODE_TO_SUMMARY"))
        assertTrue(codeEntry.contains("WireBackStackMode.CLEAR_WHOLE"))
        assertTrue(codeEntry.contains("AuthenticationLoginCompletion.RemoveDevice"))
        assertTrue(featureSources.contains("class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>"))
        listOf(
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "com.wire.kalium",
            "BuildConfig",
            "dev.zacsweers.metro",
            "com.wire.android.R",
        ).forEach { assertFalse(featureSources.contains(it), "Feature code source contains $it") }
    }

    @Test
    fun givenUsernameViewModel_whenInspectingHost_thenFeatureOwnsStateAndSessionHostOwnsAdapters() {
        val graph = sourceFile("ui/authentication/AuthenticationViewModelGraph.kt").readText()
        val sessionFactory = sourceFile("ui/authentication/SessionAuthenticationViewModelFactory.kt").readText()
        val screen = sourceFile("ui/authentication/create/username/CreateAccountUsernameScreen.kt").readText()
        val hostFactory = sourceFile(
            "ui/authentication/create/username/CreateAccountUsernameViewModelHostFactory.kt"
        ).readText()
        val featureRoot = File(
            repositoryRoot(),
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/username",
        )
        val featureSources = featureRoot.listFiles().orEmpty().joinToString("\n") { it.readText() }

        assertTrue(graph.contains("CreateAccountUsernameViewModel<CoreFailure>"))
        assertTrue(graph.contains("fun createAccountUsernameViewModel():"))
        assertTrue(sessionFactory.contains("CreateAccountUsernameViewModelHostFactory"))
        assertTrue(sessionFactory.contains("createAccountUsernameViewModelHostFactory.create()"))
        assertFalse(sessionFactory.contains("ValidateUserHandleUseCase"))
        assertFalse(sessionFactory.contains("SetUserHandleUseCase"))
        assertTrue(hostFactory.contains("SetUserHandleResult.Failure.HandleExists -> SetUsernameResult.UsernameTaken"))
        assertTrue(hostFactory.contains("AnalyticsEvent.RegistrationPersonalAccount.CreationCompleted"))
        assertTrue(screen.contains("state.error.toHandleUpdateErrorState()"))
        assertTrue(featureSources.contains("class CreateAccountUsernameViewModel<FailureT>"))
        listOf("HandleUpdateErrorState", "CoreFailure", "com.wire.kalium", "AnalyticsEvent", "dev.zacsweers.metro")
            .forEach { assertFalse(featureSources.contains(it), "Feature username source contains $it") }
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
