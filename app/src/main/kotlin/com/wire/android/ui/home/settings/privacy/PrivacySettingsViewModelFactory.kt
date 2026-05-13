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
package com.wire.android.ui.home.settings.privacy

import com.wire.android.datastore.UserDataStore
import com.wire.android.ui.analytics.AnalyticsConfiguration
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class PrivacySettingsViewModelFactory(
    private val dispatchers: DispatcherProvider,
    private val persistReadReceiptsStatusConfig: PersistReadReceiptsStatusConfigUseCase,
    private val observeReadReceiptsEnabled: ObserveReadReceiptsEnabledUseCase,
    private val persistScreenshotCensoringConfig: PersistScreenshotCensoringConfigUseCase,
    private val observeScreenshotCensoringConfig: ObserveScreenshotCensoringConfigUseCase,
    private val persistTypingIndicatorStatusConfig: PersistTypingIndicatorStatusConfigUseCase,
    private val observeTypingIndicatorEnabled: ObserveTypingIndicatorEnabledUseCase,
    private val analyticsEnabled: AnalyticsConfiguration,
    private val selfServerConfig: SelfServerConfigUseCase,
    private val dataStore: UserDataStore,
) {
    fun create(): PrivacySettingsViewModel = PrivacySettingsViewModel(
        dispatchers = dispatchers,
        persistReadReceiptsStatusConfig = persistReadReceiptsStatusConfig,
        observeReadReceiptsEnabled = observeReadReceiptsEnabled,
        persistScreenshotCensoringConfig = persistScreenshotCensoringConfig,
        observeScreenshotCensoringConfig = observeScreenshotCensoringConfig,
        persistTypingIndicatorStatusConfig = persistTypingIndicatorStatusConfig,
        observeTypingIndicatorEnabled = observeTypingIndicatorEnabled,
        analyticsEnabled = analyticsEnabled,
        selfServerConfig = selfServerConfig,
        dataStore = dataStore,
    )
}
