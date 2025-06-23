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
import com.wire.android.feature.analytics.handler.AnalyticsMigrationHandler
import com.wire.android.feature.analytics.handler.AnalyticsPropagationHandler
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsProfileProperties
import com.wire.android.feature.analytics.model.AnalyticsResult
import com.wire.android.feature.analytics.model.AnalyticsSettings
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AnonymousAnalyticsManagerImpl : AnonymousAnalyticsManager {
    private const val TAG = "AnonymousAnalyticsManagerImpl"
    private var isAnonymousUsageDataEnabled = false
    private var anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder? = null
    private val startedActivities = mutableSetOf<Activity>()
    private val mutex = Mutex()

    private val coroutineScope by lazy { CoroutineScope(SupervisorJob() + getDispatcher()) }

    // TODO: Sync with product, when we want to enable view tracking, var for testing purposes
    internal var VIEW_TRACKING_ENABLED: Boolean = false

    internal fun getDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    override fun <T> init(
        context: Context,
        analyticsSettings: AnalyticsSettings,
        analyticsResultFlow: Flow<AnalyticsResult<T>>,
        anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder,
        migrationHandler: AnalyticsMigrationHandler<T>,
        propagationHandler: AnalyticsPropagationHandler<T>
    ) {
        this.anonymousAnalyticsRecorder = anonymousAnalyticsRecorder
        globalAnalyticsManager = this

        println("YM. trying to enable countly before....")
        coroutineScope.launch {
            analyticsResultFlow
                .collectLatest { analyticsResult ->
                    mutex.withLock {
                        val result = analyticsResult.identifierResult

                        if (result is AnalyticsIdentifierResult.Enabled) {
                            println("YM. enabling countly.... $result")
                            anonymousAnalyticsRecorder.configure(
                                context = context,
                                analyticsSettings = analyticsSettings,
                            )
                            // start recording for all Activities started before the feature was enabled
                            startedActivities.forEach { activity ->
                                anonymousAnalyticsRecorder.onStart(activity = activity)
                            }
                            println("YM. enabling countly with.... $anonymousAnalyticsRecorder and ${result.identifier}")

                            handleTrackingIdentifier(
                                analyticsIdentifierResult = analyticsResult.identifierResult,
                                analyticsProfileProperties = analyticsResult.profileProperties(),
                                propagateIdentifier = {
                                    analyticsResult.manager?.let { propagationHandler.propagate(it, result.identifier) }
                                },
                                migrationComplete = {
                                    analyticsResult.manager?.let { migrationHandler.migrate(it) }
                                }
                            )
                        } else {
                            println("YM. disabling countly.... $result")
                            // immediately disable event tracking
                            anonymousAnalyticsRecorder.halt()
                        }
                        isAnonymousUsageDataEnabled = result is AnalyticsIdentifierResult.Enabled
                    }
                }
        }
    }

    override fun onStart(activity: Activity) {
        coroutineScope.launch {
            mutex.withLock {
                startedActivities.add(activity)

                if (!isAnonymousUsageDataEnabled) return@withLock

                anonymousAnalyticsRecorder?.onStart(activity = activity)
                    ?: Log.w(TAG, "Calling onStart with a null recorder.")
            }
        }
    }

    override fun onStop(activity: Activity) {
        coroutineScope.launch {
            mutex.withLock {
                startedActivities.remove(activity)

                if (!isAnonymousUsageDataEnabled) return@withLock

                anonymousAnalyticsRecorder?.onStop()
                    ?: Log.w(TAG, "Calling onStop with a null recorder.")
            }
        }
    }

    override fun sendEvent(event: AnalyticsEvent) {
        coroutineScope.launch {
            mutex.withLock {
                if (!isAnonymousUsageDataEnabled) return@withLock

                anonymousAnalyticsRecorder?.sendEvent(event = event)
                    ?: Log.w(TAG, "Calling sendEvent with key : ${event.key} with a null recorder.")
            }
        }
    }

    private suspend fun handleTrackingIdentifier(
        analyticsIdentifierResult: AnalyticsIdentifierResult,
        analyticsProfileProperties: AnalyticsProfileProperties,
        propagateIdentifier: suspend () -> Unit,
        migrationComplete: suspend () -> Unit
    ) {
        when (analyticsIdentifierResult) {
            is AnalyticsIdentifierResult.NonExistingIdentifier -> {
                anonymousAnalyticsRecorder?.setTrackingIdentifierWithoutMerge(
                    identifier = analyticsIdentifierResult.identifier,
                    shouldPropagateIdentifier = true,
                    analyticsProfileProperties = analyticsProfileProperties,
                    propagateIdentifier = propagateIdentifier
                )
            }

            is AnalyticsIdentifierResult.ExistingIdentifier -> {
                anonymousAnalyticsRecorder?.setTrackingIdentifierWithoutMerge(
                    identifier = analyticsIdentifierResult.identifier,
                    shouldPropagateIdentifier = false,
                    analyticsProfileProperties = analyticsProfileProperties,
                    propagateIdentifier = {}
                )
            }

            is AnalyticsIdentifierResult.MigrationIdentifier -> {
                anonymousAnalyticsRecorder?.setTrackingIdentifierWithMerge(
                    identifier = analyticsIdentifierResult.identifier,
                    analyticsProfileProperties = analyticsProfileProperties,
                    migrationComplete = migrationComplete
                )
            }

            is AnalyticsIdentifierResult.Disabled -> {}
            is AnalyticsIdentifierResult.RegistrationIdentifier -> {
                anonymousAnalyticsRecorder?.setTrackingIdentifierWithoutMerge(
                    identifier = analyticsIdentifierResult.identifier,
                    shouldPropagateIdentifier = false,
                    analyticsProfileProperties = analyticsProfileProperties,
                    propagateIdentifier = {}
                )
            }
        }
    }

    override fun isAnalyticsInitialized(): Boolean =
        anonymousAnalyticsRecorder?.isAnalyticsInitialized() ?: run {
            Log.w(TAG, "Calling isAnalyticsInitialized with a null recorder.")
            false
        }

    override fun recordView(screen: String) {
        if (!VIEW_TRACKING_ENABLED) {
            Log.d(TAG, "View tracking is disabled for this build.")
            return
        }
        coroutineScope.launch {
            mutex.withLock {
                if (!isAnonymousUsageDataEnabled) return@withLock
                anonymousAnalyticsRecorder?.recordView(screen.convertToCamelCase())
            }
        }
    }

    override fun stopView(screen: String) {
        if (!VIEW_TRACKING_ENABLED) {
            Log.d(TAG, "View tracking is disabled for this build.")
            return
        }
        coroutineScope.launch {
            mutex.withLock {
                if (!isAnonymousUsageDataEnabled) return@withLock
                anonymousAnalyticsRecorder?.stopView(screen.convertToCamelCase())
            }
        }
    }

    override fun applicationOnCreate() {
        if (!isAnonymousUsageDataEnabled) return

        anonymousAnalyticsRecorder?.applicationOnCreate()
            ?: Log.w(TAG, "Calling applicationOnCreate with a null recorder.")
    }
}
