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
import com.wire.kalium.logic.feature.debug.DebugScope
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class DebugModule {

    @ViewModelScoped
    @Provides
    fun providesDebugScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): DebugScope = coreLogic.getSessionScope(currentAccount).debug

    @ViewModelScoped
    @Provides
    fun provideDisableEventProcessing(debugScope: DebugScope) =
        debugScope.disableEventProcessing

    @ViewModelScoped
    @Provides
    fun provideBreakSessionUseCase(debugScope: DebugScope): BreakSessionUseCase =
        debugScope.breakSession

    @ViewModelScoped
    @Provides
    fun provideSendFCMTokenToAPIUseCase(debugScope: DebugScope) =
        debugScope.sendFCMTokenToServer

    @ViewModelScoped
    @Provides
    fun provideChangeProfilingUseCase(debugScope: DebugScope) =
        debugScope.changeProfiling

    @ViewModelScoped
    @Provides
    fun provideObserveDatabaseLoggerState(debugScope: DebugScope) =
        debugScope.observeDatabaseLoggerState

    @ViewModelScoped
    @Provides
    fun provideObserveAsyncNotificationsEnabled(debugScope: DebugScope) = debugScope.observeIsConsumableNotificationsEnabled

    @ViewModelScoped
    @Provides
    fun provideStartUsingAsyncNotifications(debugScope: DebugScope) = debugScope.startUsingAsyncNotifications

    @ViewModelScoped
    @Provides
    fun provideFeatureConfigUseCase(debugScope: DebugScope): GetFeatureConfigUseCase = debugScope.getFeatureConfig

    @ViewModelScoped
    @Provides
    fun provideDebugFeedConversationUseCase(debugScope: DebugScope) =
        debugScope.debugFeedConversationUseCase
}
