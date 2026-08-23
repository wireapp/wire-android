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

package com.wire.android.feature.conversation

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationModuleBoundaryTest {

    @Test
    fun conversationFeatureBuildScriptExistsAndDoesNotDependOnApp() {
        val buildScript = featureBuildScriptText()

        assertFalse(
            forbiddenFeatureBuildScriptEntries.any { it.containsMatchIn(buildScript) },
            ":features:conversation must not declare app, flavor, BuildConfig, or Metro configuration.",
        )
    }

    @Test
    fun appDependsOnConversationThroughTheFeatureConvention() {
        val appBuildScript = appBuildScriptText()

        assertTrue(
            inboundFeatureEdge.containsMatchIn(appBuildScript),
            ":app must declare implementationWithCoverage(projects.features.conversation).",
        )
        assertTrue(
            inboundFeatureEdge.findAll(appBuildScript).count() == 1,
            ":app must declare exactly one inbound :features:conversation edge.",
        )
    }

    @Test
    fun conversationHostConfigurationContractIsPure() {
        val configurationSource = Konsist.scopeFromFile(conversationHostConfigurationRelativePath).files

        assertEquals(1, configurationSource.size, "The host configuration must have one source file.")
        assertTrue(
            configurationSource.single().hasPackage(configurationPackage),
            "ConversationHostConfiguration must declare $configurationPackage.",
        )
        configurationSource.assertFalse { sourceFile ->
            sourceFile.hasImport { importedDeclaration ->
                forbiddenImportPrefixes.any { forbiddenPrefix ->
                    importedDeclaration.name == forbiddenPrefix ||
                            importedDeclaration.name.startsWith("$forbiddenPrefix.")
                }
            }
        }
    }

    @Test
    fun conversationHostConfigurationHasTheExactHostOwnedFieldBudget() {
        val source = conversationHostConfigurationSourceText()

        assertEquals(
            runtimeCapabilityFields,
            dataClassPropertyNames(source, "ConversationRuntimeCapabilities"),
            "Runtime capabilities must remain the six host-owned BuildConfig projections.",
        )
        assertEquals(
            visibilityFields,
            dataClassPropertyNames(source, "ConversationUiVisibility"),
            "UI visibility must remain the eight host-owned visibility projections.",
        )
        assertTrue(
            staticCompositionLocalDeclaration.containsMatchIn(source),
            "ConversationHostConfiguration must use a fail-fast static CompositionLocal.",
        )
    }

    @Test
    fun movedConversationSourcesPreserveLegacyPackagesWithoutAppImplementationImports() {
        val sourceFiles = movedConversationSources.map { (relativePath, packageName) ->
            val source = Konsist.scopeFromFile(relativePath).files

            assertEquals(1, source.size, "Missing moved source $relativePath.")
            assertTrue(source.single().hasPackage(packageName), "$relativePath must preserve $packageName.")
            source.single()
        }

        sourceFiles.assertFalse { sourceFile ->
            sourceFile.hasImport { importedDeclaration ->
                val importName = importedDeclaration.name
                importName == "com.wire.android.R" ||
                        importName == "com.wire.android.BuildConfig" ||
                        (importName.startsWith("com.wire.android.") && importName !in allowedMovedSourceImports)
            }
        }
        movedConversationSources.keys.forEach { relativePath ->
            val source = File(Konsist.projectRootPath, relativePath).readText()

            assertFalse(source.contains("BuildConfig"), "$relativePath must not use app BuildConfig.")
            assertFalse(source.contains("com.wire.android.R"), "$relativePath must not use app resources.")
        }
    }

    @Test
    fun featureScopedPreviewGenerationUsesTheConversationSpecificAggregate() {
        val buildScript = featureBuildScriptText()
        val appScopedMessageGraph = File(Konsist.projectRootPath, appScopedMessageGraphRelativePath).readText()

        assertTrue(kspPlugin.containsMatchIn(buildScript), ":features:conversation must apply KSP for @ViewModelScopedPreview.")
        assertTrue(kspProcessor.containsMatchIn(buildScript), ":features:conversation must run the preview KSP processor.")
        assertTrue(
            conversationPreviewAggregateName.containsMatchIn(buildScript),
            ":features:conversation must avoid the app ViewModelScopedPreviews aggregate name.",
        )
        assertTrue(
            appScopedMessageGraph.contains("import com.wire.android.di.ConversationViewModelScopedPreviews"),
            "The app scoped-message gateway must import the feature-owned preview aggregate.",
        )
        assertEquals(
            3,
            Regex("previewProvider = ConversationViewModelScopedPreviews").findAll(appScopedMessageGraph).count(),
            "Typing and both asset-local-path branches must use the conversation aggregate.",
        )
    }

    @Test
    fun sharedParticipantTestFactoriesHaveAcyclicOwners() {
        val coreUserFactory = File(Konsist.projectRootPath, coreUserFactoryRelativePath).readText()
        val featureParticipantFactory = File(Konsist.projectRootPath, featureParticipantFactoryRelativePath).readText()
        val appBuildScript = appBuildScriptText()

        assertTrue(coreUserFactory.contains("fun testSelfUser("))
        assertTrue(coreUserFactory.contains("fun testOtherUser("))
        assertFalse(
            coreUserFactory.contains("UIParticipant"),
            ":core:ui-common test fixtures must not depend on the conversation feature.",
        )
        assertTrue(featureParticipantFactory.contains("fun testUIParticipant("))
        assertEquals(
            1,
            featureTestFixturesEdge.findAll(appBuildScript).count(),
            ":app tests must consume the conversation-specific participant fixture exactly once.",
        )
    }

    @Test
    fun messageDetailsMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, messageDetailsViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, messageDetailsViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object MessageDetailsManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<MessageDetailsViewModel, MessageDetailsManualViewModelFactory>"
            ),
            "The MessageDetails gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("ConversationCoreManualViewModelFactory"))
        assertTrue(viewModel.contains("MessageDetailsManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"messageDetailsViewModel\""))
    }

    @Test
    fun groupConversationParticipantsMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, groupConversationParticipantsViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, groupConversationParticipantsViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object GroupConversationParticipantsManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<GroupConversationParticipantsViewModel, GroupConversationParticipantsManualViewModelFactory>"
            ),
            "The participants gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("GroupConversationParticipantsManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"groupConversationParticipantsViewModel\""))
    }

    @Test
    fun groupConversationDetailsMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, groupConversationDetailsViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, groupConversationDetailsViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object GroupConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<GroupConversationDetailsViewModel, GroupConversationDetailsManualViewModelFactory>"
            ),
            "The group-details gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("GroupConversationDetailsManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"groupConversationDetailsViewModel\""))
    }

    @Test
    fun updateChannelAccessMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, updateChannelAccessViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, updateChannelAccessViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object UpdateChannelAccessManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<UpdateChannelAccessViewModel, UpdateChannelAccessManualViewModelFactory>"
            ),
            "The channel-access gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("UpdateChannelAccessManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"updateChannelAccessViewModel\""))
    }

    @Test
    fun createPasswordGuestLinkMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, createPasswordGuestLinkViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, createPasswordGuestLinkViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object CreatePasswordGuestLinkManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<CreatePasswordGuestLinkViewModel, CreatePasswordGuestLinkManualViewModelFactory>"
            ),
            "The password-link gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("CreatePasswordGuestLinkManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"createPasswordGuestLinkViewModel\""))
    }

    @Test
    fun updateAppsAccessMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, updateAppsAccessViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, updateAppsAccessViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object UpdateAppsAccessManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<UpdateAppsAccessViewModel, UpdateAppsAccessManualViewModelFactory>"
            ),
            "The apps-access gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("UpdateAppsAccessManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"updateAppsAccessViewModel\""))
    }

    @Test
    fun editGuestAccessMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, editGuestAccessViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, editGuestAccessViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object EditGuestAccessManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<EditGuestAccessViewModel, EditGuestAccessManualViewModelFactory>"
            ),
            "The guest-access gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("EditGuestAccessManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"editGuestAccessViewModel\""))
    }

    @Test
    fun editSelfDeletingMessagesMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, editSelfDeletingMessagesViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, editSelfDeletingMessagesViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object EditSelfDeletingMessagesManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<EditSelfDeletingMessagesViewModel, EditSelfDeletingMessagesManualViewModelFactory>"
            ),
            "The self-deletion gateway must keep its dedicated assisted factory type.",
        )
        assertFalse(graph.contains("object ConversationDetailsManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("EditSelfDeletingMessagesManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"editSelfDeletingMessagesViewModel\""))
    }

    @Test
    fun conversationFoldersMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, conversationFoldersViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, conversationFoldersViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object ConversationFoldersManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModelAs<\n        ConversationFoldersVMImpl,\n        ConversationFoldersVM,\n        ConversationFoldersManualViewModelFactory,"
            ),
            "The folders gateway must preserve its assisted implementation and interface types.",
        )
        assertTrue(graph.contains("instanceKey = \"conversation_folders_\${args.selectedFolderId}\""))
        assertTrue(graph.contains("previewProvider = ViewModelScopedPreviews"))
        assertFalse(graph.contains("object ConversationSearchFolderManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("ConversationFoldersManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"conversationFoldersViewModel\""))
    }

    @Test
    fun moveConversationToFolderMetroFactoryAndResourcesAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, moveConversationToFolderViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, moveConversationToFolderViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object MoveConversationToFolderManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModelAs<\n        MoveConversationToFolderVMImpl,\n        MoveConversationToFolderVM,\n        MoveConversationToFolderManualViewModelFactory,"
            ),
            "The move-to-folder gateway must preserve its assisted implementation and interface types.",
        )
        assertTrue(
            graph.contains(
                "instanceKey = \"move_conversation_to_folder_\${args.conversationId}_\${args.currentFolderId}\""
            ),
        )
        assertTrue(graph.contains("previewProvider = ViewModelScopedPreviews"))
        assertFalse(graph.contains("object ConversationSearchFolderManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("MoveConversationToFolderManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"moveConversationToFolderViewModel\""))
        assertTrue(viewModel.contains("com.wire.android.feature.conversation.R"))

        moveConversationToFolderStringsByQualifier.forEach { (qualifier, expectedStrings) ->
            val featureStrings = File(
                Konsist.projectRootPath,
                "features/conversation/src/main/res/$qualifier/strings.xml",
            ).readText()
            val appStrings = File(
                Konsist.projectRootPath,
                "app/src/main/res/$qualifier/strings.xml",
            ).readText()

            expectedStrings.forEach { expectedString ->
                assertTrue(featureStrings.contains(expectedString), "Missing feature $qualifier string: $expectedString")
            }
            moveConversationToFolderStringNames.forEach { name ->
                assertFalse(
                    appStrings.contains("name=\"$name\""),
                    "$name must not remain in app $qualifier resources.",
                )
            }
        }
    }

    @Test
    fun newFolderDirectMetroBindingAndResourcesAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, newFolderViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, newFolderViewModelRelativePath).readText()

        assertTrue(graph.contains("@BindingContainer"))
        assertTrue(graph.contains("object NewFolderMetroViewModelBindings"))
        assertTrue(graph.contains("@ViewModelKey(NewFolderViewModel::class)"))
        assertTrue(
            graph.contains("fun newFolderViewModel(viewModel: NewFolderViewModel): ViewModel = viewModel"),
            "The feature must keep the direct ViewModel map binding.",
        )
        assertTrue(graph.contains("fun newFolderViewModel(): NewFolderViewModel ="))
        assertTrue(graph.contains("wireMetroViewModel()"))
        assertTrue(viewModel.contains("com.wire.android.feature.conversation.R"))

        newFolderFailureStringsByQualifier.forEach { (qualifier, expectedString) ->
            val featureStrings = File(
                Konsist.projectRootPath,
                "features/conversation/src/main/res/$qualifier/strings.xml",
            ).readText()
            val appStrings = File(
                Konsist.projectRootPath,
                "app/src/main/res/$qualifier/strings.xml",
            ).readText()

            assertTrue(featureStrings.contains(expectedString), "Missing feature $qualifier string: $expectedString")
            assertFalse(
                appStrings.contains("name=\"new_folder_failure\""),
                "new_folder_failure must not remain in app $qualifier resources.",
            )
        }
    }

    @Test
    fun promoteAdminMetroFactoryAndParcelableContractAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, promoteAdminViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, promoteAdminViewModelRelativePath).readText()
        val navArgs = File(Konsist.projectRootPath, promoteAdminNavArgsRelativePath).readText()

        assertTrue(featureBuildScriptText().contains("id(BuildPlugins.kotlinParcelize)"))
        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object PromoteAdminManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<\n        PromoteAdminViewModel,\n        PromoteAdminManualViewModelFactory,"
            ),
            "The promote-admin gateway must keep its dedicated assisted factory type.",
        )
        assertTrue(graph.contains("fun promoteAdminViewModel(args: PromoteAdminNavArgs): PromoteAdminViewModel"))
        assertFalse(graph.contains("object ConversationSearchFolderManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("PromoteAdminManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"promoteAdminViewModel\""))
        assertTrue(navArgs.contains("@Parcelize"))
        assertTrue(navArgs.contains("@TypeParceler<QualifiedID, QualifiedIdParceler>()"))
    }

    @Test
    fun addMembersToConversationMetroFactoryIsFeatureOwned() {
        val graph = File(Konsist.projectRootPath, addMembersToConversationViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, addMembersToConversationViewModelRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object AddMembersToConversationManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<\n        AddMembersToConversationViewModel,\n        AddMembersToConversationManualViewModelFactory,"
            ),
            "The add-members gateway must keep its dedicated assisted factory type.",
        )
        assertTrue(
            graph.contains(
                "fun addMembersToConversationViewModel(\n    args: AddMembersSearchNavArgs,\n): AddMembersToConversationViewModel"
            ),
        )
        assertFalse(graph.contains("object ConversationSearchFolderManualViewModelFactoryGroup"))
        assertTrue(viewModel.contains("AddMembersToConversationManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"addMembersToConversationViewModel\""))
    }

    @Test
    fun participantRendererPreviewsRemainAppOwned() {
        val previews = File(Konsist.projectRootPath, groupParticipantPreviewsRelativePath)
        val renderer = File(Konsist.projectRootPath, groupParticipantRendererRelativePath)

        assertTrue(previews.isFile, "Group participant previews must remain an app-owned source.")
        assertEquals(
            3,
            Regex("@PreviewMultipleThemes").findAll(previews.readText()).count(),
            "The app preview source must preserve all three multi-theme previews.",
        )
        assertFalse(
            renderer.readText().contains("PreviewMultipleThemes"),
            "The feature renderer must not import the app-internal preview annotation.",
        )
    }

    private fun featureBuildScriptText(): String {
        assertTrue(featureBuildScript.isFile, "Missing :features:conversation build.gradle.kts.")
        return featureBuildScript.readText()
    }

    private fun appBuildScriptText(): String {
        assertTrue(appBuildScript.isFile, "Missing :app build.gradle.kts.")
        return appBuildScript.readText()
    }

    private fun conversationHostConfigurationSourceText(): String {
        assertTrue(conversationHostConfigurationSource.isFile, "Missing ConversationHostConfiguration.kt.")
        return conversationHostConfigurationSource.readText()
    }

    private fun dataClassPropertyNames(source: String, className: String): Set<String> {
        val declaration = Regex(
            """data class $className\((.*?)\n\)""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ).find(source)
        assertTrue(declaration != null, "Missing $className data class.")
        return Regex("""val\s+(\w+)\s*:""")
            .findAll(requireNotNull(declaration).groupValues[1])
            .map { it.groupValues[1] }
            .toSet()
    }

    private companion object {
        const val configurationPackage = "com.wire.android.feature.conversation.config"
        val featureBuildScript = File(Konsist.projectRootPath, "features/conversation/build.gradle.kts")
        val appBuildScript = File(Konsist.projectRootPath, "app/build.gradle.kts")
        val conversationHostConfigurationSource = File(
            Konsist.projectRootPath,
            conversationHostConfigurationRelativePath,
        )
        const val conversationHostConfigurationRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/feature/conversation/config/ConversationHostConfiguration.kt"
        const val appScopedMessageGraphRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ScopedMessageViewModelGraph.kt"
        const val coreUserFactoryRelativePath =
            "core/ui-common/src/testFixtures/kotlin/com/wire/android/mapper/TestUserFactory.kt"
        const val featureParticipantFactoryRelativePath =
            "features/conversation/src/testFixtures/kotlin/com/wire/android/mapper/TestUIParticipantFactory.kt"
        const val messageDetailsViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsViewModel.kt"
        const val messageDetailsViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/MessageDetailsViewModelGraph.kt"
        const val groupConversationParticipantsViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantsViewModel.kt"
        const val groupConversationParticipantsViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/GroupConversationParticipantsViewModelGraph.kt"
        const val groupConversationDetailsViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/GroupConversationDetailsViewModel.kt"
        const val groupConversationDetailsViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/GroupConversationDetailsViewModelGraph.kt"
        const val updateChannelAccessViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updatechannelaccess/UpdateChannelAccessViewModel.kt"
        const val updateChannelAccessViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/UpdateChannelAccessViewModelGraph.kt"
        const val createPasswordGuestLinkViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/createPasswordProtectedGuestLink/CreatePasswordGuestLinkViewModel.kt"
        const val createPasswordGuestLinkViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/CreatePasswordGuestLinkViewModelGraph.kt"
        const val updateAppsAccessViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updateappsaccess/UpdateAppsAccessViewModel.kt"
        const val updateAppsAccessViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/UpdateAppsAccessViewModelGraph.kt"
        const val editGuestAccessViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/EditGuestAccessViewModel.kt"
        const val editGuestAccessViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/EditGuestAccessViewModelGraph.kt"
        const val editSelfDeletingMessagesViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/EditSelfDeletingMessagesViewModel.kt"
        const val editSelfDeletingMessagesViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/EditSelfDeletingMessagesViewModelGraph.kt"
        const val conversationFoldersViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/folder/ConversationFoldersVM.kt"
        const val conversationFoldersViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationFoldersViewModelGraph.kt"
        const val moveConversationToFolderViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/folder/MoveConversationToFolderVM.kt"
        const val moveConversationToFolderViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/MoveConversationToFolderViewModelGraph.kt"
        const val newFolderViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/folder/NewFolderViewModel.kt"
        const val newFolderViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/NewFolderViewModelGraph.kt"
        const val promoteAdminViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/promoteadmin/PromoteAdminViewModel.kt"
        const val promoteAdminNavArgsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/promoteadmin/PromoteAdminNavArgs.kt"
        const val promoteAdminViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/PromoteAdminViewModelGraph.kt"
        const val addMembersToConversationViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/search/adddembertoconversation/AddMembersToConversationViewModel.kt"
        const val addMembersSearchNavArgsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/search/AddMembersSearchNavArgs.kt"
        const val addMembersToConversationViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/AddMembersToConversationViewModelGraph.kt"
        const val groupParticipantRendererRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipants.kt"
        const val groupParticipantPreviewsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantsPreviews.kt"
        val forbiddenFeatureBuildScriptEntries = listOf(
            Regex("""projects\s*\.\s*app\b"""),
            Regex("""project\s*\(\s*(?:path\s*=\s*)?[\"']\s*:\s*app\s*[\"']"""),
            Regex("""\bbuildConfigField\s*\("""),
            Regex("""\bproductFlavors\s*\{"""),
        )
        val inboundFeatureEdge = Regex(
            """implementationWithCoverage\s*\(\s*projects\s*\.\s*features\s*\.\s*conversation\s*\)""",
        )
        val featureTestFixturesEdge = Regex(
            """testImplementation\s*\(\s*testFixtures\s*\(\s*projects\s*\.\s*features\s*\.\s*conversation\s*\)\s*\)""",
        )
        val forbiddenImportPrefixes = listOf(
            "com.wire.android.BuildConfig",
            "com.wire.android.di",
            "com.wire.android.util.debug",
            "com.wire.kalium",
            "dev.zacsweers.metro",
        )
        val runtimeCapabilityFields = setOf(
            "bubbleUiEnabled",
            "pendingMessagesEnabled",
            "developerFeaturesEnabled",
            "mlsReadReceiptsEnabled",
            "privateBuild",
            "passwordProtectedGuestLinksEnabled",
        )
        val visibilityFields = setOf(
            "audioMessages",
            "shareLocation",
            "drawing",
            "emoji",
            "gif",
            "ping",
            "topBarConversationSearch",
            "messageSearch",
        )
        val staticCompositionLocalDeclaration = Regex(
            """staticCompositionLocalOf\s*<\s*ConversationHostConfiguration\s*>\s*\{(?s:.*?)error\s*\(""",
        )
        val participantTypingSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationMemberExt.kt" to
                    "com.wire.android.ui.home.conversations",
            "features/conversation/src/main/kotlin/com/wire/android/mapper/UIParticipantMapper.kt" to
                    "com.wire.android.mapper",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/model/UIParticipant.kt" to
                    "com.wire.android.ui.home.conversations.details.participants.model",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/ObserveUsersTypingInConversationUseCase.kt" to
                    "com.wire.android.ui.home.conversations.usecase",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/typing/TypingIndicatorViewModel.kt" to
                    "com.wire.android.ui.home.conversations.typing",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/typing/UsersTypingViewState.kt" to
                    "com.wire.android.ui.home.conversations.typing",
        )
        val participantAggregationSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/model/ConversationParticipantsData.kt" to
                    "com.wire.android.ui.home.conversations.details.participants.model",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/usecase/ObserveParticipantsForConversationUseCase.kt" to
                    "com.wire.android.ui.home.conversations.details.participants.usecase",
        )
        val conversationBannerSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/banner/usecase/ObserveConversationMembersByTypesUseCase.kt" to
                    "com.wire.android.ui.home.conversations.banner.usecase",
        )
        val messageDetailsReactionSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/model/MessageDetailsReactionsData.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails.model",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/usecase/ObserveReactionsForMessageUseCase.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails.usecase",
        )
        val messageDetailsReceiptSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/model/MessageDetailsReadReceiptsData.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails.model",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/usecase/ObserveReceiptsForMessageUseCase.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails.usecase",
        )
        val messageDetailsStateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsState.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails",
        )
        val messageDetailsViewModelSources = mapOf(
            messageDetailsViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.messagedetails",
            messageDetailsViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val participantPresentationStateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantsState.kt" to
                    "com.wire.android.ui.home.conversations.details.participants",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/model/ParticipantsExpansionState.kt" to
                    "com.wire.android.ui.home.conversations.details.participants.model",
        )
        val conversationAssetPathSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/ConversationAssetPathsViewModel.kt" to
                    "com.wire.android.ui.home.conversations.messages.item",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/AssetLocalPathViewModel.kt" to
                    "com.wire.android.ui.home.conversations.messages.item",
        )
        val participantRendererSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/ConversationParticipantItem.kt" to
                    "com.wire.android.ui.home.conversations.details.participants",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantList.kt" to
                    "com.wire.android.ui.home.conversations.details.participants",
        )
        val participantRendererContainerSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipants.kt" to
                    "com.wire.android.ui.home.conversations.details.participants",
        )
        val allParticipantsSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationAllParticipantsNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.participants",
            groupConversationParticipantsViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details.participants",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationAllParticipantsScreen.kt" to
                    "com.wire.android.ui.home.conversations.details.participants",
            groupConversationParticipantsViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val groupConversationOptionsStateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/GroupConversationOptionsState.kt" to
                    "com.wire.android.ui.home.conversations.details.options",
        )
        val groupConversationDetailsViewModelSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/GroupConversationDetailsNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details",
            groupConversationDetailsViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details",
            groupConversationDetailsViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val updateChannelAccessViewModelSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updatechannelaccess/UpdateChannelAccessViewModelArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.updatechannelaccess",
            updateChannelAccessViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details.updatechannelaccess",
            updateChannelAccessViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val conversationDetailsContractSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/EditConversationNameNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.metadata",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updateappsaccess/UpdateAppsAccessNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.updateappsaccess",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updateappsaccess/UpdateAppsAccessParams.kt" to
                    "com.wire.android.ui.home.conversations.details.updateappsaccess",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/updateappsaccess/UpdateAppsAccessState.kt" to
                    "com.wire.android.ui.home.conversations.details.updateappsaccess",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/EditGuestAccessNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.editguestaccess",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/EditGuestAccessParams.kt" to
                    "com.wire.android.ui.home.conversations.details.editguestaccess",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/EditGuestAccessState.kt" to
                    "com.wire.android.ui.home.conversations.details.editguestaccess",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/createPasswordProtectedGuestLink/CreatePasswordGuestLinkNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editguestaccess/createPasswordProtectedGuestLink/CreatePasswordGuestLinkState.kt" to
                    "com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink",
        )
        val createPasswordGuestLinkViewModelSources = mapOf(
            createPasswordGuestLinkViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink",
            createPasswordGuestLinkViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val updateAppsAccessViewModelSources = mapOf(
            updateAppsAccessViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details.updateappsaccess",
            updateAppsAccessViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val editGuestAccessViewModelSources = mapOf(
            editGuestAccessViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details.editguestaccess",
            editGuestAccessViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val editSelfDeletingMessagesViewModelSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/EditSelfDeletingMessagesNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.details.editselfdeletingmessages",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/editselfdeletingmessages/EditSelfDeletingMessagesState.kt" to
                    "com.wire.android.ui.home.conversations.details.editselfdeletingmessages",
            editSelfDeletingMessagesViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.details.editselfdeletingmessages",
            editSelfDeletingMessagesViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val conversationFoldersViewModelSources = mapOf(
            conversationFoldersViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.folder",
            conversationFoldersViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val moveConversationToFolderViewModelSources = mapOf(
            moveConversationToFolderViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.folder",
            moveConversationToFolderViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val newFolderViewModelSources = mapOf(
            newFolderViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.folder",
            newFolderViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val promoteAdminViewModelSources = mapOf(
            promoteAdminViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.promoteadmin",
            promoteAdminNavArgsRelativePath to
                    "com.wire.android.ui.home.conversations.promoteadmin",
            promoteAdminViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val addMembersToConversationViewModelSources = mapOf(
            addMembersSearchNavArgsRelativePath to
                    "com.wire.android.ui.home.conversations.search",
            addMembersToConversationViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.search.adddembertoconversation",
            addMembersToConversationViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val uiAssetMessageSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/messagetypes/asset/UIAssetMessage.kt" to
                    "com.wire.android.ui.home.conversations.model.messagetypes.asset",
        )
        val messageItemTemplateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageItemTemplate.kt" to
                    "com.wire.android.ui.home.conversations.messages.item",
        )
        val interceptClickableSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/InterceptClickable.kt" to
                    "com.wire.android.ui.home.conversations.messages.item",
        )
        val memberItemToMentionSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/mention/MemberItemToMention.kt" to
                    "com.wire.android.ui.home.conversations.mention",
        )
        val movedConversationSources =
            participantTypingSources + participantAggregationSources + conversationBannerSources + messageDetailsReactionSources +
                    messageDetailsReceiptSources + messageDetailsStateSources + messageDetailsViewModelSources +
                    participantPresentationStateSources + conversationAssetPathSources + participantRendererSources +
                    participantRendererContainerSources + allParticipantsSources + groupConversationOptionsStateSources +
                    groupConversationDetailsViewModelSources + updateChannelAccessViewModelSources + conversationDetailsContractSources +
                    createPasswordGuestLinkViewModelSources + updateAppsAccessViewModelSources + editGuestAccessViewModelSources +
                    editSelfDeletingMessagesViewModelSources +
                    conversationFoldersViewModelSources + moveConversationToFolderViewModelSources + newFolderViewModelSources +
                    promoteAdminViewModelSources + addMembersToConversationViewModelSources + uiAssetMessageSources +
                    messageItemTemplateSources + interceptClickableSources + memberItemToMentionSources
        val allowedMovedSourceImports = setOf(
            "com.wire.android.di.ScopedArgs",
            "com.wire.android.di.ViewModelScopedPreview",
            "com.wire.android.di.ConversationViewModelScopedPreviews",
            "com.wire.android.di.metro.WireAssistedViewModelBinding",
            "com.wire.android.di.metro.WireAssistedViewModelFactoryGroup",
            "com.wire.android.di.metro.wireAssistedMetroViewModel",
            "com.wire.android.di.metro.wireAssistedMetroViewModelAs",
            "com.wire.android.di.metro.wireMetroViewModel",
            "com.wire.android.appLogger",
            "com.wire.android.feature.conversation.R",
            "com.wire.android.feature.conversation.config.LocalConversationHostConfiguration",
            "com.wire.android.feature.conversation.config.ConversationHostConfiguration",
            "com.wire.android.mapper.UIParticipantMapper",
            "com.wire.android.mapper.UserTypeMapper",
            "com.wire.android.mapper.UsernameMapper",
            "com.wire.android.mapper.UsernameMapper.fromExpirationToHandle",
            "com.wire.android.model.Clickable",
            "com.wire.android.model.Contact",
            "com.wire.android.model.SnackBarMessage",
            "com.wire.android.model.asSnackBarMessage",
            "com.wire.android.model.ImageAsset.UserAvatarAsset",
            "com.wire.android.model.NameBasedAvatar",
            "com.wire.android.model.UserAvatarData",
            "com.wire.android.search.widget.HighlightName",
            "com.wire.android.search.widget.HighlightSubtitle",
            "com.wire.android.ui.common.ArrowRightIcon",
            "com.wire.android.ui.common.ActionsViewModel",
            "com.wire.android.ui.common.LegalHoldIndicator",
            "com.wire.android.ui.common.MLSVerifiedIcon",
            "com.wire.android.ui.common.ProteusVerifiedIcon",
            "com.wire.android.ui.common.ProtocolLabel",
            "com.wire.android.ui.common.R",
            "com.wire.android.ui.common.UserBadge",
            "com.wire.android.ui.common.colorsScheme",
            "com.wire.android.ui.common.avatar.UserProfileAvatar",
            "com.wire.android.ui.common.avatar.UserProfileAvatarType",
            "com.wire.android.ui.common.avatar.UserProfileAvatarType.WithIndicators",
            "com.wire.android.ui.common.dimensions",
            "com.wire.android.ui.common.rememberTopBarElevationState",
            "com.wire.android.ui.common.scaffold.WireScaffold",
            "com.wire.android.ui.common.topappbar.NavigationIconType",
            "com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar",
            "com.wire.android.ui.common.textfield.textAsFlow",
            "com.wire.android.ui.common.maxTitleLines",
            "com.wire.android.ui.common.divider.WireDivider",
            "com.wire.android.ui.common.progress.WireLinearProgressIndicator",
            "com.wire.android.ui.common.rowitem.RowItemTemplate",
            "com.wire.android.ui.home.conversations.avatar",
            "com.wire.android.ui.home.conversations.details.participants.model.UIParticipant",
            "com.wire.android.ui.home.conversations.details.participants.model.MemberSectionActions",
            "com.wire.android.ui.home.conversations.details.participants.model.ParticipantsExpansionState",
            "com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData",
            "com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase",
            "com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsNavArgs",
            "com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsManager",
            "com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsManagerImpl",
            "com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel",
            "com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavArgs",
            "com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel",
            "com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState",
            "com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType",
            "com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType",
            "com.wire.android.ui.home.newconversation.channelaccess.toDomainEnum",
            "com.wire.android.ui.home.newconversation.channelaccess.toUiEnum",
            "com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData",
            "com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData",
            "com.wire.android.ui.home.conversations.messagedetails.MessageDetailsNavArgs",
            "com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModel",
            "com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase",
            "com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase",
            "com.wire.android.ui.home.conversations.MessageDetailsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.GroupConversationParticipantsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.GroupConversationDetailsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.UpdateChannelAccessManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.CreatePasswordGuestLinkManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.UpdateAppsAccessManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.EditGuestAccessManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.EditSelfDeletingMessagesManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.ConversationFoldersManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.MoveConversationToFolderManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.PromoteAdminManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.AddMembersToConversationManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.QualifiedIdParceler",
            "com.wire.android.ui.home.conversations.folder.NewFolderViewModel",
            "com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminNavArgs",
            "com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModel",
            "com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs",
            "com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModel",
            "com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel",
            "com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkNavArgs",
            "com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModel",
            "com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModelArgs",
            "com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel",
            "com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessNavArgs",
            "com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel",
            "com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessNavArgs",
            "com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel",
            "com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesNavArgs",
            "com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration",
            "com.wire.android.ui.home.messagecomposer.SelfDeletionDuration",
            "com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs",
            "com.wire.android.ui.home.conversations.folder.ConversationFoldersVM",
            "com.wire.android.ui.home.conversations.folder.ConversationFoldersVMImpl",
            "com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs",
            "com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVM",
            "com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVMImpl",
            "com.wire.android.ui.home.conversations.name",
            "com.wire.android.ui.home.conversations.previewAsset",
            "com.wire.android.ui.home.conversations.userId",
            "com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase",
            "com.wire.android.ui.home.conversationslist.model.Membership",
            "com.wire.android.ui.theme.wireColorScheme",
            "com.wire.android.ui.theme.wireDimensions",
            "com.wire.android.ui.theme.wireTypography",
            "com.wire.android.ui.theme.WireTheme",
            "com.wire.android.util.EMPTY",
            "com.wire.android.util.AppsUtil",
            "com.wire.android.util.dispatchers.DispatcherProvider",
            "com.wire.android.util.ui.FolderType",
            "com.wire.android.util.ui.UIText",
            "com.wire.android.util.ui.sectionWithElements",
            "com.wire.android.util.uiReadReceiptDateTime",
            "dev.zacsweers.metro.Inject",
        )
        val kspPlugin = Regex("""alias\s*\(\s*libs\.plugins\.ksp\s*\)""")
        val kspProcessor = Regex("""ksp\s*\(\s*project\s*\(\s*["']:ksp["']\s*\)\s*\)""")
        val conversationPreviewAggregateName = Regex(
            """wire\.viewmodelScopedPreview\.aggregateName["']?\s*,\s*["']ConversationViewModelScopedPreviews["']""",
        )
        val moveConversationToFolderStringNames = setOf(
            "move_to_folder_success",
            "move_to_folder_failed",
        )
        val moveConversationToFolderStringsByQualifier = mapOf(
            "values" to listOf(
                "<string name=\"move_to_folder_success\">“%1\$s” was moved to “%2\$s”</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” could not be moved</string>",
            ),
            "values-de" to listOf(
                "<string name=\"move_to_folder_success\">„%1\$s“ wurde nach ‚%2\$s‘ verschoben</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” konnte nicht verschoben werden</string>",
            ),
            "values-hu" to listOf(
                "<string name=\"move_to_folder_success\">\\\"%1\$s\\\" áthelyezve ide: \\\"%2\$s\\\"</string>",
                "<string name=\"move_to_folder_failed\">\\\"%1\$s\\\" áthelyezése nem sikerült</string>",
            ),
            "values-pt" to listOf(
                "<string name=\"move_to_folder_success\">“%1\$s” foi movido para “%2\$s”</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” não pôde ser movido</string>",
            ),
            "values-ru" to listOf(
                "<string name=\"move_to_folder_success\">“%1\$s” был перемещен в “%2\$s”</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” не может быть перемещен</string>",
            ),
            "values-si" to listOf(
                "<string name=\"move_to_folder_success\">“%1\$s” “%2\$s” වෙත ගෙන යන ලදී.</string>",
                "<string name=\"move_to_folder_failed\">“%1\$s” ගෙනයාමට නොහැකි විය</string>",
            ),
        )
        val newFolderFailureStringsByQualifier = mapOf(
            "values" to "<string name=\"new_folder_failure\">“%1\$s” folder could not be added</string>",
            "values-de" to "<string name=\"new_folder_failure\">Ordner “%1\$s” konnte nicht hinzugefügt werden</string>",
            "values-hu" to "<string name=\"new_folder_failure\">\\\"%1\$s\\\" mappa létrehozása nem sikerült</string>",
            "values-pt" to "<string name=\"new_folder_failure\">A pasta “%1\$s” não pôde ser adicionada</string>",
            "values-ru" to "<string name=\"new_folder_failure\">Папка “%1\$s” не может быть добавлена</string>",
            "values-si" to "<string name=\"new_folder_failure\">“%1\$s” ෆෝල්ඩරය එක් කිරීමට නොහැකි විය.</string>",
        )
    }
}
