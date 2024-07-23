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
package com.wire.android.feature.analytics

import android.app.Activity
import android.content.Context
import com.wire.android.feature.analytics.handler.AnalyticsMigrationHandler
import com.wire.android.feature.analytics.handler.AnalyticsPropagationHandler
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsResult
import com.wire.android.feature.analytics.model.AnalyticsSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

open class AnonymousAnalyticsManagerStub : AnonymousAnalyticsManager {

    override fun <T> init(
        context: Context,
        analyticsSettings: AnalyticsSettings,
        analyticsResultFlow: Flow<AnalyticsResult<T>>,
        anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder,
        migrationHandler: AnalyticsMigrationHandler<T>,
        propagationHandler: AnalyticsPropagationHandler<T>,
        dispatcher: CoroutineDispatcher
    ) = Unit

    override fun sendEvent(event: AnalyticsEvent) = Unit

    override fun onStart(activity: Activity) = Unit

    override fun onStop(activity: Activity) = Unit
}
