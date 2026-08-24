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
        assertTrue(source.contains("@Composable\nfun WelcomeChooserScreen(\n    onChooseLogin: () -> Unit,"))
        assertFalse(source.contains("internal fun WelcomeChooserScreen"))
        assertTrue(source.contains("fun NewWelcomeEmptyStartScreen()"))
        source.lineSequence()
            .filter { it.startsWith("import ") }
            .forEach { importedSymbol ->
                assertTrue(importedSymbol in allowedImports, "Unexpected NewWelcome import: $importedSymbol")
            }
        forbiddenFragments.forEach { forbidden ->
            assertFalse(source.contains(forbidden), "NewWelcome source must not use host dependency: $forbidden")
        }
    }

    @Test
    fun givenNewWelcomePresentation_whenInspectingStructure_thenItPreservesNavigationNeutralBehavior() {
        val source = Files.readString(repositoryRoot().resolve("features/authentication/src/main/kotlin/$sourcePath"))

        assertTrue(source.contains("LaunchedEffect(Unit)"))
        assertTrue(source.contains("onChooseLogin()"))
        assertTrue(source.contains("@Suppress(\"ComposeModifierMissing\")\nfun NewWelcomeEmptyStartScreen()"))
        assertTrue(source.contains("Box(modifier = Modifier.fillMaxSize())"))
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

    private companion object {
        const val sourcePath = "com/wire/android/ui/newauthentication/welcome/NewWelcomeScreen.kt"
        const val newWelcomeEmptyStartSignature =
            "Lcom/wire/android/ui/newauthentication/welcome/NewWelcomeScreenKt;->NewWelcomeEmptyStartScreen" +
                "(Landroidx/compose/runtime/Composer;I)V"
        val forbiddenFragments = listOf(
            "import com.wire.android.R",
            "import com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.navigation",
            "com.wire.android.ui.authentication.login.LoginNavArgs",
            "WireEntry",
            "NavController",
            "DestinationScope",
            "stringResource(",
            "painterResource(",
            "R.",
        )
        val allowedImports = setOf(
            "import androidx.compose.foundation.layout.Box",
            "import androidx.compose.foundation.layout.fillMaxSize",
            "import androidx.compose.runtime.Composable",
            "import androidx.compose.runtime.LaunchedEffect",
            "import androidx.compose.ui.Modifier",
        )
    }
}
