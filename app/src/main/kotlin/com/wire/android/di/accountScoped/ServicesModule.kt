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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.service.ServiceScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ServicesModule {

    @ViewModelScoped
    @Provides
    fun provideServiceScope(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): ServiceScope = coreLogic.getSessionScope(currentAccount).service

    @ViewModelScoped
    @Provides
    fun provideObserveIsServiceMemberUseCase(serviceScope: ServiceScope): ObserveIsServiceMemberUseCase =
        serviceScope.observeIsServiceMember

    @ViewModelScoped
    @Provides
    fun provideGetServiceByIdUseCase(serviceScope: ServiceScope): GetServiceByIdUseCase =
        serviceScope.getServiceById

    @ViewModelScoped
    @Provides
    fun provideObserveAllServicesUseCase(serviceScope: ServiceScope): ObserveAllServicesUseCase =
        serviceScope.observeAllServices

    @ViewModelScoped
    @Provides
    fun provideSearchServicesByNameUseCase(serviceScope: ServiceScope): SearchServicesByNameUseCase =
        serviceScope.searchServicesByName

    @ViewModelScoped
    @Provides
    fun provideObserveIsAppsAllowedForUsage(serviceScope: ServiceScope) =
        serviceScope.observeIsAppsAllowedForUsage
}
