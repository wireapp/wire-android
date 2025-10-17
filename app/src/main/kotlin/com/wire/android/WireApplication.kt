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

package com.wire.android

import android.app.Activity
import android.content.ComponentCallbacks2
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import co.touchlab.kermit.platformLogWriter
import com.wire.android.analytics.ObserveCurrentSessionAnalyticsUseCase
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.AnonymousAnalyticsRecorderImpl
import com.wire.android.feature.analytics.globalAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsSettings
import com.wire.android.util.AppNameUtil
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.DataDogLogger
import com.wire.android.util.logging.LogFileWriter
import com.wire.android.util.getGitBuildId
import com.wire.android.util.lifecycle.SyncLifecycleManager
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.android.workmanager.worker.enqueueAssetUpload
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltAndroidApp
class WireApplication : BaseApp() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: Lazy<CoreLogic>

    @Inject
    lateinit var logFileWriter: Lazy<LogFileWriter>

    @Inject
    lateinit var syncLifecycleManager: Lazy<SyncLifecycleManager>

    @Inject
    lateinit var wireWorkerFactory: Lazy<WireWorkerFactory>

    @Inject
    lateinit var globalObserversManager: Lazy<GlobalObserversManager>

    @Inject
    lateinit var globalDataStore: Lazy<GlobalDataStore>

    @Inject
    lateinit var userDataStoreProvider: Lazy<UserDataStoreProvider>

    @Inject
    @ApplicationScope
    lateinit var globalAppScope: CoroutineScope

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var analyticsManager: Lazy<AnonymousAnalyticsManager>

    @Inject
    lateinit var workManager: WorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(wireWorkerFactory.get())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        enableStrictMode()

        setupGlobalExceptionHandler()

        startActivityLifecycleCallback()

        globalAppScope.launch {
            initializeApplicationLoggingFrameworks()

            appLogger.i("$TAG app lifecycle")
            withContext(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(currentScreenManager)
            }
            launch {
                syncLifecycleManager.get().observeAppLifecycle()
            }

            appLogger.i("$TAG global observers")
            globalObserversManager.get().observe()

            launch { observeAssetUploadState() }

            observeRecentlyEndedCall()
        }
    }

    private suspend fun observeRecentlyEndedCall() {
        coreLogic.get().getGlobalScope().session.currentSessionFlow().filterIsInstance(CurrentSessionResult.Success::class)
            .filter { session -> session.accountInfo.isValid() }
            .flatMapLatest { session ->
                coreLogic.get().getSessionScope(session.accountInfo.userId).calls.observeRecentlyEndedCallMetadata()
            }
            .collect { metadata ->
                analyticsManager.get().sendEvent(AnalyticsEvent.RecentlyEndedCallEvent(metadata))
            }
    }

    private suspend fun observeAssetUploadState() {
        coreLogic.get().getGlobalScope().session.currentSessionFlow()
            .filterIsInstance<CurrentSessionResult.Success>()
            .map { it.accountInfo.userId }
            .flatMapLatest {
                coreLogic.get().getSessionScope(it).messages.observeAssetUploadState()
            }
            .collect { uploadInProgress ->
                if (uploadInProgress) {
                    appLogger.d("Uploading files...")
                    workManager.enqueueAssetUpload()
                } else {
                    appLogger.d("All files uploaded.")
                }
            }
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
//                    .penaltyDeath() // Disabled as some devices and libraries are not compliant
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectFileUriExposure()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    // .penaltyDeath() TODO: add it later after fixing reported violations
                    .build()
            )
        }
    }

    private fun setupGlobalExceptionHandler() {
        setupUncaughtExceptionHandler()
        setupHistoricalExitMonitoring()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            flushLogsBeforeCrash()
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun flushLogsBeforeCrash() {
        // Use fire-and-forget approach to avoid blocking the crash handler
        // which could lead to ANRs. We attempt a quick flush but don't wait for it.
        try {
            globalAppScope.launch(Dispatchers.IO) {
                try {
                    // Use a very short timeout to avoid delaying the crash
                    withTimeout(CRASH_FLUSH_TIMEOUT_MS) {
                        logFileWriter.get().forceFlush()
                    }
                    appLogger.i("Logs flushed before crash")
                } catch (e: Exception) {
                    // Log errors but don't block the crash handler
                    appLogger.e("Failed to flush logs before crash", e)
                }
            }
        } catch (e: Exception) {
            // Ignore any launch failures - we don't want to interfere with crash handling
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun setupHistoricalExitMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.setProcessStateSummary(ByteArray(0))

                // This will be called after the app exits, so we can't flush here,
                // but we log it for diagnostics
                globalAppScope.launch {
                    activityManager.getHistoricalProcessExitReasons(packageName, 0, MAX_HISTORICAL_EXIT_REASONS)
                        .forEach { info ->
                            logPreviousExitReason(info)
                        }
                }
            } catch (e: Exception) {
                appLogger.e("Failed to setup app exit monitoring", e)
            }
        }
    }

    private fun logPreviousExitReason(info: android.app.ApplicationExitInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (info.reason) {
                android.app.ApplicationExitInfo.REASON_ANR -> {
                    appLogger.w("Previous app exit was due to ANR at ${info.timestamp}")
                }
                android.app.ApplicationExitInfo.REASON_CRASH -> {
                    appLogger.w("Previous app exit was due to crash at ${info.timestamp}")
                }
                android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> {
                    appLogger.w("Previous app exit was due to low memory at ${info.timestamp}")
                }
                else -> {
                    appLogger.i("Previous app exit reason: ${info.reason} at ${info.timestamp}")
                }
            }
        }
    }

    @Suppress("EmptyFunctionBlock")
    private fun startActivityLifecycleCallback() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                globalAnalyticsManager.onStart(activity)
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                globalAnalyticsManager.onStop(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private suspend fun initializeApplicationLoggingFrameworks() {
        // 1. Datadog should be initialized first
        ExternalLoggerManager.initDatadogLogger(applicationContext)
        // 2. Initialize our internal logging framework
        val isLoggingEnabled = globalDataStore.get().isLoggingEnabled().first()
        val config = if (isLoggingEnabled) {
            KaliumLogger.Config(
                KaliumLogLevel.VERBOSE,
                listOf(DataDogLogger, platformLogWriter())
            )
        } else {
            KaliumLogger.Config.DISABLED
        }
        // 2. Initialize our internal logging framework
        AppLogger.init(config)
        CoreLogger.init(config)
        // 3. Initialize our internal FILE logging framework
        logFileWriter.get().start()
        // 4. Everything ready, now we can log device info
        appLogger.i("Logger enabled")
        logDeviceInformation()
        // 5. Verify if we can initialize Anonymous Analytics
        initializeAnonymousAnalytics()
    }

    private fun initializeAnonymousAnalytics() {
        if (!BuildConfig.ANALYTICS_ENABLED) return

        val anonymousAnalyticsRecorder = AnonymousAnalyticsRecorderImpl(BuildConfig.VERSION_NAME, BuildConfig.APP_NAME)
        val analyticsSettings = AnalyticsSettings(
            countlyAppKey = BuildConfig.ANALYTICS_APP_KEY,
            countlyServerUrl = BuildConfig.ANALYTICS_SERVER_URL,
            enableDebugLogging = BuildConfig.DEBUG
        )

        val analyticsResultFlow = ObserveCurrentSessionAnalyticsUseCase(
            currentSessionFlow = coreLogic.get().getGlobalScope().session.currentSessionFlow(),
            getAnalyticsContactsData = { userId ->
                coreLogic.get().getSessionScope(userId).getAnalyticsContactsData()
            },
            observeAnalyticsTrackingIdentifierStatusFlow = { userId ->
                coreLogic.get().getSessionScope(userId).observeAnalyticsTrackingIdentifierStatus()
            },
            analyticsIdentifierManagerProvider = { userId ->
                coreLogic.get().getSessionScope(userId).analyticsIdentifierManager
            },
            userDataStoreProvider = userDataStoreProvider.get(),
            globalDataStore = globalDataStore.get(),
            currentBackend = { userId ->
                coreLogic.get().getSessionScope(userId).users.serverLinks()
            }
        ).invoke()

        AnonymousAnalyticsManagerImpl.init(
            context = this,
            analyticsSettings = analyticsSettings,
            analyticsResultFlow = analyticsResultFlow,
            anonymousAnalyticsRecorder = anonymousAnalyticsRecorder,
            propagationHandler = { manager, identifier ->
                manager.propagateTrackingIdentifier(identifier)
            },
            migrationHandler = { manager ->
                manager.onMigrationComplete()
            }
        )

        AnonymousAnalyticsManagerImpl.applicationOnCreate()

        // observe the app visibility state and send AppOpen event if the app goes from the background to the foreground
        globalAppScope.launch {
            currentScreenManager
                .isAppVisibleFlow()
                .filter { isVisible -> isVisible }
                .collect {
                    val currentSessionResult = coreLogic.get().getGlobalScope().session.currentSessionFlow().first()
                    val isTeamMember = if (currentSessionResult is CurrentSessionResult.Success) {
                        coreLogic.get().getSessionScope(currentSessionResult.accountInfo.userId).team.isSelfATeamMember()
                    } else {
                        null
                    }

                    AnonymousAnalyticsManagerImpl.sendEvent(
                        AnalyticsEvent.AppOpen(isTeamMember)
                    )
                }
        }
    }

    private fun logDeviceInformation() {
        appLogger.d(
            """
            > Device info: 
                App version=${AppNameUtil.createAppName()} 
                OS version=${Build.VERSION.SDK_INT}
                Phone model=${Build.BRAND}/${Build.MODEL}
                Commit hash=${applicationContext.getGitBuildId()}
        """.trimIndent()
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        appLogger.w(
            "onTrimMemory called - App info: Memory trim level=${MemoryLevel.byLevel(level)}. " +
                    "See more at https://developer.android.com/reference/kotlin/android/content/ComponentCallbacks2"
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        appLogger.w("onLowMemory called - Stopping logging, buckling the seatbelt and hoping for the best!")
        globalAppScope.launch {
            logFileWriter.get().stop()
        }
    }

    private companion object {
        enum class MemoryLevel(val level: Int) {
            TRIM_MEMORY_BACKGROUND(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND),
            TRIM_MEMORY_COMPLETE(ComponentCallbacks2.TRIM_MEMORY_COMPLETE),
            TRIM_MEMORY_MODERATE(ComponentCallbacks2.TRIM_MEMORY_MODERATE),
            TRIM_MEMORY_RUNNING_CRITICAL(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL),
            TRIM_MEMORY_RUNNING_LOW(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW),
            TRIM_MEMORY_RUNNING_MODERATE(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE),
            TRIM_MEMORY_UI_HIDDEN(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN),

            @Suppress("MagicNumber")
            TRIM_MEMORY_UNKNOWN(-1);

            companion object {
                fun byLevel(value: Int) =
                    entries.firstOrNull { it.level == value } ?: TRIM_MEMORY_UNKNOWN
            }
        }

        private const val TAG = "WireApplication"
        private const val CRASH_FLUSH_TIMEOUT_MS = 1000L
        private const val MAX_HISTORICAL_EXIT_REASONS = 5
    }
}
