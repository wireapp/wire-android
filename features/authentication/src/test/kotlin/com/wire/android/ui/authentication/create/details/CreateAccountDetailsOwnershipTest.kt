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

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun givenDetailsPresentation_whenInspectingOwners_thenFeatureOwnsRendererAndAppOwnsOnlyHostAdaptation() {
        val root = repositoryRoot()
        val featureContent = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountDetailsContent.kt")
        )
        val appScreen = Files.readString(
            root.resolve("app/src/main/kotlin/$packagePath/CreateAccountDetailsScreen.kt")
        )
        val contentSignature = extractDelimited(
            featureContent,
            "fun <FailureT> CreateAccountDetailsContent",
            '(',
            ')',
        ).normalizeWhitespace()
        val contentSection = featureContent.substring(
            featureContent.indexOf("fun <FailureT> CreateAccountDetailsContent"),
            featureContent.indexOf("@MultipleThemePreviews"),
        )
        val scaffoldArguments = extractDelimited(contentSection, "WireScaffold", '(', ')').normalizeWhitespace()

        assertTrue(featureContent.contains("fun <FailureT> CreateAccountDetailsContent("))
        assertTrue(contentSignature.contains("modifier: Modifier = Modifier"))
        assertTrue(contentSignature.indexOf("modifier:") < contentSignature.indexOf("genericFailureContent:"))
        assertTrue(contentSignature.endsWith("genericFailureContent: @Composable (FailureT, () -> Unit) -> Unit,"))
        assertTrue(scaffoldArguments.startsWith("modifier = modifier,"))
        assertEquals(1, Regex("""\bmodifier\s*=\s*modifier\b""").findAll(contentSection).count())
        assertTrue(featureContent.contains("showTeamName: Boolean"))
        assertTrue(featureContent.contains("if (showTeamName)"))
        assertTrue(featureContent.contains("firstNameFocusRequester.requestFocus()"))
        assertTrue(featureContent.contains("keyboardController?.show()"))
        assertTrue(featureContent.contains("genericFailureContent(dialogError.coreFailure, onErrorDismiss)"))
        assertTrue(featureContent.contains("import com.wire.android.feature.authentication.R"))
        assertFalse(featureContent.contains("com.wire.android.R"))
        assertFalse(featureContent.contains("CreateAccountNavArgs"))
        assertFalse(featureContent.contains("CreateAccountFlowType"))
        assertFalse(featureContent.contains("ServerConfig"))
        assertFalse(featureContent.contains("CoreFailureErrorDialog"))

        assertTrue(appScreen.contains("CreateAccountDetailsContent("))
        assertTrue(appScreen.contains("showTeamName = navArgs.flowType == CreateAccountFlowType.CreateTeam"))
        assertTrue(appScreen.contains("ServerTitle("))
        assertTrue(appScreen.contains("CoreFailureErrorDialog(failure, onDismiss)"))
        assertTrue(appScreen.contains("firstName = firstNameTextState.text.toString().trim()"))
        assertTrue(appScreen.contains("lastName = lastNameTextState.text.toString().trim()"))
        assertTrue(appScreen.contains("password = passwordTextState.text.toString()"))
        assertTrue(appScreen.contains("teamName = teamNameTextState.text.toString().trim()"))
        assertFalse(appScreen.contains("WireScaffold("))
        assertFalse(appScreen.contains("WireTextField("))
        assertFalse(appScreen.contains("FocusRequester"))
    }

    @Test
    fun givenDetailsPresentationResources_whenInspectingOwners_thenDefinitionsMovedExactly() {
        val root = repositoryRoot()
        val appDefinitions = resourceDefinitions(root.resolve("app/src/main/res"))
        val featureDefinitions = resourceDefinitions(root.resolve("features/authentication/src/main/res"))

        assertTrue(appDefinitions.isEmpty(), "App still owns details presentation strings: $appDefinitions")
        assertEquals(86, featureDefinitions.size)
        assertEquals(
            expectedQualifiersByResource,
            featureDefinitions
                .groupBy { definition ->
                    resourceNames.single { name -> definition.contains("name=\"$name\"") }
                }
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

    private fun extractDelimited(source: String, marker: String, opening: Char, closing: Char): String {
        val markerIndex = source.indexOf(marker)
        require(markerIndex >= 0) { "Missing marker: $marker" }
        val openingIndex = source.indexOf(opening, markerIndex)
        require(openingIndex >= 0) { "Missing '$opening' after marker: $marker" }
        var depth = 0
        for (index in openingIndex until source.length) {
            when (source[index]) {
                opening -> depth++
                closing -> {
                    depth--
                    if (depth == 0) return source.substring(openingIndex + 1, index)
                }
            }
        }
        error("Missing '$closing' after marker: $marker")
    }

    private fun String.normalizeWhitespace(): String = replace(Regex("""\s+"""), " ").trim()

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/details"
        const val expectedResourceFingerprint = "b16ede84a34bbc9a00945ced68c8f092a127e3d57c73d38502fc16953fd7cc22"
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
        val resourceNames = setOf(
            "create_personal_account_details_text",
            "create_account_details_first_name_placeholder",
            "create_account_details_last_name_placeholder",
            "create_account_details_team_name_placeholder",
            "create_account_details_first_name_label",
            "create_account_details_last_name_label",
            "create_account_details_team_name_label",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>"""
        )
        val placeholderQualifiers = setOf(
            "values", "values-de", "values-es", "values-et", "values-fr", "values-hr",
            "values-hu", "values-it", "values-pl", "values-pt", "values-ru", "values-si",
        )
        val labelQualifiers = placeholderQualifiers + "values-sv"
        val expectedQualifiersByResource = mapOf(
            "create_personal_account_details_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_account_details_first_name_placeholder" to placeholderQualifiers,
            "create_account_details_last_name_placeholder" to placeholderQualifiers,
            "create_account_details_team_name_placeholder" to placeholderQualifiers,
            "create_account_details_first_name_label" to labelQualifiers,
            "create_account_details_last_name_label" to labelQualifiers,
            "create_account_details_team_name_label" to labelQualifiers,
        )
    }
}
