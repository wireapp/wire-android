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

package com.wire.android.ui

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntryOwnedViewModelGatewaySourceTest {

    @Test
    fun givenProductionSources_whenInspectingResaca_thenOnlyLazyMessageItemsUseScopedArgs() {
        val root = repositoryRoot()
        val allowed = setOf(
            "core/di/src/main/kotlin/com/wire/android/di/ViewModelScoped.kt",
            "app/src/main/kotlin/com/wire/android/media/audiomessage/AudioMessageViewModel.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ScopedMessageViewModelGraph.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/CompositeMessageArgs.kt",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/AssetLocalPathViewModel.kt",
            "core/ui-common/src/main/kotlin/com/wire/android/ui/common/connection/ConnectionActionButtonArgs.kt",
        )
        val offenders = productionSources(root)
            .filter { it.readText().contains("ScopedArgs") }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .filterNot(allowed::contains)

        assertTrue(offenders.isEmpty(), "Resaca ownership leaked outside lazy message items: $offenders")
    }

    @Test
    fun givenProductionSources_whenCreatingSessionGraphs_thenOnlyRegistryOwnsFactoryAccess() {
        val root = repositoryRoot()
        val definition = "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt"
        val directCallOffenders = productionSources(root)
            .filter { it.relativeTo(root).invariantSeparatorsPath != definition }
            .filter { Regex("""(?<!::)createSessionViewModelGraph\s*\(""").containsMatchIn(it.readText()) }
            .map { it.relativeTo(root).invariantSeparatorsPath }

        assertTrue(
            directCallOffenders.isEmpty(),
            "Session graphs must be resolved through SessionGraphStoreViewModel: $directCallOffenders",
        )
        assertTrue(
            sourceFile("com/wire/android/navigation/runtime/SessionGraphStoreViewModel.kt")
                .readText()
                .contains("appGraph::createSessionViewModelGraph"),
        )
    }

    @Test
    fun givenProductionSources_whenInspectingViewModelCreation_thenOnlyApprovedGatewaysCreateInstances() {
        val root = repositoryRoot()
        val productionSources = productionSources(root)

        productionSources.forEach { sourceFile ->
            val source = sourceFile.readText()
            val isMetroGateway =
                sourceFile.invariantSeparatorsPath.endsWith("/core/di/src/main/kotlin/com/wire/android/di/metro/WireMetroViewModel.kt")
            val isItemScopeGateway =
                sourceFile.invariantSeparatorsPath.endsWith("/core/di/src/main/kotlin/com/wire/android/di/ViewModelScoped.kt")
            val isRuntimeRegistryOwner = listOf(
                "/app/src/main/kotlin/com/wire/android/navigation/runtime/SessionGraphStoreViewModel.kt",
                // The Navigation 3 registry installs a lifecycle observer used only to report the
                // actual clear after Lifecycle's provider releases every exit-animation token.
                "/core/navigation/src/main/kotlin/com/wire/android/navigation/navigation3/WireViewModelStoreNavEntryDecorator.kt",
                // CallActivityViewModel is intentionally Activity-owned. Metro's Provider only
                // supplies the initial instance; the Activity remains its ViewModelStoreOwner.
                "/app/src/main/kotlin/com/wire/android/ui/calling/CallActivity.kt",
            ).any(sourceFile.invariantSeparatorsPath::endsWith)

            if (!isMetroGateway) {
                assertFalse(
                    Regex("""\b(assistedMetroViewModel|metroViewModel)\s*[<(]""").containsMatchIn(source),
                    "Direct Metro ViewModel creation in ${sourceFile.relativeTo(root)}",
                )
                assertFalse(
                    Regex("""\bViewModelProvider\s*\(""").containsMatchIn(source),
                    "Direct ViewModelProvider creation in ${sourceFile.relativeTo(root)}",
                )
            }
            if (!isItemScopeGateway) {
                assertFalse(
                    source.contains("resacaMetroViewModelScoped"),
                    "Direct Resaca ViewModel creation in ${sourceFile.relativeTo(root)}",
                )
            }
            if (!isRuntimeRegistryOwner) {
                assertFalse(
                    source.contains("androidx.lifecycle.viewmodel.compose.viewModel"),
                    "Direct framework ViewModel creation in ${sourceFile.relativeTo(root)}",
                )
                assertFalse(
                    source.contains("viewModelFactory {") || source.contains("by viewModels"),
                    "Direct framework ViewModel factory in ${sourceFile.relativeTo(root)}",
                )
            }
            listOf(
                "LocalWireFlowViewModelStoreOwner",
                "LocalWireViewModelScopeKey",
                "sessionKeyedMetroViewModel",
                "sessionKeyedAssistedMetroViewModel",
                "previousBackStackEntry",
                "parentBackStackEntry",
                "LastKnownCurrentAccount",
                "com.ramcosta.composedestinations",
            ).forEach { forbidden ->
                assertFalse(
                    source.contains(forbidden),
                    "Legacy ownership API $forbidden in ${sourceFile.relativeTo(root)}",
                )
            }
        }
    }

    @Test
    fun givenFullyMigratedEntryHelpers_whenInspectingSources_thenOnlyWireGatewayDefinesIdentity() {
        fullyMigratedSources().forEach { sourceFile ->
            val source = sourceFile.readText()

            assertTrue(
                source.contains("wireMetroViewModel") || source.contains("wireAssistedMetroViewModel"),
                "Missing Wire ViewModel gateway in ${sourceFile.name}",
            )
            assertFalse(
                source.contains("sessionKeyed"),
                "Legacy session-keyed gateway remains in ${sourceFile.name}",
            )
            assertFalse(
                source.contains("LocalWireViewModelScopeKey"),
                "Graph identity remains part of entry ownership in ${sourceFile.name}",
            )
        }
    }

    @Test
    fun givenPreviouslyDeferredHelpers_whenInspectingSources_thenLegacyGatewayIsGone() {
        val home = sourceFile("com/wire/android/ui/home/HomeViewModelGraph.kt").readText()
        val calling = sourceFile("com/wire/android/ui/calling/CallingManualViewModelFactoryGroup.kt").readText()
        val misc = sourceFile("com/wire/android/ui/MiscViewModelGraph.kt").readText()

        listOf(home, calling, misc).forEach { source ->
            assertFalse(source.contains("sessionKeyed"))
            assertFalse(source.contains("LocalWireViewModelScopeKey"))
        }

        listOf(
            "fun analyticsUsageViewModel(): AnalyticsUsageViewModel =\n    wireMetroViewModel()",
            "fun importMediaAuthenticatedViewModel(): ImportMediaAuthenticatedViewModel =\n    wireMetroViewModel()",
        ).forEach { migratedLeaf ->
            assertTrue(misc.contains(migratedLeaf), "Entry-owned Misc leaf regressed: $migratedLeaf")
        }
        assertTrue(
            misc.contains(
                "fun e2EIEnrollmentViewModel(\n" +
                    "    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),\n" +
                    "): E2EIEnrollmentViewModel =\n" +
                    "    wireMetroViewModel(owner = owner)"
            ),
            "Flow-owned E2EI ViewModel must use the explicit owner through the Wire gateway",
        )
    }

    @Test
    fun givenMultipleInstancesInsideOneEntry_whenInspectingSources_thenExplicitInstanceKeysArePreserved() {
        val home = sourceFile("com/wire/android/ui/home/HomeViewModelGraph.kt").readText()
        val calling = sourceFile("com/wire/android/ui/calling/CallingManualViewModelFactoryGroup.kt").readText()
        val core = sourceFile(
            "com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt"
        ).readText()
        val folders = featureSourceFile(
            "com/wire/android/ui/home/conversations/ConversationFoldersViewModelGraph.kt"
        ).readText()
        val moveToFolder = featureSourceFile(
            "com/wire/android/ui/home/conversations/MoveConversationToFolderViewModelGraph.kt"
        ).readText()

        assertTrue(home.contains("instanceKey = \"list_\$conversationsSource\""))
        listOf("incoming", "outgoing", "ongoing", "shared").forEach { callType ->
            assertTrue(calling.contains("instanceKey = \"${callType}_\$conversationId\""))
        }
        assertTrue(
            core.contains(
                "conversationCoreViewModel<ConversationAssetPathsViewModelImpl>(key = conversationKey)"
            )
        )
        assertTrue(core.contains("conversationCoreViewModel(key = conversationKey)"))
        assertTrue(core.contains("instanceKey = conversationId.value"))
        assertTrue(folders.contains("instanceKey = \"conversation_folders_\${args.selectedFolderId}\""))
        assertTrue(
            moveToFolder.contains(
                "instanceKey = \"move_conversation_to_folder_\${args.conversationId}_\${args.currentFolderId}\""
            )
        )
    }

    @Test
    fun givenConversationEntryOwnedTools_whenInspectingSources_thenResacaIsReservedForListItems() {
        val source = sourceFile(
            "com/wire/android/ui/home/conversations/ScopedMessageViewModelGraph.kt"
        ).readText()

        listOf(
            "messageOptionsMenuViewModel",
            "typingIndicatorViewModel",
            "selfDeletingMessageActionViewModel",
            "isFileSharingEnabledViewModel",
            "recordAudioViewModel",
        ).forEach { functionName ->
            val declarationStart = source.lastIndexOf("fun $functionName(")
            assertTrue(declarationStart >= 0, "Missing $functionName")
            val nextDeclaration = source.indexOf("\n@Composable", declarationStart + 1)
                .takeIf { it >= 0 }
                ?: source.length
            val declaration = source.substring(declarationStart, nextDeclaration)

            assertTrue(
                declaration.contains("wireAssistedMetroViewModel"),
                "$functionName must use the entry-owned Wire gateway",
            )
            assertFalse(
                declaration.contains("wireManualMetroViewModelScoped") ||
                    declaration.contains("scopedMessageViewModel"),
                "$functionName must not use Resaca as a navigation ownership workaround",
            )
        }
    }

    private fun fullyMigratedSources(): List<File> =
        listOf(
            "com/wire/android/ui/debug/DebugInfoViewModelGraph.kt",
            "com/wire/android/ui/home/settings/SettingsViewModelGraph.kt",
            "com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt",
            "com/wire/android/ui/home/conversations/ConversationDetailsViewModelGraph.kt",
        ).map(::sourceFile)

    private fun productionSources(root: File): List<File> =
        listOf(
            File(root, "app/src/main"),
            File(root, "core"),
            File(root, "features"),
        ).flatMap { sourceRoot ->
            sourceRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
                .toList()
        }

    private fun sourceFile(relativePath: String): File {
        val root = repositoryRoot()
        return File(root, "app/src/main/kotlin/$relativePath").also {
            assertTrue(it.isFile, "Missing ${it.path}")
        }
    }

    private fun featureSourceFile(relativePath: String): File {
        val root = repositoryRoot()
        return File(root, "features/conversation/folders/src/main/kotlin/$relativePath").also {
            assertTrue(it.isFile, "Missing ${it.path}")
        }
    }

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
}
