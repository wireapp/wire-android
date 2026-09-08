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

package com.wire.android.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wire.android.BuildConfig
import com.wire.android.WireApplication
import com.wire.android.appLogger
import com.wire.android.di.metro.WireViewModelDiagnostics
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.routes.utility.DebugRoute
import com.wire.android.navigation.routes.utility.LogManagementRoute
import com.wire.android.navigation.runtime.WireActivityIntentCoordinator
import com.wire.android.navigation.runtime.WireActivityIntentEffect
import com.wire.android.navigation.runtime.WireActivityIntentRequest
import com.wire.android.navigation.runtime.startup.WireInitialRouteResolver
import com.wire.android.navigation.runtime.startup.WireStartupLoginType
import com.wire.android.navigation.runtime.startup.toWireSessionId
import com.wire.android.notification.broadcastreceivers.DynamicReceiversManager
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.sharing.hasTrustedWireShareCaller
import com.wire.android.ui.sharing.sharingUris
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ShakeDetector
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.getProviderAuthority
import com.wire.android.util.launchUpdateTheApp
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject

@Suppress("TooManyFunctions", "LargeClass")
class WireActivity : BaseActivity() {

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var lockCodeTimeManager: Lazy<LockCodeTimeManager>

    @Inject
    lateinit var switchAccountObserver: SwitchAccountObserver

    @Inject
    lateinit var loginTypeSelector: LoginTypeSelector

    @Inject
    lateinit var dynamicReceiversManager: DynamicReceiversManager

    @Inject
    lateinit var managedConfigurationsManager: ManagedConfigurationsManager

    private val viewModel: WireActivityViewModel by lazy(LazyThreadSafetyMode.NONE) {
        wireMetroViewModel(
            owner = this,
            factory = wireApplicationGraph.metroViewModelFactory,
        )
    }

    @VisibleForTesting
    internal fun viewModelForTest(): WireActivityViewModel = viewModel

    private val intentCoordinator = WireActivityIntentCoordinator()
    private lateinit var shakeDetector: ShakeDetector

    // This flag is used to keep the splash screen open until the first screen is drawn.
    private var shouldKeepSplashOpen = true
    private var isAppLockActivityLaunching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val startupAt = SystemClock.elapsedRealtime()

        traceStartup("activity.onCreate.begin")
        // We need to keep the splash screen open until the first screen is drawn.
        // Otherwise a white screen is displayed.
        // It's an API limitation, at some point we may need to remove it
        val splashScreen = installSplashScreen()
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)
        WireViewModelDiagnostics.ownerAvailable(this, ACTIVITY_COORDINATOR_OWNER_KEY)
        splashScreen.setKeepOnScreenCondition { shouldKeepSplashOpen }
        traceStartup("activity.onCreate.afterSuper", startupAt)
        val initialIntentRequest = captureIntentRequest(intent, savedInstanceState)

        enableEdgeToEdge()
        setupOrientationForDevice()
        shakeDetector = ShakeDetector(this)

        lifecycleScope.launch {
            traceStartup("activity.startupCoroutine.begin", startupAt)

            traceStartup("activity.observePersistentConnectionStatus.start", startupAt)
            viewModel.observePersistentConnectionStatus()

            traceStartup("activity.initialAppState.start", startupAt)
            val initialAppState = viewModel.initialAppState()
            val initialRoute = WireInitialRouteResolver.resolve(
                initialAppState = initialAppState,
                loginType = WireStartupLoginType.fromCanUseNewLogin(loginTypeSelector.canUseNewLogin()),
                activeSessionId = viewModel.globalAppState.currentUserId?.toWireSessionId(),
            )
            traceStartup("activity.initialAppState.resolved:${initialRoute.routeId}", startupAt)
            setComposableContent(initialRoute)
            traceStartup("activity.setContent.done", startupAt)

            // When the app is locked, get the app lock screen up before the splash screen is
            // dismissed so that protected content never flashes. Waiting for the current user id
            // is finite here because a locked app implies a logged-in session; when not logged in
            // there is nothing to protect. Locks that happen after startup are handled by the
            // lifecycle observer below.
            if (initialAppState != InitialAppState.NotLoggedIn && lockCodeTimeManager.value.isAppLocked()) {
                observeAppLockUserId(
                    isAppLocked = lockCodeTimeManager.value.observeAppLock(),
                    currentUserId = snapshotFlow { viewModel.globalAppState.currentUserId },
                ).first().let { currentUserId ->
                    startAppLockActivity(currentUserId = currentUserId)
                }
                traceStartup("activity.appLock.launched", startupAt)
            }

            traceStartup("activity.splash.hide", startupAt)
            shouldKeepSplashOpen = false
            traceStartup("activity.splash.dismissed", startupAt)
            (application as? WireApplication)?.initializeDeferredLoggingAfterSplash()
            traceStartup("activity.deferredLogging.triggered", startupAt)

            handleNewIntent(initialIntentRequest)
            traceStartup("activity.initialIntent.dispatched", startupAt)
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                isAppLockActivityLaunching = false
                observeAppLockUserId(
                    isAppLocked = lockCodeTimeManager.value.observeAppLock(),
                    currentUserId = snapshotFlow { viewModel.globalAppState.currentUserId },
                ).first().let { currentUserId ->
                    startAppLockActivity(currentUserId = currentUserId)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dynamicReceiversManager.registerAll()
        if (BuildConfig.EMM_SUPPORT_ENABLED) {
            lifecycleScope.launch(Dispatchers.IO) {
                managedConfigurationsManager.refreshServerConfig()
                managedConfigurationsManager.refreshSSOCodeConfig()
            }
            viewModel.applyPersistentWebSocketConfigFromMDM()
        }
    }

    override fun onStop() {
        super.onStop()
        dynamicReceiversManager.unregisterAll()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action?.equals(Intent.ACTION_SYNC) == true) {
            handleSynchronizeExternalData(intent)
            return
        }
        setIntentPreservingCaller(intent)
        handleNewIntent(captureIntentRequest(intent))
    }

    private fun handleNewIntent(request: WireActivityIntentRequest) {
        intentCoordinator.enqueue(request)
    }

    private fun setComposableContent(startDestination: WireRoute) {
        val hostDependencies = WireActivityHostDependencies(
            activity = this,
            viewModel = viewModel,
            appGraph = wireApplicationGraph,
            loginTypeSelector = loginTypeSelector,
            intentCoordinator = intentCoordinator,
            currentScreenManager = currentScreenManager,
            switchAccountObserver = switchAccountObserver,
            shakeEvents = shakeDetector.observeShakes(),
            onIntentRequest = ::handleDeepLinkOrIntent,
            onShake = ::handleShakeShortcut,
            onStartTeamAppLock = {
                startAppLockActivity(setTeamAppLock = true)
            },
            onUpdateApp = ::updateTheApp,
            onScreenshotCensoringChanged = { enabled ->
                if (enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            },
        )
        setContent {
            WireActivityNavigation3Host(
                startDestination = startDestination,
                dependencies = hostDependencies,
            )
        }
    }

    private fun traceStartup(event: String, startedAt: Long? = null) {
        val elapsed = startedAt?.let { " (+${SystemClock.elapsedRealtime() - it}ms)" }.orEmpty()
        Log.i(TAG, "startup:$event$elapsed")
    }

    private fun updateTheApp() = this.launchUpdateTheApp()

    override fun onResume() {
        super.onResume()
        shakeDetector.start()
    }

    override fun onPause() {
        shakeDetector.stop()
        super.onPause()
    }

    private fun startAppLockActivity(
        setTeamAppLock: Boolean = false,
        currentUserId: UserId? = viewModel.globalAppState.currentUserId,
    ) {
        if (isAppLockActivityLaunching) {
            // Dedupes concurrent launches (startup path vs. resumed observer). A team app lock
            // setup landing in this tiny window is also dropped: AppLockActivity only reads its
            // extras in onCreate, so launching again would either be ignored (singleTop onNewIntent)
            // or stack a duplicate instance. The team feature-flag flow re-prompts later.
            appLogger.w("$TAG appLock: launch already in flight, skipping (setTeamAppLock=$setTeamAppLock)")
            return
        }
        val resolvedUserId = currentUserId ?: run {
            appLogger.e("$TAG appLock: missing current user id, skipping app lock activity")
            return
        }
        isAppLockActivityLaunching = true
        startActivity(
            Intent(this, AppLockActivity::class.java).apply {
                putExtra(AppLockActivity.EXTRA_USER_ID, resolvedUserId.toString())
                if (setTeamAppLock) {
                    putExtra(AppLockActivity.SET_TEAM_APP_LOCK, true)
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        intentCoordinator.saveInstanceState(outState, intent)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        intentCoordinator.restoreActivityIntent(savedInstanceState)?.let {
            this.intent = it
        }
    }

    private fun handleSynchronizeExternalData(intent: Intent) {
        if (!BuildConfig.DEBUG) {
            appLogger.e("Synchronizing external data is only allowed on debug builds")
            return
        }

        intent.data?.lastPathSegment.let { eventsPath ->
            openFileInput(eventsPath)?.let { inputStream ->
                viewModel.handleSynchronizeExternalData(inputStream)
            }
        }
    }

    /*
     * This method is responsible for handling deep links from given intent
     */
    private suspend fun handleDeepLinkOrIntent(
        runtime: WireNavigation3Runtime,
        authenticationRouter: AuthenticationNavigation3Router,
        request: WireActivityIntentRequest,
    ) {
        val effect = intentCoordinator.handle(
            request = request,
            isEmptyWelcomeStartDestination = {
                runtime.navigator.routes.firstOrNull() is NewWelcomeEmptyStartRoute
            },
            handleNonDeepLinkIntent = { viewModel.handleIntentsThatAreNotDeepLinks(it) },
            handleDeepLink = {
                viewModel.handleDeepLink(
                    intent = it,
                    providerAuthority = getProviderAuthority(),
                    hasTrustedWireShareCaller = request.hasTrustedWireShareCaller,
                )
            },
        )
        if (effect == WireActivityIntentEffect.OPEN_LOGIN) {
            authenticationRouter.openLoginFromActivity()
        }
    }

    private fun handleShakeShortcut(runtime: WireNavigation3Runtime) {
        viewModel.globalAppState.currentUserId?.toWireSessionId()?.let { sessionId ->
            val route = if (BuildConfig.PRIVATE_BUILD && BuildConfig.DEBUG_SCREEN_ENABLED) {
                DebugRoute(sessionId)
            } else {
                LogManagementRoute(sessionId)
            }
            runtime.navigator.navigate(
                WireNavigationCommand(route, WireBackStackMode.UPDATE_EXISTING)
            )
        }
    }

    override fun onDestroy() {
        WireViewModelDiagnostics.ownerReleased(this, ACTIVITY_COORDINATOR_OWNER_KEY)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WireActivity"
        private const val ACTIVITY_COORDINATOR_OWNER_KEY = "activity:wire"
    }
}

/**
 * Keeps the verified sender attached to a replacement intent on Android 15+.
 * This must be called while [AppCompatActivity.onNewIntent] is executing because
 * [AppCompatActivity.getCurrentCaller] is only available during that callback.
 */
internal fun AppCompatActivity.setIntentPreservingCaller(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        setIntent(intent, currentCaller)
    } else {
        setIntent(intent)
    }
}

/**
 * Snapshots the share caller validation while this intent is current. The caller is Activity state
 * and may change before the intent queue drains, so the trust result must travel with the intent.
 */
internal fun AppCompatActivity.captureIntentRequest(
    intent: Intent,
    savedInstanceState: Bundle? = null,
): WireActivityIntentRequest = WireActivityIntentRequest(
    intent = intent,
    savedInstanceState = savedInstanceState,
    hasTrustedWireShareCaller = hasTrustedWireShareCaller(
        providerAuthority = getProviderAuthority(),
        uris = intent.sharingUris(),
    ),
)

internal fun observeAppLockUserId(
    isAppLocked: Flow<Boolean>,
    currentUserId: Flow<UserId?>,
): Flow<UserId> = combine(isAppLocked, currentUserId) { isLocked, userId ->
    if (isLocked) userId else null
}.filterNotNull()

val LocalActivity = staticCompositionLocalOf<AppCompatActivity> {
    error("No Activity provided")
}
