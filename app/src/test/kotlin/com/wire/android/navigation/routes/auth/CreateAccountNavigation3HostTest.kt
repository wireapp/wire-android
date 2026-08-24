package com.wire.android.navigation.routes.auth

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountNavigation3HostTest {
    @Test
    fun `all create account routes retain their registered identities`() {
        val entries = appSource("navigation/routes/auth/CreateAccountNavigation3Entries.kt")
        listOf(
            "CreateAccountSelectorRoute", "CreateAccountDataDetailRoute", "CreateAccountVerificationCodeRoute",
            "CreatePersonalAccountOverviewRoute", "CreateTeamAccountOverviewRoute", "CreateAccountEmailRoute",
            "CreateAccountDetailsRoute", "CreateAccountCodeRoute", "CreateAccountSummaryRoute", "CreateAccountUsernameRoute",
        ).forEach { route -> assertTrue(entries.contains("wireEntry<$route"), "Missing $route") }
    }

    @Test
    fun `new create account entries use route DTOs without legacy bridges`() {
        val entries = appSource("navigation/routes/auth/CreateAccountNavigation3Entries.kt")
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
        val entries = appSource("navigation/routes/auth/CreateAccountNavigation3Entries.kt")
        assertTrue(graph.contains("CreateAccountRouteFlowType"))
        assertTrue(graph.contains("CreateAccountRegistrationInfo"))
        assertTrue(entries.contains("CreateAccountSummaryRoute(type, userId.toWireSessionId(), route.flowId)"))
        assertTrue(entries.contains("AuthenticationLoginCompletion.InitialSync(route.sessionId)"))
        assertTrue(entries.contains("AuthenticationLoginCompletion.RemoveDevice"))
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

    private fun repositoryRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
