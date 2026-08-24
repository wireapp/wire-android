/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.username

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountUsernameOwnershipTest {

    @Test
    fun givenUsernameStateEngine_thenFeatureOwnsViewModelStateAndContracts() {
        val root = repositoryRoot()
        sourceFiles.forEach { source ->
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/$source")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$packagePath/$source")))
        }
        assertTrue(
            Files.isRegularFile(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountUsernameViewModelHostFactory.kt"))
        )
    }

    @Test
    fun givenFeatureUsernameSources_thenHostTypesDoNotCrossBoundary() {
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
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountUsernameViewModel.kt")
        )
        assertTrue(viewModel.contains("class CreateAccountUsernameViewModel<FailureT>"))
        assertTrue(viewModel.contains("private val gateway: CreateAccountUsernameGateway<FailureT>"))
        assertTrue(viewModel.contains("private val analytics: CreateAccountUsernameAnalytics"))
        assertTrue(viewModel.indexOf("analytics.usernameScreenShown()") < viewModel.indexOf("textState.textAsFlow()"))
        assertTrue(viewModel.indexOf("analytics.accountCreationCompleted()") < viewModel.indexOf("success ="))
    }

    @Test
    fun givenUsernamePresentation_thenFeatureOwnsRendererAndAppOwnsNewAuthHostLayout() {
        val root = repositoryRoot()
        val featureContent = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountUsernameContent.kt")
        )
        val appScreen = Files.readString(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountUsernameScreen.kt"))

        assertTrue(featureContent.contains("fun <FailureT> CreateAccountUsernameContent("))
        assertTrue(featureContent.contains("typealias CreateAccountUsernameLayout"))
        assertTrue(featureContent.contains(".forceLowercase()"))
        assertTrue(featureContent.contains(".patternWithCallback(USERNAME_PATTERN, animate)"))
        assertTrue(featureContent.contains(".maxLengthWithCallback(MAX_USERNAME_LENGTH, animate)"))
        assertTrue(featureContent.contains("Pattern.compile(\"^[a-z0-9._-]*$\")"))
        assertTrue(featureContent.contains("painterResource(mentionIconResId)"))
        assertTrue(featureContent.contains("genericFailureContent(error.failure, onErrorDismiss)"))
        assertFalse(featureContent.contains("HandleUpdateErrorState"))
        assertFalse(featureContent.contains("com.wire.android.R"))
        assertFalse(featureContent.contains("NewAuthContainer"))
        assertFalse(featureContent.contains("CoreFailure"))

        assertTrue(appScreen.contains("CreateAccountUsernameContent("))
        assertTrue(appScreen.contains("NewAuthContainer("))
        assertTrue(appScreen.contains("NewAuthHeader("))
        assertTrue(appScreen.contains("CoreFailureErrorDialog(failure, onDismiss)"))
        assertFalse(appScreen.contains("toHandleUpdateErrorState"))
        assertFalse(appScreen.contains("UsernameTextField("))
        assertTrue(
            Files.isRegularFile(
                root.resolve("app/src/main/kotlin/com/wire/android/ui/authentication/create/common/handle/UsernameTextField.kt")
            )
        )
    }

    @Test
    fun givenExclusiveUsernameResources_thenFeatureOwnsExactDefinitions() {
        val root = repositoryRoot()
        val appDefinitions = resourceDefinitions(root.resolve("app/src/main/res"))
        val featureDefinitions = resourceDefinitions(root.resolve("features/authentication/src/main/res"))

        assertTrue(appDefinitions.isEmpty(), "App still owns username presentation strings: $appDefinitions")
        assertEquals(15, featureDefinitions.size)
        assertEquals(
            expectedQualifiersByResource,
            featureDefinitions
                .groupBy { definition -> resourceNames.single { definition.contains("name=\"$it\"") } }
                .mapValues { (_, definitions) -> definitions.map { it.substringBefore('|') }.toSet() },
        )
        assertEquals(expectedResourceFingerprint, sha256(featureDefinitions.joinToString("\n")))
    }

    private fun resourceDefinitions(resourceRoot: Path): List<String> =
        Files.walk(resourceRoot).use { paths ->
            paths
                .filter { path ->
                    Files.isRegularFile(path) &&
                        path.fileName.toString().endsWith(".xml") &&
                        path.parent.fileName.toString().startsWith("values")
                }
                .flatMap { path ->
                    val qualifier = path.parent.fileName.toString()
                    resourceRegex.findAll(Files.readString(path)).map { match ->
                        "$qualifier|${match.value.trim()}"
                    }.toList().stream()
                }
                .sorted()
                .toList()
        }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/username"
        val sourceFiles = listOf(
            "CreateAccountUsernameViewModel.kt",
            "CreateAccountUsernameViewState.kt",
            "CreateAccountUsernameContracts.kt",
            "CreateAccountUsernameContent.kt",
        )
        val forbiddenImports = listOf(
            "com.wire.kalium",
            "com.wire.android.analytics",
            "com.wire.android.feature.analytics",
            "HandleUpdateErrorState",
            "CoreFailure",
            "dev.zacsweers.metro",
        )
        val resourceNames = setOf(
            "create_account_set_username_title",
            "create_account_username_text",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>"""
        )
        const val expectedResourceFingerprint = "f31f1c6392a30b13d9db7d566ee9df529d12ae068c54d10e8801456c7cfae9b8"
        val expectedQualifiersByResource = mapOf(
            "create_account_set_username_title" to setOf("values", "values-de", "values-ru"),
            "create_account_username_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
        )
    }
}
