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
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsSettings

open class AnonymousAnalyticsRecorderStub : AnonymousAnalyticsRecorder {
    override fun configure(context: Context, analyticsSettings: AnalyticsSettings) = Unit

    override fun onStart(activity: Activity) = Unit

    override fun onStop() = Unit

    override fun sendEvent(event: AnalyticsEvent) = Unit

    override fun halt() = Unit

    override suspend fun setTrackingIdentifierWithMerge(
        identifier: String,
        isTeamMember: Boolean,
        migrationComplete: suspend () -> Unit
    ) = Unit

    override suspend fun setTrackingIdentifierWithoutMerge(
        identifier: String,
        shouldPropagateIdentifier: Boolean,
        isTeamMember: Boolean,
        propagateIdentifier: suspend () -> Unit
    ) = Unit

    override fun isAnalyticsInitialized(): Boolean = false
    override fun applicationOnCreate() = Unit
    override fun recordView(screen: String) = Unit
    override fun stopView(screen: String) = Unit
}
