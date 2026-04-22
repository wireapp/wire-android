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
import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.model.FailureReason
import com.wire.android.ui.debug.AiAssistantDebugViewModelImpl
import com.wire.android.ui.debug.AiModelUiStatus
import com.wire.android.util.ui.UIText
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
    fun `given ai model authorization is required, when downloading, then info message emits authorization error`() = runTest {
        // given
        val (_, viewModel) = AiAssistantDebugArrangement()
            .withAiModelStatus(AiModelStatus.NotDownloaded)
            .withAiModelDownloadState(AiModelDownloadState.AuthRequired)
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.downloadAiModel()

            // then
            assertEquals(UIText.StringResource(R.string.debug_settings_ai_model_auth_required), awaitItem())
        }
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
}

private class AiAssistantDebugArrangement {

    @MockK
    lateinit var aiModelManager: AiModelManager

    private val viewModel by lazy {
        AiAssistantDebugViewModelImpl(
            aiModelManager = aiModelManager
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        withAiModelStatus(AiModelStatus.NotDownloaded)
        withAiModelDownloadState()
    }

    fun withAiModelStatus(status: AiModelStatus) = apply {
        every {
            aiModelManager.observeModelStatus()
        } returns flowOf(status)
    }

    fun withAiModelDownloadState(state: AiModelDownloadState? = null) = apply {
        every {
            aiModelManager.downloadModel()
        } returns if (state == null) emptyFlow() else flowOf(state)
    }

    fun arrange() = this to viewModel
}
