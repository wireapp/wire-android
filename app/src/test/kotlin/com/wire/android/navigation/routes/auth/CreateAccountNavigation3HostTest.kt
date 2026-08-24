package com.wire.android.navigation.routes.auth

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountNavigation3HostTest {
    @Test
    fun `all create account routes retain their registered identities`() {
        val entries = createAccountEntries().getValue("CreateAccountNavigation3Entries.kt")
        listOf(
            "CreateAccountSelectorRoute", "CreateAccountDataDetailRoute", "CreateAccountVerificationCodeRoute",
            "CreatePersonalAccountOverviewRoute", "CreateTeamAccountOverviewRoute", "CreateAccountEmailRoute",
            "CreateAccountDetailsRoute", "CreateAccountCodeRoute", "CreateAccountSummaryRoute", "CreateAccountUsernameRoute",
        ).forEach { route -> assertTrue(entries.contains("wireEntry<$route"), "Missing $route") }
    }

    @Test
    fun `new create account entries use route DTOs without legacy bridges`() {
        val entries = createAccountEntries().values.joinToString("\n")
        listOf("CreateAccountEmailRoute", "CreateAccountDetailsRoute", "CreateAccountCodeRoute").forEach {
            assertTrue(entries.contains(it))
        }
        listOf("CreateAccountNavArgs", "CreateAccountFlowType", "CreateAccountLegacyMappers", "toLegacyNavArgs()").forEach {
            assertFalse(entries.contains(it), "Legacy bridge remains: $it")
        }
    }

    @Test
    fun `route DTOs drive host factories and preserve completion policies`() {
        val graph = appSource("ui/authentication/AuthenticationViewModelGraph.kt")
        val entries = createAccountEntries()
        val code = entries.getValue("CreateAccountNavigation3FormEntries.kt")
        val completion = entries.getValue("CreateAccountNavigation3CompletionEntries.kt")
        val verification = entries.getValue("CreateAccountNavigation3VerificationEntry.kt")
        assertTrue(graph.contains("CreateAccountRouteFlowType"))
        assertTrue(graph.contains("CreateAccountRegistrationInfo"))
        assertTrue(code.contains("CreateAccountSummaryRoute(type, userId.toCreateAccountSessionId(), route.flowId)"))
        assertTrue(code.contains("AuthenticationLoginCompletion.RemoveDevice(it.toCreateAccountSessionId())"))
        assertTrue(completion.contains("AuthenticationLoginCompletion.InitialSync(route.sessionId)"))
        assertTrue(verification.contains("CreateAccountUsernameRoute(userId.toCreateAccountSessionId(), route.flowId)"))
        assertTrue(code.indexOf("CreateAccountSummaryRoute(") < code.indexOf("WireBackStackMode.CLEAR_WHOLE"))
        assertTrue(completion.indexOf("CreateAccountUsernameRoute(") < completion.indexOf("WireBackStackMode.CLEAR_WHOLE"))
    }

    @Test
    fun `deleted parcel DTOs and mapper cannot return`() {
        val root = repositoryRoot()
        listOf(
            "ui/authentication/create/common/CreateAccountFlowType.kt",
            "navigation/routes/auth/CreateAccountLegacyMappers.kt",
            "ui/authentication/create/overview/CreateAccountOverviewNavArgs.kt",
            "ui/authentication/create/summary/CreateAccountSummaryNavArgs.kt",
        ).forEach { assertFalse(File(root, "app/src/main/kotlin/com/wire/android/$it").exists()) }
    }

    private fun appSource(path: String): String = File(repositoryRoot(), "app/src/main/kotlin/com/wire/android/$path").readText()

    private fun createAccountEntries(): Map<String, String> = entryFileNames.associateWith { fileName ->
        appSource("navigation/routes/auth/$fileName")
    }

    private fun repositoryRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) {
        it.parentFile
    }
        .first { File(it, "app/src/main/kotlin").isDirectory }

    private companion object {
        val entryFileNames = listOf(
            "CreateAccountNavigation3Entries.kt",
            "CreateAccountNavigation3AccountEntries.kt",
            "CreateAccountNavigation3FormEntries.kt",
            "CreateAccountNavigation3VerificationEntry.kt",
            "CreateAccountNavigation3CompletionEntries.kt",
        )
    }
}
