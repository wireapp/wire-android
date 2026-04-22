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
package com.wire.android.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.collectAndShowSnackbar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@WireRootDestination
@Composable
fun AiAssistantDebugScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AiAssistantDebugViewModel =
        hiltViewModelScoped<AiAssistantDebugViewModelImpl, AiAssistantDebugViewModel>(),
) {
    LocalSnackbarHostState.current.collectAndShowSnackbar(snackbarFlow = viewModel.infoMessage)

    AiAssistantDebugScreenContent(
        state = viewModel.state,
        onNavigationPressed = navigator::navigateBack,
        onDownloadAiModel = viewModel::downloadAiModel,
        modifier = modifier
    )
}

@Composable
fun AiAssistantDebugScreenContent(
    state: AiAssistantDebugState,
    onNavigationPressed: () -> Unit,
    onDownloadAiModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                titleContent = {
                    WireTopAppBarTitle(
                        title = stringResource(R.string.debug_settings_ai_assistant),
                        style = typography().title01,
                        maxLines = 2
                    )
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = onNavigationPressed
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                AiModelOption(
                    state = state.aiModelOptionState,
                    onDownloadAiModel = onDownloadAiModel
                )
            }
        }
    )
}

@Composable
private fun AiModelOption(
    state: AiModelOptionState,
    onDownloadAiModel: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Column(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.debug_settings_ai_assistant_model)
                )
                Text(
                    style = MaterialTheme.wireTypography.body02,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    text = state.statusText()
                )
            }
        },
        actions = {
            if (state.showDownloadButton) {
                WirePrimaryButton(
                    minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    onClick = onDownloadAiModel,
                    text = stringResource(R.string.debug_settings_ai_model_download),
                    fillMaxWidth = false,
                    loading = state.isDownloading,
                    state = if (state.isDownloading) WireButtonState.Disabled else WireButtonState.Default
                )
            }
        }
    )
}

@Composable
private fun AiModelOptionState.statusText(): String =
    when (status) {
        AiModelUiStatus.NotDownloaded -> stringResource(R.string.debug_settings_ai_model_not_downloaded)
        is AiModelUiStatus.Downloading -> status.progress?.let {
            stringResource(R.string.debug_settings_ai_model_downloading_with_progress, (it * PERCENT_MULTIPLIER).toInt())
        } ?: stringResource(R.string.debug_settings_ai_model_downloading)
        AiModelUiStatus.Downloaded -> stringResource(R.string.debug_settings_ai_model_downloaded)
    }

@ViewModelScopedPreview
interface AiAssistantDebugViewModel {
    val infoMessage: SharedFlow<UIText> get() = MutableSharedFlow()
    val state: AiAssistantDebugState get() = AiAssistantDebugState()
    fun downloadAiModel() {}
}

@HiltViewModel
class AiAssistantDebugViewModelImpl @Inject constructor(
    private val aiModelManager: AiModelManager,
) : ViewModel(), AiAssistantDebugViewModel {

    override var state by mutableStateOf(AiAssistantDebugState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    init {
        observeAiModelStatus()
    }

    override fun downloadAiModel() {
        if (state.aiModelOptionState.isDownloading) return

        viewModelScope.launch {
            aiModelManager.downloadModel().collect { downloadState ->
                when (downloadState) {
                    AiModelDownloadState.AuthRequired ->
                        _infoMessage.emit(UIText.StringResource(R.string.debug_settings_ai_model_auth_required))

                    is AiModelDownloadState.Failed ->
                        _infoMessage.emit(
                            UIText.StringResource(
                                R.string.debug_settings_ai_model_download_failed,
                                downloadState.reason.toString()
                            )
                        )

                    is AiModelDownloadState.Downloading,
                    is AiModelDownloadState.Ready,
                    AiModelDownloadState.Starting -> {
                        // Status is exposed through observeModelStatus.
                    }
                }
            }
        }
    }

    private fun observeAiModelStatus() {
        viewModelScope.launch {
            aiModelManager.observeModelStatus().collect { modelStatus ->
                state = state.copy(aiModelOptionState = modelStatus.toUiState())
            }
        }
    }

    private fun AiModelStatus.toUiState(): AiModelOptionState =
        when (this) {
            AiModelStatus.NotDownloaded -> AiModelOptionState(
                status = AiModelUiStatus.NotDownloaded,
                showDownloadButton = true,
                isDownloading = false
            )

            is AiModelStatus.Downloading -> AiModelOptionState(
                status = AiModelUiStatus.Downloading(progress),
                showDownloadButton = true,
                isDownloading = true
            )

            is AiModelStatus.Ready -> AiModelOptionState(
                status = AiModelUiStatus.Downloaded,
                showDownloadButton = false,
                isDownloading = false
            )
        }
}

data class AiAssistantDebugState(
    val aiModelOptionState: AiModelOptionState = AiModelOptionState()
)

data class AiModelOptionState(
    val status: AiModelUiStatus = AiModelUiStatus.NotDownloaded,
    val showDownloadButton: Boolean = true,
    val isDownloading: Boolean = false,
)

sealed interface AiModelUiStatus {
    data object NotDownloaded : AiModelUiStatus
    data class Downloading(val progress: Float?) : AiModelUiStatus
    data object Downloaded : AiModelUiStatus
}

private const val PERCENT_MULTIPLIER = 100

@PreviewMultipleThemes
@Composable
fun PreviewAiAssistantDebugScreen() = WireTheme {
    AiAssistantDebugScreenContent(
        state = AiAssistantDebugState(),
        onNavigationPressed = {},
        onDownloadAiModel = {}
    )
}
