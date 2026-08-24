/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.email

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountEmailOwnershipTest {

    @Test
    fun givenEmailStateEngine_thenFeatureOwnsViewModelStateAndGateway() {
        val root = repositoryRoot()
        sourceFiles.forEach { source ->
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/$source")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$packagePath/$source")))
        }
        assertTrue(Files.isRegularFile(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountEmailViewModelHostFactory.kt")))
    }

    @Test
    fun givenFeatureEmailSources_thenHostTypesDoNotCrossBoundary() {
        val root = repositoryRoot()
        val roots = listOf(
            root.resolve("features/authentication/src/main/kotlin/$packagePath"),
            root.resolve("features/authentication/src/test/kotlin/$packagePath"),
        )
        val imports = roots.flatMap { sourceRoot ->
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

        val source = Files.readString(root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountEmailViewModel.kt"))
        assertTrue(source.contains("class CreateAccountEmailViewModel<FlowT, LinksT, FailureT>"))
        assertTrue(source.contains("val customServerConfig: LinksT?"))
        assertTrue(source.contains("private val gateway: CreateAccountEmailGateway<LinksT, FailureT>"))
        assertTrue(source.contains("}.invokeOnCompletion"))
        assertTrue(source.contains("ActivationCodeResult.AuthScopeUnavailable -> return@launch"))
    }

    @Test
    fun givenEmailPresentation_thenFeatureOwnsRendererAndAppOwnsHostEffects() {
        val root = repositoryRoot()
        val featureContent = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountEmailContent.kt")
        )
        val appScreen = Files.readString(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountEmailScreen.kt"))

        assertTrue(featureContent.contains("fun <FlowT, FailureT> CreateAccountEmailContent("))
        assertTrue(featureContent.contains("focusRequester.requestFocus()"))
        assertTrue(featureContent.contains("EmailErrorText(state.error, text, onLearnMorePressed)"))
        assertTrue(featureContent.contains("if (state.termsDialogVisible)"))
        assertTrue(featureContent.contains("genericFailureContent(dialogError.coreFailure, onErrorDismiss)"))
        assertFalse(featureContent.contains("com.wire.android.R"))
        assertFalse(featureContent.contains("CustomTabsHelper"))
        assertFalse(featureContent.contains("ServerConfig"))
        assertFalse(featureContent.contains("CreateAccountFlowType"))
        assertFalse(featureContent.contains("CreateAccountNavArgs"))

        assertTrue(appScreen.contains("CreateAccountEmailContent("))
        assertTrue(appScreen.contains("val termsUrl = tosUrl()"))
        assertTrue(appScreen.contains("CustomTabsHelper.launchUrl(context, termsUrl)"))
        assertTrue(appScreen.contains("CustomTabsHelper.launchUrl(context, learnMoreUrl)"))
        assertTrue(appScreen.contains("CoreFailureErrorDialog(failure, onDismiss)"))
        assertTrue(appScreen.contains("email = emailTextState.text.trim().toString().lowercase()"))
        assertFalse(appScreen.contains("WireScaffold("))
        assertFalse(appScreen.contains("WireTextField("))
        assertFalse(appScreen.contains("WireDialog("))
    }

    @Test
    fun givenExclusiveEmailResources_thenFeatureOwnsExactDefinitions() {
        val root = repositoryRoot()
        val appDefinitions = resourceDefinitions(root.resolve("app/src/main/res"))
        val featureDefinitions = resourceDefinitions(root.resolve("features/authentication/src/main/res"))

        assertTrue(appDefinitions.isEmpty(), "App still owns email presentation strings: $appDefinitions")
        assertEquals(36, featureDefinitions.size)
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
        const val packagePath = "com/wire/android/ui/authentication/create/email"
        val sourceFiles = listOf(
            "CreateAccountEmailViewModel.kt",
            "CreateAccountEmailViewState.kt",
            "CreateAccountEmailGateway.kt",
            "CreateAccountEmailContent.kt",
        )
        val forbiddenImports = listOf(
            "com.wire.kalium",
            "com.wire.android.di",
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "ServerConfig",
            "CoreLogic",
            "CoreFailure",
            "dev.zacsweers.metro",
        )
        val resourceNames = setOf(
            "create_personal_account_email_text",
            "create_team_email_text",
            "create_account_email_footer_text",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>"""
        )
        const val expectedResourceFingerprint = "d720a54c825fba594f0e4cacd3ddc8d7463eb7b94be68377676f265e736712aa"
        val expectedQualifiersByResource = mapOf(
            "create_personal_account_email_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_team_email_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_account_email_footer_text" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hr",
                "values-hu", "values-it", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
        )
    }
}
