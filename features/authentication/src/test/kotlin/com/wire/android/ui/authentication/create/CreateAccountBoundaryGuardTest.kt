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
    fun `create account resources are exclusively feature-owned with stable contents`() {
        val root = root()
        val names = listOf(
            "create_personal_account_text",
            "create_team_content_title",
            "create_team_text",
            "create_team_learn_more",
            "create_team_title",
            "create_account_code_text",
        )
        val app = resourceDefinitions(root.resolve("app/src/main/res"), names)
        val feature = resourceDefinitions(root.resolve("features/authentication/src/main/res"), names)

        assertTrue(app.isEmpty(), "Create Account resource definitions remain in app: $app")
        assertEquals(60, feature.size)
        assertEquals("ba6fab33fea30d230a9fefe82c9706c6f549cdb0b56662155160bc45618f1de1", sha256(feature))
    }

    @Test
    fun `new create account completion clears state before terminal routes`() {
        val entries = navigationEntries()
        assertTrue(entries.getValue("CreateAccountNavigation3Entries.kt").contains("createAccountCodeEntry(route"))
        val code = entries.getValue("CreateAccountNavigation3FormEntries.kt")
        assertTrue(code.contains("CreateAccountSummaryRoute(type, userId.toCreateAccountSessionId(), route.flowId)"))
        assertTrue(code.indexOf("CreateAccountSummaryRoute(") < code.indexOf("WireBackStackMode.CLEAR_WHOLE"))
        assertTrue(code.contains("AuthenticationLoginCompletion.RemoveDevice(it.toCreateAccountSessionId())"))
        val verification = entries.getValue("CreateAccountNavigation3VerificationEntry.kt")
        assertTrue(verification.contains("CreateAccountUsernameRoute(userId.toCreateAccountSessionId(), route.flowId)"))
        assertTrue(verification.indexOf("CreateAccountUsernameRoute(") < verification.indexOf("WireBackStackMode.CLEAR_WHOLE"))
        val completion = entries.getValue("CreateAccountNavigation3CompletionEntries.kt")
        assertTrue(completion.contains("CreateAccountUsernameRoute(route.sessionId, route.flowId)"))
        assertTrue(completion.contains("AuthenticationLoginCompletion.InitialSync(route.sessionId)"))
        assertTrue(completion.indexOf("CreateAccountUsernameRoute(") < completion.indexOf("WireBackStackMode.CLEAR_WHOLE"))
    }

    private fun navigationEntries(): Map<String, String> {
        val directory = root().resolve("app/src/main/kotlin/com/wire/android/navigation/routes/auth")
        return navigationFileNames.associateWith { fileName ->
            val path = directory.resolve(fileName)
            assertTrue(Files.isRegularFile(path), "Missing split Navigation 3 entry: $fileName")
            Files.readString(path)
        }
    }

    private fun resourceDefinitions(root: Path, names: List<String>): List<String> = Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.parent.fileName.toString().startsWith("values") }
            .flatMap { path -> resourceRegex(names).findAll(Files.readString(path))
                .map { "${path.parent.fileName}|${it.value.trim()}" }.toList().stream() }.sorted().toList()
    }

    private fun kotlinSources(root: Path): List<Path> = Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }.toList()
    }

    private fun sha256(value: List<String>): String = MessageDigest.getInstance("SHA-256")
        .digest(value.joinToString("\n").toByteArray()).joinToString("") { "%02x".format(it) }

    private fun resourceRegex(names: List<String>) =
        Regex("""<string\s+name="(${names.joinToString("|")})"[^>]*>.*?</string>""")

    private fun root(): Path = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
        .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val navigationFileNames = listOf(
            "CreateAccountNavigation3Entries.kt",
            "CreateAccountNavigation3FormEntries.kt",
            "CreateAccountNavigation3VerificationEntry.kt",
            "CreateAccountNavigation3CompletionEntries.kt",
        )
    }
}
