package com.wire.android.ui.authentication.create

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountBoundaryGuardTest {
    @Test
    fun `feature create account sources exclude host and legacy contracts`() {
        val feature = root().resolve("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create")
        val source = kotlinSources(feature).joinToString("\n") { Files.readString(it) }
        listOf(
            "com.wire.kalium", "dev.zacsweers.metro", "CreateAccountNavArgs", "CreateAccountFlowType",
            "CreateAccountOverviewNavArgs", "CreateAccountSummaryNavArgs", "android.os.Parcelable",
        ).forEach { forbidden -> assertFalse(source.contains(forbidden), "Feature leaks $forbidden") }
    }

    @Test
    fun `app legacy create account DTO files are absent while typed route DTO remains feature-owned`() {
        val root = root()
        val app = root.resolve("app/src/main/kotlin/com/wire/android")
        listOf(
            "ui/authentication/create/common/CreateAccountFlowType.kt",
            "navigation/routes/auth/CreateAccountLegacyMappers.kt",
            "ui/authentication/create/overview/CreateAccountOverviewNavArgs.kt",
            "ui/authentication/create/summary/CreateAccountSummaryNavArgs.kt",
        ).forEach { assertFalse(Files.exists(app.resolve(it)), "Legacy file remains: $it") }
        assertTrue(
            Files.isRegularFile(
                root.resolve("features/authentication/src/main/kotlin/com/wire/android/navigation/routes/auth/CreateAccountNavigation3.kt"),
            )
        )
    }

    @Test
    fun `exclusive overview resources are feature-owned with stable contents`() {
        val root = root()
        val names = listOf(
            "create_personal_account_text", "create_team_content_title", "create_team_text", "create_team_learn_more",
        )
        val app = resourceDefinitions(root.resolve("app/src/main/res"), names)
        val feature = resourceDefinitions(root.resolve("features/authentication/src/main/res"), names)
        assertTrue(app.isEmpty(), "App still owns overview resources: $app")
        assertEquals(45, feature.size)
        assertEquals("b6cdbc2a637613ac166b64925b0c9bb23d3dd638cd0ca080235d68de37570be3", sha256(feature))
    }

    @Test
    fun `new create account completion clears state before terminal routes`() {
        val entries =
            Files.readString(
                root().resolve(
                    "app/src/main/kotlin/com/wire/android/navigation/routes/auth/CreateAccountNavigation3Entries.kt",
                ),
            )
        assertTrue(entries.contains("WireBackStackMode.CLEAR_WHOLE"))
        assertTrue(entries.contains("CreateAccountSummaryRoute(type, userId.toWireSessionId(), route.flowId)"))
        assertTrue(entries.contains("AuthenticationLoginCompletion.InitialSync(route.sessionId)"))
        assertTrue(entries.contains("AuthenticationLoginCompletion.RemoveDevice"))
    }

    private fun resourceDefinitions(root: Path, names: List<String>): List<String> = Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.parent.fileName.toString().startsWith("values") }
            .flatMap { path -> names.filter { Files.readString(path).contains("name=\"$it\"") }
                .map { "${path.parent.fileName}|$it" }.stream() }.sorted().toList()
    }

    private fun kotlinSources(root: Path): List<Path> = Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }.toList()
    }

    private fun sha256(value: List<String>): String = MessageDigest.getInstance("SHA-256")
        .digest(value.joinToString("\n").toByteArray()).joinToString("") { "%02x".format(it) }

    private fun root(): Path = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
        .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }
}
