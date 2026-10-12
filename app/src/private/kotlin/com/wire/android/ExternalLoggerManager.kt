package com.wire.android

import android.app.Activity
import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.log.Logs
import com.datadog.android.log.LogsConfiguration
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy
import com.datadog.android.rum.tracking.ComponentPredicate
import com.datadog.android.trace.Trace
import com.datadog.android.trace.TraceConfiguration
import com.wire.android.ui.WireActivity
import com.wire.android.util.sha256
import com.wire.android.util.getDeviceIdString

private const val LONG_TASK_THRESH_HOLD_MS = 1000L
private const val UNKNOWN_USER_ID = "unknown"

object ExternalLoggerManager {

    fun initDatadogLogger(context: Context) {

        val clientToken = BuildConfig.DATADOG_CLIENT_TOKEN
        val applicationId = BuildConfig.DATADOG_APP_ID

        if (clientToken == null || applicationId == null) {
            return
        }

        val environmentName = "internal"
        val appVariantName = "com.wire.android.${BuildConfig.FLAVOR}.${BuildConfig.BUILD_TYPE}"

        val configuration = Configuration.Builder(
            clientToken = clientToken,
            env = environmentName,
            variant = appVariantName,
        )
            .setCrashReportsEnabled(true)
            .useSite(DatadogSite.EU1)
            .build()

        Datadog.initialize(context, configuration, TrackingConsent.GRANTED)
        Datadog.setUserInfo(id = context.getDeviceIdString()?.sha256() ?: UNKNOWN_USER_ID)

        Logs.enable(LogsConfiguration.Builder().build())
        Trace.enable(TraceConfiguration.Builder().build())

        Rum.enable(
            RumConfiguration.Builder(applicationId)
                .useViewTrackingStrategy(
                    ActivityViewTrackingStrategy(
                        trackExtras = true,
                        componentPredicate = object : ComponentPredicate<Activity> {
                            override fun accept(component: Activity): Boolean {
                                // reject Activities which are hosts of Compose views, so that they are not counted as views
                                return component !is WireActivity
                            }

                            override fun getViewName(component: Activity): String? = null
                        }
                    )
                )
                .trackUserInteractions()
                .trackBackgroundEvents(true)
                .trackLongTasks(LONG_TASK_THRESH_HOLD_MS)
                .build()
        )
    }
}
