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
import android.os.Build
import android.util.Log
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsEventConstants
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.APP_NAME
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.APP_VERSION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.DEVICE_MODEL
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.OS_VERSION
import com.wire.android.feature.analytics.model.AnalyticsProfileProperties
import com.wire.android.feature.analytics.model.AnalyticsSettings
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig
import ly.count.android.sdk.UtilsInternalLimits

class AnonymousAnalyticsRecorderImpl(
    private val appVersion: String,
    private val appName: String
) : AnonymousAnalyticsRecorder {

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
        }

        Countly.sharedInstance()?.init(countlyConfig)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val globalSegmentations = mapOf<String, String>(
            APP_NAME to AnalyticsEventConstants.APP_NAME_ANDROID,
            APP_VERSION to packageInfo.versionName.orEmpty()
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
        Countly.sharedInstance()?.events()?.recordEvent(
            event.key,
            event.toSegmentation().toMutableMap()
                .plus(APP_VERSION to appVersion)
                .plus(APP_NAME to appName)
                .plus(DEVICE_MODEL to Build.MODEL)
                .plus(OS_VERSION to Build.VERSION.RELEASE)
        )
    }

    override fun halt() = wrapCountlyRequest {
        isConfigured = false
        Countly.sharedInstance()?.consent()?.removeConsentAll()
    }

    override suspend fun setTrackingIdentifierWithMerge(
        identifier: String,
        analyticsProfileProperties: AnalyticsProfileProperties,
        migrationComplete: suspend () -> Unit
    ) {
        wrapCountlyRequest {
            Countly.sharedInstance()?.deviceId()?.changeWithMerge(identifier)
        }.also {
            migrationComplete()
        }

        setUserProfileProperties(profileProperties = analyticsProfileProperties)
    }

    override suspend fun setTrackingIdentifierWithoutMerge(
        identifier: String,
        shouldPropagateIdentifier: Boolean,
        analyticsProfileProperties: AnalyticsProfileProperties,
        propagateIdentifier: suspend () -> Unit
    ) {
        wrapCountlyRequest {
            Countly.sharedInstance()?.deviceId()?.changeWithoutMerge(identifier)
        }

        setUserProfileProperties(profileProperties = analyticsProfileProperties)

        if (shouldPropagateIdentifier) {
            propagateIdentifier()
        }
    }

    private fun setUserProfileProperties(profileProperties: AnalyticsProfileProperties) = wrapCountlyRequest {
        Countly.sharedInstance()?.userProfile()?.apply {
            setProperty(AnalyticsEventConstants.TEAM_IS_TEAM, profileProperties.isTeamMember)
            profileProperties.teamId?.let {
                setProperty(AnalyticsEventConstants.TEAM_TEAM_ID, it)
            }
            profileProperties.teamMembersAmount?.let {
                setProperty(AnalyticsEventConstants.TEAM_TEAM_SIZE, it)
            }
            profileProperties.isEnterprise?.let {
                setProperty(AnalyticsEventConstants.TEAM_IS_ENTERPRISE, it)
            }
            profileProperties.contactsAmount?.let {
                setProperty(AnalyticsEventConstants.USER_CONTACTS, it)
            }
        }
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
