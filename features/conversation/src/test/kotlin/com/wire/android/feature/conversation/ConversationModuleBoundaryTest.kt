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
    fun imageAssetPagingUsesTheExactPagingDependencyBudget() {
        val buildScript = featureBuildScriptText()

        assertEquals(
            1,
            pagingRuntimeApiDependency.findAll(buildScript).count(),
            ":features:conversation must expose exactly one Paging runtime API dependency.",
        )
        assertEquals(
            1,
            pagingTestingDependency.findAll(buildScript).count(),
            ":features:conversation must declare exactly one Paging testing dependency.",
        )
        assertFalse(
            pagingComposeDependency.containsMatchIn(buildScript),
            "The image-asset paging seam must not add Paging Compose.",
        )
    }

    @Test
    fun conversationSearchPagingUseCaseIsFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(conversationSearchPagingUseCaseRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.usecase"))
        assertEquals(
            setOf(
                "androidx.paging.PagingConfig",
                "androidx.paging.PagingData",
                "androidx.paging.flatMap",
                "com.wire.android.mapper.MessageMapper",
                "com.wire.android.ui.home.conversations.model.UIMessage",
                "com.wire.android.util.dispatchers.DispatcherProvider",
                "com.wire.kalium.logic.data.id.ConversationId",
                "com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase",
                "kotlinx.coroutines.flow.Flow",
                "kotlinx.coroutines.flow.flowOf",
                "kotlinx.coroutines.flow.flowOn",
                "kotlinx.coroutines.flow.map",
                "dev.zacsweers.metro.Inject",
                "kotlin.math.max",
            ),
            importedDeclarations(source),
        )
        assertTrue(source.contains("class GetConversationMessagesFromSearchUseCase @Inject constructor("))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(source.contains("BuildConfig"))
        assertFalse(
            File(Konsist.projectRootPath, legacyConversationSearchPagingUseCaseRelativePath).exists(),
            "$legacyConversationSearchPagingUseCaseRelativePath must be absent.",
        )
    }

    @Test
    fun conversationAssetPagingUseCaseAndPagingItemAreFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(conversationAssetPagingUseCaseRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.usecase"))
        assertEquals(
            setOf(
                "androidx.paging.PagingConfig",
                "androidx.paging.PagingData",
                "androidx.paging.flatMap",
                "androidx.paging.insertSeparators",
                "com.wire.android.mapper.MessageMapper",
                "com.wire.android.ui.common.monthYearHeader",
                "com.wire.android.ui.home.conversations.model.UIMessage",
                "com.wire.android.util.dispatchers.DispatcherProvider",
                "com.wire.kalium.logic.data.id.ConversationId",
                "com.wire.kalium.logic.feature.asset.GetPaginatedFlowOfAssetMessageByConversationIdUseCase",
                "kotlinx.coroutines.flow.Flow",
                "kotlinx.coroutines.flow.flowOn",
                "kotlinx.coroutines.flow.map",
                "kotlinx.datetime.Instant",
                "kotlinx.datetime.TimeZone",
                "kotlinx.datetime.toLocalDateTime",
                "dev.zacsweers.metro.Inject",
                "kotlin.math.max",
            ),
            importedDeclarations(source),
        )
        assertTrue(source.contains("class GetAssetMessagesFromConversationUseCase @Inject constructor("))
        assertTrue(source.contains("sealed class UIPagingItem"))
        assertTrue(source.contains("data class Message(val uiMessage: UIMessage, val date: Instant) : UIPagingItem()"))
        assertTrue(source.contains("data class Label(val date: String) : UIPagingItem()"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(source.contains("BuildConfig"))
        assertFalse(
            File(Konsist.projectRootPath, legacyConversationAssetPagingUseCaseRelativePath).exists(),
            "$legacyConversationAssetPagingUseCaseRelativePath must be absent.",
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
    fun editConversationMetadataStateSeamHasTheExactDependencyBudget() {
        val stateSource = File(Konsist.projectRootPath, editConversationMetadataStateRelativePath).readText()
        val validatorSource = File(Konsist.projectRootPath, editGroupNameValidatorRelativePath).readText()
        val viewModelSource = File(Konsist.projectRootPath, editConversationMetadataViewModelRelativePath).readText()
        val graphSource = File(Konsist.projectRootPath, editConversationMetadataViewModelGraphRelativePath).readText()

        listOf(stateSource, validatorSource, viewModelSource).forEach { source ->
            assertTrue(
                source.contains("package com.wire.android.ui.home.conversations.details.metadata"),
                "The edit metadata seam must preserve its package.",
            )
        }
        assertEquals(emptySet<String>(), importedDeclarations(stateSource))
        assertEquals(
            setOf(
                "com.wire.android.ui.common.groupname.GroupNamePolicy",
                "com.wire.android.ui.common.groupname.GroupNamePolicyResult",
            ),
            importedDeclarations(validatorSource),
            "The edit-name validator may depend only on the neutral core policy.",
        )
        assertEquals(
            setOf(
                "androidx.compose.runtime.Composable",
                "com.wire.android.di.metro.WireAssistedViewModelFactoryGroup",
                "com.wire.android.di.metro.wireAssistedMetroViewModel",
                "com.wire.android.di.metro.wireMetroViewModel",
                "com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel",
                "com.wire.android.ui.home.conversations.details.metadata.EditConversationNameNavArgs",
            ),
            importedDeclarations(graphSource),
            "The edit metadata graph must contain only feature gateway and neutral Metro imports.",
        )
        forbiddenEditMetadataViewModelImports.forEach { forbiddenImport ->
            assertFalse(
                importedDeclarations(viewModelSource).contains(forbiddenImport),
                "EditConversationMetadataViewModel must not import $forbiddenImport.",
            )
        }
        assertTrue(viewModelSource.contains("EditConversationMetadataManualViewModelFactoryGroup::class"))
        assertTrue(viewModelSource.contains("factoryMethod = \"editConversationMetadataViewModel\""))
        assertTrue(graphSource.contains("object EditConversationMetadataManualViewModelFactoryGroup"))
        assertFalse(
            File(Konsist.projectRootPath, legacyAppEditConversationMetadataViewModelRelativePath).exists(),
            "EditConversationMetadataViewModel must not remain app-owned.",
        )
    }

    @Test
    fun checkAssetRestrictionsPresentationHasFeatureOwnershipAndExactImports() {
        val dialogStateSource = featureSource(assetTooLargeDialogStateRelativePath)
        val assetBundleSource = featureSource(assetBundleRelativePath)
        val importedMediaSource = featureSource(importedMediaAssetRelativePath)
        val viewModelSource = featureSource(checkAssetRestrictionsViewModelRelativePath)
        val graphSource = featureSource(checkAssetRestrictionsViewModelGraphRelativePath)

        assertTrue(dialogStateSource.contains("package com.wire.android.ui.home.conversations"))
        assertTrue(assetBundleSource.contains("package com.wire.android.ui.home.conversations.model"))
        assertTrue(importedMediaSource.contains("package com.wire.android.ui.sharing"))
        assertTrue(viewModelSource.contains("package com.wire.android.ui.home.conversations.media"))
        assertTrue(graphSource.contains("package com.wire.android.ui.home.conversations"))
        assertEquals(
            setOf("com.wire.kalium.logic.data.asset.AttachmentType"),
            importedDeclarations(dialogStateSource),
        )
        assertEquals(assetBundleImports, importedDeclarations(assetBundleSource))
        assertEquals(
            setOf("com.wire.android.ui.home.conversations.model.AssetBundle"),
            importedDeclarations(importedMediaSource),
        )
        assertEquals(checkAssetRestrictionsViewModelImports, importedDeclarations(viewModelSource))
        assertEquals(checkAssetRestrictionsGraphImports, importedDeclarations(graphSource))
        assertTrue(assetBundleSource.contains("@TypeParceler<Path, PathParceler>()"))
        assertTrue(assetBundleSource.contains("parcel.readString().orEmpty().toPath()"))
        assertTrue(assetBundleSource.contains("parcel.writeString(this.toString())"))
        assertTrue(graphSource.contains("object CheckAssetRestrictionsMetroViewModelBindings"))
        assertTrue(graphSource.contains("@ViewModelKey(CheckAssetRestrictionsViewModel::class)"))
        legacyCheckAssetRestrictionsPaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
        val composerState = File(Konsist.projectRootPath, messageComposerViewStateRelativePath).readText()
        assertFalse(composerState.contains("sealed class AssetTooLargeDialogState"))
    }

    @Test
    fun messagePresentationPrimitivesAreFeatureOwnedWithExactDependencies() {
        val dateGroupingSource = featureSource(messageDateGroupingMapperRelativePath)
        val copyableSource = featureSource(copyableRelativePath)
        val markdownNodeSource = featureSource(markdownNodeRelativePath)

        assertTrue(dateGroupingSource.contains("package com.wire.android.mapper"))
        assertTrue(copyableSource.contains("package com.wire.android.util"))
        assertTrue(markdownNodeSource.contains("package com.wire.android.ui.markdown"))
        assertEquals(messageDateGroupingImports, importedDeclarations(dateGroupingSource))
        assertEquals(setOf("android.content.res.Resources"), importedDeclarations(copyableSource))
        assertEquals(
            setOf("kotlinx.collections.immutable.PersistentList"),
            importedDeclarations(markdownNodeSource),
        )
        assertTrue(dateGroupingSource.contains("sealed interface MessageDateTimeGroup"))
        assertTrue(copyableSource.contains("interface Copyable"))
        assertTrue(markdownNodeSource.contains("data class MarkdownPreview"))
        legacyMessagePresentationPrimitivePaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
    }

    @Test
    fun uiMessageModelClosureIsFeatureOwnedWithoutTheAppMarkdownParser() {
        val uiMessageSource = featureSource(uiMessageRelativePath)
        val uiQuotedMessageSource = featureSource(uiQuotedMessageRelativePath)

        assertTrue(uiMessageSource.contains("package com.wire.android.ui.home.conversations.model"))
        assertTrue(uiQuotedMessageSource.contains("package com.wire.android.ui.home.conversations.model"))
        assertTrue(uiMessageSource.contains("sealed interface UIMessage"))
        assertTrue(uiMessageSource.contains("val quotedMessage: UIQuotedMessage? = null"))
        assertTrue(uiQuotedMessageSource.contains("sealed class UIQuotedMessage"))
        assertTrue(uiQuotedMessageSource.contains("fun UIMessage.Regular.mapToQuotedContent()"))
        assertFalse(uiMessageSource.contains("com.wire.android.ui.markdown.MarkdownConstants"))
        assertFalse(uiMessageSource.contains("org.commonmark"))
        assertFalse(uiQuotedMessageSource.contains("org.commonmark"))
        assertFalse(featureBuildScriptText().contains("commonmark"))
        assertTrue(uiMessageSource.contains("private const val MESSAGE_PREVIEW_NON_BREAKING_SPACE = \"&nbsp;\""))
        legacyUiMessageModelPaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
    }

    @Test
    fun messageClickActionsAreFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(messageClickActionsRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertEquals(
            setOf(
                "com.wire.android.ui.home.conversations.model.MessageSenderId",
                "com.wire.android.ui.home.conversations.model.UIMessage",
                "com.wire.kalium.logic.data.id.ConversationId",
                "com.wire.kalium.logic.data.user.UserId",
            ),
            importedDeclarations(source),
        )
        assertTrue(source.contains("sealed class MessageClickActions"))
        assertTrue(source.contains("data class FullItem("))
        assertTrue(source.contains("data class Content("))
        assertFalse(
            File(Konsist.projectRootPath, legacyMessageClickActionsRelativePath).exists(),
            "$legacyMessageClickActionsRelativePath must be absent.",
        )
    }

    @Test
    fun linkPreviewMessageBodyIsFeatureOwnedWithTheAppConsumerVisibilityContract() {
        val source = featureSource(linkPreviewMessageBodyRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertEquals(
            setOf(
                "com.wire.android.ui.home.conversations.model.MessageBody",
                "com.wire.android.util.ui.UIText",
                "com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview",
            ),
            importedDeclarations(source),
        )
        assertTrue(source.contains("fun MessageBody.shouldHideStandalonePreviewedUrl(preview: MessageLinkPreview): Boolean"))
        assertFalse(source.contains("internal fun MessageBody.shouldHideStandalonePreviewedUrl"))
        assertFalse(
            File(Konsist.projectRootPath, legacyLinkPreviewMessageBodyRelativePath).exists(),
            "$legacyLinkPreviewMessageBodyRelativePath must be absent.",
        )
    }

    @Test
    fun messageAuthorRowIsFeatureOwnedWithTheLegacyPublicContract() {
        val source = featureSource(messageAuthorRowRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(source.contains("fun MessageAuthorRow("))
        assertTrue(source.contains("fun MessageSmallLabel("))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(
            File(Konsist.projectRootPath, legacyMessageAuthorRowRelativePath).exists(),
            "$legacyMessageAuthorRowRelativePath must be absent.",
        )
    }

    @Test
    fun regularMessageItemLeadingIsFeatureOwnedWithTheLegacyPublicContract() {
        val source = featureSource(regularMessageItemLeadingRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(source.contains("fun RegularMessageItemLeading("))
        assertTrue(source.contains("com.wire.android.model.Clickable"))
        assertTrue(source.contains("com.wire.android.ui.common.avatar.UserProfileAvatar"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(
            File(Konsist.projectRootPath, legacyRegularMessageItemLeadingRelativePath).exists(),
            "$legacyRegularMessageItemLeadingRelativePath must be absent.",
        )
    }

    @Test
    fun offlineMessageIndicatorIsFeatureOwnedWithTheNarrowAppConsumerContract() {
        val source = featureSource(offlineMessageIndicatorRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(source.contains("fun PagingData<UIMessage>.withOfflineIndicator("))
        assertFalse(source.contains("internal fun PagingData<UIMessage>.withOfflineIndicator("))
        assertTrue(source.contains("internal fun offlineMessage("))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(
            File(Konsist.projectRootPath, legacyOfflineMessageIndicatorRelativePath).exists(),
            "$legacyOfflineMessageIndicatorRelativePath must be absent.",
        )
    }

    @Test
    fun selfDeletionTimerStateAndItsTestAreFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(messageExpirationRelativePath)
        val testSource = File(Konsist.projectRootPath, selfDeletionTimerTestRelativePath).readText()

        assertTrue(source.contains("package com.wire.android.ui.home.conversations"))
        assertTrue(source.contains("fun rememberSelfDeletionTimer(expirationStatus: ExpirationStatus)"))
        assertTrue(source.contains("class SelfDeletionTimerHelper"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertFalse(source.contains("com.wire.android.R"))
        assertTrue(testSource.contains("package com.wire.android"))
        assertTrue(testSource.contains("class SelfDeletionTimerTest"))
        assertFalse(
            File(Konsist.projectRootPath, legacyMessageExpirationRelativePath).exists(),
            "$legacyMessageExpirationRelativePath must be absent.",
        )
        assertFalse(
            File(Konsist.projectRootPath, legacySelfDeletionTimerTestRelativePath).exists(),
            "$legacySelfDeletionTimerTestRelativePath must be absent.",
        )
    }

    @Test
    fun selfDeletionIconMetricsAndTheirTestsAreFeatureOwnedWithTheAppRendererSeam() {
        val source = featureSource(deletionIconMetricsRelativePath)
        val testSource = File(Konsist.projectRootPath, deletionIconMetricsTestRelativePath).readText()
        val appRenderer = File(Konsist.projectRootPath, messageExpirationItemsRelativePath).readText()

        assertTrue(source.contains("data class DeletionIconMetrics("))
        assertTrue(source.contains("enum class QuantizeStrategy"))
        assertTrue(source.contains("internal fun computeDeletionIconMetrics("))
        assertTrue(source.contains("fun SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable.iconMetrics("))
        assertFalse(source.contains("START_ANGLE_TOP_DEG"))
        assertFalse(source.contains("STROKE_WIDTH_FRACTION"))
        assertTrue(appRenderer.contains("private const val DELETION_ICON_START_ANGLE_TOP_DEG = -90f"))
        assertTrue(appRenderer.contains("private const val DELETION_ICON_STROKE_WIDTH_FRACTION = 0.11f"))
        assertTrue(testSource.contains("class DeletionIconMetricsTest"))
        assertEquals(7, Regex("@Test").findAll(testSource).count())
        assertFalse(File(Konsist.projectRootPath, legacyDeletionIconMetricsRelativePath).exists())
        assertFalse(File(Konsist.projectRootPath, legacyDeletionIconMetricsTestRelativePath).exists())
    }

    @Test
    fun groupConversationAvatarIsFeatureOwnedWithTheLegacyPublicContract() {
        val source = featureSource(groupConversationAvatarRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversationslist.common"))
        assertTrue(source.contains("fun GroupConversationAvatar("))
        assertTrue(source.contains("avatarData: ConversationAvatar.Group"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(File(Konsist.projectRootPath, legacyGroupConversationAvatarRelativePath).exists())
    }

    @Test
    fun participantPreviewsAreFeatureOwnedWithTheNeutralPreviewAnnotation() {
        val itemPreviews = featureSource(conversationParticipantItemPreviewsRelativePath)
        val listPreviews = featureSource(groupConversationParticipantsPreviewsRelativePath)

        listOf(itemPreviews, listPreviews).forEach { source ->
            assertTrue(source.contains("com.wire.android.ui.common.preview.MultipleThemePreviews"))
            assertFalse(source.contains("com.wire.android.util.PreviewMultipleThemes"))
            assertFalse(source.contains("com.wire.android.R"))
        }
        assertEquals(6, listOf(itemPreviews, listPreviews).sumOf { Regex("@MultipleThemePreviews").findAll(it).count() })
        assertFalse(File(Konsist.projectRootPath, legacyConversationParticipantItemPreviewsRelativePath).exists())
        assertFalse(File(Konsist.projectRootPath, legacyGroupConversationParticipantsPreviewsRelativePath).exists())
    }

    @Test
    fun messageBubbleIsFeatureOwnedWithTheLegacyPublicContract() {
        val source = featureSource(messageBubbleItemRelativePath)

        assertTrue(source.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(source.contains("fun MessageBubbleItem("))
        assertTrue(source.contains("message: UIMessage.Regular"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(File(Konsist.projectRootPath, legacyMessageBubbleItemRelativePath).exists())
    }

    @Test
    fun systemMessageLeadingAndContentContractAreFeatureOwned() {
        val leading = featureSource(systemMessageItemLeadingRelativePath)
        val content = featureSource(systemMessageContentRelativePath)
        val appFactory = File(Konsist.projectRootPath, systemMessageItemRelativePath).readText()

        assertTrue(leading.contains("fun SystemMessageItemLeading(messageContent: SystemMessageContent"))
        assertTrue(content.contains("data class SystemMessageContent("))
        assertTrue(content.contains("val annotatedStringBuilder: @Composable (expanded: Boolean) -> AnnotatedString"))
        assertTrue(appFactory.contains("fun SystemMessage.buildContent("))
        assertFalse(appFactory.contains("data class SystemMessageContent("))
        assertFalse(leading.contains("com.wire.android.R"))
        assertFalse(content.contains("com.wire.android.R"))
        assertFalse(File(Konsist.projectRootPath, legacySystemMessageItemLeadingRelativePath).exists())
    }

    @Test
    fun conversationAssetMessagesPresentationHasAFeatureOwnedGatewayAndAppAssemblyOnly() {
        val graph = featureSource(conversationAssetMessagesViewModelGraphRelativePath)
        val viewModel = featureSource(conversationAssetMessagesViewModelRelativePath)
        val state = featureSource(conversationAssetMessagesViewStateRelativePath)
        val sessionGraph = File(Konsist.projectRootPath, appSessionViewModelGraphRelativePath).readText()
        val formerCoreGraph = File(Konsist.projectRootPath, conversationCoreViewModelGraphRelativePath).readText()

        assertTrue(graph.contains("object ConversationAssetMessagesManualViewModelFactoryGroup"))
        assertTrue(graph.contains("fun conversationAssetMessagesViewModel(args: ConversationMediaNavArgs)"))
        assertTrue(viewModel.contains("ConversationAssetMessagesManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"conversationAssetMessagesViewModel\""))
        assertTrue(state.contains("data class ConversationAssetMessagesViewState("))
        assertFalse(viewModel.contains("ConversationCoreManualViewModelFactoryGroup"))
        assertFalse(viewModel.contains("com.wire.android.R"))
        assertEquals(
            1,
            Regex("ConversationAssetMessagesManualViewModelFactoryMetroBindings::class")
                .findAll(sessionGraph)
                .count(),
        )
        assertFalse(formerCoreGraph.contains("conversationAssetMessagesViewModel"))
        assertFalse(File(Konsist.projectRootPath, legacyConversationAssetMessagesViewModelRelativePath).exists())
        assertFalse(File(Konsist.projectRootPath, legacyConversationAssetMessagesViewStateRelativePath).exists())
    }

    @Test
    fun messageResourceProviderIsFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(messageResourceProviderRelativePath)

        assertTrue(source.contains("package com.wire.android.mapper"))
        assertTrue(source.contains("import androidx.annotation.StringRes"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R"))
        assertTrue(source.contains("data class MessageResourceProvider("))
        assertTrue(source.contains("memberNameDeleted: Int = R.string.member_name_deleted_label"))
        assertTrue(source.contains("memberNameYouLowercase: Int = R.string.member_name_you_label_lowercase"))
        assertTrue(source.contains("memberNameYouTitlecase: Int = R.string.member_name_you_label_titlecase"))
        assertTrue(source.contains("sentAMessageWithContent: Int = R.string.sent_a_message_with_content"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(
            File(Konsist.projectRootPath, legacyMessageResourceProviderRelativePath).exists(),
            "$legacyMessageResourceProviderRelativePath must be absent.",
        )
    }

    @Test
    fun systemMessageContentMapperAndItsTestAreFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(systemMessageContentMapperRelativePath)
        val testSource = File(Konsist.projectRootPath, systemMessageContentMapperTestRelativePath).readText()

        assertTrue(source.contains("package com.wire.android.mapper"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R"))
        assertTrue(source.contains("class SystemMessageContentMapper @Inject constructor("))
        assertTrue(source.contains("private val messageResourceProvider: MessageResourceProvider"))
        assertTrue(source.contains("enum class SelfNameType"))
        assertFalse(source.contains("com.wire.android.R"))
        assertTrue(testSource.contains("package com.wire.android.mapper"))
        assertTrue(testSource.contains("class SystemMessageContentMapperTest"))
        legacySystemMessageContentMapperPaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
    }

    @Test
    fun isoFormatterIsFeatureOwnedWithItsLegacyInjectionContract() {
        val source = featureSource(isoFormatterRelativePath)

        assertTrue(source.contains("package com.wire.android.util.time"))
        assertTrue(source.contains("class ISOFormatter @Inject constructor()"))
        assertTrue(source.contains("fun fromInstantToTimeFormatter(instant: Instant): String"))
        assertFalse(File(Konsist.projectRootPath, legacyIsoFormatterRelativePath).exists())
    }

    @Test
    fun regularMessageMapperAndItsFocusedTestAreFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(regularMessageMapperRelativePath)
        val testSource = File(Konsist.projectRootPath, regularMessageMapperTestRelativePath).readText()

        assertTrue(source.contains("package com.wire.android.mapper"))
        assertTrue(source.contains("class RegularMessageMapper @Inject constructor("))
        assertTrue(source.contains("private val messageResourceProvider: MessageResourceProvider"))
        assertTrue(source.contains("private val isoFormatter: ISOFormatter"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertFalse(source.contains("com.wire.android.R"))
        assertTrue(testSource.contains("package com.wire.android.mapper"))
        assertTrue(testSource.contains("class RegularMessageContentMapperTest"))
        assertTrue(testSource.contains("private fun regularMessage(content: MessageContent.Regular)"))
        assertFalse(testSource.contains("com.wire.android.framework.TestMessage"))
        legacyRegularMessageMapperPaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
    }

    @Test
    fun messageContentAndFinalMappersAndFocusedTestsAreFeatureOwnedWithLegacyContracts() {
        val contentMapperSource = featureSource(messageContentMapperRelativePath)
        val messageMapperSource = featureSource(messageMapperRelativePath)
        val contentMapperTestSource = File(Konsist.projectRootPath, messageContentMapperTestRelativePath).readText()
        val messageMapperTestSource = File(Konsist.projectRootPath, messageMapperTestRelativePath).readText()

        assertTrue(contentMapperSource.contains("package com.wire.android.mapper"))
        assertTrue(contentMapperSource.contains("class MessageContentMapper @Inject constructor("))
        assertTrue(contentMapperSource.contains("private val regularMessageMapper: RegularMessageMapper"))
        assertTrue(contentMapperSource.contains("private val systemMessageMapper: SystemMessageContentMapper"))
        assertTrue(messageMapperSource.contains("package com.wire.android.mapper"))
        assertTrue(messageMapperSource.contains("class MessageMapper @Inject constructor("))
        assertTrue(messageMapperSource.contains("private val userTypeMapper: UserTypeMapper"))
        assertTrue(messageMapperSource.contains("private val messageContentMapper: MessageContentMapper"))
        assertTrue(messageMapperSource.contains("private val isoFormatter: ISOFormatter"))
        assertTrue(contentMapperTestSource.contains("package com.wire.android.mapper"))
        assertTrue(contentMapperTestSource.contains("class MessageContentMapperTest"))
        assertFalse(contentMapperTestSource.contains("com.wire.android.framework.TestMessage"))
        assertTrue(messageMapperTestSource.contains("package com.wire.android.mapper"))
        assertTrue(messageMapperTestSource.contains("class MessageMapperTest"))
        assertFalse(messageMapperTestSource.contains("com.wire.android.framework.TestMessage"))
        legacyMessageContentAndFinalMapperPaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
    }

    @Test
    fun messagePreviewContentMapperAndFocusedTestAreFeatureOwnedWithTheLegacyContract() {
        val source = featureSource(messagePreviewContentMapperRelativePath)
        val testSource = File(Konsist.projectRootPath, messagePreviewContentMapperTestRelativePath).readText()

        assertTrue(source.contains("package com.wire.android.mapper"))
        assertTrue(source.contains("import com.wire.android.feature.conversation.R as conversationR"))
        assertTrue(source.contains("private const val NON_BREAKING_SPACE = \"&nbsp;\""))
        assertTrue(source.contains("fun MessagePreview?.toUIPreview("))
        assertTrue(source.contains("fun MessagePreview.uiLastMessageContent("))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(source.contains("MarkdownConstants"))
        assertTrue(testSource.contains("package com.wire.android.mapper"))
        assertTrue(testSource.contains("class MessagePreviewContentMapperTest"))
        assertFalse(testSource.contains("com.wire.android.framework.TestMessage"))
        assertFalse(testSource.contains("CoroutineTestExtension"))
        legacyMessagePreviewContentMapperPaths.forEach { relativePath ->
            assertFalse(File(Konsist.projectRootPath, relativePath).exists(), "$relativePath must be absent.")
        }
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
        assertFalse(
            File(Konsist.projectRootPath, appGetUsersForMessageUseCaseRelativePath).exists(),
            "GetUsersForMessageUseCase must not remain app-owned after its feature move.",
        )
        assertFalse(
            File(Konsist.projectRootPath, appConversationRoleProjectionRelativePath).exists(),
            "ObserveConversationRoleForUserUseCase must not remain app-owned after its feature move.",
        )
        appImageAssetPagingSourceRelativePaths.forEach { relativePath ->
            assertFalse(
                File(Konsist.projectRootPath, relativePath).exists(),
                "$relativePath must not remain app-owned after the image-asset paging move.",
            )
        }
        appConversationMediaSearchArgumentRelativePaths.forEach { relativePath ->
            assertFalse(
                File(Konsist.projectRootPath, relativePath).exists(),
                "$relativePath must not remain app-owned after the conversation argument move.",
            )
        }
    }

    @Test
    fun featureScopedPreviewGenerationUsesTheConversationSpecificAggregate() {
        val buildScript = featureBuildScriptText()
        val appScopedMessageGraph = File(Konsist.projectRootPath, appScopedMessageGraphRelativePath).readText()
        val compositeMessageGraph = File(Konsist.projectRootPath, compositeMessageViewModelGraphRelativePath).readText()

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
        assertTrue(
            compositeMessageGraph.contains("previewProvider = ConversationViewModelScopedPreviews"),
            "The feature-owned CompositeMessage Resaca gateway must use the conversation preview aggregate.",
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
    fun compositeMessageArgsViewModelAndMetroFactoryAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, compositeMessageViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, compositeMessageViewModelRelativePath).readText()
        val args = File(Konsist.projectRootPath, compositeMessageArgsRelativePath).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("object CompositeMessageManualViewModelFactoryGroup"))
        assertTrue(graph.contains("CompositeMessageManualViewModelFactory"))
        assertTrue(graph.contains("wireManualMetroViewModelScoped<"))
        assertTrue(graph.contains("previewProvider = ConversationViewModelScopedPreviews"))
        assertTrue(viewModel.contains("CompositeMessageManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"compositeMessageViewModel\""))
        assertTrue(args.contains("@Serializable"))
        assertTrue(args.contains("const val ARGS_KEY = \"CompositeMessageArgsKey\""))
        assertTrue(args.contains("override val key = \"\$ARGS_KEY:\$conversationId:\$messageId\""))
    }

    @Test
    fun conversationInfoMetroFactoryAndArgumentsAreFeatureOwned() {
        val graph = File(
            Konsist.projectRootPath,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "ConversationInfoViewModelGraph.kt",
        ).readText()
        val viewModel = File(
            Konsist.projectRootPath,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/info/" +
                    "ConversationInfoViewModel.kt",
        ).readText()
        val args = File(
            Konsist.projectRootPath,
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/info/" +
                    "ConversationInfoViewModelArgs.kt",
        ).readText()

        assertTrue(graph.contains("object ConversationInfoManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<ConversationInfoViewModel, ConversationInfoManualViewModelFactory>"
            ),
        )
        assertTrue(viewModel.contains("ConversationInfoManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"conversationInfoViewModel\""))
        assertTrue(viewModel.contains("@Assisted private val args: ConversationInfoViewModelArgs"))
        assertTrue(args.contains("val deletedAccountLabel: UIText"))
        assertFalse(viewModel.contains("ConversationNavArgs"))
        assertFalse(viewModel.contains("com.wire.android.R"))
    }

    @Test
    fun conversationCallMetroFactoryAndConversationIdContractAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, conversationCallViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, conversationCallViewModelRelativePath).readText()
        val buildScript = featureBuildScriptText()

        assertTrue(graph.contains("object ConversationCallManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<ConversationCallViewModel, ConversationCallManualViewModelFactory>"
            ),
        )
        assertTrue(graph.contains("fun conversationCallViewModel(conversationId: ConversationId)"))
        assertTrue(viewModel.contains("ConversationCallManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("@Assisted conversationId: ConversationId"))
        assertTrue(viewModel.contains("fun create(conversationId: ConversationId): ConversationCallViewModel"))
        assertTrue(viewModel.contains("val conversationId: QualifiedID = conversationId"))
        assertFalse(graph.contains("ConversationNavArgs"))
        assertFalse(viewModel.contains("ConversationNavArgs"))
        assertEquals(
            1,
            Regex("""api\s*\(\s*projects\.core\.calling\s*\)""").findAll(buildScript).count(),
            ":features:conversation must expose exactly one :core:calling API edge for callManager's public ABI.",
        )

        val allowedAndroidImports = setOf(
            "com.wire.android.di.CurrentAccount",
            "com.wire.android.di.metro.WireAssistedViewModelBinding",
            "com.wire.android.di.metro.WireAssistedViewModelFactoryGroup",
            "com.wire.android.di.metro.wireAssistedMetroViewModel",
            "com.wire.android.di.metro.wireMetroViewModel",
            "com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase",
        )
        listOf(graph, viewModel).flatMap { source ->
            Regex("""^import\s+(com\.wire\.android\.[^\s]+)$""", RegexOption.MULTILINE)
                .findAll(source)
                .map { it.groupValues[1] }
                .toList()
        }.forEach { importName ->
            assertTrue(importName in allowedAndroidImports, "Unexpected Android import: $importName")
        }
    }

    @Test
    fun conversationMigrationMetroFactoryAndConversationIdContractAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, conversationMigrationViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, conversationMigrationViewModelRelativePath).readText()

        assertTrue(graph.contains("object ConversationMigrationManualViewModelFactoryGroup"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<ConversationMigrationViewModel, " +
                    "ConversationMigrationManualViewModelFactory>"
            ),
        )
        assertTrue(graph.contains("fun conversationMigrationViewModel(conversationId: ConversationId)"))
        assertTrue(viewModel.contains("ConversationMigrationManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"conversationMigrationViewModel\""))
        assertTrue(viewModel.contains("@Assisted conversationId: ConversationId"))
        assertTrue(viewModel.contains("fun create(conversationId: ConversationId): ConversationMigrationViewModel"))
        assertTrue(viewModel.contains("private val conversationId: QualifiedID = conversationId"))
        assertFalse(graph.contains("ConversationNavArgs"))
        assertFalse(viewModel.contains("ConversationNavArgs"))
        assertFalse(graph.contains("ConversationCoreManualViewModelFactory"))
        assertFalse(viewModel.contains("ConversationCoreManualViewModelFactoryGroup"))
    }

    @Test
    fun conversationBannerViewModelFactoryAndResourcesAreFeatureOwned() {
        val graph = File(Konsist.projectRootPath, conversationBannerViewModelGraphRelativePath).readText()
        val viewModel = File(Konsist.projectRootPath, conversationBannerViewModelRelativePath).readText()

        assertFalse(
            File(Konsist.projectRootPath, appConversationBannerViewModelRelativePath).exists(),
            "ConversationBannerViewModel must not remain app-owned.",
        )
        listOf(graph, viewModel).forEach { source ->
            assertFalse(source.contains("ConversationNavArgs"), "The feature banner factory accepts only ConversationId.")
            assertFalse(source.contains("com.wire.android.R"), "The feature banner ViewModel must not use app resources.")
            assertFalse(
                source.contains("ConversationCoreManualViewModelFactory"),
                "The banner ViewModel must not leak back into the app core factory group.",
            )
        }
        assertTrue(graph.contains("object ConversationBannerManualViewModelFactoryGroup"))
        assertTrue(graph.contains("fun conversationBannerViewModel(): ConversationBannerViewModel"))
        assertTrue(graph.contains("fun conversationBannerViewModel(conversationId: ConversationId)"))
        assertTrue(
            graph.contains(
                "wireAssistedMetroViewModel<ConversationBannerViewModel, ConversationBannerManualViewModelFactory>"
            ),
        )
        assertTrue(viewModel.contains("ConversationBannerManualViewModelFactoryGroup::class"))
        assertTrue(viewModel.contains("factoryMethod = \"conversationBannerViewModel\""))
        assertTrue(viewModel.contains("@Assisted conversationId: ConversationId"))
        assertTrue(viewModel.contains("fun create(conversationId: ConversationId): ConversationBannerViewModel"))
        assertTrue(viewModel.contains("val conversationId: QualifiedID = conversationId"))
        assertTrue(viewModel.contains("import com.wire.android.feature.conversation.R"))

        val featureResources = File(Konsist.projectRootPath, conversationFeatureResourcesRelativePath)
        val appResources = File(Konsist.projectRootPath, appResourcesRelativePath)
        val crowdinConfiguration = File(Konsist.projectRootPath, crowdinConfigurationRelativePath).readText()
        val expectedBannerResourceFiles = conversationBannerResourceCountsByQualifier.keys
            .map { "$it/strings.xml" }
            .toSet()
        val actualBannerResourceFiles = featureResources.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .filter { resourceFile ->
                stringResourceIds(resourceFile).any { it in conversationBannerStateMessageIds }
            }
            .map { it.relativeTo(featureResources).invariantSeparatorsPath }
            .toSet()

        assertEquals(expectedBannerResourceFiles, actualBannerResourceFiles)
        conversationBannerResourceCountsByQualifier.forEach { (qualifier, expectedCount) ->
            val resourceFile = File(featureResources, "$qualifier/strings.xml")
            val expectedIds = if (expectedCount == conversationBannerStateMessageIds.size) {
                conversationBannerStateMessageIds
            } else {
                conversationBannerNonServiceStateMessageIds
            }
            val actualBannerIds = stringResourceIds(resourceFile)
                .filter { it in conversationBannerStateMessageIds }

            assertEquals(expectedCount, actualBannerIds.size, "Unexpected $qualifier banner count.")
            assertEquals(expectedIds, actualBannerIds.toSet(), "Unexpected $qualifier banner IDs.")
        }

        val featureDefinitions = stringResourceIds(featureResources)
        val featureStateDefinitions = featureDefinitions.filter { it in conversationBannerStateMessageIds }
        val appDefinitions = stringResourceIds(appResources)

        assertEquals(25, featureResources.walkTopDown().count { it.isFile && it.extension == "xml" })
        assertEquals(25, featureResources.listFiles().orEmpty().count { it.isDirectory })
        assertEquals(615, featureDefinitions.size)
        assertEquals(95, featureStateDefinitions.size)
        assertEquals(conversationBannerStateMessageIds, featureStateDefinitions.toSet())
        assertTrue(
            crowdinConfiguration.contains("\"source\": \"/features/conversation/src/main/res/values/strings.xml\"") &&
                    crowdinConfiguration.contains(
                        "\"translation\": \"/features/conversation/src/main/res/values-%two_letters_code%/%original_file_name%\""
                    ),
            "Conversation resources must remain registered through the standard Crowdin strings.xml mapping.",
        )
        assertTrue(
            appDefinitions.none { it in conversationBannerStateMessageIds },
            "App resources must define zero banner state messages after the ownership move.",
        )
        assertEquals(23, appDefinitions.count { it in conversationBannerSpanLabelIds })
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

    private fun stringResourceIds(resourceFile: File): List<String> {
        assertTrue(resourceFile.exists(), "Missing resource owner ${resourceFile.path}.")
        return if (resourceFile.isDirectory) {
            resourceFile.walkTopDown()
                .filter { it.isFile && it.extension == "xml" }
                .flatMap { stringResourceIds(it).asSequence() }
                .toList()
        } else {
            stringResourceName.findAll(resourceFile.readText()).map { it.groupValues[1] }.toList()
        }
    }

    private companion object {
        const val messageDateGroupingMapperRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/MessageDateGroupingMapper.kt"
        const val copyableRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/util/Copyable.kt"
        const val markdownNodeRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/markdown/MarkdownNode.kt"
        const val uiMessageRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/UIMessage.kt"
        const val uiQuotedMessageRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/UIQuotedMessage.kt"
        const val messageClickActionsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageClickActions.kt"
        const val legacyMessageClickActionsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageClickActions.kt"
        const val linkPreviewMessageBodyRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/LinkPreviewMessageBody.kt"
        const val legacyLinkPreviewMessageBodyRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/LinkPreviewMessageBody.kt"
        const val messageAuthorRowRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageAuthorRow.kt"
        const val legacyMessageAuthorRowRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageAuthorRow.kt"
        const val regularMessageItemLeadingRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/RegularMessageItemLeading.kt"
        const val legacyRegularMessageItemLeadingRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/RegularMessageItemLeading.kt"
        const val offlineMessageIndicatorRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/OfflineMessageIndicator.kt"
        const val legacyOfflineMessageIndicatorRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/OfflineMessageIndicator.kt"
        const val messageExpirationRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/MessageExpiration.kt"
        const val selfDeletionTimerTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/SelfDeletionTimerTest.kt"
        const val legacyMessageExpirationRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/MessageExpiration.kt"
        const val legacySelfDeletionTimerTestRelativePath =
            "app/src/test/kotlin/com/wire/android/SelfDeletionTimerTest.kt"
        const val deletionIconMetricsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SelfDeletionTimerHelper.kt"
        const val deletionIconMetricsTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/ui/home/conversations/messages/DeletionIconMetricsTest.kt"
        const val legacyDeletionIconMetricsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SelfDeletionTimerHelper.kt"
        const val legacyDeletionIconMetricsTestRelativePath =
            "app/src/test/kotlin/com/wire/android/ui/home/conversations/messages/DeletionIconMetricsTest.kt"
        const val messageExpirationItemsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageExpirationItems.kt"
        const val groupConversationAvatarRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversationslist/common/GroupConversationAvatar.kt"
        const val legacyGroupConversationAvatarRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversationslist/common/GroupConversationAvatar.kt"
        const val conversationParticipantItemPreviewsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/ConversationParticipantItemPreviews.kt"
        const val groupConversationParticipantsPreviewsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantsPreviews.kt"
        const val legacyConversationParticipantItemPreviewsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/ConversationParticipantItemPreviews.kt"
        const val legacyGroupConversationParticipantsPreviewsRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/GroupConversationParticipantsPreviews.kt"
        const val messageBubbleItemRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageBubbleItem.kt"
        const val legacyMessageBubbleItemRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageBubbleItem.kt"
        const val systemMessageItemLeadingRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SystemMessageItemLeading.kt"
        const val systemMessageContentRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SystemMessageContent.kt"
        const val legacySystemMessageItemLeadingRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SystemMessageItemLeading.kt"
        const val systemMessageItemRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SystemMessageItem.kt"
        const val conversationAssetMessagesViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationAssetMessagesViewModelGraph.kt"
        const val conversationAssetMessagesViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationAssetMessagesViewModel.kt"
        const val conversationAssetMessagesViewStateRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationAssetMessagesViewState.kt"
        const val legacyConversationAssetMessagesViewModelRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationAssetMessagesViewModel.kt"
        const val legacyConversationAssetMessagesViewStateRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationAssetMessagesViewState.kt"
        const val appSessionViewModelGraphRelativePath =
            "app/src/main/kotlin/com/wire/android/di/metro/AppSessionViewModelGraph.kt"
        const val conversationCoreViewModelGraphRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationCoreViewModelGraph.kt"
        const val conversationSearchPagingUseCaseRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/GetConversationMessagesFromSearchUseCase.kt"
        const val legacyConversationSearchPagingUseCaseRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/GetConversationMessagesFromSearchUseCase.kt"
        const val conversationAssetPagingUseCaseRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/GetAssetMessagesFromConversationUseCase.kt"
        const val legacyConversationAssetPagingUseCaseRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/GetAssetMessagesFromConversationUseCase.kt"
        const val messageReactionsItemRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageReactionsItem.kt"
        const val reactionPillRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/ReactionPill.kt"
        const val messageResourceProviderRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/MessageResourceProvider.kt"
        const val legacyMessageResourceProviderRelativePath =
            "app/src/main/kotlin/com/wire/android/mapper/MessageResourceProvider.kt"
        const val systemMessageContentMapperRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/SystemMessageContentMapper.kt"
        const val systemMessageContentMapperTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/mapper/SystemMessageContentMapperTest.kt"
        val legacySystemMessageContentMapperPaths = listOf(
            "app/src/main/kotlin/com/wire/android/mapper/SystemMessageContentMapper.kt",
            "app/src/test/kotlin/com/wire/android/mapper/SystemMessageContentMapperTest.kt",
        )
        const val isoFormatterRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/util/time/ISOFormatter.kt"
        const val legacyIsoFormatterRelativePath =
            "app/src/main/kotlin/com/wire/android/util/time/ISOFormatter.kt"
        const val regularMessageMapperRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/RegularMessageContentMapper.kt"
        const val regularMessageMapperTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/mapper/RegularMessageContentMapperTest.kt"
        val legacyRegularMessageMapperPaths = listOf(
            "app/src/main/kotlin/com/wire/android/mapper/RegularMessageContentMapper.kt",
            "app/src/test/kotlin/com/wire/android/mapper/RegularMessageContentMapperTest.kt",
        )
        const val messageContentMapperRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/MessageContentMapper.kt"
        const val messageMapperRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/MessageMapper.kt"
        const val messagePreviewContentMapperRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/mapper/MessagePreviewContentMapper.kt"
        const val messagePreviewContentMapperTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/mapper/MessagePreviewContentMapperTest.kt"
        val legacyMessagePreviewContentMapperPaths = listOf(
            "app/src/main/kotlin/com/wire/android/mapper/MessagePreviewContentMapper.kt",
            "app/src/test/kotlin/com/wire/android/mapper/MessagePreviewContentMapperTest.kt",
        )
        const val messageContentMapperTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/mapper/MessageContentMapperTest.kt"
        const val messageMapperTestRelativePath =
            "features/conversation/src/test/kotlin/com/wire/android/mapper/MessageMapperTest.kt"
        val legacyMessageContentAndFinalMapperPaths = listOf(
            "app/src/main/kotlin/com/wire/android/mapper/MessageContentMapper.kt",
            "app/src/main/kotlin/com/wire/android/mapper/MessageMapper.kt",
            "app/src/test/kotlin/com/wire/android/mapper/MessageContentMapperTest.kt",
            "app/src/test/kotlin/com/wire/android/mapper/MessageMapperTest.kt",
        )
        val legacyMessagePresentationPrimitivePaths = listOf(
            "app/src/main/kotlin/com/wire/android/mapper/MessageDateGroupingMapper.kt",
            "app/src/main/kotlin/com/wire/android/util/Copyable.kt",
            "app/src/main/kotlin/com/wire/android/ui/markdown/MarkdownNode.kt",
            "app/src/test/kotlin/com/wire/android/mapper/MessageDateGroupingMapperTest.kt",
        )
        val legacyUiMessageModelPaths = listOf(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/model/UIMessage.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/model/UIQuotedMessage.kt",
        )
        val messageDateGroupingImports = setOf(
            "java.time.LocalDate",
            "java.time.ZoneId",
            "java.util.Calendar",
            "kotlinx.datetime.Instant",
            "kotlinx.datetime.toJavaInstant",
        )
        const val assetTooLargeDialogStateRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/AssetTooLargeDialogState.kt"
        const val assetBundleRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/AssetBundle.kt"
        const val importedMediaAssetRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/sharing/ImportedMediaAsset.kt"
        const val checkAssetRestrictionsViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/" +
                    "CheckAssetRestrictionsViewModel.kt"
        const val checkAssetRestrictionsViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "CheckAssetRestrictionsViewModelGraph.kt"
        const val messageComposerViewStateRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/MessageComposerViewState.kt"
        val legacyCheckAssetRestrictionsPaths = listOf(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationDetailsViewModelGraph.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/CheckAssetRestrictionsViewModel.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/model/AssetBundle.kt",
            "app/src/main/kotlin/com/wire/android/ui/sharing/ImportedMediaAsset.kt",
        )
        val assetBundleImports = setOf(
            "android.net.Uri",
            "android.os.Parcel",
            "android.os.Parcelable",
            "androidx.compose.runtime.Stable",
            "com.wire.kalium.logic.data.asset.AttachmentType",
            "kotlin.math.roundToInt",
            "kotlinx.parcelize.Parceler",
            "kotlinx.parcelize.Parcelize",
            "kotlinx.parcelize.TypeParceler",
            "okio.Path",
            "okio.Path.Companion.toPath",
        )
        val checkAssetRestrictionsViewModelImports = setOf(
            "androidx.compose.runtime.getValue",
            "androidx.compose.runtime.mutableStateOf",
            "androidx.compose.runtime.setValue",
            "androidx.lifecycle.ViewModel",
            "com.wire.android.ui.home.conversations.AssetTooLargeDialogState",
            "com.wire.android.ui.home.conversations.model.AssetBundle",
            "com.wire.android.ui.sharing.ImportedMediaAsset",
            "dev.zacsweers.metro.Inject",
        )
        val checkAssetRestrictionsGraphImports = setOf(
            "androidx.compose.runtime.Composable",
            "androidx.lifecycle.ViewModel",
            "com.wire.android.di.metro.wireMetroViewModel",
            "com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel",
            "dev.zacsweers.metro.BindingContainer",
            "dev.zacsweers.metro.IntoMap",
            "dev.zacsweers.metro.Provides",
            "dev.zacsweers.metrox.viewmodel.ViewModelKey",
        )
        const val editConversationMetadataStateRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/" +
                    "EditConversationMetadataState.kt"
        const val editGroupNameValidatorRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/" +
                    "EditGroupNameValidator.kt"
        const val editConversationMetadataViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/" +
                    "EditConversationMetadataViewModel.kt"
        const val editConversationMetadataViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/" +
                    "EditConversationMetadataViewModelGraph.kt"
        const val legacyAppEditConversationMetadataViewModelRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/metadata/" +
                    "EditConversationMetadataViewModel.kt"
        val forbiddenEditMetadataViewModelImports = setOf(
            "com.wire.android.ui.common.groupname.GroupMetadataState",
            "com.wire.android.ui.common.groupname.GroupNameMode",
            "com.wire.android.ui.common.groupname.GroupNameValidator",
            "com.wire.android.R",
            "com.wire.android.BuildConfig",
            "com.wire.android.model.Contact",
            "com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType",
            "com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType",
            "com.wire.android.ui.home.newconversation.channelhistory.ChannelHistoryType",
            "com.wire.kalium.logic.data.conversation.CreateConversationParam",
            "androidx.navigation.NavController",
        )

        fun importedDeclarations(source: String): Set<String> =
            Regex("""^import\s+([^\s]+)$""", RegexOption.MULTILINE)
                .findAll(source)
                .map { it.groupValues[1] }
                .toSet()

        fun featureSource(relativePath: String): String =
            File(Konsist.projectRootPath, relativePath).also { file ->
                assertTrue(file.isFile, "Missing feature source $relativePath.")
            }.readText()

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
        const val compositeMessageViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/CompositeMessageViewModel.kt"
        const val compositeMessageArgsRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/CompositeMessageArgs.kt"
        const val compositeMessageViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/CompositeMessageViewModelGraph.kt"
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
        const val conversationCallViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/call/ConversationCallViewModel.kt"
        const val conversationCallViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/call/ConversationCallViewModelGraph.kt"
        const val conversationMigrationViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/migration/ConversationMigrationViewModel.kt"
        const val conversationMigrationViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationMigrationViewModelGraph.kt"
        const val conversationBannerViewModelRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/banner/ConversationBannerViewModel.kt"
        const val conversationBannerViewModelGraphRelativePath =
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationBannerViewModelGraph.kt"
        const val appConversationBannerViewModelRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/banner/ConversationBannerViewModel.kt"
        const val conversationFeatureResourcesRelativePath = "features/conversation/src/main/res"
        const val appResourcesRelativePath = "app/src/main/res"
        const val crowdinConfigurationRelativePath = "crowdin.yml"
        const val appGetUsersForMessageUseCaseRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/GetUsersForMessageUseCase.kt"
        const val appConversationRoleProjectionRelativePath =
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/usecase/" +
                    "ObserveConversationRoleForUserUseCase.kt"
        val appImageAssetPagingSourceRelativePaths = listOf(
            "app/src/main/kotlin/com/wire/android/mapper/UIAssetMapper.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/ObserveImageAssetMessagesFromConversationUseCase.kt",
            "app/src/main/kotlin/com/wire/android/util/time/TimeZoneProvider.kt",
        )
        val appConversationMediaSearchArgumentRelativePaths = listOf(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/media/ConversationMediaNavArgs.kt",
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                    "SearchConversationMessagesNavArgs.kt",
        )
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
            conversationBannerViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.banner",
            conversationBannerViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
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
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/options/LoadingGroupConversation.kt" to
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
        val visualMediaParamsSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/messagetypes/image/VisualMediaParams.kt" to
                    "com.wire.android.ui.home.conversations.model.messagetypes.image",
        )
        val conversationInfoStateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/info/ConversationInfoViewState.kt" to
                    "com.wire.android.ui.home.conversations.info",
        )
        val conversationInfoViewModelSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/info/ConversationInfoViewModel.kt" to
                    "com.wire.android.ui.home.conversations.info",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/info/ConversationInfoViewModelArgs.kt" to
                    "com.wire.android.ui.home.conversations.info",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationInfoViewModelGraph.kt" to
                    "com.wire.android.ui.home.conversations",
        )
        val deleteMessageDialogStateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/delete/DeleteMessageDialogState.kt" to
                    "com.wire.android.ui.home.conversations.delete",
        )
        val uiMentionSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/model/UIMention.kt" to
                    "com.wire.android.ui.home.conversations.model",
        )
        val conversationScreenDialogTypeSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationScreenDialogType.kt" to
                    "com.wire.android.ui.home.conversations",
        )
        val conversationCallViewStateSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/call/ConversationCallViewState.kt" to
                    "com.wire.android.ui.home.conversations.call",
        )
        val conversationCallViewModelSources = mapOf(
            conversationCallViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.call",
            conversationCallViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations.call",
        )
        val conversationMigrationViewModelSources = mapOf(
            conversationMigrationViewModelRelativePath to
                    "com.wire.android.ui.home.conversations.migration",
            conversationMigrationViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
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
        val messageDetailsEmptyScreenTextSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsEmptyScreenText.kt" to
                    "com.wire.android.ui.home.conversations.messagedetails",
        )
        val compositeMessageSources = mapOf(
            compositeMessageViewModelRelativePath to
                    "com.wire.android.ui.home.conversations",
            compositeMessageArgsRelativePath to
                    "com.wire.android.ui.home.conversations.model",
            compositeMessageViewModelGraphRelativePath to
                    "com.wire.android.ui.home.conversations",
        )
        val getUsersForMessageUseCaseSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/GetUsersForMessageUseCase.kt" to
                    "com.wire.android.ui.home.conversations.usecase",
        )
        val conversationRoleProjectionSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/details/participants/usecase/" +
                    "ObserveConversationRoleForUserUseCase.kt" to
                    "com.wire.android.ui.home.conversations.details.participants.usecase",
        )
        val imageAssetPagingSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/mapper/UIAssetMapper.kt" to
                    "com.wire.android.mapper",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/usecase/ObserveImageAssetMessagesFromConversationUseCase.kt" to
                    "com.wire.android.ui.home.conversations.usecase",
            "features/conversation/src/main/kotlin/com/wire/android/util/time/TimeZoneProvider.kt" to
                    "com.wire.android.util.time",
        )
        val conversationMediaSearchArgumentSources = mapOf(
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/media/" +
                    "ConversationMediaNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.media",
            "features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/search/messages/" +
                    "SearchConversationMessagesNavArgs.kt" to
                    "com.wire.android.ui.home.conversations.search.messages",
        )
        val uiMessageModelSources = mapOf(
            uiMessageRelativePath to "com.wire.android.ui.home.conversations.model",
            uiQuotedMessageRelativePath to "com.wire.android.ui.home.conversations.model",
        )
        val messageClickActionsSources = mapOf(
            messageClickActionsRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val linkPreviewMessageBodySources = mapOf(
            linkPreviewMessageBodyRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val messageAuthorRowSources = mapOf(
            messageAuthorRowRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val regularMessageItemLeadingSources = mapOf(
            regularMessageItemLeadingRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val offlineMessageIndicatorSources = mapOf(
            offlineMessageIndicatorRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val groupConversationAvatarSources = mapOf(
            groupConversationAvatarRelativePath to "com.wire.android.ui.home.conversationslist.common",
        )
        val participantPreviewSources = mapOf(
            conversationParticipantItemPreviewsRelativePath to
                    "com.wire.android.ui.home.conversations.details.participants",
            groupConversationParticipantsPreviewsRelativePath to
                    "com.wire.android.ui.home.conversations.details.participants",
        )
        val messageBubbleItemSources = mapOf(
            messageBubbleItemRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val systemMessageLeadingSources = mapOf(
            systemMessageItemLeadingRelativePath to "com.wire.android.ui.home.conversations.messages.item",
            systemMessageContentRelativePath to "com.wire.android.ui.home.conversations.messages.item",
        )
        val conversationAssetMessagesPresentationSources = mapOf(
            conversationAssetMessagesViewModelGraphRelativePath to "com.wire.android.ui.home.conversations",
            conversationAssetMessagesViewModelRelativePath to "com.wire.android.ui.home.conversations.media",
            conversationAssetMessagesViewStateRelativePath to "com.wire.android.ui.home.conversations.media",
        )
        val conversationSearchPagingUseCaseSources = mapOf(
            conversationSearchPagingUseCaseRelativePath to "com.wire.android.ui.home.conversations.usecase",
        )
        val conversationAssetPagingUseCaseSources = mapOf(
            conversationAssetPagingUseCaseRelativePath to "com.wire.android.ui.home.conversations.usecase",
        )
        val reactionPresentationSources = mapOf(
            messageReactionsItemRelativePath to "com.wire.android.ui.home.conversations.messages.item",
            reactionPillRelativePath to "com.wire.android.ui.home.conversations.messages",
        )
        val messageResourceProviderSources = mapOf(
            messageResourceProviderRelativePath to "com.wire.android.mapper",
        )
        val systemMessageContentMapperSources = mapOf(
            systemMessageContentMapperRelativePath to "com.wire.android.mapper",
        )
        val isoFormatterSources = mapOf(
            isoFormatterRelativePath to "com.wire.android.util.time",
        )
        val regularMessageMapperSources = mapOf(
            regularMessageMapperRelativePath to "com.wire.android.mapper",
        )
        val messagePreviewContentMapperSources = mapOf(
            messagePreviewContentMapperRelativePath to "com.wire.android.mapper",
        )
        val messageContentAndFinalMapperSources = mapOf(
            messageContentMapperRelativePath to "com.wire.android.mapper",
            messageMapperRelativePath to "com.wire.android.mapper",
        )
        val movedConversationSources =
            participantTypingSources + participantAggregationSources + conversationBannerSources + messageDetailsReactionSources +
                    messageDetailsReceiptSources + messageDetailsStateSources + messageDetailsViewModelSources +
                    participantPresentationStateSources + conversationAssetPathSources + participantRendererSources +
                    participantRendererContainerSources + allParticipantsSources + groupConversationOptionsStateSources +
                    groupConversationDetailsViewModelSources + updateChannelAccessViewModelSources + conversationDetailsContractSources +
                    createPasswordGuestLinkViewModelSources + updateAppsAccessViewModelSources + editGuestAccessViewModelSources +
                    editSelfDeletingMessagesViewModelSources +
                    promoteAdminViewModelSources + addMembersToConversationViewModelSources + uiAssetMessageSources +
                    visualMediaParamsSources + conversationInfoStateSources + conversationInfoViewModelSources +
                    deleteMessageDialogStateSources + uiMentionSources +
                    conversationScreenDialogTypeSources + conversationCallViewStateSources + conversationCallViewModelSources +
                    conversationMigrationViewModelSources +
                    messageItemTemplateSources + interceptClickableSources +
                    memberItemToMentionSources +
                    messageDetailsEmptyScreenTextSources + compositeMessageSources +
                    getUsersForMessageUseCaseSources + conversationRoleProjectionSources + imageAssetPagingSources +
                    conversationMediaSearchArgumentSources + uiMessageModelSources + messageClickActionsSources +
                    linkPreviewMessageBodySources + messageAuthorRowSources + regularMessageItemLeadingSources +
                    offlineMessageIndicatorSources + groupConversationAvatarSources + participantPreviewSources +
                    messageBubbleItemSources + systemMessageLeadingSources + conversationAssetMessagesPresentationSources +
                    conversationSearchPagingUseCaseSources + conversationAssetPagingUseCaseSources +
                    reactionPresentationSources +
                    messageResourceProviderSources +
                    systemMessageContentMapperSources + isoFormatterSources + regularMessageMapperSources +
                    messageContentAndFinalMapperSources + messagePreviewContentMapperSources
        val allowedMovedSourceImports = setOf(
            "com.wire.android.di.ScopedArgs",
            "com.wire.android.di.ViewModelScopedPreview",
            "com.wire.android.di.ConversationViewModelScopedPreviews",
            "com.wire.android.di.CurrentAccount",
            "com.wire.android.di.metro.WireAssistedViewModelBinding",
            "com.wire.android.di.metro.WireAssistedViewModelFactoryGroup",
            "com.wire.android.di.metro.wireAssistedMetroViewModel",
            "com.wire.android.di.metro.wireAssistedMetroViewModelAs",
            "com.wire.android.di.metro.wireMetroViewModel",
            "com.wire.android.di.wireManualMetroViewModelScoped",
            "com.wire.android.appLogger",
            "com.wire.android.feature.conversation.R",
            "com.wire.android.mapper.MessageDateTimeGroup",
            "com.wire.android.mapper.MessageMapper",
            "com.wire.android.mapper.groupedUIMessageDateTime",
            "com.wire.android.mapper.shouldDisplayDatesDifferenceDivider",
            "com.wire.android.feature.conversation.config.LocalConversationHostConfiguration",
            "com.wire.android.feature.conversation.config.ConversationHostConfiguration",
            "com.wire.android.mapper.UIParticipantMapper",
            "com.wire.android.mapper.UIAssetMapper",
            "com.wire.android.mapper.UserTypeMapper",
            "com.wire.android.mapper.UsernameMapper",
            "com.wire.android.mapper.UsernameMapper.fromExpirationToHandle",
            "com.wire.android.model.Clickable",
            "com.wire.android.model.Contact",
            "com.wire.android.model.SnackBarMessage",
            "com.wire.android.model.asSnackBarMessage",
            "com.wire.android.model.ImageAsset",
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
            "com.wire.android.ui.common.applyIf",
            "com.wire.android.ui.common.button.WireSecondaryButton",
            "com.wire.android.ui.common.button.wireSecondaryButtonColors",
            "com.wire.android.ui.common.colorsScheme",
            "com.wire.android.ui.common.avatar.UserProfileAvatar",
            "com.wire.android.ui.common.avatar.UserProfileAvatarType",
            "com.wire.android.ui.common.avatar.UserProfileAvatarType.WithIndicators",
            "com.wire.android.ui.common.dimensions",
            "com.wire.android.ui.common.typography",
            "com.wire.android.ui.common.preview.MultipleThemePreviews",
            "com.wire.android.ui.common.rememberTopBarElevationState",
            "com.wire.android.ui.common.shimmerPlaceholder",
            "com.wire.android.ui.common.scaffold.WireScaffold",
            "com.wire.android.ui.common.topappbar.NavigationIconType",
            "com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar",
            "com.wire.android.ui.common.textfield.textAsFlow",
            "com.wire.android.ui.common.maxTitleLines",
            "com.wire.android.ui.common.monthYearHeader",
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
            "com.wire.android.ui.home.conversations.info.ConversationInfoViewModel",
            "com.wire.android.ui.home.conversations.info.ConversationInfoViewModelArgs",
            "com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase",
            "com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase",
            "com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage",
            "com.wire.android.ui.home.conversations.MessageDetailsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.CompositeMessageManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.model.CompositeMessageArgs",
            "com.wire.android.ui.home.conversations.ConversationInfoManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.ConversationMigrationManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel",
            "com.wire.android.ui.home.conversations.ConversationBannerManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel",
            "com.wire.android.ui.home.conversations.banner.usecase.ObserveConversationMembersByTypesUseCase",
            "com.wire.android.ui.home.conversations.GroupConversationParticipantsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.GroupConversationDetailsManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.UpdateChannelAccessManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.CreatePasswordGuestLinkManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.UpdateAppsAccessManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.EditGuestAccessManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.EditSelfDeletingMessagesManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.PromoteAdminManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.AddMembersToConversationManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.ConversationAssetMessagesManualViewModelFactoryGroup",
            "com.wire.android.ui.home.conversations.QualifiedIdParceler",
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
            "com.wire.android.ui.home.conversations.findUser",
            "com.wire.android.ui.home.conversations.model.DEFAULT_LOCATION_ZOOM",
            "com.wire.android.ui.home.conversations.model.DeliveryStatusContent",
            "com.wire.android.ui.home.conversations.model.MessageBody",
            "com.wire.android.ui.home.conversations.model.UILastMessageContent",
            "com.wire.android.ui.home.conversations.model.MessageButton",
            "com.wire.android.ui.home.conversations.model.ExpirationStatus",
            "com.wire.android.ui.home.conversations.model.MessageEditStatus",
            "com.wire.android.ui.home.conversations.model.MessageFlowStatus",
            "com.wire.android.ui.home.conversations.model.MessageFooter",
            "com.wire.android.ui.home.conversations.model.MessageHeader",
            "com.wire.android.ui.home.conversations.model.MessageSenderId",
            "com.wire.android.ui.home.conversations.model.MessageSource",
            "com.wire.android.ui.home.conversations.model.MessageStatus",
            "com.wire.android.ui.home.conversations.model.MessageTime",
            "com.wire.android.ui.home.conversations.model.Reaction",
            "com.wire.android.ui.home.conversations.messages.ReactionPill",
            "com.wire.android.ui.home.conversations.messages.item.interceptCombinedClickable",
            "com.wire.android.ui.home.conversations.model.UIMessage",
            "com.wire.android.ui.home.conversations.model.UIMessageContent",
            "com.wire.android.ui.home.conversations.model.UIQuotedMessage",
            "com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModel",
            "com.wire.android.ui.home.conversations.media.ConversationMediaNavArgs",
            "com.wire.android.ui.home.conversations.info.ConversationAvatar",
            "com.wire.android.ui.home.conversations.model.messagetypes.image.VisualMediaParams",
            "com.wire.android.ui.home.conversations.model.messagetypes.image.MaxBounds",
            "com.wire.android.ui.home.messagecomposer.SelfDeletionDuration",
            "com.wire.android.ui.markdown.MarkdownNode",
            "com.wire.android.ui.markdown.MarkdownPreview",
            "com.wire.android.ui.theme.Accent",
            "com.wire.android.ui.theme.color",
            "com.wire.android.util.Copyable",
            "com.wire.android.ui.home.conversations.name",
            "com.wire.android.ui.home.conversations.previewAsset",
            "com.wire.android.ui.home.conversations.userId",
            "com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase",
            "com.wire.android.ui.home.conversations.usecase.GetAssetMessagesFromConversationUseCase",
            "com.wire.android.ui.home.conversations.usecase.ObserveImageAssetMessagesFromConversationUseCase",
            "com.wire.android.ui.home.conversations.usecase.UIImageAssetPagingItem",
            "com.wire.android.ui.home.conversations.usecase.UIPagingItem",
            "com.wire.android.ui.home.conversationslist.model.Membership",
            "com.wire.android.ui.theme.wireColorScheme",
            "com.wire.android.ui.theme.wireDimensions",
            "com.wire.android.ui.theme.wireTypography",
            "com.wire.android.ui.theme.WireTheme",
            "com.wire.android.util.EMPTY",
            "com.wire.android.util.SupportPage",
            "com.wire.android.util.AppsUtil",
            "com.wire.android.util.dispatchers.DispatcherProvider",
            "com.wire.android.util.time.TimeZoneProvider",
            "com.wire.android.util.time.ISOFormatter",
            "com.wire.android.util.ui.FolderType",
            "com.wire.android.util.ui.UIText",
            "com.wire.android.util.ui.UiTextResolver",
            "com.wire.android.util.formatFullDateShortTime",
            "com.wire.android.util.ui.toUIText",
            "com.wire.android.util.uiMessageDateTime",
            "com.wire.android.util.ui.sectionWithElements",
            "com.wire.android.util.uiReadReceiptDateTime",
            "dev.zacsweers.metro.Inject",
        )
        val kspPlugin = Regex("""alias\s*\(\s*libs\.plugins\.ksp\s*\)""")
        val pagingRuntimeApiDependency = Regex("""api\s*\(\s*libs\.androidx\.paging3\s*\)""")
        val pagingTestingDependency = Regex("""testImplementation\s*\(\s*libs\.androidx\.paging\.testing\s*\)""")
        val pagingComposeDependency = Regex("""libs\.androidx\.paging3Compose\b""")
        val kspProcessor = Regex("""ksp\s*\(\s*project\s*\(\s*["']:ksp["']\s*\)\s*\)""")
        val conversationPreviewAggregateName = Regex(
            """wire\.viewmodelScopedPreview\.aggregateName["']?\s*,\s*["']ConversationViewModelScopedPreviews["']""",
        )
        val stringResourceName = Regex("<string\\b[^>]*\\bname=\"([^\"]+)\"")
        val conversationBannerStateMessageIds = setOf(
            "conversation_banner_federated_externals_guests_services_present",
            "conversation_banner_federated_externals_guests_present",
            "conversation_banner_federated_externals_services_present",
            "conversation_banner_federated_guests_services_present",
            "conversation_banner_externals_guests_services_present",
            "conversation_banner_federated_services_present",
            "conversation_banner_federated_guests_present",
            "conversation_banner_federated_externals_present",
            "conversation_banner_externals_services_present",
            "conversation_banner_externals_guests_present",
            "conversation_banner_guests_services_present",
            "conversation_banner_federated_present",
            "conversation_banner_guests_present",
            "conversation_banner_externals_present",
            "conversation_banner_services_active",
        )
        val conversationBannerNonServiceStateMessageIds = conversationBannerStateMessageIds
            .filterNot { it.contains("services") }
            .toSet()
        val conversationBannerSpanLabelIds = setOf(
            "conversation_banner_federated",
            "conversation_banner_externals",
            "conversation_banner_guests",
            "conversation_banner_services",
        )
        val conversationBannerResourceCountsByQualifier = linkedMapOf(
            "values" to 15,
            "values-de" to 15,
            "values-es" to 15,
            "values-ru" to 15,
            "values-hu" to 7,
            "values-it" to 7,
            "values-pl" to 7,
            "values-pt" to 7,
            "values-si" to 7,
        )
    }
}
