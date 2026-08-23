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
        val movedConversationSources =
            participantTypingSources + participantAggregationSources + conversationBannerSources + messageDetailsReactionSources +
                    messageDetailsReceiptSources + messageDetailsStateSources + messageDetailsViewModelSources +
                    participantPresentationStateSources + conversationAssetPathSources + participantRendererSources
        val allowedMovedSourceImports = setOf(
            "com.wire.android.di.ScopedArgs",
            "com.wire.android.di.ViewModelScopedPreview",
            "com.wire.android.di.metro.WireAssistedViewModelBinding",
            "com.wire.android.di.metro.WireAssistedViewModelFactoryGroup",
            "com.wire.android.di.metro.wireAssistedMetroViewModel",
            "com.wire.android.di.metro.wireMetroViewModel",
            "com.wire.android.feature.conversation.R",
            "com.wire.android.mapper.UIParticipantMapper",
            "com.wire.android.mapper.UserTypeMapper",
            "com.wire.android.mapper.UsernameMapper",
            "com.wire.android.mapper.UsernameMapper.fromExpirationToHandle",
            "com.wire.android.model.Clickable",
            "com.wire.android.model.ImageAsset.UserAvatarAsset",
            "com.wire.android.model.NameBasedAvatar",
            "com.wire.android.model.UserAvatarData",
            "com.wire.android.search.widget.HighlightName",
            "com.wire.android.search.widget.HighlightSubtitle",
            "com.wire.android.ui.common.ArrowRightIcon",
            "com.wire.android.ui.common.LegalHoldIndicator",
            "com.wire.android.ui.common.MLSVerifiedIcon",
            "com.wire.android.ui.common.ProteusVerifiedIcon",
            "com.wire.android.ui.common.ProtocolLabel",
            "com.wire.android.ui.common.R",
            "com.wire.android.ui.common.UserBadge",
            "com.wire.android.ui.common.avatar.UserProfileAvatar",
            "com.wire.android.ui.common.avatar.UserProfileAvatarType",
            "com.wire.android.ui.common.avatar.UserProfileAvatarType.WithIndicators",
            "com.wire.android.ui.common.dimensions",
            "com.wire.android.ui.common.maxTitleLines",
            "com.wire.android.ui.common.divider.WireDivider",
            "com.wire.android.ui.common.rowitem.RowItemTemplate",
            "com.wire.android.ui.home.conversations.avatar",
            "com.wire.android.ui.home.conversations.details.participants.model.UIParticipant",
            "com.wire.android.ui.home.conversations.details.participants.model.MemberSectionActions",
            "com.wire.android.ui.home.conversations.details.participants.model.ParticipantsExpansionState",
            "com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData",
            "com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData",
            "com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData",
            "com.wire.android.ui.home.conversations.messagedetails.MessageDetailsNavArgs",
            "com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModel",
            "com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase",
            "com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase",
            "com.wire.android.ui.home.conversations.MessageDetailsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.name",
            "com.wire.android.ui.home.conversations.previewAsset",
            "com.wire.android.ui.home.conversations.userId",
            "com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase",
            "com.wire.android.ui.home.conversationslist.model.Membership",
            "com.wire.android.ui.theme.wireColorScheme",
            "com.wire.android.ui.theme.wireDimensions",
            "com.wire.android.ui.theme.wireTypography",
            "com.wire.android.util.EMPTY",
            "com.wire.android.util.dispatchers.DispatcherProvider",
            "com.wire.android.util.ui.FolderType",
            "com.wire.android.util.ui.sectionWithElements",
            "com.wire.android.util.uiReadReceiptDateTime",
            "dev.zacsweers.metro.Inject",
        )
        val kspPlugin = Regex("""alias\s*\(\s*libs\.plugins\.ksp\s*\)""")
        val kspProcessor = Regex("""ksp\s*\(\s*project\s*\(\s*["']:ksp["']\s*\)\s*\)""")
        val conversationPreviewAggregateName = Regex(
            """wire\.viewmodelScopedPreview\.aggregateName["']?\s*,\s*["']ConversationViewModelScopedPreviews["']""",
        )
    }
}
