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
import com.wire.android.feature.analytics.model.AnalyticsEventConstants
import com.wire.android.feature.analytics.model.AnalyticsSettings
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig

class AnonymousAnalyticsRecorderImpl : AnonymousAnalyticsRecorder {

    private var isConfigured: Boolean = false

    override fun configure(
        context: Context,
        analyticsSettings: AnalyticsSettings
    ) {
        if (isConfigured) return

        val countlyConfig = CountlyConfig(
            context,
            analyticsSettings.countlyAppKey,
            analyticsSettings.countlyServerUrl
        )
        countlyConfig.setLoggingEnabled(analyticsSettings.enableDebugLogging)

        Countly.sharedInstance().init(countlyConfig)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val globalSegmentations = mapOf<String, Any>(
            AnalyticsEventConstants.APP_NAME to AnalyticsEventConstants.APP_NAME_ANDROID,
            AnalyticsEventConstants.APP_VERSION to packageInfo.versionName
        )
        Countly.sharedInstance().views().setGlobalViewSegmentation(globalSegmentations)
        isConfigured = true
    }

    override fun onStart(activity: Activity) {
        Countly.sharedInstance().onStart(activity)
    }

    override fun onStop() {
        Countly.sharedInstance().onStop()
    }

    override fun sendEvent(event: AnalyticsEvent) {
        Countly.sharedInstance().events().recordEvent(event.key, event.toSegmentation())
    }
}
