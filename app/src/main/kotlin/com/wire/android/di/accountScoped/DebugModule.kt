/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.debug.ChangeProfilingUseCase
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.DebugScope
import com.wire.kalium.logic.feature.debug.DisableEventProcessingUseCase
import com.wire.kalium.logic.feature.debug.GetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCUseCase
import com.wire.kalium.logic.feature.debug.ObserveDatabaseLoggerStateUseCase
import com.wire.kalium.logic.feature.debug.ObserveDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.SetDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.SetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import dagger.Module
import dagger.Provides

@Module
@Suppress("TooManyFunctions")
class DebugModule {

    @Provides
    fun providesDebugScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): DebugScope = coreLogic.getSessionScope(currentAccount).debug

    @Provides
    fun provideDisableEventProcessing(debugScope: DebugScope): DisableEventProcessingUseCase =
        debugScope.disableEventProcessing

    @Provides
    fun provideBreakSessionUseCase(debugScope: DebugScope): BreakSessionUseCase =
        debugScope.breakSession

    @Provides
    fun provideSendFCMTokenToAPIUseCase(debugScope: DebugScope): SendFCMTokenUseCase =
        debugScope.sendFCMTokenToServer

    @Provides
    fun provideChangeProfilingUseCase(debugScope: DebugScope): ChangeProfilingUseCase =
        debugScope.changeProfiling

    @Provides
    fun provideObserveDatabaseLoggerState(debugScope: DebugScope): ObserveDatabaseLoggerStateUseCase =
        debugScope.observeDatabaseLoggerState

    @Provides
    fun provideObserveAsyncNotificationsEnabled(debugScope: DebugScope): ObserveIsConsumableNotificationsEnabledUseCase =
        debugScope.observeIsConsumableNotificationsEnabled

    @Provides
    fun provideStartUsingAsyncNotifications(debugScope: DebugScope): StartUsingAsyncNotificationsUseCase =
        debugScope.startUsingAsyncNotifications

    @Provides
    fun provideFeatureConfigUseCase(debugScope: DebugScope): GetFeatureConfigUseCase = debugScope.getFeatureConfig

    @Provides
    fun provideGetDebugE2EICertificateExpirationUseCase(debugScope: DebugScope): GetDebugE2EICertificateExpirationUseCase =
        debugScope.getDebugE2EICertificateExpiration

    @Provides
    fun provideSetDebugE2EICertificateExpirationUseCase(debugScope: DebugScope): SetDebugE2EICertificateExpirationUseCase =
        debugScope.setDebugE2EICertificateExpiration

    @Provides
    fun provideObserveDebugCRLExpirationAfterOneMinuteUseCase(
        debugScope: DebugScope
    ): ObserveDebugCRLExpirationAfterOneMinuteUseCase =
        debugScope.observeDebugCRLExpirationAfterOneMinute

    @Provides
    fun provideSetDebugCRLExpirationAfterOneMinuteUseCase(
        debugScope: DebugScope
    ): SetDebugCRLExpirationAfterOneMinuteUseCase =
        debugScope.setDebugCRLExpirationAfterOneMinute

    @Provides
    fun provideGetConversationEpochFromCCUseCase(debugScope: DebugScope): GetConversationEpochFromCCUseCase =
        debugScope.getConversationEpochFromCC

    @Provides
    fun provideDebugFeedConversationUseCase(debugScope: DebugScope): DebugFeedConversationUseCase =
        debugScope.debugFeedConversationUseCase

    @Provides
    fun provideRepairFaultyRemovalKeysUseCase(debugScope: DebugScope): RepairFaultyRemovalKeysUseCase =
        debugScope.repairFaultyRemovalKeysUseCase

    @Provides
    fun provideGetConversationCryptoStatsUseCase(debugScope: DebugScope): GetConversationCryptoStatsUseCase =
        debugScope.getConversationCryptoStats
}
