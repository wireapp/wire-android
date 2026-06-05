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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.feature.aiassistant.AiEmbeddingModelManager
import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.test.AiModelHealthCheckResult
import com.wire.android.feature.aiassistant.test.AiModelTestEngine
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
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
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.text.Regex

@WireRootDestination
@Composable
fun AiAssistantDebugScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AiAssistantDebugViewModel = aiAssistantDebugViewModel(),
) {
    val context = LocalContext.current

    LocalSnackbarHostState.current.collectAndShowSnackbar(snackbarFlow = viewModel.infoMessage)

    AiAssistantDebugScreenContent(
        state = viewModel.state,
        onNavigationPressed = navigator::navigateBack,
        onDownloadAiModel = viewModel::downloadAiModel,
        onDownloadEmbeddingModel = viewModel::downloadEmbeddingModel,
        onModelSelected = viewModel::selectModel,
        onDismissAuthorizationDialog = viewModel::dismissAuthorizationDialog,
        onAuthorizeModelAccess = viewModel::authorizeModelAccess,
        modifier = modifier
    )

    LaunchedEffect(viewModel, context) {
        viewModel.authorizationUrl.collect { url ->
            CustomTabsHelper.launchUrl(context, url)
        }
    }
}

@Composable
fun AiAssistantDebugScreenContent(
    state: AiAssistantDebugState,
    onNavigationPressed: () -> Unit,
    onDownloadAiModel: () -> Unit,
    onDownloadEmbeddingModel: () -> Unit,
    onModelSelected: (AiModelDescriptor) -> Unit,
    onDismissAuthorizationDialog: () -> Unit,
    onAuthorizeModelAccess: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    state.authorizationDialogState?.let { dialogState ->
        AiModelAuthorizationDialog(
            state = dialogState,
            onDismiss = onDismissAuthorizationDialog,
            onAuthorize = onAuthorizeModelAccess
        )
    }

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
                if (state.availableModels.isNotEmpty()) {
                    AiModelSelectorOption(
                        availableModels = state.availableModels,
                        selectedModel = state.selectedModel,
                        onModelSelected = onModelSelected
                    )
                }
                AiModelOption(
                    title = stringResource(R.string.debug_settings_ai_assistant_model),
                    state = state.aiModelOptionState,
                    onDownloadAiModel = onDownloadAiModel
                )
                AiModelOption(
                    title = stringResource(R.string.debug_settings_ai_embedding_model),
                    state = state.embeddingModelOptionState,
                    onDownloadAiModel = onDownloadEmbeddingModel
                )
                AiModelHealthCheckOption(state = state.healthCheckState)
            }
        }
    )
}

@Composable
private fun AiModelAuthorizationDialog(
    state: AiModelAuthorizationDialogState,
    onDismiss: () -> Unit,
    onAuthorize: (String) -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.debug_settings_ai_model_auth_required_title),
        text = state.message.asString(),
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
            onClick = onDismiss
        ),
        optionButton1Properties = state.authorizeUrl?.let { url ->
            WireDialogButtonProperties(
                text = stringResource(R.string.debug_settings_ai_model_authorize),
                type = WireDialogButtonType.Primary,
                onClick = { onAuthorize(url) }
            )
        }
    )
}

@Composable
private fun AiModelSelectorOption(
    availableModels: List<AiModelDescriptor>,
    selectedModel: AiModelDescriptor?,
    onModelSelected: (AiModelDescriptor) -> Unit,
) {
    val modelNames = availableModels.map { it.displayName }
    val selectedIndex = availableModels.indexOf(selectedModel).takeIf { it >= 0 } ?: 0
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Column(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.debug_settings_ai_model_select)
                )
                WireDropDown(
                    items = modelNames,
                    label = null,
                    selectedItemIndex = selectedIndex,
                    autoUpdateSelection = false,
                    showDefaultTextIndicator = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions().spacing8x, end = dimensions().spacing24x),
                    onSelected = { index -> onModelSelected(availableModels[index]) }
                )
            }
        }
    )
}

@Composable
private fun AiModelOption(
    title: String,
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
                    text = title
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

@Composable
private fun AiModelHealthCheckOption(state: AiModelHealthCheckState) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Column(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.debug_settings_ai_model_health_check)
                )
                Text(
                    style = MaterialTheme.wireTypography.body02,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    text = state.statusText()
                )
            }
        }
    )
}

@Composable
private fun AiModelHealthCheckState.statusText(): String =
    when (this) {
        AiModelHealthCheckState.Unavailable -> stringResource(R.string.debug_settings_ai_model_health_check_unavailable)
        AiModelHealthCheckState.Running -> stringResource(R.string.debug_settings_ai_model_health_check_running)
        AiModelHealthCheckState.Healthy -> stringResource(R.string.debug_settings_ai_model_health_check_healthy)
        is AiModelHealthCheckState.Failed -> stringResource(R.string.debug_settings_ai_model_health_check_failed, reason)
    }

@ViewModelScopedPreview
interface AiAssistantDebugViewModel {
    val infoMessage: SharedFlow<UIText> get() = MutableSharedFlow()
    val authorizationUrl: SharedFlow<String> get() = MutableSharedFlow()
    val state: AiAssistantDebugState get() = AiAssistantDebugState()
    fun downloadAiModel() {}
    fun downloadEmbeddingModel() {}
    fun selectModel(descriptor: AiModelDescriptor) {}
    fun dismissAuthorizationDialog() {}
    fun authorizeModelAccess(url: String) {}
}

class AiAssistantDebugViewModelImpl(
    private val aiModelManager: AiModelManager,
    private val aiEmbeddingModelManager: AiEmbeddingModelManager,
    private val aiModelTestEngine: AiModelTestEngine,
) : ViewModel(), AiAssistantDebugViewModel {

    override var state by mutableStateOf(AiAssistantDebugState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()
    private val _authorizationUrl = MutableSharedFlow<String>()
    override val authorizationUrl = _authorizationUrl.asSharedFlow()
    private var healthCheckJob: Job? = null
    private var checkedModelPath: String? = null

    init {
        state = state.copy(
            availableModels = aiModelManager.availableModels,
            selectedModel = aiModelManager.selectedModel.value
        )
        observeSelectedModel()
        observeAiModelStatus()
        observeEmbeddingModelStatus()
    }

    override fun downloadAiModel() {
        if (state.aiModelOptionState.isDownloading) return

        viewModelScope.launch {
            aiModelManager.downloadModel().collect { downloadState ->
                when (downloadState) {
                    is AiModelDownloadState.AuthRequired ->
                        state = state.copy(
                            authorizationDialogState = downloadState.toAuthorizationDialogState()
                        )

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

    override fun downloadEmbeddingModel() {
        android.util.Log.d("AiAssistantDebug", "downloadEmbeddingModel clicked, isDownloading=${state.embeddingModelOptionState.isDownloading}")
        if (state.embeddingModelOptionState.isDownloading) return

        viewModelScope.launch {
            aiEmbeddingModelManager.downloadModel().collect { downloadState ->
                android.util.Log.d("AiAssistantDebug", "embedding downloadState=$downloadState")
                when (downloadState) {
                    is AiModelDownloadState.AuthRequired ->
                        state = state.copy(
                            authorizationDialogState = downloadState.toAuthorizationDialogState()
                        )

                    is AiModelDownloadState.Failed ->
                        _infoMessage.emit(
                            UIText.StringResource(
                                R.string.debug_settings_ai_embedding_model_download_failed,
                                downloadState.reason.toString()
                            )
                        )

                    is AiModelDownloadState.Downloading,
                    is AiModelDownloadState.Ready,
                    AiModelDownloadState.Starting -> {
                        // Status is exposed through observeEmbeddingModelStatus.
                    }
                }
            }
        }
    }

    override fun selectModel(descriptor: AiModelDescriptor) {
        aiModelManager.selectModel(descriptor)
    }

    override fun dismissAuthorizationDialog() {
        state = state.copy(authorizationDialogState = null)
    }

    override fun authorizeModelAccess(url: String) {
        dismissAuthorizationDialog()
        viewModelScope.launch {
            _authorizationUrl.emit(url)
        }
    }

    private fun observeSelectedModel() {
        viewModelScope.launch {
            aiModelManager.selectedModel.collect { descriptor ->
                state = state.copy(selectedModel = descriptor)
            }
        }
    }

    private fun observeAiModelStatus() {
        viewModelScope.launch {
            aiModelManager.observeModelStatus().collect { modelStatus ->
                state = state.copy(aiModelOptionState = modelStatus.toUiState())
                updateHealthCheck(modelStatus)
            }
        }
    }

    private fun observeEmbeddingModelStatus() {
        viewModelScope.launch {
            aiEmbeddingModelManager.observeModelStatus().collect { modelStatus ->
                state = state.copy(embeddingModelOptionState = modelStatus.toUiState())
            }
        }
    }

    private fun updateHealthCheck(modelStatus: AiModelStatus) {
        when (modelStatus) {
            AiModelStatus.NotDownloaded,
            is AiModelStatus.Downloading -> {
                checkedModelPath = null
                healthCheckJob?.cancel()
                healthCheckJob = null
                state = state.copy(healthCheckState = AiModelHealthCheckState.Unavailable)
            }

            is AiModelStatus.Ready -> runHealthCheckIfNeeded(modelStatus.localPath)
        }
    }

    private fun runHealthCheckIfNeeded(modelPath: String) {
        if (checkedModelPath == modelPath) return

        healthCheckJob?.cancel()
        checkedModelPath = modelPath
        state = state.copy(healthCheckState = AiModelHealthCheckState.Running)
        healthCheckJob = viewModelScope.launch {
            state = state.copy(healthCheckState = aiModelTestEngine.runHealthCheck(modelPath).toUiState())
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

    private fun AiModelHealthCheckResult.toUiState(): AiModelHealthCheckState =
        when (this) {
            AiModelHealthCheckResult.Healthy -> AiModelHealthCheckState.Healthy
            AiModelHealthCheckResult.EmptyResponse -> AiModelHealthCheckState.Failed(
                reason = EMPTY_RESPONSE_FAILURE_REASON
            )
            AiModelHealthCheckResult.MissingModel -> AiModelHealthCheckState.Failed(
                reason = MISSING_MODEL_FAILURE_REASON
            )
            AiModelHealthCheckResult.UnsupportedModel -> AiModelHealthCheckState.Failed(
                reason = UNSUPPORTED_MODEL_FAILURE_REASON
            )
            is AiModelHealthCheckResult.InferenceFailed -> AiModelHealthCheckState.Failed(message)
        }

    private fun AiModelDownloadState.AuthRequired.toAuthorizationDialogState(): AiModelAuthorizationDialogState =
        AiModelAuthorizationDialogState(
            message = message?.takeIf { it.isNotBlank() }?.let(UIText::DynamicString)
                ?: UIText.StringResource(R.string.debug_settings_ai_model_auth_required),
            authorizeUrl = message?.extractFirstUrl()
        )
}

data class AiAssistantDebugState(
    val availableModels: List<AiModelDescriptor> = emptyList(),
    val selectedModel: AiModelDescriptor? = null,
    val aiModelOptionState: AiModelOptionState = AiModelOptionState(),
    val embeddingModelOptionState: AiModelOptionState = AiModelOptionState(),
    val healthCheckState: AiModelHealthCheckState = AiModelHealthCheckState.Unavailable,
    val authorizationDialogState: AiModelAuthorizationDialogState? = null
)

data class AiModelAuthorizationDialogState(
    val message: UIText,
    val authorizeUrl: String?
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

sealed interface AiModelHealthCheckState {
    data object Unavailable : AiModelHealthCheckState
    data object Running : AiModelHealthCheckState
    data object Healthy : AiModelHealthCheckState
    data class Failed(val reason: String) : AiModelHealthCheckState
}

private const val PERCENT_MULTIPLIER = 100
private const val EMPTY_RESPONSE_FAILURE_REASON = "Model returned an empty response"
private const val MISSING_MODEL_FAILURE_REASON = "Model file is missing"
private const val UNSUPPORTED_MODEL_FAILURE_REASON = "Model type is not supported by the MediaPipe health check"
private val URL_REGEX = Regex("""https?://[^\s"'<>]+""")

internal fun String.extractFirstUrl(): String? = URL_REGEX.find(this)?.value

@PreviewMultipleThemes
@Composable
fun PreviewAiAssistantDebugScreen() = WireTheme {
    AiAssistantDebugScreenContent(
        state = AiAssistantDebugState(),
        onNavigationPressed = {},
        onDownloadAiModel = {},
        onDownloadEmbeddingModel = {},
        onModelSelected = {},
        onDismissAuthorizationDialog = {},
        onAuthorizeModelAccess = {}
    )
}
