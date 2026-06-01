/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig.DOMAIN_REMOVAL_KEYS_FOR_REPAIR
import com.wire.android.appLogger
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.ui.debug.DebugDataOptionsViewModelImpl.Companion.PERCENT_MULTIPLIER
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.ui.UIText
import com.wire.android.util.uiText
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.RepairResult
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsResult
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.debug.GetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS
import com.wire.kalium.logic.feature.debug.ObserveDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.SetDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.SetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.TargetedRepairParam
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenError
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenResult
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.days

@Suppress("TooManyFunctions")
@ViewModelScopedPreview
interface DebugDataOptionsViewModel {
    val infoMessage: SharedFlow<UIText> get() = MutableSharedFlow()
    val state: DebugDataOptionsState get() = DebugDataOptionsState()
    val e2eiCertificateExpirationInputState: TextFieldState get() = TextFieldState("6")
    fun currentAccount(): UserId = UserId("value", "domain")
    fun checkCrlRevocationList() {}
    fun forceCRLExpirationAfterOneMinute(enabled: Boolean) {}
    fun restartSlowSyncForRecovery() {}
    fun enrollE2EICertificate() {}
    fun updateE2EICertificateExpiration(seconds: Long) {}
    fun updateE2EICertificateExpirationInput(minutes: String) {}
    fun handleE2EIEnrollmentResult(result: FinalizeEnrollmentResult) {}
    fun dismissCertificateDialog() {}
    fun forceUpdateApiVersions() {}
    fun disableEventProcessing(disabled: Boolean) {}
    fun forceSendFCMToken() {}
    fun enableAsyncNotifications(enabled: Boolean) {}

    fun repairFaultRemovalKeys() {}
}

@Suppress("LongParameterList", "TooManyFunctions")
class DebugDataOptionsViewModelImpl(
    private val context: Context,
    val currentAccount: UserId,
    private val updateApiVersions: UpdateApiVersionsScheduler,
    private val mlsKeyPackageCount: MLSKeyPackageCountUseCase,
    private val restartSlowSyncProcessForRecovery: RestartSlowSyncProcessForRecoveryUseCase,
    private val checkCrlRevocationList: CheckCrlRevocationListUseCase,
    private val getCurrentAnalyticsTrackingIdentifier: GetCurrentAnalyticsTrackingIdentifierUseCase,
    private val sendFCMToken: SendFCMTokenUseCase,
    private val dispatcherProvider: DispatcherProvider,
    private val selfServerConfigUseCase: SelfServerConfigUseCase,
    private val getDefaultProtocolUseCase: GetDefaultProtocolUseCase,
    private val observeAsyncNotificationsEnabled: ObserveIsConsumableNotificationsEnabledUseCase,
    private val startUsingAsyncNotifications: StartUsingAsyncNotificationsUseCase,
    private val repairFaultyRemovalKeys: RepairFaultyRemovalKeysUseCase,
    private val getDebugE2EICertificateExpiration: GetDebugE2EICertificateExpirationUseCase,
    private val setDebugE2EICertificateExpiration: SetDebugE2EICertificateExpirationUseCase,
    private val observeDebugCRLExpirationAfterOneMinute: ObserveDebugCRLExpirationAfterOneMinuteUseCase,
    private val setDebugCRLExpirationAfterOneMinute: SetDebugCRLExpirationAfterOneMinuteUseCase,
    private val aiModelManager: AiModelManager,
) : ViewModel(), DebugDataOptionsViewModel {
    private companion object {
        val DEFAULT_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS = 90.days.inWholeSeconds
        const val SECONDS_PER_MINUTE = 60L
        const val MINUTES_ROUNDING_OFFSET_SECONDS = SECONDS_PER_MINUTE - 1
        val MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_MINUTES =
            MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS / SECONDS_PER_MINUTE
        const val PERCENT_MULTIPLIER = 100
    }

    override var state by mutableStateOf(
        DebugDataOptionsState()
    )
    override val e2eiCertificateExpirationInputState = TextFieldState("6")

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    init {
        observeAsyncNotificationsEnabledData()
        observeMlsMetadata()
        observeDebugCRLExpiration()
        observeE2EICertificateExpirationInput()
        setGitHashAndDeviceId()
        setAnalyticsTrackingId()
        setServerConfigData()
        setDefaultProtocol()
        loadDebugE2EICertificateExpiration()
        observeAiModelStatus()
    }

    private fun observeAsyncNotificationsEnabledData() {
        viewModelScope.launch {
            observeAsyncNotificationsEnabled().collect {
                state = state.copy(isAsyncNotificationsEnabled = it)
            }
        }
    }

    private fun setDefaultProtocol() {
        viewModelScope.launch {
            val defaultProtocol = when (getDefaultProtocolUseCase()) {
                SupportedProtocol.PROTEUS -> "Proteus"
                SupportedProtocol.MLS -> "MLS"
            }
            state = state.copy(defaultProtocol = defaultProtocol)
        }
    }

    private fun setServerConfigData() {
        viewModelScope.launch {
            val result = selfServerConfigUseCase()
            if (result is SelfServerConfigUseCase.Result.Success) {
                state = state.copy(
                    isFederationEnabled = result.serverLinks.metaData.federation,
                    currentApiVersion = when (result.serverLinks.metaData.commonApiVersion) {
                        CommonApiVersionType.Unknown -> "Unknown"
                        else -> result.serverLinks.metaData.commonApiVersion.version.toString()
                    },
                )
            }
        }
    }

    private fun setAnalyticsTrackingId() {
        viewModelScope.launch {
            getCurrentAnalyticsTrackingIdentifier()?.let { trackingId ->
                state = state.copy(
                    analyticsTrackingId = trackingId
                )
            }
        }
    }

    private fun setGitHashAndDeviceId() {
        viewModelScope.launch {
            val deviceId = context.getDeviceIdString() ?: "null"
            val gitBuildId = context.getGitBuildId()
            state = state.copy(
                debugId = deviceId,
                commitish = gitBuildId
            )
        }
    }

    override fun currentAccount(): UserId = currentAccount

    override fun checkCrlRevocationList() {
        viewModelScope.launch {
            checkCrlRevocationList(
                forceUpdate = true
            )
        }
    }

    override fun forceCRLExpirationAfterOneMinute(enabled: Boolean) {
        viewModelScope.launch {
            setDebugCRLExpirationAfterOneMinute(enabled)
            if (enabled) {
                checkCrlRevocationList(forceUpdate = true)
            }
        }
    }

    override fun restartSlowSyncForRecovery() {
        viewModelScope.launch {
            restartSlowSyncProcessForRecovery()
        }
    }

    override fun enrollE2EICertificate() {
        val normalizedMinutes = e2eiCertificateExpirationInputState.text.toString()
            .toLongOrNull()
            ?.coerceAtLeast(MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_MINUTES)
            ?: MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_MINUTES
        setE2EICertificateExpiration(normalizedMinutes * SECONDS_PER_MINUTE)
        state = state.copy(startGettingE2EICertificate = true)
    }

    override fun updateE2EICertificateExpiration(seconds: Long) {
        setE2EICertificateExpiration(seconds)
    }

    override fun updateE2EICertificateExpirationInput(minutes: String) {
        if (e2eiCertificateExpirationInputState.text.toString() != minutes) {
            e2eiCertificateExpirationInputState.setTextAndPlaceCursorAtEnd(minutes)
        }
    }

    override fun handleE2EIEnrollmentResult(result: FinalizeEnrollmentResult) {
        state = when (result) {
            is FinalizeEnrollmentResult.Failure.OAuthError -> {
                state.copy(
                    certificate = result.reason,
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            }

            is FinalizeEnrollmentResult.Failure -> {
                state.copy(
                    certificate = result.toString(),
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            }

            is FinalizeEnrollmentResult.Success -> {
                state.copy(
                    certificate = result.certificate,
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            }
        }
    }

    override fun dismissCertificateDialog() {
        state = state.copy(
            showCertificate = false,
        )
    }

    override fun forceUpdateApiVersions() {
        updateApiVersions.scheduleImmediateApiVersionUpdate()
    }

    override fun disableEventProcessing(disabled: Boolean) {
        viewModelScope.launch {
            disableEventProcessing(disabled)
            state = state.copy(isEventProcessingDisabled = disabled)
        }
    }

    override fun enableAsyncNotifications(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                when (val result = startUsingAsyncNotifications()) {
                    is StartUsingAsyncNotificationsResult.Failure ->
                        _infoMessage.emit(UIText.DynamicString("Can't enable async notifications, error: ${result.coreFailure.uiText()}"))

                    is StartUsingAsyncNotificationsResult.Success -> state = state.copy(isAsyncNotificationsEnabled = enabled)
                }
            }
        }
    }

    override fun repairFaultRemovalKeys() {
        viewModelScope.launch {
            state = state.copy(mlsInfoState = state.mlsInfoState.copy(isLoadingRepair = true))
            val (domain, faultyKey) = DOMAIN_REMOVAL_KEYS_FOR_REPAIR.entries.firstOrNull { it.key == currentAccount.domain }
                ?: run {
                    appLogger.w("No faulty removal keys configured for repair")
                    _infoMessage.emit(UIText.DynamicString("No faulty removal keys configured for repair"))
                    state = state.copy(mlsInfoState = state.mlsInfoState.copy(isLoadingRepair = false))
                    return@launch
                }

            val result = repairFaultyRemovalKeys(
                param = TargetedRepairParam(
                    domain = domain,
                    faultyKeys = faultyKey
                )
            )
            when (result) {
                RepairResult.Error -> appLogger.e("Error occurred during repair of faulty removal keys")
                RepairResult.NoConversationsToRepair -> appLogger.i("No conversations to repair")
                RepairResult.RepairNotNeeded -> appLogger.i("Repair not needed")
                is RepairResult.RepairPerformed -> {
                    _infoMessage.emit(UIText.DynamicString("Reset finalized"))
                    appLogger.i("Repair performed: ${result.toLogString()}")
                }
            }
            state = state.copy(mlsInfoState = state.mlsInfoState.copy(isLoadingRepair = false))
        }
    }

    override fun forceSendFCMToken() {
        viewModelScope.launch {
            withContext(dispatcherProvider.io()) {
                val result = sendFCMToken()
                when (result) {
                    is SendFCMTokenResult.Failure -> {
                        when (result.error.status) {
                            SendFCMTokenError.Reason.CANT_GET_CLIENT_ID -> {
                                _infoMessage.emit(UIText.DynamicString("Can't get client ID, error: ${result.error.error}"))
                            }

                            SendFCMTokenError.Reason.CANT_GET_NOTIFICATION_TOKEN -> {
                                _infoMessage.emit(UIText.DynamicString("Can't get notification token, error: ${result.error.error}"))
                            }

                            SendFCMTokenError.Reason.CANT_REGISTER_TOKEN -> {
                                _infoMessage.emit(UIText.DynamicString("Can't register token, error: ${result.error.error}"))
                            }
                        }
                    }

                    is SendFCMTokenResult.Success -> _infoMessage.emit(UIText.DynamicString("Token registered"))
                }
            }
        }
    }

    private fun observeMlsMetadata() {
        viewModelScope.launch {
            mlsKeyPackageCount().let {
                when (it) {
                    is MLSKeyPackageCountResult.Success -> {
                        state = state.copy(
                            mlsInfoState = state.mlsInfoState.copy(
                                keyPackagesCount = it.count,
                                mlsClientId = it.clientId.value
                            )
                        )
                    }

                    is MLSKeyPackageCountResult.Failure.NetworkCallFailure -> {
                        state = state.copy(mlsInfoState = state.mlsInfoState.copy(mlsErrorMessage = "Network Error!"))
                    }

                    is MLSKeyPackageCountResult.Failure.FetchClientIdFailure -> {
                        state = state.copy(mlsInfoState = state.mlsInfoState.copy(mlsErrorMessage = "ClientId Fetch Error!"))
                    }

                    is MLSKeyPackageCountResult.Failure.Generic -> {}
                    MLSKeyPackageCountResult.Failure.NotEnabled -> {
                        state = state.copy(mlsInfoState = state.mlsInfoState.copy(mlsErrorMessage = "Not Enabled!"))
                    }
                }
            }
        }
    }

    private fun observeDebugCRLExpiration() {
        viewModelScope.launch {
            observeDebugCRLExpirationAfterOneMinute().collect { enabled ->
                state = state.copy(forceCRLExpirationAfterOneMinute = enabled)
            }
        }
    }

    private fun loadDebugE2EICertificateExpiration() {
        viewModelScope.launch {
            val currentExpiration = getDebugE2EICertificateExpiration()
            if (currentExpiration == DEFAULT_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS) {
                // For debug UX we default to the minimum test-friendly value instead of 90 days.
                setE2EICertificateExpiration(MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS)
            } else {
                val minutes = (currentExpiration + MINUTES_ROUNDING_OFFSET_SECONDS) / SECONDS_PER_MINUTE
                e2eiCertificateExpirationInputState.setTextAndPlaceCursorAtEnd(minutes.toString())
                state = state.copy(
                    e2eiCertificateExpirationSeconds = currentExpiration
                )
            }
        }
    }

    private fun observeE2EICertificateExpirationInput() {
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { e2eiCertificateExpirationInputState.text.toString() }
                .drop(1)
                .distinctUntilChanged()
                .collectLatest { minutesText ->
                    val minutes = minutesText.toLongOrNull() ?: return@collectLatest
                    if (minutes >= MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_MINUTES) {
                        applyE2EICertificateExpiration(minutes * SECONDS_PER_MINUTE)
                    }
                }
        }
    }

    private fun setE2EICertificateExpiration(seconds: Long) {
        val expiration = seconds.coerceAtLeast(MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS)
        val minutes = (expiration + MINUTES_ROUNDING_OFFSET_SECONDS) / SECONDS_PER_MINUTE
        e2eiCertificateExpirationInputState.setTextAndPlaceCursorAtEnd(minutes.toString())
        applyE2EICertificateExpiration(expiration)
    }

    private fun applyE2EICertificateExpiration(seconds: Long) {
        val expiration = seconds.coerceAtLeast(MIN_DEBUG_E2EI_CERTIFICATE_EXPIRATION_SECONDS)
        state = state.copy(
            e2eiCertificateExpirationSeconds = expiration
        )
        viewModelScope.launch {
            setDebugE2EICertificateExpiration(expiration)
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
