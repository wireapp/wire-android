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
import android.app.Application
import android.content.Context
import android.util.Log
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsEventConstants
import com.wire.android.feature.analytics.model.AnalyticsSettings
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig
import ly.count.android.sdk.UtilsInternalLimits

class AnonymousAnalyticsRecorderImpl : AnonymousAnalyticsRecorder {

    private var isConfigured: Boolean = false

    override fun configure(
        context: Context,
        analyticsSettings: AnalyticsSettings
    ) = wrapCountlyRequest {
        if (isConfigured) return@wrapCountlyRequest

        val countlyConfig = CountlyConfig(
            context,
            analyticsSettings.countlyAppKey,
            analyticsSettings.countlyServerUrl
        ).apply {
            setApplication(context.applicationContext as Application)
            enableTemporaryDeviceIdMode() // Nothing is sent until a proper ID is placed
            setLoggingEnabled(analyticsSettings.enableDebugLogging)
            crashes.apply {
                enableCrashReporting()
            }
        }

        Countly.sharedInstance()?.init(countlyConfig)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val globalSegmentations = mapOf<String, Any>(
            AnalyticsEventConstants.APP_NAME to AnalyticsEventConstants.APP_NAME_ANDROID,
            AnalyticsEventConstants.APP_VERSION to packageInfo.versionName
        )
        Countly.sharedInstance()?.views()?.setGlobalViewSegmentation(globalSegmentations)
        isConfigured = true
    }

    override fun onStart(activity: Activity) = wrapCountlyRequest {
        Countly.sharedInstance()?.onStart(activity)
    }

    override fun onStop() = wrapCountlyRequest {
        Countly.sharedInstance()?.onStop()
    }

    /**
     * We need to change our segmentation map to [MutableMap] because
     * Countly is doing additional operations on it.
     * See [UtilsInternalLimits.removeUnsupportedDataTypes]
     */
    override fun sendEvent(event: AnalyticsEvent) = wrapCountlyRequest {
        Countly.sharedInstance()?.events()?.recordEvent(event.key, event.toSegmentation().toMutableMap())
    }

    override fun halt() = wrapCountlyRequest {
        isConfigured = false
        Countly.sharedInstance()?.consent()?.removeConsentAll()
    }

    override suspend fun setTrackingIdentifierWithMerge(
        identifier: String,
        isTeamMember: Boolean,
        migrationComplete: suspend () -> Unit
    ) {
        wrapCountlyRequest {
            Countly.sharedInstance()?.deviceId()?.changeWithMerge(identifier)
        }.also {
            migrationComplete()
        }

        setUserProfileProperties(isTeamMember = isTeamMember)
    }

    override suspend fun setTrackingIdentifierWithoutMerge(
        identifier: String,
        shouldPropagateIdentifier: Boolean,
        isTeamMember: Boolean,
        propagateIdentifier: suspend () -> Unit
    ) {
        wrapCountlyRequest {
            Countly.sharedInstance()?.deviceId()?.changeWithoutMerge(identifier)
        }

        setUserProfileProperties(isTeamMember = isTeamMember)

        if (shouldPropagateIdentifier) {
            propagateIdentifier()
        }
    }

    private fun setUserProfileProperties(isTeamMember: Boolean) = wrapCountlyRequest {
        Countly.sharedInstance()?.userProfile()?.setProperty(
            AnalyticsEventConstants.TEAM_IS_TEAM,
            isTeamMember
        )
        Countly.sharedInstance()?.userProfile()?.save()
    }

    override fun isAnalyticsInitialized(): Boolean = Countly.sharedInstance().isInitialized

    override fun applicationOnCreate() = wrapCountlyRequest {
        if (isConfigured) return@wrapCountlyRequest

        Countly.applicationOnCreate()
    }

    override fun recordView(screen: String) = wrapCountlyRequest {
        Countly.sharedInstance()?.views()?.startAutoStoppedView(screen)
    }

    override fun stopView(screen: String) = wrapCountlyRequest {
        Countly.sharedInstance()?.views()?.stopViewWithName(screen)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun wrapCountlyRequest(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // Countly SDK throws exceptions on some cases, just log it
            // We don't want to crash the app because of that.
            Log.wtf(TAG, "Countly SDK request failed", e)
        }
    }

    companion object {
        private const val TAG = "AnonymousAnalyticsRecorderImpl"
    }
}
