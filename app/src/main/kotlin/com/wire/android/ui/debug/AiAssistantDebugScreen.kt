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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.feature.aiassistant.AiEmbeddingModelManager
import com.wire.android.feature.aiassistant.AiInferenceBackend
import com.wire.android.feature.aiassistant.AiInferenceConfig
import com.wire.android.feature.aiassistant.AiInferenceConfigStore
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
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
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
import com.wire.android.workmanager.worker.CreateMessageEmbeddingsWorkScheduler
import com.wire.android.workmanager.worker.CreateMessageEmbeddingsWorkStatus
import com.wire.android.workmanager.worker.EmbeddingWorkOperation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.message.SearchMessagesSemanticallyGloballyUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
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
        onCreateEmbeddings = viewModel::createEmbeddings,
        onImportEmbeddings = viewModel::importEmbeddings,
        onSearchMessages = viewModel::searchMessages,
        onModelSelected = viewModel::selectModel,
        onInferenceBackendSelected = viewModel::selectInferenceBackend,
        onCpuThreadsSelected = viewModel::selectCpuThreads,
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
    onCreateEmbeddings: () -> Unit,
    onImportEmbeddings: () -> Unit,
    onModelSelected: (AiModelDescriptor) -> Unit,
    onInferenceBackendSelected: (AiInferenceBackend) -> Unit,
    onCpuThreadsSelected: (Int?) -> Unit,
    onDismissAuthorizationDialog: () -> Unit,
    onAuthorizeModelAccess: (String) -> Unit,
    onSearchMessages: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var showSearchDialog by remember { mutableStateOf(false) }

    state.authorizationDialogState?.let { dialogState ->
        AiModelAuthorizationDialog(
            state = dialogState,
            onDismiss = onDismissAuthorizationDialog,
            onAuthorize = onAuthorizeModelAccess
        )
    }

    if (showSearchDialog) {
        AiSemanticSearchDialog(
            onDismiss = { showSearchDialog = false },
            onStartSearch = { query ->
                showSearchDialog = false
                onSearchMessages(query)
            }
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
                AiInferenceConfigOption(
                    config = state.inferenceConfig,
                    onInferenceBackendSelected = onInferenceBackendSelected,
                    onCpuThreadsSelected = onCpuThreadsSelected
                )
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
                AiCreateEmbeddingsOption(
                    isCreatingEmbeddings = state.isCreatingEmbeddings,
                    isImportingEmbeddings = state.isImportingEmbeddings,
                    isSearchingMessages = state.isSearchingMessages,
                    onCreateEmbeddings = onCreateEmbeddings,
                    onImportEmbeddings = onImportEmbeddings,
                    onSearchMessages = { showSearchDialog = true }
                )
                AiModelHealthCheckOption(state = state.healthCheckState)
            }
        }
    )
}

@Composable
private fun AiSemanticSearchDialog(
    onDismiss: () -> Unit,
    onStartSearch: (String) -> Unit,
) {
    val queryTextState = rememberTextFieldState()
    val isQueryBlank = queryTextState.text.isBlank()

    WireDialog(
        title = stringResource(R.string.debug_settings_ai_semantic_search),
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
            onClick = onDismiss
        ),
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(R.string.debug_settings_ai_start_search),
            type = WireDialogButtonType.Primary,
            state = if (isQueryBlank) WireButtonState.Disabled else WireButtonState.Default,
            onClick = { onStartSearch(queryTextState.text.toString()) }
        ),
        content = {
            WireTextField(
                modifier = Modifier.fillMaxWidth(),
                textState = queryTextState,
                labelText = stringResource(R.string.debug_settings_ai_search_query),
                state = WireTextFieldState.Default,
                testTag = SEMANTIC_SEARCH_QUERY_TEST_TAG
            )
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
private fun AiInferenceConfigOption(
    config: AiInferenceConfig,
    onInferenceBackendSelected: (AiInferenceBackend) -> Unit,
    onCpuThreadsSelected: (Int?) -> Unit,
) {
    val backendItems = AiInferenceBackend.entries.map { it.displayName() }
    val selectedBackendIndex = AiInferenceBackend.entries.indexOf(config.backend).takeIf { it >= 0 } ?: 0
    val cpuThreadValues = listOf<Int?>(null) + AiInferenceConfig.CPU_THREADS_RANGE.toList()
    val cpuThreadItems = cpuThreadValues.map { it?.toString() ?: stringResource(R.string.debug_settings_ai_inference_cpu_threads_auto) }
    val selectedCpuThreadIndex = cpuThreadValues.indexOf(config.cpuThreads).takeIf { it >= 0 } ?: 0

    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Column(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = stringResource(R.string.debug_settings_ai_inference)
                )
                WireDropDown(
                    items = backendItems,
                    label = stringResource(R.string.debug_settings_ai_inference_backend),
                    selectedItemIndex = selectedBackendIndex,
                    autoUpdateSelection = false,
                    showDefaultTextIndicator = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions().spacing8x, end = dimensions().spacing24x),
                    onSelected = { index -> onInferenceBackendSelected(AiInferenceBackend.entries[index]) }
                )
                if (config.backend == AiInferenceBackend.CPU) {
                    WireDropDown(
                        items = cpuThreadItems,
                        label = stringResource(R.string.debug_settings_ai_inference_cpu_threads),
                        selectedItemIndex = selectedCpuThreadIndex,
                        autoUpdateSelection = false,
                        showDefaultTextIndicator = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensions().spacing8x, end = dimensions().spacing24x),
                        onSelected = { index -> onCpuThreadsSelected(cpuThreadValues[index]) }
                    )
                }
            }
        }
    )
}

@Composable
private fun AiInferenceBackend.displayName(): String =
    when (this) {
        AiInferenceBackend.CPU -> stringResource(R.string.debug_settings_ai_inference_cpu)
        AiInferenceBackend.GPU -> stringResource(R.string.debug_settings_ai_inference_gpu)
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
private fun AiCreateEmbeddingsOption(
    isCreatingEmbeddings: Boolean,
    isImportingEmbeddings: Boolean,
    isSearchingMessages: Boolean,
    onCreateEmbeddings: () -> Unit,
    onImportEmbeddings: () -> Unit,
    onSearchMessages: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                modifier = Modifier.padding(start = dimensions().spacing8x),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.debug_settings_ai_message_embeddings)
            )
        },
        actions = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)) {
                WirePrimaryButton(
                    minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    onClick = onCreateEmbeddings,
                    text = stringResource(R.string.debug_settings_ai_create_embeddings),
                    fillMaxWidth = false,
                    loading = isCreatingEmbeddings,
                    state = if (isCreatingEmbeddings || isImportingEmbeddings) {
                        WireButtonState.Disabled
                    } else {
                        WireButtonState.Default
                    }
                )
                WirePrimaryButton(
                    minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    onClick = onImportEmbeddings,
                    text = stringResource(R.string.debug_settings_ai_import_embeddings),
                    fillMaxWidth = false,
                    loading = isImportingEmbeddings,
                    state = if (isCreatingEmbeddings || isImportingEmbeddings) {
                        WireButtonState.Disabled
                    } else {
                        WireButtonState.Default
                    }
                )
                WirePrimaryButton(
                    minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    onClick = onSearchMessages,
                    text = stringResource(R.string.debug_settings_ai_search),
                    fillMaxWidth = false,
                    loading = isSearchingMessages,
                    state = if (isSearchingMessages) WireButtonState.Disabled else WireButtonState.Default
                )
            }
        }
    )
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
    fun createEmbeddings() {}
    fun importEmbeddings() {}
    fun searchMessages(query: String) {}
    fun selectModel(descriptor: AiModelDescriptor) {}
    fun selectInferenceBackend(backend: AiInferenceBackend) {}
    fun selectCpuThreads(cpuThreads: Int?) {}
    fun dismissAuthorizationDialog() {}
    fun authorizeModelAccess(url: String) {}
}

class AiAssistantDebugViewModelImpl(
    private val aiModelManager: AiModelManager,
    private val aiEmbeddingModelManager: AiEmbeddingModelManager,
    private val aiModelTestEngine: AiModelTestEngine,
    private val inferenceConfigStore: AiInferenceConfigStore,
    private val currentAccount: UserId,
    private val createMessageEmbeddingsWorkScheduler: CreateMessageEmbeddingsWorkScheduler,
    private val searchMessagesSemanticallyGlobally: SearchMessagesSemanticallyGloballyUseCase,
) : ViewModel(), AiAssistantDebugViewModel {

    override var state by mutableStateOf(AiAssistantDebugState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()
    private val _authorizationUrl = MutableSharedFlow<String>()
    override val authorizationUrl = _authorizationUrl.asSharedFlow()
    private var healthCheckJob: Job? = null
    private var checkedHealthCheckKey: HealthCheckKey? = null
    private var currentAiModelStatus: AiModelStatus = AiModelStatus.NotDownloaded

    init {
        state = state.copy(
            availableModels = aiModelManager.availableModels,
            selectedModel = aiModelManager.selectedModel.value
        )
        observeSelectedModel()
        observeInferenceConfig()
        observeAiModelStatus()
        observeEmbeddingModelStatus()
        observeCreateEmbeddingsWorkStatus()
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
        if (state.embeddingModelOptionState.isDownloading) return

        viewModelScope.launch {
            aiEmbeddingModelManager.downloadModel().collect { downloadState ->
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

    override fun createEmbeddings() {
        if (state.isCreatingEmbeddings || state.isImportingEmbeddings) return

        viewModelScope.launch {
            state = state.copy(isCreatingEmbeddings = true)
            when (val status = createMessageEmbeddingsWorkScheduler.enqueue(currentAccount).first { it.isTerminal }) {
                is CreateMessageEmbeddingsWorkStatus.Succeeded ->
                    _infoMessage.emit(
                        UIText.StringResource(
                            R.string.debug_settings_ai_create_embeddings_success,
                            status.summary.createdEmbeddings,
                            status.summary.skippedMessages,
                            status.summary.failedMessages
                        )
                    )

                is CreateMessageEmbeddingsWorkStatus.Failed ->
                    _infoMessage.emit(
                        UIText.StringResource(
                            R.string.debug_settings_ai_create_embeddings_failed,
                            status.cause.orEmpty()
                        )
                    )

                CreateMessageEmbeddingsWorkStatus.Idle,
                is CreateMessageEmbeddingsWorkStatus.Running -> {
                    // Filtered out by terminal predicate.
                }
            }
        }
    }

    override fun importEmbeddings() {
        if (state.isCreatingEmbeddings || state.isImportingEmbeddings) return

        viewModelScope.launch {
            state = state.copy(isImportingEmbeddings = true)
            when (val status = createMessageEmbeddingsWorkScheduler.enqueueImport(currentAccount).first { it.isTerminal }) {
                is CreateMessageEmbeddingsWorkStatus.Succeeded ->
                    _infoMessage.emit(
                        UIText.StringResource(
                            R.string.debug_settings_ai_import_embeddings_success,
                            status.summary.importedEmbeddings,
                            status.summary.skippedMessages,
                            status.summary.failedMessages
                        )
                    )

                is CreateMessageEmbeddingsWorkStatus.Failed ->
                    _infoMessage.emit(
                        UIText.StringResource(
                            R.string.debug_settings_ai_import_embeddings_failed,
                            status.cause.orEmpty()
                        )
                    )

                CreateMessageEmbeddingsWorkStatus.Idle,
                is CreateMessageEmbeddingsWorkStatus.Running -> {
                    // Filtered out by terminal predicate.
                }
            }
        }
    }

    override fun searchMessages(query: String) {
        if (query.isBlank() || state.isSearchingMessages) return

        state = state.copy(isSearchingMessages = true)
        viewModelScope.launch {
            try {
                when (val result = searchMessagesSemanticallyGlobally(query)) {
                    is SearchMessagesSemanticallyGloballyUseCase.Result.Success ->
                        logSemanticSearchResults(query, result.messages)

                    is SearchMessagesSemanticallyGloballyUseCase.Result.Failure -> {
                        appLogger.e("$SEMANTIC_SEARCH_LOG_TAG: Search failed: ${result.cause}")
                        _infoMessage.emit(
                            UIText.StringResource(
                                R.string.debug_settings_ai_semantic_search_failed,
                                result.cause.toString()
                            )
                        )
                    }
                }
            } catch (throwable: Throwable) {
                appLogger.e("$SEMANTIC_SEARCH_LOG_TAG: Search failed", throwable)
                _infoMessage.emit(
                    UIText.StringResource(
                        R.string.debug_settings_ai_semantic_search_failed,
                        throwable.toString()
                    )
                )
            } finally {
                state = state.copy(isSearchingMessages = false)
            }
        }
    }

    private fun logSemanticSearchResults(query: String, messages: List<Message.Standalone>) {
        appLogger.i("$SEMANTIC_SEARCH_LOG_TAG: query=\"$query\", resultCount=${messages.size}")
        messages.forEachIndexed { index, message ->
            val contentType = message.content::class.simpleName.orEmpty()
            val contentText = when(val content = message.content) {
                is MessageContent.Text -> content.value
                is MessageContent.TextEdited -> content.newContent
                is MessageContent.Multipart -> content.value
                is MessageContent.MultipartEdited -> content.newTextContent
                else -> "Unsupported"
            }
            appLogger.i(
                "$SEMANTIC_SEARCH_LOG_TAG: rank=${index + 1}, " +
                    "text = $contentText " +
                    "conversationId=${message.conversationId}, " +
                    "messageId=${message.id}, contentType=$contentType, date=${message.date}"
            )
        }
    }

    override fun selectModel(descriptor: AiModelDescriptor) {
        aiModelManager.selectModel(descriptor)
    }

    override fun selectInferenceBackend(backend: AiInferenceBackend) {
        if (backend == state.inferenceConfig.backend) return

        when (backend) {
            AiInferenceBackend.CPU -> persistInferenceConfig(state.inferenceConfig.copy(backend = AiInferenceBackend.CPU))
            AiInferenceBackend.GPU -> testAndCommitGpuInference()
        }
    }

    override fun selectCpuThreads(cpuThreads: Int?) {
        if (cpuThreads == state.inferenceConfig.cpuThreads) return

        persistInferenceConfig(
            state.inferenceConfig.copy(
                backend = AiInferenceBackend.CPU,
                cpuThreads = cpuThreads
            )
        )
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

    private fun observeInferenceConfig() {
        viewModelScope.launch {
            inferenceConfigStore.observeConfig().collect { config ->
                if (state.inferenceConfig != config) {
                    state = state.copy(inferenceConfig = config)
                    updateHealthCheck(currentAiModelStatus)
                }
            }
        }
    }

    private fun observeAiModelStatus() {
        viewModelScope.launch {
            aiModelManager.observeModelStatus().collect { modelStatus ->
                currentAiModelStatus = modelStatus
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

    private fun observeCreateEmbeddingsWorkStatus() {
        viewModelScope.launch {
            createMessageEmbeddingsWorkScheduler.observe(currentAccount).collect { status ->
                state = state.copy(
                    isCreatingEmbeddings = status is CreateMessageEmbeddingsWorkStatus.Running &&
                        status.operation == EmbeddingWorkOperation.CREATE,
                    isImportingEmbeddings = status is CreateMessageEmbeddingsWorkStatus.Running &&
                        status.operation == EmbeddingWorkOperation.IMPORT_SQL_VECTORS
                )
            }
        }
    }

    private fun updateHealthCheck(modelStatus: AiModelStatus) {
        when (modelStatus) {
            AiModelStatus.NotDownloaded,
            is AiModelStatus.Downloading -> {
                checkedHealthCheckKey = null
                healthCheckJob?.cancel()
                healthCheckJob = null
                state = state.copy(healthCheckState = AiModelHealthCheckState.Unavailable)
            }

            is AiModelStatus.Ready -> runHealthCheckIfNeeded(modelStatus.localPath)
        }
    }

    private fun runHealthCheckIfNeeded(modelPath: String) {
        val healthCheckKey = HealthCheckKey(modelPath, state.inferenceConfig)
        if (checkedHealthCheckKey == healthCheckKey) return

        healthCheckJob?.cancel()
        checkedHealthCheckKey = healthCheckKey
        state = state.copy(healthCheckState = AiModelHealthCheckState.Running)
        healthCheckJob = viewModelScope.launch {
            state = state.copy(healthCheckState = aiModelTestEngine.runHealthCheck(modelPath, healthCheckKey.config).toUiState())
        }
    }

    private fun persistInferenceConfig(config: AiInferenceConfig) {
        viewModelScope.launch {
            inferenceConfigStore.setConfig(config)
        }
    }

    private fun testAndCommitGpuInference() {
        val modelStatus = currentAiModelStatus
        if (modelStatus !is AiModelStatus.Ready) {
            state = state.copy(healthCheckState = AiModelHealthCheckState.Unavailable)
            return
        }

        val gpuConfig = AiInferenceConfig(
            backend = AiInferenceBackend.GPU,
            cpuThreads = state.inferenceConfig.cpuThreads
        )
        val healthCheckKey = HealthCheckKey(modelStatus.localPath, gpuConfig)
        healthCheckJob?.cancel()
        checkedHealthCheckKey = healthCheckKey
        state = state.copy(healthCheckState = AiModelHealthCheckState.Running)
        healthCheckJob = viewModelScope.launch {
            when (val result = aiModelTestEngine.runHealthCheck(modelStatus.localPath, gpuConfig)) {
                AiModelHealthCheckResult.Healthy -> {
                    inferenceConfigStore.setConfig(gpuConfig)
                    state = state.copy(healthCheckState = AiModelHealthCheckState.Healthy)
                }

                else -> {
                    checkedHealthCheckKey = null
                    state = state.copy(healthCheckState = result.toUiState())
                }
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
    val inferenceConfig: AiInferenceConfig = AiInferenceConfig.DEFAULT,
    val aiModelOptionState: AiModelOptionState = AiModelOptionState(),
    val embeddingModelOptionState: AiModelOptionState = AiModelOptionState(),
    val isCreatingEmbeddings: Boolean = false,
    val isImportingEmbeddings: Boolean = false,
    val isSearchingMessages: Boolean = false,
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
private const val UNSUPPORTED_MODEL_FAILURE_REASON = "Model type is not supported by the LiteRT-LM health check"
private const val SEMANTIC_SEARCH_LOG_TAG = "AI semantic search"
private const val SEMANTIC_SEARCH_QUERY_TEST_TAG = "semantic-search-query"
private val URL_REGEX = Regex("""https?://[^\s"'<>]+""")

private val CreateMessageEmbeddingsWorkStatus.isTerminal: Boolean
    get() = this is CreateMessageEmbeddingsWorkStatus.Succeeded || this is CreateMessageEmbeddingsWorkStatus.Failed

private data class HealthCheckKey(
    val modelPath: String,
    val config: AiInferenceConfig
)

internal fun String.extractFirstUrl(): String? = URL_REGEX.find(this)?.value

@PreviewMultipleThemes
@Composable
fun PreviewAiAssistantDebugScreen() = WireTheme {
    AiAssistantDebugScreenContent(
        state = AiAssistantDebugState(),
        onNavigationPressed = {},
        onDownloadAiModel = {},
        onDownloadEmbeddingModel = {},
        onCreateEmbeddings = {},
        onImportEmbeddings = {},
        onSearchMessages = {},
        onModelSelected = {},
        onInferenceBackendSelected = {},
        onCpuThreadsSelected = {},
        onDismissAuthorizationDialog = {},
        onAuthorizeModelAccess = {}
    )
}
