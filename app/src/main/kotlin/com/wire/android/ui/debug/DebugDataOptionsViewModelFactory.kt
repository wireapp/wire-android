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

import com.wire.android.di.CurrentAccount
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.debug.GetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.SetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class DebugDataOptionsViewModelFactory(
    private val debugDataInfoProvider: DebugDataInfoProvider,
    @CurrentAccount private val currentAccount: UserId,
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
) {
    fun create(): DebugDataOptionsViewModelImpl = DebugDataOptionsViewModelImpl(
        debugDataInfoProvider = debugDataInfoProvider,
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
    )
}
