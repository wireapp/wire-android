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
package com.wire.android.ui

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import dagger.Lazy
import dev.zacsweers.metro.Inject

@Inject
class CallFeedbackViewModelFactory(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val isAnalyticsAvailable: Lazy<IsAnalyticsAvailableUseCase>,
    private val analyticsManager: Lazy<AnonymousAnalyticsManager>,
) {
    fun create(): CallFeedbackViewModel = CallFeedbackViewModel(
        coreLogic = coreLogic,
        currentSessionFlow = currentSessionFlow,
        isAnalyticsAvailable = isAnalyticsAvailable,
        analyticsManager = analyticsManager,
    )
}
