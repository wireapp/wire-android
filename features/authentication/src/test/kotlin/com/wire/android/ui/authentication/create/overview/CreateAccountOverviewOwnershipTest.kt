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
package com.wire.android.ui.authentication.create.overview

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountOverviewOwnershipTest {

    @Test
    fun givenOverviewStateEngine_thenFeatureOwnsViewModelAndAppOwnsHostFactory() {
        val root = repositoryRoot()
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountOverviewViewModel.kt")))
        assertTrue(
            Files.isRegularFile(
                root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountOverviewViewModel.kt")
            )
        )
        assertTrue(
            Files.isRegularFile(
                root.resolve("app/src/main/kotlin/$packagePath/CreateAccountOverviewViewModelHostFactory.kt")
            )
        )
    }

    @Test
    fun givenFeatureOverviewSources_thenHostTypesDoNotCrossBoundary() {
        val root = repositoryRoot()
        val sourceRoots = listOf(
            root.resolve("features/authentication/src/main/kotlin/$packagePath"),
            root.resolve("features/authentication/src/test/kotlin/$packagePath"),
        )
        val imports = sourceRoots.flatMap { sourceRoot ->
            Files.walk(sourceRoot).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                    .flatMap { Files.lines(it) }
                    .filter { it.startsWith("import ") }
                    .toList()
            }
        }
        forbiddenImports.forEach { forbidden ->
            assertFalse(imports.any { it.contains(forbidden) }, "Forbidden feature dependency: $forbidden")
        }

        val source = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountOverviewViewModel.kt")
        )
        assertTrue(source.contains("class CreateAccountOverviewViewModel<LinksT>"))
        assertTrue(source.contains("val customServerConfig: LinksT?"))
        assertTrue(source.contains("private val pricingUrl: (LinksT) -> String"))
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/overview"
        val forbiddenImports = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "CreateAccountOverviewNavArgs",
            "ServerConfig",
            "Parcelable",
            "dev.zacsweers.metro",
        )
    }
}
