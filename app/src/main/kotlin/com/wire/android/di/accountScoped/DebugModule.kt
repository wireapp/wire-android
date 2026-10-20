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
import com.wire.kalium.logic.di.KaliumDebugApi
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.debug.ChangeProfilingUseCase
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.DisableEventProcessingUseCase
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCUseCase
import com.wire.kalium.logic.feature.debug.GetCryptoServiceReportUseCase
import com.wire.kalium.logic.feature.debug.GetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.debug.GetSqlCipherVersionUseCase
import com.wire.kalium.logic.feature.debug.ObserveDatabaseLoggerStateUseCase
import com.wire.kalium.logic.feature.debug.ObserveDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.SetDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.debug.SetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
@Suppress("TooManyFunctions")
class DebugModule {

    @Provides
    fun provideDebugApi(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): KaliumDebugApi = coreLogic.getSessionScope(currentAccount).debugApi

    @Provides
    fun provideDisableEventProcessing(api: KaliumDebugApi): DisableEventProcessingUseCase =
        api.debugDisableEventProcessing

    @Provides
    fun provideBreakSessionUseCase(api: KaliumDebugApi): BreakSessionUseCase =
        api.debugBreakSession

    @Provides
    fun provideSendFCMTokenToAPIUseCase(api: KaliumDebugApi): SendFCMTokenUseCase =
        api.debugSendFCMTokenToServer

    @Provides
    fun provideChangeProfilingUseCase(api: KaliumDebugApi): ChangeProfilingUseCase =
        api.debugChangeProfiling

    @Provides
    fun provideObserveDatabaseLoggerState(api: KaliumDebugApi): ObserveDatabaseLoggerStateUseCase =
        api.debugObserveDatabaseLoggerState

    @Provides
    fun provideFeatureConfigUseCase(api: KaliumDebugApi): GetFeatureConfigUseCase = api.debugGetFeatureConfig

    @Provides
    fun provideGetSqlCipherVersionUseCase(api: KaliumDebugApi): GetSqlCipherVersionUseCase = api.debugGetSqlCipherVersion

    @Provides
    fun provideGetCryptoServiceReportUseCase(api: KaliumDebugApi): GetCryptoServiceReportUseCase =
        api.debugGetCryptoServiceReport

    @Provides
    fun provideGetDebugE2EICertificateExpirationUseCase(api: KaliumDebugApi): GetDebugE2EICertificateExpirationUseCase =
        api.debugGetDebugE2EICertificateExpiration

    @Provides
    fun provideSetDebugE2EICertificateExpirationUseCase(api: KaliumDebugApi): SetDebugE2EICertificateExpirationUseCase =
        api.debugSetDebugE2EICertificateExpiration

    @Provides
    fun provideObserveDebugCRLExpirationAfterOneMinuteUseCase(
        api: KaliumDebugApi
    ): ObserveDebugCRLExpirationAfterOneMinuteUseCase =
        api.debugObserveDebugCRLExpirationAfterOneMinute

    @Provides
    fun provideSetDebugCRLExpirationAfterOneMinuteUseCase(
        api: KaliumDebugApi
    ): SetDebugCRLExpirationAfterOneMinuteUseCase =
        api.debugSetDebugCRLExpirationAfterOneMinute

    @Provides
    fun provideGetConversationEpochFromCCUseCase(api: KaliumDebugApi): GetConversationEpochFromCCUseCase =
        api.debugGetConversationEpochFromCC

    @Provides
    fun provideDebugFeedConversationUseCase(api: KaliumDebugApi): DebugFeedConversationUseCase =
        api.debugDebugFeedConversationUseCase

    @Provides
    fun provideRepairFaultyRemovalKeysUseCase(api: KaliumDebugApi): RepairFaultyRemovalKeysUseCase =
        api.debugRepairFaultyRemovalKeysUseCase

    @Provides
    fun provideGetConversationCryptoStatsUseCase(api: KaliumDebugApi): GetConversationCryptoStatsUseCase =
        api.debugGetConversationCryptoStats
}
