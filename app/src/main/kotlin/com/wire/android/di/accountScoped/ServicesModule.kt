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
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.service.ServiceScope
import com.wire.kalium.logic.feature.service.SyncServicesUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
class ServicesModule {

    @Provides
    fun provideServiceScope(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): ServiceScope = coreLogic.getSessionScope(currentAccount).service

    @Provides
    fun provideObserveIsServiceMemberUseCase(serviceScope: ServiceScope): ObserveIsServiceMemberUseCase =
        serviceScope.observeIsServiceMember

    @Provides
    fun provideGetServiceByIdUseCase(serviceScope: ServiceScope): GetServiceByIdUseCase =
        serviceScope.getServiceById

    @Provides
    fun provideObserveAllServicesUseCase(serviceScope: ServiceScope): ObserveAllServicesUseCase =
        serviceScope.observeAllServices

    @Provides
    fun provideSyncServicesUseCase(serviceScope: ServiceScope): SyncServicesUseCase =
        serviceScope.syncServices

    @Provides
    fun provideSearchServicesByNameUseCase(serviceScope: ServiceScope): SearchServicesByNameUseCase =
        serviceScope.searchServicesByName

    @Provides
    fun provideObserveIsAppsAllowedForUsage(serviceScope: ServiceScope): ObserveIsAppsAllowedForUsageUseCase =
        serviceScope.observeIsAppsAllowedForUsage
}
