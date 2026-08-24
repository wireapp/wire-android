/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.details

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountDetailsOwnershipTest {

    @Test
    fun givenDetailsStateEngine_thenFeatureOwnsViewModelStateAndGateway() {
        val root = repositoryRoot()
        sourceFiles.forEach { source ->
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/$source")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$packagePath/$source")))
        }
        assertTrue(
            Files.isRegularFile(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountDetailsViewModelHostFactory.kt"))
        )
    }

    @Test
    fun givenFeatureDetailsSources_thenHostTypesDoNotCrossBoundary() {
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

        val viewModel = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountDetailsViewModel.kt")
        )
        assertTrue(viewModel.contains("class CreateAccountDetailsViewModel<LinksT, FailureT>"))
        assertTrue(viewModel.contains("val customServerConfig: LinksT?"))
        assertTrue(viewModel.contains("private val requiresTeamName: Boolean"))
        assertTrue(viewModel.contains("private val gateway: CreateAccountDetailsGateway"))
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/details"
        val sourceFiles = listOf(
            "CreateAccountDetailsViewModel.kt",
            "CreateAccountDetailsViewState.kt",
            "CreateAccountDetailsGateway.kt",
        )
        val forbiddenImports = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "ServerConfig",
            "Parcelable",
            "dev.zacsweers.metro",
        )
    }
}
