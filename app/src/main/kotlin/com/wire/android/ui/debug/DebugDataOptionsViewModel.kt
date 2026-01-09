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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig.DOMAIN_REMOVAL_KEYS_FOR_REPAIR
import com.wire.android.appLogger
import com.wire.android.di.CurrentAccount
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.ui.UIText
import com.wire.android.util.uiText
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.RepairResult
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsResult
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.debug.TargetedRepairParam
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenError
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelScopedPreview
interface DebugDataOptionsViewModel {
    val infoMessage: SharedFlow<UIText> get() = MutableSharedFlow()
    val state: DebugDataOptionsState get() = DebugDataOptionsState()
    fun currentAccount(): UserId = UserId("value", "domain")
    fun checkCrlRevocationList() {}
    fun restartSlowSyncForRecovery() {}
    fun enrollE2EICertificate() {}
    fun handleE2EIEnrollmentResult(result: FinalizeEnrollmentResult) {}
    fun dismissCertificateDialog() {}
    fun forceUpdateApiVersions() {}
    fun disableEventProcessing(disabled: Boolean) {}
    fun forceSendFCMToken() {}
    fun enableAsyncNotifications(enabled: Boolean) {}

    fun repairFaultRemovalKeys() {}
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class DebugDataOptionsViewModelImpl
@Inject constructor(
    @ApplicationContext private val context: Context,
    @CurrentAccount val currentAccount: UserId,
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
) : ViewModel(), DebugDataOptionsViewModel {

    override var state by mutableStateOf(
        DebugDataOptionsState()
    )

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    init {
        observeAsyncNotificationsEnabledData()
        observeMlsMetadata()
        setGitHashAndDeviceId()
        setAnalyticsTrackingId()
        setServerConfigData()
        setDefaultProtocol()
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
            state = state.copy(
                defaultProtocol = when (getDefaultProtocolUseCase()) {
                    SupportedProtocol.PROTEUS -> "Proteus"
                    SupportedProtocol.MLS -> "MLS"
                }
            )
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

    override fun restartSlowSyncForRecovery() {
        viewModelScope.launch {
            restartSlowSyncProcessForRecovery()
        }
    }

    override fun enrollE2EICertificate() {
        state = state.copy(startGettingE2EICertificate = true)
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
                result.fold(
                    {
                        when (it.status) {
                            SendFCMTokenError.Reason.CANT_GET_CLIENT_ID -> {
                                _infoMessage.emit(UIText.DynamicString("Can't get client ID, error: ${it.error}"))
                            }

                            SendFCMTokenError.Reason.CANT_GET_NOTIFICATION_TOKEN -> {
                                _infoMessage.emit(UIText.DynamicString("Can't get notification token, error: ${it.error}"))
                            }

                            SendFCMTokenError.Reason.CANT_REGISTER_TOKEN -> {
                                _infoMessage.emit(UIText.DynamicString("Can't register token, error: ${it.error}"))
                            }
                        }
                    },
                    {
                        _infoMessage.emit(UIText.DynamicString("Token registered"))
                    }
                )
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
    //endregion
}
//endregion

@Serializable
object DebugDataOptions : ScopedArgs {
    override val key = "DebugDataOptionsKey"
}
