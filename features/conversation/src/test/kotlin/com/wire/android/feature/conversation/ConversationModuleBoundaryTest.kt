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
    fun participantTypingScopedPreviewGenerationUsesAFeatureSpecificAggregate() {
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
            "The app typing gateway must import the feature-owned preview aggregate.",
        )
        assertEquals(
            1,
            Regex("previewProvider = ConversationViewModelScopedPreviews").findAll(appScopedMessageGraph).count(),
            "Only the feature-owned typing preview must use the conversation aggregate.",
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
        val movedConversationSources = participantTypingSources + participantAggregationSources
        val allowedMovedSourceImports = setOf(
            "com.wire.android.di.ViewModelScopedPreview",
            "com.wire.android.mapper.UIParticipantMapper",
            "com.wire.android.mapper.UserTypeMapper",
            "com.wire.android.model.ImageAsset.UserAvatarAsset",
            "com.wire.android.model.NameBasedAvatar",
            "com.wire.android.model.UserAvatarData",
            "com.wire.android.ui.home.conversations.avatar",
            "com.wire.android.ui.home.conversations.details.participants.model.UIParticipant",
            "com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData",
            "com.wire.android.ui.home.conversations.name",
            "com.wire.android.ui.home.conversations.previewAsset",
            "com.wire.android.ui.home.conversations.userId",
            "com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase",
            "com.wire.android.ui.home.conversationslist.model.Membership",
            "com.wire.android.util.dispatchers.DispatcherProvider",
        )
        val kspPlugin = Regex("""alias\s*\(\s*libs\.plugins\.ksp\s*\)""")
        val kspProcessor = Regex("""ksp\s*\(\s*project\s*\(\s*["']:ksp["']\s*\)\s*\)""")
        val conversationPreviewAggregateName = Regex(
            """wire\.viewmodelScopedPreview\.aggregateName["']?\s*,\s*["']ConversationViewModelScopedPreviews["']""",
        )
    }
}
