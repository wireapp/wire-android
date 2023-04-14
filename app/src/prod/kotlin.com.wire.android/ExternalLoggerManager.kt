package com.wire.android

import android.app.Activity
import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy
import com.datadog.android.rum.tracking.ComponentPredicate
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.WireActivity
import com.wire.android.util.getDeviceId
import com.wire.android.util.sha256
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val LONG_TASK_THRESH_HOLD_MS = 1000L

object ExternalLoggerManager {

    fun initDatadogLogger(context: Context, globalDataStore: GlobalDataStore) = Unit

}
