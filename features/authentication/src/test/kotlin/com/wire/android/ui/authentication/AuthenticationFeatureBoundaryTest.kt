/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationFeatureBoundaryTest {

    @Test
    fun `all feature production imports and module dependencies respect the boundary`() {
        val root = repositoryRoot()
        val productionRoot = root.resolve("features/authentication/src/main/kotlin")
        val imports = Files.walk(productionRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .flatMap { Files.lines(it).filter { line -> line.startsWith("import ") } }
                .toList()
        }
        forbiddenImportPrefixes.forEach { forbidden ->
            assertFalse(imports.any { it.startsWith("import $forbidden") }, "Forbidden feature import: $forbidden")
        }

        val gradle = Files.readString(root.resolve("features/authentication/build.gradle.kts"))
        forbiddenDependencies.forEach { forbidden ->
            assertFalse(gradle.contains(forbidden), "Forbidden feature dependency: $forbidden")
        }
    }

    @Test
    fun `permanent app adapter allowlist remains explicit and presentation-free`() {
        val root = repositoryRoot()
        permanentHostAdapters.forEach { (relative, hostMarker) ->
            val file = root.resolve("app/src/main/kotlin/$relative")
            assertTrue(Files.isRegularFile(file), "Missing permanent adapter: $relative")
            assertTrue(Files.readString(file).contains(hostMarker), "$relative no longer contains $hostMarker")
        }

        presentationAdapters.forEach { relative ->
            val source = Files.readString(root.resolve("app/src/main/kotlin/$relative"))
            assertFalse(source.contains("WireScaffold("), "$relative owns feature layout")
            assertFalse(source.contains("WireDialog("), "$relative owns feature dialog rendering")
        }
    }

    @Test
    fun `dead authentication bridges stay deleted`() {
        val root = repositoryRoot()
        deadSources.forEach { relative ->
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$relative")), "Dead bridge restored: $relative")
        }
        val appProduction = Files.walk(root.resolve("app/src/main/kotlin")).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .map(Files::readString)
                .toList()
        }
        deadSymbols.forEach { symbol ->
            assertFalse(appProduction.any { it.contains(symbol) }, "Dead bridge reference restored: $symbol")
        }
    }

    @Test
    fun `mixed certificate route remains host exception while initial sync route is feature-owned`() {
        val root = repositoryRoot()
        val certificateRoute = Files.readString(
            root.resolve(
                "app/src/main/kotlin/com/wire/android/ui/settings/devices/e2ei/E2eiCertificateDetailsNavigation3.kt"
            )
        )
        val initialSyncRoute = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/com/wire/android/navigation/routes/utility/InitialSyncRoute.kt")
        )
        val readme = Files.readString(root.resolve("features/authentication/README.md"))

        assertTrue(certificateRoute.contains("data class DuringLogin("))
        assertTrue(certificateRoute.contains("data class AfterLogin("))
        assertTrue(certificateRoute.contains("AuthenticationScreenRoute"))
        assertTrue(initialSyncRoute.contains("data class InitialSyncRoute"))
        assertTrue(initialSyncRoute.contains("package com.wire.android.navigation.routes.utility"))
        assertTrue(readme.contains("E2eiCertificateDetailsRoute"))
        assertTrue(readme.contains("InitialSync"))
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val forbiddenImportPrefixes = setOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.config",
            "com.wire.android.datastore",
            "com.wire.android.di",
            "com.wire.android.R",
            "com.wire.android.navigation.navigation3",
            "com.wire.android.navigation.runtime",
            "com.wire.android.navigation.style",
            "dev.zacsweers.metro",
        )
        val forbiddenDependencies = setOf(
            "projects.app",
            "projects.features.",
            "projects.kalium",
            "libs.kalium",
        )
        val permanentHostAdapters = mapOf(
            "com/wire/android/ui/authentication/create/common/ServerTitle.kt" to "ServerConfig.Links",
            "com/wire/android/ui/authentication/login/LoginErrorDialogMapper.kt" to "CoreFailure",
            "com/wire/android/ui/authentication/login/ServerConfigAuthenticationExtensions.kt" to "ServerConfig.Links",
            "com/wire/android/ui/authentication/devices/register/AuthenticationFailureDialog.kt" to "R.string.error_no_network_title",
            "com/wire/android/navigation/routes/auth/AuthenticationLegacyMappers.kt" to "ServerConfig.Links",
        )
        val presentationAdapters = setOf(
            "com/wire/android/ui/authentication/create/common/ServerTitle.kt",
            "com/wire/android/ui/authentication/login/LoginErrorDialogMapper.kt",
            "com/wire/android/ui/authentication/devices/register/AuthenticationFailureDialog.kt",
        )
        val deadSources = setOf(
            "com/wire/android/ui/authentication/login/LoginNavigationItem.kt",
            "com/wire/android/ui/authentication/login/LoginViewModel.kt",
            "com/wire/android/ui/authentication/login/sso/SSOUrlConfig.kt",
            "com/wire/android/ui/newauthentication/login/NewLoginDestination.kt",
            "com/wire/android/ui/settings/devices/e2ei/E2eiCertificateDetailsScreenNavArgs.kt",
            "com/wire/android/ui/settings/devices/e2ei/E2eiCertificateDetailsLegacyMappers.kt",
        )
        val deadSymbols = setOf(
            "toNewLoginPasswordRoute(",
            "toWelcomeRoute(",
            "toPasswordOrSsoDestination(",
            "E2eiCertificateDetailsScreenNavArgs",
        )
    }
}
