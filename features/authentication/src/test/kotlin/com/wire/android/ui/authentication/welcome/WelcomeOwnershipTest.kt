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
package com.wire.android.ui.authentication.welcome

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WelcomeOwnershipTest {

    @Test
    fun givenWelcomeStateEngine_thenFeatureOwnsItAndAppOwnsOnlyHostComposition() {
        val root = repositoryRoot()
        movedSourceNames.forEach { sourceName ->
            val relativePath = "$welcomePackagePath/$sourceName"
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$relativePath")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$relativePath")))
        }
        assertFalse(Files.exists(root.resolve("app/src/test/kotlin/$welcomePackagePath/WelcomeViewModelTest.kt")))
        assertTrue(
            Files.isRegularFile(root.resolve("features/authentication/src/test/kotlin/$welcomePackagePath/WelcomeViewModelTest.kt"))
        )
        assertTrue(
            Files.isRegularFile(root.resolve("app/src/main/kotlin/$welcomePackagePath/WelcomeViewModelHostFactory.kt"))
        )
    }

    @Test
    fun givenFeatureWelcomeSources_thenHostKaliumAndMetroTypesDoNotCrossTheBoundary() {
        val root = repositoryRoot()
        val sourceRoot = root.resolve("features/authentication/src/main/kotlin/$welcomePackagePath")
        val sources = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .map(Files::readString)
                .toList()
        }
        val imports = listOf(
            sourceRoot,
            root.resolve("features/authentication/src/test/kotlin/$welcomePackagePath"),
        ).flatMap { rootPath ->
            Files.walk(rootPath).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                    .flatMap { Files.lines(it) }
                    .filter { it.startsWith("import ") }
                    .toList()
            }
        }
        forbiddenFragments.forEach { forbidden ->
            assertFalse(imports.any { it.contains(forbidden) }, "Forbidden feature dependency: $forbidden")
        }
        assertTrue(sources.any { it.contains("class WelcomeViewModel<LinksT>") })
        assertTrue(sources.any { it.contains("data class WelcomeScreenState<LinksT>") })
        assertTrue(sources.any { it.contains("fun interface WelcomeSessionGateway") })
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val welcomePackagePath = "com/wire/android/ui/authentication/welcome"
        val movedSourceNames = listOf(
            "WelcomeViewModel.kt",
            "WelcomeScreenState.kt",
            "WelcomeSessionGateway.kt",
        )
        val forbiddenFragments = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "WelcomeNavArgs",
            "ServerConfig",
            "AccountInfo",
            "GetAllSessionsResult",
            "dev.zacsweers.metro",
        )
    }
}
