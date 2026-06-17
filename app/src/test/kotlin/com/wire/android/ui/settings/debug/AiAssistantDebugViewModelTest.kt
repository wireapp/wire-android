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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.ui.settings.debug

import app.cash.turbine.test
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.aiassistant.AiEmbeddingModelManager
import com.wire.android.feature.aiassistant.AiInferenceBackend
import com.wire.android.feature.aiassistant.AiInferenceConfig
import com.wire.android.feature.aiassistant.AiInferenceConfigStore
import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.model.FailureReason
import com.wire.android.feature.aiassistant.test.AiModelHealthCheckResult
import com.wire.android.feature.aiassistant.test.AiModelTestEngine
import com.wire.android.ui.debug.AiAssistantDebugViewModelImpl
import com.wire.android.ui.debug.AiModelHealthCheckState
import com.wire.android.ui.debug.AiModelUiStatus
import com.wire.android.ui.debug.extractFirstUrl
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.resolveForTest
import com.wire.android.workmanager.worker.CreateMessageEmbeddingsWorkScheduler
import com.wire.android.workmanager.worker.CreateMessageEmbeddingsWorkStatus
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.SearchMessagesSemanticallyGloballyUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AiAssistantDebugViewModelTest {

    @Test
    fun `given ai model is not downloaded, then ai model option shows download button`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .arrange()

        assertEquals(AiModelUiStatus.NotDownloaded, viewModel.state.aiModelOptionState.status)
        assertEquals(true, viewModel.state.aiModelOptionState.showDownloadButton)
        assertEquals(false, viewModel.state.aiModelOptionState.isDownloading)
    }

    @Test
    fun `given ai model is downloading with progress, then ai model option shows loading button`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Downloading(0.5F))
            .arrange()

        assertEquals(AiModelUiStatus.Downloading(0.5F), viewModel.state.aiModelOptionState.status)
        assertEquals(true, viewModel.state.aiModelOptionState.showDownloadButton)
        assertEquals(true, viewModel.state.aiModelOptionState.isDownloading)
    }

    @Test
    fun `given ai model is ready, then ai model option hides download button`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .arrange()

        assertEquals(AiModelUiStatus.Downloaded, viewModel.state.aiModelOptionState.status)
        assertEquals(false, viewModel.state.aiModelOptionState.showDownloadButton)
        assertEquals(false, viewModel.state.aiModelOptionState.isDownloading)
    }

    @Test
    fun `given embedding model is not downloaded, then embedding model option shows download button`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.NotDownloaded)
            .arrange()

        assertEquals(AiModelUiStatus.NotDownloaded, viewModel.state.embeddingModelOptionState.status)
        assertEquals(true, viewModel.state.embeddingModelOptionState.showDownloadButton)
        assertEquals(false, viewModel.state.embeddingModelOptionState.isDownloading)
    }

    @Test
    fun `given embedding model is downloading with progress, then embedding model option shows loading button`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.Downloading(0.5F))
            .arrange()

        assertEquals(AiModelUiStatus.Downloading(0.5F), viewModel.state.embeddingModelOptionState.status)
        assertEquals(true, viewModel.state.embeddingModelOptionState.showDownloadButton)
        assertEquals(true, viewModel.state.embeddingModelOptionState.isDownloading)
    }

    @Test
    fun `given embedding model is ready, then embedding model option hides download button`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.Ready("embeddingPath"))
            .arrange()

        assertEquals(AiModelUiStatus.Downloaded, viewModel.state.embeddingModelOptionState.status)
        assertEquals(false, viewModel.state.embeddingModelOptionState.showDownloadButton)
        assertEquals(false, viewModel.state.embeddingModelOptionState.isDownloading)
        coVerify(exactly = 0) { arrangement.aiModelTestEngine.runHealthCheck("embeddingPath", any()) }
    }

    @Test
    fun `given ai model is ready, then health check is started automatically`() = runTest {
        // given
        val (arrangement, _) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .arrange()

        // then
        coVerify(exactly = 1) { arrangement.aiModelTestEngine.runHealthCheck("localPath", AiInferenceConfig.DEFAULT) }
    }

    @Test
    fun `given ai model is not downloaded, then health check is not started`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .arrange()

        // then
        assertEquals(AiModelHealthCheckState.Unavailable, viewModel.state.healthCheckState)
        coVerify(exactly = 0) { arrangement.aiModelTestEngine.runHealthCheck(any(), any()) }
    }

    @Test
    fun `given ai model is downloading, then health check is not started`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Downloading(0.5F))
            .arrange()

        // then
        assertEquals(AiModelHealthCheckState.Unavailable, viewModel.state.healthCheckState)
        coVerify(exactly = 0) { arrangement.aiModelTestEngine.runHealthCheck(any(), any()) }
    }

    @Test
    fun `given ai model is ready, when health check is suspended, then running state is shown`() = runTest {
        // given
        val healthCheckResult = CompletableDeferred<AiModelHealthCheckResult>()
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .withAiModelHealthCheckResult { healthCheckResult.await() }
            .arrange()

        // then
        assertEquals(AiModelHealthCheckState.Running, viewModel.state.healthCheckState)
    }

    @Test
    fun `given ai model is ready, when health check succeeds, then healthy state is shown`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .withAiModelHealthCheckResult(AiModelHealthCheckResult.Healthy)
            .arrange()

        // then
        assertEquals(AiModelHealthCheckState.Healthy, viewModel.state.healthCheckState)
    }

    @Test
    fun `given ai model is ready, when health check returns empty response, then failed state is shown`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .withAiModelHealthCheckResult(AiModelHealthCheckResult.EmptyResponse)
            .arrange()

        // then
        assertEquals(
            AiModelHealthCheckState.Failed("Model returned an empty response"),
            viewModel.state.healthCheckState
        )
    }

    @Test
    fun `given ai model is ready, when health check fails, then failed state is shown`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .withAiModelHealthCheckResult(AiModelHealthCheckResult.InferenceFailed("Inference failed"))
            .arrange()

        // then
        assertEquals(AiModelHealthCheckState.Failed("Inference failed"), viewModel.state.healthCheckState)
    }

    @Test
    fun `given same ready model path is emitted twice, then health check is started once`() = runTest {
        // given
        val (arrangement, _) = AiAssistantDebugArrangement()
            .withAiModelStatuses(AiModelStatus.Ready("localPath"), AiModelStatus.Ready("localPath"))
            .arrange()

        // then
        coVerify(exactly = 1) { arrangement.aiModelTestEngine.runHealthCheck("localPath", AiInferenceConfig.DEFAULT) }
    }

    @Test
    fun `given stored inference config, then state contains inference config`() = runTest {
        val config = AiInferenceConfig(backend = AiInferenceBackend.CPU, cpuThreads = 4)
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withInferenceConfig(config)
            .arrange()

        assertEquals(config, viewModel.state.inferenceConfig)
    }

    @Test
    fun `given ready model, when cpu inference is selected, then config is persisted and health check reruns`() = runTest {
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withInferenceConfig(AiInferenceConfig(backend = AiInferenceBackend.GPU, cpuThreads = 4))
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .arrange()

        viewModel.selectInferenceBackend(AiInferenceBackend.CPU)

        val expectedConfig = AiInferenceConfig(backend = AiInferenceBackend.CPU, cpuThreads = 4)
        assertEquals(expectedConfig, arrangement.inferenceConfigStore.config.value)
        coVerify(exactly = 1) { arrangement.aiModelTestEngine.runHealthCheck("localPath", expectedConfig) }
    }

    @Test
    fun `given ready model, when cpu threads are selected, then config is persisted and health check reruns`() = runTest {
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .arrange()

        viewModel.selectCpuThreads(4)

        val expectedConfig = AiInferenceConfig(backend = AiInferenceBackend.CPU, cpuThreads = 4)
        assertEquals(expectedConfig, arrangement.inferenceConfigStore.config.value)
        coVerify(exactly = 1) { arrangement.aiModelTestEngine.runHealthCheck("localPath", expectedConfig) }
    }

    @Test
    fun `given ready model, when gpu inference test succeeds, then gpu config is persisted`() = runTest {
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .withAiModelHealthCheckResult(AiModelHealthCheckResult.Healthy)
            .arrange()

        viewModel.selectInferenceBackend(AiInferenceBackend.GPU)

        val expectedConfig = AiInferenceConfig(backend = AiInferenceBackend.GPU)
        assertEquals(expectedConfig, arrangement.inferenceConfigStore.config.value)
        assertEquals(AiModelHealthCheckState.Healthy, viewModel.state.healthCheckState)
        coVerify(exactly = 1) { arrangement.aiModelTestEngine.runHealthCheck("localPath", expectedConfig) }
    }

    @Test
    fun `given ready model, when gpu inference test fails, then gpu config is not persisted`() = runTest {
        val initialConfig = AiInferenceConfig(backend = AiInferenceBackend.CPU, cpuThreads = 2)
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withInferenceConfig(initialConfig)
            .withAiModelStatus(AiModelStatus.Ready("localPath"))
            .withAiModelHealthCheckResult(AiModelHealthCheckResult.InferenceFailed("GPU unavailable"))
            .arrange()

        viewModel.selectInferenceBackend(AiInferenceBackend.GPU)

        assertEquals(initialConfig, arrangement.inferenceConfigStore.config.value)
        assertEquals(AiModelHealthCheckState.Failed("GPU unavailable"), viewModel.state.healthCheckState)
        coVerify(exactly = 1) {
            arrangement.aiModelTestEngine.runHealthCheck(
                "localPath",
                AiInferenceConfig(backend = AiInferenceBackend.GPU, cpuThreads = 2)
            )
        }
    }

    @Test
    fun `given ai model is not downloaded, when downloading, then model download is started`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.Ready("localPath"))
            .arrange()

        // when
        viewModel.downloadAiModel()

        // then
        verify(exactly = 1) { arrangement.aiModelManager.downloadModel() }
    }

    @Test
    fun `given embedding model is not downloaded, when downloading, then embedding model download is started`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.NotDownloaded)
            .withEmbeddingModelDownloadState(AiModelDownloadState.Ready("embeddingPath"))
            .arrange()

        // when
        viewModel.downloadEmbeddingModel()

        // then
        verify(exactly = 1) { arrangement.aiEmbeddingModelManager.downloadModel() }
        verify(exactly = 0) { arrangement.aiModelManager.downloadModel() }
    }

    @Test
    fun `given ai model is downloading, when downloading, then model download is not started again`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.Downloading(0.5F))
            .arrange()

        // when
        viewModel.downloadAiModel()

        // then
        verify(exactly = 0) { arrangement.aiModelManager.downloadModel() }
    }

    @Test
    fun `given embedding model is downloading, when downloading, then embedding model download is not started again`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.Downloading(0.5F))
            .arrange()

        // when
        viewModel.downloadEmbeddingModel()

        // then
        verify(exactly = 0) { arrangement.aiEmbeddingModelManager.downloadModel() }
    }

    @Test
    fun `given available models, then state contains available models and selected model`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .arrange()

        assertEquals(listOf(testDescriptor), viewModel.state.availableModels)
        assertEquals(testDescriptor, viewModel.state.selectedModel)
    }

    @Test
    fun `given restored selected model from manager, then state contains restored selection`() = runTest {
        val restoredDescriptor = testDescriptor.copy(
            displayName = "Restored model",
            repositoryId = "google/restored-model"
        )
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAvailableModels(listOf(testDescriptor, restoredDescriptor))
            .withSelectedModel(restoredDescriptor)
            .arrange()

        assertEquals(restoredDescriptor, viewModel.state.selectedModel)
    }

    @Test
    fun `given available models, when model is selected, then selectModel is called on manager`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .arrange()

        // when
        viewModel.selectModel(testDescriptor)

        // then
        verify(exactly = 1) { arrangement.aiModelManager.selectModel(testDescriptor) }
    }

    @Test
    fun `given ai model authorization is required with message, when downloading, then authorization dialog is shown`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.AuthRequired(AUTH_REQUIRED_MESSAGE))
            .arrange()

        // when
        viewModel.downloadAiModel()

        // then
        assertEquals(UIText.DynamicString(AUTH_REQUIRED_MESSAGE), viewModel.state.authorizationDialogState?.message)
        assertEquals(
            "https://huggingface.co/litert-community/gemma-3-270m-it",
            viewModel.state.authorizationDialogState?.authorizeUrl
        )
    }

    @Test
    fun `given ai model authorization is required without message, when downloading, then dialog falls back to generic copy`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.AuthRequired())
            .arrange()

        // when
        viewModel.downloadAiModel()

        // then
        assertEquals(
            "AI model download requires Hugging Face authorization",
            viewModel.state.authorizationDialogState?.message?.resolveForTest(fakeStrings)
        )
        assertEquals(null, viewModel.state.authorizationDialogState?.authorizeUrl)
    }

    @Test
    fun `given embedding model authorization is required with message, when downloading, then authorization dialog is shown`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.NotDownloaded)
            .withEmbeddingModelDownloadState(AiModelDownloadState.AuthRequired(AUTH_REQUIRED_MESSAGE))
            .arrange()

        // when
        viewModel.downloadEmbeddingModel()

        // then
        assertEquals(UIText.DynamicString(AUTH_REQUIRED_MESSAGE), viewModel.state.authorizationDialogState?.message)
        assertEquals(
            "https://huggingface.co/litert-community/gemma-3-270m-it",
            viewModel.state.authorizationDialogState?.authorizeUrl
        )
    }

    @Test
    fun `given authorization dialog is shown, when dismissed, then dialog is cleared`() = runTest {
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.AuthRequired(AUTH_REQUIRED_MESSAGE))
            .arrange()

        viewModel.downloadAiModel()

        viewModel.dismissAuthorizationDialog()

        assertEquals(null, viewModel.state.authorizationDialogState)
    }

    @Test
    fun `given authorization dialog is shown, when authorizing, then url is emitted and dialog is cleared`() = runTest {
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.AuthRequired(AUTH_REQUIRED_MESSAGE))
            .arrange()

        viewModel.downloadAiModel()

        viewModel.authorizationUrl.test {
            viewModel.authorizeModelAccess("https://huggingface.co/litert-community/gemma-3-270m-it")

            assertEquals("https://huggingface.co/litert-community/gemma-3-270m-it", awaitItem())
        }
        assertEquals(null, viewModel.state.authorizationDialogState)
    }

    @Test
    fun `given auth message without valid url, when extracting url, then result is null`() {
        assertEquals(null, "Access denied without link".extractFirstUrl())
    }

    @Test
    fun `given ai model download fails, when downloading, then info message emits failure`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.Failed(FailureReason.Network))
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.downloadAiModel()

            // then
            assertEquals(UIText.StringResource(R.string.debug_settings_ai_model_download_failed, "Network"), awaitItem())
        }
    }

    @Test
    fun `given embedding model download fails, when downloading, then info message emits failure`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withEmbeddingModelStatus(AiModelStatus.NotDownloaded)
            .withEmbeddingModelDownloadState(AiModelDownloadState.Failed(FailureReason.Network))
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.downloadEmbeddingModel()

            // then
            assertEquals(
                UIText.StringResource(R.string.debug_settings_ai_embedding_model_download_failed, "Network"),
                awaitItem()
            )
        }
    }

    @Test
    fun `when creating embeddings, then work is enqueued and success message is emitted`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withCreateEmbeddingsStatus(
                CreateMessageEmbeddingsWorkStatus.Succeeded(
                    CreateMessageEmbeddingsWorkStatus.Summary(
                        totalMessages = 10,
                        processedMessages = 10,
                        createdEmbeddings = 7,
                        skippedMessages = 2,
                        failedMessages = 1,
                        modelId = "deterministic-local-v1"
                    )
                )
            )
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.createEmbeddings()

            // then
            assertEquals(
                UIText.StringResource(R.string.debug_settings_ai_create_embeddings_success, 7, 2, 1),
                awaitItem()
            )
        }
        assertEquals(1, arrangement.createMessageEmbeddingsWorkScheduler.enqueueCount)
        assertEquals(false, viewModel.state.isCreatingEmbeddings)
    }

    @Test
    fun `given creating embeddings fails, when creating embeddings, then failure message is emitted`() = runTest {
        // given
        val failure = CoreFailure.Unknown(RuntimeException("Embedding failed"))
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withCreateEmbeddingsStatus(CreateMessageEmbeddingsWorkStatus.Failed(failure.toString()))
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.createEmbeddings()

            // then
            assertEquals(
                UIText.StringResource(R.string.debug_settings_ai_create_embeddings_failed, failure.toString()),
                awaitItem()
            )
        }
        assertEquals(false, viewModel.state.isCreatingEmbeddings)
    }

    @Test
    fun `given creating embeddings is running, when creating embeddings again, then work is not enqueued again`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withObservedCreateEmbeddingsStatus(CreateMessageEmbeddingsWorkStatus.Running(progress = null))
            .arrange()

        // when
        viewModel.createEmbeddings()
        viewModel.createEmbeddings()

        // then
        assertEquals(true, viewModel.state.isCreatingEmbeddings)
        assertEquals(0, arrangement.createMessageEmbeddingsWorkScheduler.enqueueCount)
    }

    @Test
    fun `given blank query, when searching messages, then semantic search is not called`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .arrange()

        // when
        viewModel.searchMessages("   ")

        // then
        coVerify(exactly = 0) { arrangement.searchMessagesSemanticallyGlobally(any(), any()) }
        assertEquals(false, viewModel.state.isSearchingMessages)
    }

    @Test
    fun `given query, when semantic search succeeds, then use case is called and loading clears`() = runTest {
        // given
        val (arrangement, viewModel) = AiAssistantDebugArrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(emptyList()))
            .arrange()

        // when
        viewModel.searchMessages("hello")
        runCurrent()

        // then
        coVerify(exactly = 1) { arrangement.searchMessagesSemanticallyGlobally("hello", any()) }
        assertEquals(false, viewModel.state.isSearchingMessages)
    }

    @Test
    fun `given semantic search fails, when searching messages, then failure message is emitted`() = runTest {
        // given
        val failure = CoreFailure.Unknown(RuntimeException("Search failed"))
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Failure(failure))
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.searchMessages("hello")

            // then
            assertEquals(
                UIText.StringResource(R.string.debug_settings_ai_semantic_search_failed, failure.toString()),
                awaitItem()
            )
        }
        assertEquals(false, viewModel.state.isSearchingMessages)
    }
}

private val testDescriptor = AiModelDescriptor(
    displayName = "Test model",
    repositoryId = "google/test-model",
    artifactPath = "test-model.litertlm",
    localDirectoryName = "test-model",
    localFileName = "model.litertlm"
)
private val SELF_USER_ID = UserId("self-user", "wire.com")
private val emptyCreateEmbeddingsSummary = CreateMessageEmbeddingsWorkStatus.Summary(
    totalMessages = 0,
    processedMessages = 0,
    createdEmbeddings = 0,
    skippedMessages = 0,
    failedMessages = 0,
    modelId = "deterministic-local-v1"
)

private const val AUTH_REQUIRED_MESSAGE =
    "Access to model litert-community/gemma-3-270m-it is restricted and you are " +
        "not in the authorized list. Visit https://huggingface.co/litert-community/" +
        "gemma-3-270m-it to ask for access."
private val fakeStrings = mapOf(
    R.string.debug_settings_ai_model_auth_required to "AI model download requires Hugging Face authorization"
)

private class AiAssistantDebugArrangement {

    @MockK
    lateinit var aiModelManager: AiModelManager

    @MockK
    lateinit var aiEmbeddingModelManager: AiEmbeddingModelManager

    @MockK
    lateinit var aiModelTestEngine: AiModelTestEngine

    @MockK
    lateinit var searchMessagesSemanticallyGlobally: SearchMessagesSemanticallyGloballyUseCase

    val inferenceConfigStore = FakeAiInferenceConfigStore()
    val createMessageEmbeddingsWorkScheduler = FakeCreateMessageEmbeddingsWorkScheduler()

    private val viewModel by lazy {
        AiAssistantDebugViewModelImpl(
            aiModelManager = aiModelManager,
            aiEmbeddingModelManager = aiEmbeddingModelManager,
            aiModelTestEngine = aiModelTestEngine,
            inferenceConfigStore = inferenceConfigStore,
            currentAccount = SELF_USER_ID,
            createMessageEmbeddingsWorkScheduler = createMessageEmbeddingsWorkScheduler,
            searchMessagesSemanticallyGlobally = searchMessagesSemanticallyGlobally
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { aiModelManager.availableModels } returns listOf(testDescriptor)
        every { aiModelManager.selectedModel } returns MutableStateFlow(testDescriptor)
        withAiModelStatus(AiModelStatus.NotDownloaded)
        withEmbeddingModelStatus(AiModelStatus.NotDownloaded)
        withAiModelDownloadState()
        withEmbeddingModelDownloadState()
        withAiModelHealthCheckResult(AiModelHealthCheckResult.Healthy)
        withInferenceConfig(AiInferenceConfig.DEFAULT)
        withCreateEmbeddingsStatus(CreateMessageEmbeddingsWorkStatus.Succeeded(emptyCreateEmbeddingsSummary))
        withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(emptyList()))
    }

    fun withAiModelStatus(status: AiModelStatus) = apply {
        withAiModelStatuses(status)
    }

    fun withAvailableModels(models: List<AiModelDescriptor>) = apply {
        every { aiModelManager.availableModels } returns models
    }

    fun withSelectedModel(descriptor: AiModelDescriptor) = apply {
        every { aiModelManager.selectedModel } returns MutableStateFlow(descriptor)
    }

    fun withInferenceConfig(config: AiInferenceConfig) = apply {
        inferenceConfigStore.config.value = config
    }

    fun withAiModelStatuses(vararg statuses: AiModelStatus) = apply {
        every {
            aiModelManager.observeModelStatus()
        } returns flowOf(*statuses)
    }

    fun withEmbeddingModelStatus(status: AiModelStatus) = apply {
        withEmbeddingModelStatuses(status)
    }

    fun withEmbeddingModelStatuses(vararg statuses: AiModelStatus) = apply {
        every {
            aiEmbeddingModelManager.observeModelStatus()
        } returns flowOf(*statuses)
    }

    fun withAiModelDownloadState(state: AiModelDownloadState? = null) = apply {
        every {
            aiModelManager.downloadModel()
        } returns if (state == null) emptyFlow() else flowOf(state)
    }

    fun withEmbeddingModelDownloadState(state: AiModelDownloadState? = null) = apply {
        every {
            aiEmbeddingModelManager.downloadModel()
        } returns if (state == null) emptyFlow() else flowOf(state)
    }

    fun withAiModelHealthCheckResult(result: AiModelHealthCheckResult) = apply {
        withAiModelHealthCheckResult { result }
    }

    fun withAiModelHealthCheckResult(result: suspend () -> AiModelHealthCheckResult) = apply {
        coEvery {
            aiModelTestEngine.runHealthCheck(any(), any())
        } coAnswers {
            result()
        }
    }

    fun withObservedCreateEmbeddingsStatus(status: CreateMessageEmbeddingsWorkStatus) = apply {
        createMessageEmbeddingsWorkScheduler.observedStatus.value = status
    }

    fun withCreateEmbeddingsStatus(status: CreateMessageEmbeddingsWorkStatus) = apply {
        createMessageEmbeddingsWorkScheduler.enqueuedStatus = status
    }

    fun withSemanticSearchResult(result: SearchMessagesSemanticallyGloballyUseCase.Result) = apply {
        coEvery {
            searchMessagesSemanticallyGlobally(any(), any())
        } returns result
    }

    fun arrange() = this to viewModel
}

private class FakeAiInferenceConfigStore(
    initialConfig: AiInferenceConfig = AiInferenceConfig.DEFAULT
) : AiInferenceConfigStore {
    val config = MutableStateFlow(initialConfig)

    override fun observeConfig(): Flow<AiInferenceConfig> = config

    override suspend fun setConfig(config: AiInferenceConfig) {
        this.config.value = config
    }
}

private class FakeCreateMessageEmbeddingsWorkScheduler : CreateMessageEmbeddingsWorkScheduler {
    val observedStatus = MutableStateFlow<CreateMessageEmbeddingsWorkStatus>(CreateMessageEmbeddingsWorkStatus.Idle)
    var enqueuedStatus: CreateMessageEmbeddingsWorkStatus = CreateMessageEmbeddingsWorkStatus.Succeeded(emptyCreateEmbeddingsSummary)
    var enqueueCount = 0

    override fun enqueue(userId: UserId): Flow<CreateMessageEmbeddingsWorkStatus> {
        enqueueCount++
        observedStatus.value = enqueuedStatus
        return flowOf(CreateMessageEmbeddingsWorkStatus.Running(progress = null), enqueuedStatus)
    }

    override fun observe(userId: UserId): Flow<CreateMessageEmbeddingsWorkStatus> = observedStatus
}
