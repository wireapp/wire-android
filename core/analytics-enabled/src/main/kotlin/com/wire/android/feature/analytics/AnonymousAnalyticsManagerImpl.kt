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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object AnonymousAnalyticsManagerImpl : AnonymousAnalyticsManager {
    private const val TAG = "AnonymousAnalyticsManagerImpl"
    private var isAnonymousUsageDataEnabled = false
    private var anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder? = null
    private val startedActivities = mutableSetOf<Activity>()

    init {
        globalAnalyticsManager = this
    }

    override fun init(
        context: Context,
        analyticsSettings: AnalyticsSettings,
        isEnabledFlow: Flow<Boolean>,
        anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder,
        dispatcher: CoroutineDispatcher
    ) {
        this.anonymousAnalyticsRecorder = anonymousAnalyticsRecorder

        CoroutineScope(dispatcher).launch {
            isEnabledFlow
                .collectLatest { enabled ->
                    synchronized(this@AnonymousAnalyticsManagerImpl) {
                        if (enabled) {
                            anonymousAnalyticsRecorder.configure(
                                context = context,
                                analyticsSettings = analyticsSettings,
                            )
                            // start recording for all Activities started before the feature was enabled
                            startedActivities.forEach { activity ->
                                anonymousAnalyticsRecorder.onStart(activity = activity)
                            }
                        } else {
                            // immediately disable event tracking
                            anonymousAnalyticsRecorder.halt()
                        }
                        isAnonymousUsageDataEnabled = enabled
                    }
                }
        }
    }

    override fun onStart(activity: Activity) = synchronized(this@AnonymousAnalyticsManagerImpl) {
        startedActivities.add(activity)

        if (!isAnonymousUsageDataEnabled) return@synchronized

        anonymousAnalyticsRecorder?.onStart(activity = activity)
            ?: Log.w(TAG, "Calling onStart with a null recorder.")
    }

    override fun onStop(activity: Activity) = synchronized(this@AnonymousAnalyticsManagerImpl) {
        startedActivities.remove(activity)

        if (!isAnonymousUsageDataEnabled) return@synchronized

        anonymousAnalyticsRecorder?.onStop()
            ?: Log.w(TAG, "Calling onStop with a null recorder.")
    }

    override fun sendEvent(event: AnalyticsEvent) = synchronized(this@AnonymousAnalyticsManagerImpl) {
        if (!isAnonymousUsageDataEnabled) return@synchronized

        anonymousAnalyticsRecorder?.sendEvent(event = event)
            ?: Log.w(TAG, "Calling sendEvent with key : ${event.key} with a null recorder.")
    }
}
