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

package com.wire.android.feature.conversation.folders

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationFoldersModuleBoundaryTest {

    @Test
    fun foldersOwnsExactlyTheSixPackagePreservingProductionSources() {
        val actual = childMainKotlin.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { file ->
                file.relativeTo(childMainKotlin).invariantSeparatorsPath to
                    requireNotNull(packageDeclaration.find(file.readText())).groupValues[1]
            }
        assertEquals(expectedProductionSources, actual)
        expectedProductionSources.keys.forEach { relativePath ->
            assertFalse(File(facadeMainKotlin, relativePath).exists(), "$relativePath must not remain in the facade.")
        }
    }

    @Test
    fun childHasTheExactPluginAndDependencyBudget() {
        val buildScript = childBuildScript.readText()
        val dependencyLines = buildScript.lineSequence()
            .map(String::trim)
            .filter { line -> dependencyPrefixes.any(line::startsWith) }
            .toSet()

        assertEquals(expectedDependencyLines, dependencyLines)
        expectedPluginEntries.forEach { plugin ->
            assertEquals(1, buildScript.lineSequence().count { it.trim() == plugin })
        }
        forbiddenBuildEntries.forEach { forbidden ->
            assertFalse(buildScript.contains(forbidden), "The folders module has no budget for $forbidden.")
        }
        assertTrue(buildScript.contains("namespace = \"com.wire.android.feature.conversation.folders\""))
    }

    @Test
    fun dependencyDirectionKeepsAppOnTheFacadeAndTheFacadeOnFolders() {
        val childBuild = childBuildScript.readText()
        val facadeBuild = facadeBuildScript.readText()
        val appBuild = appBuildScript.readText()

        assertFalse(childBuild.contains("projects.app") || childBuild.contains("project(\":app\")"))
        assertFalse(
            childBuild.contains("projects.features.conversation") || childBuild.contains("project(\":features:conversation\")"),
        )
        assertEquals(1, facadeFoldersApiEdge.findAll(facadeBuild).count())
        assertEquals(1, facadeFoldersKoverEdge.findAll(facadeBuild).count())
        assertEquals(0, directAppFoldersEdge.findAll(appBuild).count())
        assertEquals(1, facadeAppEdge.findAll(appBuild).count())
        assertEquals(1, explicitSettingsInclude.findAll(settingsBuildScript.readText()).count())
    }

    @Test
    fun folderResourcesAndRImportsAreChildOwned() {
        val allChildKotlin = (childMainKotlin.walkTopDown() + childTestKotlin.walkTopDown())
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name == "ConversationFoldersModuleBoundaryTest.kt" }
            .toList()
        assertEquals(
            3,
            allChildKotlin.count { it.readText().contains("import com.wire.android.feature.conversation.folders.R") },
        )
        allChildKotlin.forEach { file ->
            assertFalse(file.readText().contains("import com.wire.android.feature.conversation.R"))
        }

        assertEquals(
            expectedStringsByQualifier.keys,
            childResources.listFiles().orEmpty().filter(File::isDirectory).map(File::getName).toSet(),
        )
        var entryCount = 0
        expectedStringsByQualifier.forEach { (qualifier, expectedLines) ->
            val stringsFile = File(childResources, "$qualifier/strings.xml")
            assertTrue(stringsFile.isFile, "Missing $qualifier/strings.xml")
            val actualLines = stringLine.findAll(stringsFile.readText()).map { it.value.trim() }.toSet()
            assertEquals(expectedLines, actualLines, "Unexpected strings in $qualifier.")
            entryCount += actualLines.size
        }
        assertEquals(18, entryCount)

        val facadeResourceText = File(repoRoot, "features/conversation/src/main/res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .joinToString(separator = "\n") { it.readText() }
        folderStringNames.forEach { name -> assertFalse(facadeResourceText.contains("name=\"$name\"")) }
    }

    @Test
    fun metroGroupsGatewaysKeysAndDirectBindingRemainStable() {
        val foldersGraph = childSource("com/wire/android/ui/home/conversations/ConversationFoldersViewModelGraph.kt")
        val foldersViewModel = childSource("com/wire/android/ui/home/conversations/folder/ConversationFoldersVM.kt")
        val moveGraph = childSource("com/wire/android/ui/home/conversations/MoveConversationToFolderViewModelGraph.kt")
        val moveViewModel = childSource("com/wire/android/ui/home/conversations/folder/MoveConversationToFolderVM.kt")
        val newFolderGraph = childSource("com/wire/android/ui/home/conversations/NewFolderViewModelGraph.kt")

        assertTrue(foldersGraph.contains("object ConversationFoldersManualViewModelFactoryGroup"))
        assertTrue(foldersGraph.contains("fun conversationFoldersViewModel("))
        assertTrue(foldersGraph.contains("instanceKey = \"conversation_folders_\${args.selectedFolderId}\""))
        assertTrue(foldersViewModel.contains("group = ConversationFoldersManualViewModelFactoryGroup::class"))
        assertTrue(foldersViewModel.contains("factoryMethod = \"conversationFoldersViewModel\""))
        assertTrue(moveGraph.contains("object MoveConversationToFolderManualViewModelFactoryGroup"))
        assertTrue(moveGraph.contains("fun moveConversationToFolderViewModel("))
        assertTrue(
            moveGraph.contains(
                "instanceKey = \"move_conversation_to_folder_\${args.conversationId}_\${args.currentFolderId}\"",
            ),
        )
        assertTrue(moveViewModel.contains("MoveConversationToFolderManualViewModelFactoryGroup::class"))
        assertTrue(moveViewModel.contains("factoryMethod = \"moveConversationToFolderViewModel\""))
        assertTrue(newFolderGraph.contains("@BindingContainer"))
        assertTrue(newFolderGraph.contains("object NewFolderMetroViewModelBindings"))
        assertTrue(newFolderGraph.contains("@ViewModelKey(NewFolderViewModel::class)"))
        assertTrue(newFolderGraph.contains("fun newFolderViewModel(viewModel: NewFolderViewModel): ViewModel = viewModel"))
        assertTrue(newFolderGraph.contains("fun newFolderViewModel(): NewFolderViewModel ="))
        assertTrue(newFolderGraph.contains("wireMetroViewModel()"))
    }

    @Test
    fun scopedPreviewAggregateIsUniqueAndContainsOnlyTheTwoAssistedFolderPreviews() {
        val childBuild = childBuildScript.readText()
        val productionText = childMainKotlin.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }

        assertEquals(1, childAggregateName.findAll(childBuild).count())
        assertEquals(0, childAggregateName.findAll(facadeBuildScript.readText()).count())
        assertEquals(2, productionText.lineSequence().count { it.trim() == "@ViewModelScopedPreview" })
        assertEquals(
            2,
            productionText.lineSequence().count {
                it.contains("ConversationFoldersViewModelScopedPreviews as ViewModelScopedPreviews")
            },
        )
        assertFalse(productionText.contains("ConversationViewModelScopedPreviews as ViewModelScopedPreviews"))
    }

    @Test
    fun appRetainsFolderScreensRoutesAndNavigationRuntimeOwnership() {
        appOwnedFolderHosts.forEach { relativePath ->
            assertTrue(File(repoRoot, relativePath).isFile, "App host source moved unexpectedly: $relativePath")
        }
        val navigation = File(
            repoRoot,
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAuxNavigation3Entries.kt",
        ).readText()
        assertTrue(navigation.contains("wireEntry<ConversationFoldersRoute>"))
        assertTrue(navigation.contains("wireEntry<NewConversationFolderRoute>"))
        assertTrue(navigation.contains("ConversationFoldersRouteScreen("))
        assertTrue(navigation.contains("NewConversationFolderNavigation3Entry(runtime, newFolderViewModel())"))
    }

    private fun childSource(relativePath: String): String =
        File(childMainKotlin, relativePath).also { assertTrue(it.isFile, "Missing $it") }.readText()

    private companion object {
        val repoRoot = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val childRoot = File(repoRoot, "features/conversation/folders")
        val childMainKotlin = File(childRoot, "src/main/kotlin")
        val childTestKotlin = File(childRoot, "src/test/kotlin")
        val childResources = File(childRoot, "src/main/res")
        val facadeMainKotlin = File(repoRoot, "features/conversation/src/main/kotlin")
        val childBuildScript = File(childRoot, "build.gradle.kts")
        val facadeBuildScript = File(repoRoot, "features/conversation/build.gradle.kts")
        val appBuildScript = File(repoRoot, "app/build.gradle.kts")
        val settingsBuildScript = File(repoRoot, "settings.gradle.kts")

        val expectedProductionSources = mapOf(
            "com/wire/android/ui/home/conversations/ConversationFoldersViewModelGraph.kt" to "com.wire.android.ui.home.conversations",
            "com/wire/android/ui/home/conversations/MoveConversationToFolderViewModelGraph.kt" to "com.wire.android.ui.home.conversations",
            "com/wire/android/ui/home/conversations/NewFolderViewModelGraph.kt" to "com.wire.android.ui.home.conversations",
            "com/wire/android/ui/home/conversations/folder/ConversationFoldersVM.kt" to "com.wire.android.ui.home.conversations.folder",
            "com/wire/android/ui/home/conversations/folder/MoveConversationToFolderVM.kt" to "com.wire.android.ui.home.conversations.folder",
            "com/wire/android/ui/home/conversations/folder/NewFolderViewModel.kt" to "com.wire.android.ui.home.conversations.folder",
        )
        val packageDeclaration = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
        val dependencyPrefixes = setOf("api(", "implementation(", "testImplementation(", "testRuntimeOnly(", "ksp(")
        val expectedDependencyLines = setOf(
            "api(projects.core.uiCommon)", "api(\"com.wire.kalium:kalium-logic\")",
            "api(libs.androidx.lifecycle.viewModel)", "api(libs.coroutines.android)",
            "api(libs.ktx.immutableCollections)", "api(libs.ktx.serialization)",
            "api(enforcedPlatform(libs.compose.bom))", "api(libs.androidx.compose.runtime)",
            "api(\"androidx.compose.foundation:foundation\")", "implementation(projects.core.di)",
            "implementation(libs.metrox.viewModelCompose)", "testImplementation(libs.junit5.core)",
            "testImplementation(libs.coroutines.test)", "testImplementation(libs.konsist)",
            "testImplementation(libs.mockk.core)", "testImplementation(libs.turbine)",
            "testImplementation(testFixtures(projects.core.uiCommon))", "testRuntimeOnly(libs.junit5.engine)",
            "ksp(project(\":ksp\"))",
        )
        val expectedPluginEntries = setOf(
            "id(libs.plugins.wire.android.library.get().pluginId)", "id(libs.plugins.wire.kover.get().pluginId)",
            "id(BuildPlugins.junit5)", "id(libs.plugins.wire.compose.compiler.get().pluginId)",
            "alias(libs.plugins.compose.stability.analyzer)", "alias(libs.plugins.kotlin.serialization)",
            "alias(libs.plugins.ksp)",
        )
        val forbiddenBuildEntries = setOf(
            "kotlinParcelize", "projects.core.search", "libs.compose.material3", "libs.compose.ui.preview",
            "libs.ktx.dateTime", "libs.okio.core", "testFixtures.enable", "testFixturesImplementation",
        )
        val facadeFoldersApiEdge = Regex("""api\s*\(\s*projects\.features\.conversation\.folders\s*\)""")
        val facadeFoldersKoverEdge = Regex("""kover\s*\(\s*projects\.features\.conversation\.folders\s*\)""")
        val directAppFoldersEdge = Regex("""projects\.features\.conversation\.folders|project\s*\(\s*["']:features:conversation:folders["']\s*\)""")
        val facadeAppEdge = Regex("""implementationWithCoverage\s*\(\s*projects\.features\.conversation\s*\)""")
        val explicitSettingsInclude = Regex("""include\s*\(\s*["']:features:conversation:folders["']\s*\)""")
        val childAggregateName = Regex("""wire\.viewmodelScopedPreview\.aggregateName["']?\s*,\s*["']ConversationFoldersViewModelScopedPreviews["']""")
        val stringLine = Regex("""<string\s+name="[^"]+">.*</string>""")
        val folderStringNames = setOf("move_to_folder_success", "move_to_folder_failed", "new_folder_failure")
        val expectedStringsByQualifier = mapOf(
            "values" to setOf(
                "<string name=\"move_to_folder_success\">“%1\$s” was moved to “%2\$s”</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” could not be moved</string>",
                "<string name=\"new_folder_failure\">“%1\$s” folder could not be added</string>",
            ),
            "values-de" to setOf(
                "<string name=\"move_to_folder_success\">„%1\$s“ wurde nach ‚%2\$s‘ verschoben</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” konnte nicht verschoben werden</string>",
                "<string name=\"new_folder_failure\">Ordner “%1\$s” konnte nicht hinzugefügt werden</string>",
            ),
            "values-hu" to setOf(
                "<string name=\"move_to_folder_success\">\\\"%1\$s\\\" áthelyezve ide: \\\"%2\$s\\\"</string>",
                "<string name=\"move_to_folder_failed\">\\\"%1\$s\\\" áthelyezése nem sikerült</string>",
                "<string name=\"new_folder_failure\">\\\"%1\$s\\\" mappa létrehozása nem sikerült</string>",
            ),
            "values-pt" to setOf(
                "<string name=\"move_to_folder_success\">“%1\$s” foi movido para “%2\$s”</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” não pôde ser movido</string>",
                "<string name=\"new_folder_failure\">A pasta “%1\$s” não pôde ser adicionada</string>",
            ),
            "values-ru" to setOf(
                "<string name=\"move_to_folder_success\">“%1\$s” был перемещен в “%2\$s”</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” не может быть перемещен</string>",
                "<string name=\"new_folder_failure\">Папка “%1\$s” не может быть добавлена</string>",
            ),
            "values-si" to setOf(
                "<string name=\"move_to_folder_success\">“%1\$s” “%2\$s” වෙත ගෙන යන ලදී.</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” ගෙනයාමට නොහැකි විය</string>",
                "<string name=\"new_folder_failure\">“%1\$s” ෆෝල්ඩරය එක් කිරීමට නොහැකි විය.</string>",
            ),
        )
        val appOwnedFolderHosts = setOf(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAuxNavigation3.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAuxNavigation3Entries.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/folder/ConversationFoldersNavArgs.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/folder/ConversationFoldersScreen.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/folder/NewConversationFolderScreen.kt",
        )
    }
}
