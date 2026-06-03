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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.ui.debug.conversation.DebugConversationViewModel
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModel
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModel
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModel
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModel
import com.wire.android.ui.home.whatsnew.WhatsNewViewModel
import com.wire.android.ui.settings.about.AboutThisAppViewModel
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.logging.LogFileWriter
import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.backup.CreateObfuscatedCopyUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.debug.ChangeProfilingUseCase
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCUseCase
import com.wire.kalium.logic.feature.debug.GetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.debug.ObserveDatabaseLoggerStateUseCase
import com.wire.kalium.logic.feature.debug.ObserveDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.SetDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.SetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Suppress("LongParameterList")
class DebugInfoViewModelFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    @CurrentAccount private val currentAccount: UserId,
    private val logFileWriter: LogFileWriter,
    private val currentClientIdUseCase: ObserveCurrentClientIdUseCase,
    private val globalDataStore: GlobalDataStore,
    private val changeProfilingUseCase: ChangeProfilingUseCase,
    private val observeDatabaseLoggerState: ObserveDatabaseLoggerStateUseCase,
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
    private val createUnencryptedCopy: CreateObfuscatedCopyUseCase,
    private val fileManager: FileManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val resetMLSConversation: ResetMLSConversationUseCase,
    private val fetchConversation: FetchConversationUseCase,
    private val feedConversation: DebugFeedConversationUseCase,
    private val getConversationEpochFromCC: GetConversationEpochFromCCUseCase,
    private val getConversationCryptoStats: GetConversationCryptoStatsUseCase,
    private val getFeatureConfig: GetFeatureConfigUseCase,
) {
    fun userDebugViewModel() = UserDebugViewModel(
        currentAccount = currentAccount,
        logFileWriter = logFileWriter,
        currentClientIdUseCase = currentClientIdUseCase,
        globalDataStore = globalDataStore,
        changeProfilingUseCase = changeProfilingUseCase,
        observeDatabaseLoggerState = observeDatabaseLoggerState,
    )

    fun logManagementViewModel() = LogManagementViewModel(
        logFileWriter = logFileWriter,
        globalDataStore = globalDataStore,
    )

    fun debugDataOptionsViewModel() = DebugDataOptionsViewModelImpl(
        context = context,
        currentAccount = currentAccount,
        updateApiVersions = updateApiVersions,
        mlsKeyPackageCount = mlsKeyPackageCount,
        restartSlowSyncProcessForRecovery = restartSlowSyncProcessForRecovery,
        checkCrlRevocationList = checkCrlRevocationList,
        getCurrentAnalyticsTrackingIdentifier = getCurrentAnalyticsTrackingIdentifier,
        sendFCMToken = sendFCMToken,
        dispatcherProvider = dispatcherProvider,
        selfServerConfigUseCase = selfServerConfigUseCase,
        getDefaultProtocolUseCase = getDefaultProtocolUseCase,
        observeAsyncNotificationsEnabled = observeAsyncNotificationsEnabled,
        startUsingAsyncNotifications = startUsingAsyncNotifications,
        repairFaultyRemovalKeys = repairFaultyRemovalKeys,
        getDebugE2EICertificateExpiration = getDebugE2EICertificateExpiration,
        setDebugE2EICertificateExpiration = setDebugE2EICertificateExpiration,
        observeDebugCRLExpirationAfterOneMinute = observeDebugCRLExpirationAfterOneMinute,
        setDebugCRLExpirationAfterOneMinute = setDebugCRLExpirationAfterOneMinute,
    )

    fun exportObfuscatedCopyViewModel() = ExportObfuscatedCopyViewModelImpl(
        createUnencryptedCopy = createUnencryptedCopy,
        dispatcher = dispatcherProvider,
        fileManager = fileManager,
    )

    fun debugConversationViewModel(savedStateHandle: SavedStateHandle) = DebugConversationViewModel(
        conversationDetails = conversationDetails,
        resetMLSConversation = resetMLSConversation,
        fetchConversation = fetchConversation,
        feedConversation = feedConversation,
        getConversationEpochFromCC = getConversationEpochFromCC,
        savedStateHandle = savedStateHandle,
    )

    fun conversationCryptoStatsViewModel() = ConversationCryptoStatsViewModel(
        getConversationCryptoStats = getConversationCryptoStats,
    )

    fun debugFeatureFlagsViewModel() = DebugFeatureFlagsViewModel(
        getFeatureConfig = getFeatureConfig,
    )

    fun whatsNewViewModel() = WhatsNewViewModel(context = context)

    fun aboutThisAppViewModel() = AboutThisAppViewModel(context = context)

    fun dependenciesViewModel() = DependenciesViewModel(context = context)

    fun licensesViewModel() = LicensesViewModel(context = context)
}
