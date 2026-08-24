/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.newauthentication.welcome

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NewWelcomeOwnershipTest {

    @Test
    fun givenNewWelcomePresentation_whenInspectingOwners_thenFeatureOwnsItWithoutHostDependencies() {
        val root = repositoryRoot()
        val appSource = root.resolve("app/src/main/kotlin/$sourcePath")
        val featureSource = root.resolve("features/authentication/src/main/kotlin/$sourcePath")

        assertFalse(Files.exists(appSource), "App still owns NewWelcome presentation")
        assertTrue(Files.isRegularFile(featureSource), "Feature does not own NewWelcome presentation")

        val source = Files.readString(featureSource)
        assertTrue(source.contains("package com.wire.android.ui.newauthentication.welcome"))
        assertTrue(publicWelcomeChooserSignature.containsMatchIn(source))
        assertTrue(composableWelcomeChooserSignature.containsMatchIn(source))
        assertTrue(publicEmptyStartSignature.containsMatchIn(source))
        forbiddenHostDependencies.forEach { forbidden ->
            assertFalse(forbidden.containsMatchIn(source), "NewWelcome source must not use: ${forbidden.pattern}")
        }
    }

    @Test
    fun givenNewWelcomePresentation_whenInspectingStructure_thenItPreservesNavigationNeutralBehavior() {
        val source = Files.readString(repositoryRoot().resolve("features/authentication/src/main/kotlin/$sourcePath"))
        val welcomeChooserBody = functionBody(source, "WelcomeChooserScreen")
        val launchedEffect = requireNotNull(launchedEffectUnit.find(welcomeChooserBody)) {
            "WelcomeChooserScreen must contain a Unit-keyed login chooser effect"
        }
        val launchedEffectBody = blockBodyAfter(welcomeChooserBody, launchedEffect.range.last + 1)
        val emptyStartBody = functionBody(source, "NewWelcomeEmptyStartScreen")

        assertTrue(onChooseLoginInvocation.containsMatchIn(launchedEffectBody))
        assertTrue(composableEmptyStartSignature.containsMatchIn(source))
        assertTrue(fullSizeBox.containsMatchIn(emptyStartBody))
    }

    @Test
    fun givenNewWelcomeProfileEntries_whenInspectingMovedPresentation_thenTheyKeepTheExistingJvmSignature() {
        val root = repositoryRoot()

        listOf("app/src/main/baseline-prof.txt", "app/src/main/startup-prof.txt").forEach { profile ->
            assertTrue(
                Files.readString(root.resolve(profile)).contains(newWelcomeEmptyStartSignature),
                "$profile must retain the NewWelcome empty-start signature",
            )
        }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private fun functionBody(source: String, name: String): String {
        val declaration = requireNotNull(Regex("""\bfun\s+${Regex.escape(name)}\s*\(""").find(source)) {
            "Missing $name declaration"
        }
        return blockBodyAfter(source, declaration.range.last + 1)
    }

    private fun blockBodyAfter(source: String, startIndex: Int): String {
        val openingBrace = source.indexOf('{', startIndex)
        require(openingBrace >= 0) { "Missing function block" }

        var depth = 0
        for (index in openingBrace until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(openingBrace + 1, index)
                }
            }
        }
        error("Unclosed function block")
    }

    private companion object {
        const val sourcePath = "com/wire/android/ui/newauthentication/welcome/NewWelcomeScreen.kt"
        const val newWelcomeEmptyStartSignature =
            "Lcom/wire/android/ui/newauthentication/welcome/NewWelcomeScreenKt;->NewWelcomeEmptyStartScreen" +
                "(Landroidx/compose/runtime/Composer;I)V"
        val publicWelcomeChooserSignature = Regex(
            """(?m)^\s*fun\s+WelcomeChooserScreen\s*\(\s*onChooseLogin\s*:\s*\(\s*\)\s*->\s*Unit\s*,?\s*\)"""
        )
        val publicEmptyStartSignature = Regex(
            """(?m)^\s*fun\s+NewWelcomeEmptyStartScreen\s*\(\s*\)"""
        )
        val composableWelcomeChooserSignature = Regex("""@Composable\s+fun\s+WelcomeChooserScreen\s*\(""")
        val launchedEffectUnit = Regex("""LaunchedEffect\s*\(\s*Unit\s*\)""")
        val onChooseLoginInvocation = Regex("""\bonChooseLogin\s*\(\s*\)""")
        val composableEmptyStartSignature = Regex(
            """@Composable\s+@Suppress\s*\(\s*"ComposeModifierMissing"\s*\)\s*fun\s+NewWelcomeEmptyStartScreen\s*\("""
        )
        val fullSizeBox = Regex("""Box\s*\(\s*modifier\s*=\s*Modifier\s*\.\s*fillMaxSize\s*\(\s*\)\s*\)""")
        val forbiddenHostDependencies = listOf(
            Regex("""\bcom\.wire\.(?:kalium|android\.(?:R|BuildConfig|navigation|datastore))\b"""),
            Regex("""(?m)^\s*import\s+com\.wire\.android\.ui\.authentication\."""),
            Regex("""(?m)^\s*import\s+androidx\.compose\.ui\.res\."""),
            Regex("""\b(?:LoginNavArgs|WireEntry|NavController|DestinationScope)\b"""),
            Regex("""\b(?:stringResource|painterResource)\s*\("""),
            Regex("""\bR\s*\."""),
        )
    }
}
