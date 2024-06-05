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
import android.util.Log
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object AnonymousAnalyticsManagerImpl : AnonymousAnalyticsManager {
    private const val TAG = "AnonymousAnalyticsManagerImpl"
    private var isAnonymousUsageDataEnabled = false
    private var anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder? = null

    override fun init(
        context: Context,
        analyticsSettings: AnalyticsSettings,
        isEnabledFlowProvider: suspend () -> Flow<Boolean>,
        anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder,
        dispatcher: CoroutineDispatcher
    ) {
        this.anonymousAnalyticsRecorder = anonymousAnalyticsRecorder

        CoroutineScope(dispatcher).launch {
            isEnabledFlowProvider().collect { enabled ->
                isAnonymousUsageDataEnabled = enabled
                if (enabled) {
                    anonymousAnalyticsRecorder.configure(
                        context = context,
                        analyticsSettings = analyticsSettings,
                    )
                }
            }
        }
        globalAnalyticsManager = this
    }

    override fun onStart(activity: Activity) {
        if (!isAnonymousUsageDataEnabled) return

        anonymousAnalyticsRecorder?.onStart(activity = activity)
            ?: Log.w(TAG, "Calling onStart with a null recorder.")
    }

    override fun onStop() {
        if (!isAnonymousUsageDataEnabled) return

        anonymousAnalyticsRecorder?.onStop()
            ?: Log.w(TAG, "Calling onStop with a null recorder.")
    }

    override fun sendEvent(event: AnalyticsEvent) {
        if (!isAnonymousUsageDataEnabled) return

        anonymousAnalyticsRecorder?.sendEvent(event = event)
            ?: Log.w(TAG, "Calling sendEvent with key : ${event.key} with a null recorder.")
    }
}
