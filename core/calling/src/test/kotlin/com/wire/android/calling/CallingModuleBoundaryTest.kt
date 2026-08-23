/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.calling

import com.lemonappdev.konsist.api.Konsist
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CallingModuleBoundaryTest {

    @Test
    fun callingProductionScopeIsExactAndPackagePreserving() {
        assertEquals(expectedProductionFiles, productionFiles.map { it.relativeTo(callingSourceRoot).path }.toSet())
        productionFiles.forEach { source ->
            assertTrue(
                source.readText().contains("package com.wire.android.ui.home.conversations.call"),
                "${source.name} must retain the legacy coordinator package.",
            )
        }
    }

    @Test
    fun callingSourcesDoNotImportRuntimeOrFeatureImplementations() {
        productionFiles.forEach { source ->
            val sourceText = source.readText()
            assertFalse(
                forbiddenSourceImports.any { it.containsMatchIn(sourceText) },
                "${source.name} imports an app runtime, feature implementation, Metro, Navigation3 runtime, analytics implementation, or AVS type.",
            )
        }
    }

    @Test
    fun appOwnsTheRuntimeActionAndDialogAdapters() {
        assertTrue(runtimeActionsFile.isFile, "The app runtime action adapter must remain outside :core:calling.")
        assertTrue(runtimeDialogsFile.isFile, "The app runtime dialog renderer must remain outside :core:calling.")
        assertTrue(runtimeActionsFile.readText().contains("HandleActions"), "The app adapter must handle coordinator actions.")
        assertTrue(runtimeDialogsFile.readText().contains("HandleJoinOrStartCallScreenDialogs"), "The app adapter must render coordinator dialogs.")
    }

    @Test
    fun callingBuildScriptHasNeutralDependencyBudgetAndMetroOptOuts() {
        val buildScript = buildScript.readText()
        assertFalse(forbiddenProjectEdges.any { it.containsMatchIn(buildScript) }, ":core:calling must not depend on app, features, or core navigation.")
        assertTrue(apiUiCommonEdge.containsMatchIn(buildScript), ":core:calling must expose ActionsManager through api(:core:ui-common).")
        assertTrue(apiKaliumLogicEdge.containsMatchIn(buildScript), ":core:calling must expose public Kalium Logic types through api.")
        assertTrue(apiCoroutinesEdge.containsMatchIn(buildScript), ":core:calling must expose Flow through api(coroutines).")
        assertTrue(androidxCoreEdge.containsMatchIn(buildScript), ":core:calling must directly provide VisibleForTesting through androidx.core.")
        assertTrue(metroCompilerOptOut.containsMatchIn(buildScript), ":core:calling must set metro.enabled to false.")
        assertTrue(
            metroRuntimeOptOut.containsMatchIn(buildScript),
            ":core:calling must set metro.automaticallyAddRuntimeDependencies to false.",
        )
    }

    @Test
    fun appHasExactlyOneInboundCallingEdge() {
        val appScript = appBuildScript.readText()
        assertEquals(
            1,
            appInboundCallingEdge.findAll(appScript).count(),
            ":app must declare exactly one implementationWithCoverage(projects.core.calling) edge.",
        )
    }

    private companion object {
        val repoRoot = File(Konsist.projectRootPath)
        val callingSourceRoot = File(repoRoot, "core/calling/src/main/kotlin")
        val buildScript = File(repoRoot, "core/calling/build.gradle.kts")
        val appBuildScript = File(repoRoot, "app/build.gradle.kts")
        val runtimeActionsFile = File(
            repoRoot,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/call/JoinOrStartCallRuntimeActions.kt",
        )
        val runtimeDialogsFile = File(
            repoRoot,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/call/JoinOrStartCallRuntimeDialogs.kt",
        )
        val productionFiles = callingSourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .toList()
        val expectedProductionFiles = setOf(
            "com/wire/android/ui/home/conversations/call/JoinOrStartCallManager.kt",
            "com/wire/android/ui/home/conversations/call/JoinOrStartCallScreenDialogs.kt",
            "com/wire/android/ui/home/conversations/call/JoinOrStartCallViewActions.kt",
            "com/wire/android/ui/home/conversations/call/JoinOrStartCallViewState.kt",
            "com/wire/android/ui/home/conversations/call/ObserveConversationParticipantCount.kt",
        )
        val forbiddenSourceImports = listOf(
            Regex("""import\s+com\.wire\.android\.(R|BuildConfig|services\.|ui\.calling\.|ui\.common\.(dialogs\.calling|error\.CoreFailureErrorDialog)|navigation\.runtime\.|feature\.analytics\.AnonymousAnalyticsManagerImpl|feature\.)"""),
            Regex("""import\s+com\.waz\.avs\."""),
            Regex("""import\s+dev\.zacsweers\.metro"""),
        )
        val forbiddenProjectEdges = listOf(
            Regex("""projects\s*\.\s*app\b|project\s*\(\s*(?:path\s*=\s*)?["']\s*:\s*app\s*["']"""),
            Regex("""projects\s*\.\s*features\s*\.|project\s*\(\s*(?:path\s*=\s*)?["']\s*:\s*features\s*:"""),
            Regex("""projects\s*\.\s*core\s*\.\s*navigation\b|project\s*\(\s*(?:path\s*=\s*)?["']\s*:\s*core\s*:\s*navigation\s*["']"""),
        )
        val metroCompilerOptOut = Regex("""metro\s*\{(?s:.*?)enabled\s*\.\s*set\s*\(\s*false\s*\)""")
        val metroRuntimeOptOut = Regex(
            """metro\s*\{(?s:.*?)automaticallyAddRuntimeDependencies\s*\.\s*set\s*\(\s*false\s*\)""",
        )
        val appInboundCallingEdge = Regex(
            """implementationWithCoverage\s*\(\s*projects\s*\.\s*core\s*\.\s*calling\s*\)""",
        )
        val apiUiCommonEdge = Regex("""api\s*\(\s*projects\s*\.\s*core\s*\.\s*uiCommon\s*\)""")
        val apiKaliumLogicEdge = Regex("""api\s*\(\s*["']com\.wire\.kalium:kalium-logic["']\s*\)""")
        val apiCoroutinesEdge = Regex("""api\s*\(\s*libs\s*\.\s*coroutines\s*\.\s*android\s*\)""")
        val androidxCoreEdge = Regex("""implementation\s*\(\s*libs\s*\.\s*androidx\s*\.\s*core\s*\)""")
    }
}
