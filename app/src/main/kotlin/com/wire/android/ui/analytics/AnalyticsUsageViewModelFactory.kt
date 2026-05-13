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
package com.wire.android.ui.analytics

import com.wire.android.datastore.UserDataStore
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.Lazy
import dev.zacsweers.metro.Inject

@Inject
class AnalyticsUsageViewModelFactory(
    private val analyticsEnabled: AnalyticsConfiguration,
    private val dataStore: Lazy<UserDataStore>,
    private val selfServerConfig: Lazy<SelfServerConfigUseCase>,
) {
    fun create(): AnalyticsUsageViewModel = AnalyticsUsageViewModel(
        analyticsEnabled = analyticsEnabled,
        dataStore = dataStore,
        selfServerConfig = selfServerConfig,
    )
}
