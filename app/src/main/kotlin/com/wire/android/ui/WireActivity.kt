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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountCodeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountDataDetailScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountEmailScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountSelectorScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountSummaryScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountUsernameScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountVerificationCodeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreatePersonalAccountOverviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateTeamAccountOverviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.E2EIEnrollmentScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.E2EiCertificateDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.DebugScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.LogManagementScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginPasswordScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginVerificationCodeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewWelcomeEmptyStartScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.RegisterDeviceScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.RemoveDeviceScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.SelfDevicesScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.SelfUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.WelcomeScreenDestination
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.config.CustomUiConfigurationProvider
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.metro.AppAuthenticationViewModelGraph
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.di.metro.LocalWireViewModelScopeKey
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.WireApplicationGraph
import com.wire.android.di.metro.createSessionViewModelGraph
import com.wire.android.di.metro.sessionKeyedMetroViewModelKey
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.model.LocalWireSessionImageLoader
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.MainNavHost
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.baseRoute
import com.wire.android.navigation.getBaseRoute
import com.wire.android.navigation.rememberNavigator
import com.wire.android.navigation.safeDestination
import com.wire.android.navigation.safeRoute
import com.wire.android.navigation.startDestination
import com.wire.android.navigation.style.BackgroundStyle
import com.wire.android.navigation.style.BackgroundType
import com.wire.android.notification.broadcastreceivers.DynamicReceiversManager
import com.wire.android.ui.authentication.LocalAuthenticationCancelUserId
import com.wire.android.ui.authentication.devices.common.SessionBackedAuthenticationNavArgs
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.CommonTopAppBarState
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.WireTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateUI
import com.wire.android.ui.home.E2EICertificateRevokedDialog
import com.wire.android.ui.home.E2EIRequiredDialog
import com.wire.android.ui.home.E2EIResultDialog
import com.wire.android.ui.home.E2EISnoozeDialog
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.featureFlagNotificationViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedDialog
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedState
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedDialog
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedState
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.settings.devices.e2ei.E2EICertificateDetails
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.LocalSelfUserProfileLogoutAction
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.LocalSyncStateObserver
import com.wire.android.util.ShakeDetector
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.SyncStateObserver
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.launchUpdateTheApp
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import dev.zacsweers.metrox.viewmodel.metroViewModel as metroxViewModel

@OptIn(ExperimentalComposeUiApi::class)
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

    private val viewModel: WireActivityViewModel by viewModels {
        viewModelFactory {
            initializer {
                wireApplicationGraph.wireActivityViewModel
            }
        }
    }

    private val newIntents = Channel<Pair<Intent, Bundle?>>(Channel.UNLIMITED) // keep new intents until subscribed but do not replay them
    private lateinit var shakeDetector: ShakeDetector

    // This flag is used to keep the splash screen open until the first screen is drawn.
    private var shouldKeepSplashOpen = true

    override fun onCreate(savedInstanceState: Bundle?) {

        appLogger.i("$TAG splash install")
        // We need to keep the splash screen open until the first screen is drawn.
        // Otherwise a white screen is displayed.
        // It's an API limitation, at some point we may need to remove it
        val splashScreen = installSplashScreen()
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { shouldKeepSplashOpen }

        enableEdgeToEdge()
        setupOrientationForDevice()
        shakeDetector = ShakeDetector(this)

        lifecycleScope.launch {

            appLogger.i("$TAG persistent connection status")
            viewModel.observePersistentConnectionStatus()

            appLogger.i("$TAG init login type selector")
            appLogger.i("$TAG start destination")
            val startDestination = when (val initialAppState = viewModel.initialAppState()) {
                InitialAppState.NotLoggedIn -> when (loginTypeSelector.canUseNewLogin()) {
                    true -> NewWelcomeEmptyStartScreenDestination()
                    false -> WelcomeScreenDestination()
                }

                is InitialAppState.EnrollE2EI -> E2EIEnrollmentScreenDestination(
                    SessionBackedAuthenticationNavArgs.from(initialAppState.userId)
                )

                InitialAppState.LoggedIn -> HomeScreenDestination()
            }
            appLogger.i("$TAG composable content")
            setComposableContent(startDestination)

            appLogger.i("$TAG splash hide")
            shouldKeepSplashOpen = false

            handleNewIntent(intent, savedInstanceState)
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
        setIntent(intent)
        handleNewIntent(intent)
    }

    private fun handleNewIntent(intent: Intent, savedInstanceState: Bundle? = null) = lifecycleScope.launch {
        newIntents.send(intent to savedInstanceState)
    }

    private fun setComposableContent(startDestination: Direction) {
        setContent {
            WireActivityRoot(startDestination)
        }
    }

    @Composable
    private fun WireActivityRoot(
        startDestination: Direction,
        appGraph: WireApplicationGraph = LocalContext.current.wireApplicationGraph,
        sessionGraphStore: SessionGraphStoreViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    SessionGraphStoreViewModel(appGraph)
                }
            }
        ),
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val authenticationViewModelGraph = remember(appGraph) {
            appGraph.authenticationViewModelGraph
        }

        CompositionLocalProvider(
            LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
            LocalWireViewModelScopeKey provides null,
            LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
            LocalSyncStateObserver provides SyncStateObserver(viewModel.observeSyncFlowState),
            LocalCustomUiConfigurationProvider provides CustomUiConfigurationProvider,
            LocalSnackbarHostState provides snackbarHostState,
            LocalActivity provides this
        ) {
            HandleThemeChanges(viewModel.globalAppState.themeOption)
            WireTheme(accent = viewModel.globalAppState.userAccent) {
                WireActivityThemedContent(
                    startDestination = startDestination,
                    appGraph = appGraph,
                    authenticationViewModelGraph = authenticationViewModelGraph,
                    sessionGraphStore = sessionGraphStore,
                    context = context,
                )
            }
        }
    }

    @Composable
    private fun WireActivityThemedContent(
        startDestination: Direction,
        appGraph: WireApplicationGraph,
        authenticationViewModelGraph: AppAuthenticationViewModelGraph,
        sessionGraphStore: SessionGraphStoreViewModel,
        context: Context,
    ) {
        val navigator = rememberNavigator(
            finish = this@WireActivity::finish,
            isAllowedToNavigate = ::isNavigationAllowed
        )
        val currentBackStackEntryState = navigator.navController.currentBackStackEntryAsState()
        val currentBaseRoute = currentBackStackEntryState.value
            ?.destination
            ?.route
            ?.getBaseRoute()
        val currentUserId = viewModel.globalAppState.currentUserId
        val sessionBackedAuthenticationUserId = currentBackStackEntryState.value
            ?.arguments
            ?.sessionBackedAuthenticationUserId()
        val isUserUiBlocked = viewModel.globalAppState.blockUserUI != null
        val isAuthenticationRoute = currentBaseRoute in authenticationGraphRoutes
        val isSessionTransitionInProgress = viewModel.globalAppState.isSessionTransitionInProgress
        val graphContext = rememberWireActivityGraphContext(
            appGraph = appGraph,
            authenticationViewModelGraph = authenticationViewModelGraph,
            sessionGraphStore = sessionGraphStore,
            currentUserId = currentUserId,
            sessionBackedAuthenticationUserId = sessionBackedAuthenticationUserId,
            currentBaseRoute = currentBaseRoute,
            startDestinationBaseRoute = startDestination.baseRoute,
            isUserUiBlocked = isUserUiBlocked,
            isSessionTransitionInProgress = isSessionTransitionInProgress,
        )
        val lastSessionGraphContext = remember { mutableStateOf<WireActivityGraphContext?>(null) }
        if (graphContext?.sessionGraph != null) {
            lastSessionGraphContext.value = graphContext
        }
        val contentGraphContext = graphContext ?: when {
            currentBaseRoute != null && !isAuthenticationRoute -> lastSessionGraphContext.value
            else -> null
        }
        val backgroundType by remember {
            derivedStateOf {
                currentBackStackEntryState.value?.safeDestination()?.style.let {
                    (it as? BackgroundStyle)?.backgroundType() ?: BackgroundType.Default
                }
            }
        }

        HandleSessionGraphEffects(
            currentUserId = currentUserId,
            sessionBackedAuthenticationUserId = sessionBackedAuthenticationUserId,
            currentBaseRoute = currentBaseRoute,
            isAuthenticationRoute = isAuthenticationRoute,
            isUserUiBlocked = isUserUiBlocked,
            isSessionTransitionInProgress = isSessionTransitionInProgress,
            graphContext = graphContext,
            navigator = navigator,
        )
        WireActivityMainContent(
            startDestination = startDestination,
            navigator = navigator,
            graphContext = contentGraphContext,
            backgroundType = backgroundType,
            context = context,
        )
    }

    private fun isNavigationAllowed(navigationCommand: NavigationCommand): Boolean {
        if (navigationCommand.destination.baseRoute != NewLoginScreenDestination.baseRoute) return true

        // Enterprise login first needs to verify whether another session can be created.
        return viewModel.checkNumberOfSessions()
    }

    @Composable
    private fun HandleSessionGraphEffects(
        currentUserId: UserId?,
        sessionBackedAuthenticationUserId: UserId?,
        currentBaseRoute: String?,
        isAuthenticationRoute: Boolean,
        isUserUiBlocked: Boolean,
        isSessionTransitionInProgress: Boolean,
        graphContext: WireActivityGraphContext?,
        navigator: Navigator,
    ) {
        graphContext?.activityViewModels?.let {
            LaunchedEffect(it.legalHoldRequestedViewModel) {
                it.legalHoldRequestedViewModel.observeLegalHoldRequest()
            }
        }
        LaunchedEffect(currentBaseRoute, currentUserId, sessionBackedAuthenticationUserId, graphContext?.sessionGraph) {
            appLogger.i(
                "$TAG graph route=$currentBaseRoute userId=$currentUserId " +
                        "sessionGraph=${graphContext?.sessionGraph != null} " +
                        "selected=${graphContext?.graph?.viewModelScopeKey}"
            )
        }
        LaunchedEffect(isSessionTransitionInProgress, isAuthenticationRoute) {
            if (isSessionTransitionInProgress && isAuthenticationRoute) {
                viewModel.finishSessionTransition()
            }
        }
        LaunchedEffect(currentUserId, currentBaseRoute, isSessionTransitionInProgress, isUserUiBlocked) {
            handleSessionNavigationState(
                SessionNavigationState(
                    currentUserId = currentUserId,
                    currentBaseRoute = currentBaseRoute,
                    isAuthenticationRoute = isAuthenticationRoute,
                    isUserUiBlocked = isUserUiBlocked,
                    isSessionTransitionInProgress = isSessionTransitionInProgress,
                ),
                navigator = navigator,
            )
        }
    }

    @Composable
    private fun WireActivityMainContent(
        startDestination: Direction,
        navigator: Navigator,
        graphContext: WireActivityGraphContext?,
        backgroundType: BackgroundType,
        context: Context,
    ) {
        if (backgroundType == BackgroundType.Auth) {
            WireAuthBackgroundLayout()
        }
        if (graphContext == null) {
            HandleDialogs(navigator, null)
            return
        }
        graphContext.ProvideViewModelGraph(
            logoutAction = { wipeData ->
                viewModel.doHardLogout(
                    clearUserData = { userId -> UserDataStore(context, userId) },
                    switchAccountActions = NavigationSwitchAccountActions(
                        navigate = navigator::navigate,
                        canUseNewLogin = loginTypeSelector::canUseNewLogin
                    ),
                    wipeData = wipeData
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
            ) {
                WireTopAppBar(
                    commonTopAppBarState = graphContext.activityViewModels
                        ?.commonTopAppBarViewModel
                        ?.state ?: CommonTopAppBarState(),
                    backgroundType = backgroundType,
                )
                MainNavHost(
                    navigator = navigator,
                    loginTypeSelector = loginTypeSelector,
                    startDestination = startDestination,
                    modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars)
                )

                // Navigation graph creation is async enough that commands issued too early
                // can crash before the graph is fully built.
                SetUpNavigation(navigator)
                HandleScreenshotCensoring()
                HandleDialogs(navigator, graphContext.activityViewModels)
                HandleViewActions(viewModel.actions, navigator, loginTypeSelector)
            }
        }
    }

    @Composable
    private fun rememberWireActivityGraphContext(
        appGraph: WireApplicationGraph,
        authenticationViewModelGraph: AppAuthenticationViewModelGraph,
        sessionGraphStore: SessionGraphStoreViewModel,
        currentUserId: UserId?,
        sessionBackedAuthenticationUserId: UserId?,
        currentBaseRoute: String?,
        startDestinationBaseRoute: String,
        isUserUiBlocked: Boolean,
        isSessionTransitionInProgress: Boolean,
    ): WireActivityGraphContext? {
        if (isUserUiBlocked) return null

        val effectiveBaseRoute = currentBaseRoute ?: startDestinationBaseRoute
        val usesNoSessionAuthenticationGraph = effectiveBaseRoute in noSessionAuthenticationGraphRoutes
        val usesAuthenticationGraph = effectiveBaseRoute in authenticationGraphRoutes
        val sessionGraph = remember(
            appGraph,
            currentUserId,
            sessionBackedAuthenticationUserId,
            usesNoSessionAuthenticationGraph,
            isSessionTransitionInProgress,
        ) {
            sessionGraphStore.resolveSessionGraph(
                currentUserId = currentUserId,
                sessionBackedAuthenticationUserId = sessionBackedAuthenticationUserId,
                usesNoSessionAuthenticationGraph = usesNoSessionAuthenticationGraph,
                isSessionTransitionInProgress = isSessionTransitionInProgress,
            )
        }
        val graph = resolveActiveGraph(
            ActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = sessionGraph,
                effectiveBaseRoute = effectiveBaseRoute,
                currentBaseRoute = currentBaseRoute,
                usesAuthenticationGraph = usesAuthenticationGraph,
                usesNoSessionAuthenticationGraph = usesNoSessionAuthenticationGraph,
                isSessionTransitionInProgress = isSessionTransitionInProgress,
            )
        )
        val activityViewModels = sessionGraph?.let {
            wireActivityScopedViewModels(it)
        }
        return graph?.let {
            WireActivityGraphContext(
                graph = it,
                viewModelFactory = (it as? ViewModelGraph)?.metroViewModelFactory ?: appGraph.metroViewModelFactory,
                sessionGraph = sessionGraph,
                activityViewModels = activityViewModels,
            )
        }
    }

    private fun SessionGraphStoreViewModel.resolveSessionGraph(
        currentUserId: UserId?,
        sessionBackedAuthenticationUserId: UserId?,
        usesNoSessionAuthenticationGraph: Boolean,
        isSessionTransitionInProgress: Boolean,
    ): AppSessionViewModelGraph? = when {
        usesNoSessionAuthenticationGraph -> null
        isSessionTransitionInProgress -> null
        sessionBackedAuthenticationUserId != null -> graphFor(sessionBackedAuthenticationUserId)
        currentUserId != null -> graphFor(currentUserId)
        else -> null
    }

    private fun resolveActiveGraph(request: ActiveGraphRequest): MetroViewModelGraph? = when {
        request.isSessionTransitionInProgress && !request.usesAuthenticationGraph -> null
        request.usesNoSessionAuthenticationGraph -> request.authenticationViewModelGraph
        request.sessionGraph != null -> request.sessionGraph
        request.effectiveBaseRoute in authenticationGraphRoutes -> request.authenticationViewModelGraph
        request.currentBaseRoute == null -> request.authenticationViewModelGraph
        else -> null
    }

    private fun handleSessionNavigationState(
        state: SessionNavigationState,
        navigator: Navigator,
    ) {
        when {
            state.isUserUiBlocked -> {
                appLogger.i("$TAG blocking session dialog visible on route=${state.currentBaseRoute}, waiting for user action")
            }
            state.isSessionTransitionInProgress -> {
                handleSessionTransition(state.currentBaseRoute, state.isAuthenticationRoute, navigator)
            }
            state.currentUserId != null && state.currentBaseRoute == NewWelcomeEmptyStartScreenDestination.baseRoute -> {
                appLogger.i("$TAG valid session on empty auth start, navigating to home")
                navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
            }
            state.currentUserId == null && state.currentBaseRoute == NewWelcomeEmptyStartScreenDestination.baseRoute -> {
                appLogger.i("$TAG no session left on empty auth start, navigating to login")
                navigator.navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
            }
            state.currentUserId == null && state.currentBaseRoute != null && !state.isAuthenticationRoute -> {
                appLogger.i("$TAG no session left on route=${state.currentBaseRoute}, trying to switch account")
                resolveMissingCurrentSession(navigator)
            }
        }
    }

    private fun handleSessionTransition(
        currentBaseRoute: String?,
        isAuthenticationRoute: Boolean,
        navigator: Navigator,
    ) {
        if (
            currentBaseRoute != null &&
            !isAuthenticationRoute &&
            viewModel.globalAppState.sessionTransitionReason != SessionTransitionReason.SELF_LOGOUT
        ) {
            appLogger.i("$TAG session transition on route=$currentBaseRoute, resolving current session")
            resolveMissingCurrentSession(navigator)
        }
    }

    private fun resolveMissingCurrentSession(navigator: Navigator) {
        viewModel.resolveMissingCurrentSession(
            NavigationSwitchAccountActions(
                navigate = navigator::navigate,
                canUseNewLogin = loginTypeSelector::canUseNewLogin,
            )
        )
    }

    @Composable
    private fun WireActivityGraphContext.ProvideViewModelGraph(
        logoutAction: (wipeData: Boolean) -> Unit,
        content: @Composable () -> Unit,
    ) {
        val imageLoader = sessionGraph?.wireSessionImageLoader
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides viewModelFactory,
            LocalWireViewModelScopeKey provides graph.viewModelScopeKey,
            LocalAuthenticationCancelUserId provides sessionGraph?.currentAccount,
            LocalWireSessionImageLoader provides imageLoader,
            LocalSelfUserProfileLogoutAction provides logoutAction,
        ) {
            content()
        }
    }

    @Composable
    private fun wireActivityScopedViewModels(graph: AppSessionViewModelGraph): WireActivityScopedViewModels {
        val scopeKey = graph.viewModelScopeKey
        return WireActivityScopedViewModels(
            callFeedbackViewModel = metroxViewModel(
                key = sessionKeyedMetroViewModelKey(
                    defaultKey = CallFeedbackViewModel::class.qualifiedName,
                    key = null,
                    scopeKey = scopeKey,
                ),
            ),
            featureFlagNotificationViewModel = featureFlagNotificationViewModel(),
            commonTopAppBarViewModel = viewModel(
                key = "CommonTopAppBarViewModel:$scopeKey",
                factory = viewModelFactory {
                    initializer {
                        graph.commonViewModelFactory.commonTopAppBarViewModel(
                            CommonTopAppBarParams(showNoNetwork = true, showSync = true, showActiveCalls = true)
                        )
                    }
                }
            ),
            legalHoldRequestedViewModel = viewModel(
                key = "LegalHoldRequestedViewModel:$scopeKey",
                factory = viewModelFactory {
                    initializer {
                        graph.miscViewModelFactory.legalHoldRequestedViewModel()
                    }
                }
            ),
            legalHoldDeactivatedViewModel = viewModel(
                key = "LegalHoldDeactivatedViewModel:$scopeKey",
                factory = viewModelFactory {
                    initializer {
                        graph.miscViewModelFactory.legalHoldDeactivatedViewModel()
                    }
                }
            ),
        )
    }

    @Composable
    private fun HandleThemeChanges(themeOption: ThemeOption) {
        LaunchedEffect(themeOption) {
            val themeNightMode = when (themeOption) {
                ThemeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeOption.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
            val currentNightMode = AppCompatDelegate.getDefaultNightMode()
            if (themeNightMode != currentNightMode) {
                AppCompatDelegate.setDefaultNightMode(themeNightMode)
            }
        }
    }

    @Composable
    private fun SetUpNavigation(navigator: Navigator) {
        val currentKeyboardController by rememberUpdatedState(LocalSoftwareKeyboardController.current)
        val currentNavigator by rememberUpdatedState(navigator)
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                newIntents
                    .receiveAsFlow()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .collectLatest { (intent, savedInstanceState) ->
                        currentKeyboardController?.hide()
                        handleDeepLinkOrIntent(currentNavigator, intent, savedInstanceState)
                    }
            }
        }

        DisposableEffect(navigator.navController) {
            val updateScreenSettingsListener =
                NavController.OnDestinationChangedListener { _, _, _ ->
                    currentKeyboardController?.hide()
                }
            navigator.navController.addOnDestinationChangedListener(updateScreenSettingsListener)
            navigator.navController.addOnDestinationChangedListener(currentScreenManager)

            onDispose {
                navigator.navController.removeOnDestinationChangedListener(
                    updateScreenSettingsListener
                )
                navigator.navController.removeOnDestinationChangedListener(currentScreenManager)
            }
        }

        DisposableEffect(switchAccountObserver, navigator) {
            NavigationSwitchAccountActions(
                {
                    lifecycleScope.launch(Dispatchers.Main) {
                        navigator.navigate(it)
                    }
                },
                loginTypeSelector::canUseNewLogin
            ).let {
                switchAccountObserver.register(it)
                onDispose {
                    switchAccountObserver.unregister(it)
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                shakeDetector.observeShakes()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                    .collectLatest {
                        handleShakeShortcut(currentNavigator)
                    }
            }
        }
    }

    @Composable
    private fun HandleScreenshotCensoring() {
        LaunchedEffect(viewModel.globalAppState.screenshotCensoringEnabled) {
            if (viewModel.globalAppState.screenshotCensoringEnabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    @Suppress("ComplexMethod")
    @Composable
    private fun HandleDialogs(
        navigator: Navigator,
        activityViewModels: WireActivityScopedViewModels?,
    ) {
        val navigate: (NavigationCommand) -> Unit = { navigator.navigate(it) }
        val context = LocalContext.current
        if (activityViewModels == null) {
            UpdateAppDialog(viewModel.globalAppState.updateAppDialog, ::updateTheApp)
            CustomBackendDialog(
                state = viewModel.globalAppState.customBackendDialog,
                onDismiss = {
                    viewModel.dismissCustomBackendDialog()
                    if (navigator.isEmptyWelcomeStartDestination()) {
                        navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
                    }
                },
                onConfirm = viewModel::customBackendDialogProceedButtonClicked,
                onTryAgain = viewModel::onCustomServerConfig
            )
            MaxAccountDialog(
                shouldShow = viewModel.globalAppState.maxAccountDialog,
                onConfirm = {
                    viewModel.dismissMaxAccountDialog()
                    navigate(NavigationCommand(SelfUserProfileScreenDestination))
                },
                onDismiss = viewModel::dismissMaxAccountDialog
            )
            AccountLoggedOutDialog(
                viewModel.globalAppState.blockUserUI
            ) {
                viewModel.tryToSwitchAccount(NavigationSwitchAccountActions(navigate, loginTypeSelector::canUseNewLogin))
            }
            CrossBackendLoginBlockedDialog(
                shouldShow = viewModel.globalAppState.crossBackendLoginBlockedDialog,
                onDismiss = viewModel::dismissCrossBackendLoginBlockedDialog
            )
            return
        }
        val callFeedbackViewModel = activityViewModels.callFeedbackViewModel
        val featureFlagNotificationViewModel = activityViewModels.featureFlagNotificationViewModel
        val legalHoldRequestedViewModel = activityViewModels.legalHoldRequestedViewModel
        val legalHoldDeactivatedViewModel = activityViewModels.legalHoldDeactivatedViewModel
        val callFeedbackSheetState =
            rememberWireModalSheetState<Unit>(onDismissAction = {
                callFeedbackViewModel.skipCallFeedback(false)
            })
        with(featureFlagNotificationViewModel.featureFlagState) {
            if (shouldShowTeamAppLockDialog) {
                TeamAppLockFeatureFlagDialog(
                    isTeamAppLockEnabled = isTeamAppLockEnabled,
                    onConfirm = {
                        featureFlagNotificationViewModel.dismissTeamAppLockDialog()
                        if (isTeamAppLockEnabled) {
                            val isUserAppLockSet = featureFlagNotificationViewModel.featureFlagState.isUserAppLockSet
                            // No need to setup another app lock if the user already has one
                            if (!isUserAppLockSet) {
                                startAppLockActivity(setTeamAppLock = true)
                            } else {
                                featureFlagNotificationViewModel.markTeamAppLockStatusAsNot()
                            }
                        } else {
                            with(featureFlagNotificationViewModel) {
                                markTeamAppLockStatusAsNot()
                                confirmAppLockNotEnforced()
                            }
                        }
                    }
                )
            } else {
                if (legalHoldRequestedViewModel.state is LegalHoldRequestedState.Visible) {
                    LegalHoldRequestedDialog(
                        state = legalHoldRequestedViewModel.state as LegalHoldRequestedState.Visible,
                        passwordTextState = legalHoldRequestedViewModel.passwordTextState,
                        notNowClicked = legalHoldRequestedViewModel::notNowClicked,
                        acceptClicked = legalHoldRequestedViewModel::acceptClicked,
                    )
                }
                if (legalHoldDeactivatedViewModel.state is LegalHoldDeactivatedState.Visible) {
                    LegalHoldDeactivatedDialog(
                        dialogDismissed = legalHoldDeactivatedViewModel::dismiss,
                    )
                }
                if (showFileSharingDialog) {
                    FileRestrictionDialog(
                        isFileSharingEnabled = (isFileSharingState !is FeatureFlagState.FileSharingState.DisabledByTeam),
                        hideDialogStatus = featureFlagNotificationViewModel::dismissFileSharingDialog
                    )
                }

                if (shouldShowGuestRoomLinkDialog) {
                    GuestRoomLinkFeatureFlagDialog(
                        isGuestRoomLinkEnabled = isGuestRoomLinkEnabled,
                        onDismiss = featureFlagNotificationViewModel::dismissGuestRoomLinkDialog
                    )
                }

                if (shouldShowSelfDeletingMessagesDialog) {
                    SelfDeletingMessagesDialog(
                        areSelfDeletingMessagesEnabled = areSelfDeletedMessagesEnabled,
                        enforcedTimeout = enforcedTimeoutDuration,
                        hideDialogStatus = featureFlagNotificationViewModel::dismissSelfDeletingMessagesDialog
                    )
                }
                val logoutOptionsDialogState = rememberVisibilityState<LogoutOptionsDialogState>()

                LogoutOptionsDialog(
                    dialogState = logoutOptionsDialogState,
                    checkboxEnabled = false,
                    logout = {
                        viewModel.doHardLogout(
                            { UserDataStore(context, it) },
                            NavigationSwitchAccountActions(navigate, loginTypeSelector::canUseNewLogin)
                        )
                        logoutOptionsDialogState.dismiss()
                    }
                )

                if (shouldShowE2eiCertificateRevokedDialog) {
                    E2EICertificateRevokedDialog(
                        onLogout = {
                            logoutOptionsDialogState.show(
                                LogoutOptionsDialogState(
                                    shouldWipeData = true
                                )
                            )
                        },
                        onContinue = featureFlagNotificationViewModel::dismissE2EICertificateRevokedDialog,
                    )
                }

                e2EIRequired?.let {
                    E2EIRequiredDialog(
                        e2EIRequired = e2EIRequired,
                        isE2EILoading = isE2EILoading,
                        getCertificate = featureFlagNotificationViewModel::enrollE2EICertificate,
                        snoozeDialog = featureFlagNotificationViewModel::snoozeE2EIdRequiredDialog
                    )
                }

                e2EISnoozeInfo?.let {
                    E2EISnoozeDialog(
                        timeLeft = e2EISnoozeInfo.timeLeft,
                        dismissDialog = featureFlagNotificationViewModel::dismissSnoozeE2EIdRequiredDialog
                    )
                }

                e2EIResult?.let {
                    E2EIResultDialog(
                        result = e2EIResult,
                        updateCertificate = featureFlagNotificationViewModel::enrollE2EICertificate,
                        snoozeDialog = featureFlagNotificationViewModel::snoozeE2EIdRequiredDialog,
                        openCertificateDetails = {
                            navigate(
                                NavigationCommand(
                                    E2EiCertificateDetailsScreenDestination(
                                        E2EICertificateDetails.DuringLoginCertificateDetails(it)
                                    )
                                )
                            )
                        },
                        dismissSuccessDialog = featureFlagNotificationViewModel::dismissSuccessE2EIdDialog,
                        isE2EILoading = isE2EILoading
                    )
                }

                UpdateAppDialog(viewModel.globalAppState.updateAppDialog, ::updateTheApp)
                JoinConversationDialog(
                    viewModel.globalAppState.conversationJoinedDialog,
                    navigate,
                    viewModel::onJoinConversationFlowCompleted
                )
                CustomBackendDialog(
                    state = viewModel.globalAppState.customBackendDialog,
                    onDismiss = {
                        viewModel.dismissCustomBackendDialog()
                        if (navigator.isEmptyWelcomeStartDestination()) {
                            // if "welcome empty start" screen then switch "start" screen to proper one
                            navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
                        }
                    },
                    onConfirm = viewModel::customBackendDialogProceedButtonClicked,
                    onTryAgain = viewModel::onCustomServerConfig
                )
                MaxAccountDialog(
                    shouldShow = viewModel.globalAppState.maxAccountDialog,
                    onConfirm = {
                        viewModel.dismissMaxAccountDialog()
                        navigate(NavigationCommand(SelfUserProfileScreenDestination))
                    },
                    onDismiss = viewModel::dismissMaxAccountDialog
                )
                CrossBackendLoginBlockedDialog(
                    shouldShow = viewModel.globalAppState.crossBackendLoginBlockedDialog,
                    onDismiss = viewModel::dismissCrossBackendLoginBlockedDialog
                )
                AccountLoggedOutDialog(
                    viewModel.globalAppState.blockUserUI
                ) { viewModel.tryToSwitchAccount(NavigationSwitchAccountActions(navigate, loginTypeSelector::canUseNewLogin)) }
                NewClientDialog(
                    viewModel.globalAppState.newClientDialog,
                    { navigate(NavigationCommand(SelfDevicesScreenDestination)) },
                    {
                        viewModel.switchAccount(
                            userId = it,
                            actions = NavigationSwitchAccountActions(navigate, loginTypeSelector::canUseNewLogin),
                            onComplete = { navigate(NavigationCommand(SelfDevicesScreenDestination)) }
                        )
                    },
                    viewModel::dismissNewClientsDialog
                )
            }
            if (showCallEndedBecauseOfConversationDegraded) {
                GuestCallWasEndedBecauseOfVerificationDegradedDialog(
                    featureFlagNotificationViewModel::dismissCallEndedBecauseOfConversationDegraded
                )
            }

            CallFeedbackDialog(
                sheetState = callFeedbackSheetState,
                onRated = callFeedbackViewModel::rateCall,
                onSkipClicked = callFeedbackViewModel::skipCallFeedback
            )

            if (startGettingE2EICertificate) {
                GetE2EICertificateUI(
                    enrollmentResultHandler = {
                        featureFlagNotificationViewModel.handleE2EIEnrollmentResult(it)
                    },
                    isNewClient = false
                )
            }
        }

        LaunchedEffect(Unit) {
            callFeedbackViewModel.showCallFeedbackFlow.collectLatest {
                callFeedbackSheetState.show()
            }
        }
    }

    private fun updateTheApp() = this.launchUpdateTheApp()

    override fun onResume() {
        super.onResume()
        shakeDetector.start()

        lifecycleScope.launch {
            lockCodeTimeManager.value.observeAppLock()
                // Listen to one flow in a lifecycle-aware manner using flowWithLifecycle
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .first().let {
                    if (it) {
                        withContext(Dispatchers.Main) {
                            startAppLockActivity()
                        }
                    }
                }
        }
    }

    override fun onPause() {
        shakeDetector.stop()
        super.onPause()
    }

    private fun startAppLockActivity(setTeamAppLock: Boolean = false) {
        val currentUserId = viewModel.globalAppState.currentUserId ?: run {
            appLogger.e("$TAG appLock: missing current user id, skipping app lock activity")
            return
        }
        startActivity(
            Intent(this, AppLockActivity::class.java).apply {
                putExtra(AppLockActivity.EXTRA_USER_ID, currentUserId.toString())
                if (setTeamAppLock) {
                    putExtra(AppLockActivity.SET_TEAM_APP_LOCK, true)
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HANDLED_DEEPLINK_FLAG, true)
        outState.putParcelable(ORIGINAL_SAVED_INTENT_FLAG, intent)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getOriginalIntent()?.let {
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

    @Suppress("ComplexCondition", "LongMethod", "CyclomaticComplexMethod")
    /*
     * This method is responsible for handling deep links from given intent
     */
    private suspend fun handleDeepLinkOrIntent(
        navigator: Navigator,
        intent: Intent?,
        savedInstanceState: Bundle? = null
    ) {
        val navigate: (NavigationCommand) -> Unit = {
            runOnUiThread {
                navigator.navigate(it)
            }
        }
        val originalIntent = savedInstanceState.getOriginalIntent()
        if (intent == null
            || intent.action == Intent.ACTION_MAIN // The app is opened from launcher so no deep link to handle, only start intents if any
            || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
            || originalIntent == intent // This is the case when the activity is recreated and already handled
            || intent.getBooleanExtra(HANDLED_DEEPLINK_FLAG, false)
        ) {
            val handled = viewModel.handleIntentsThatAreNotDeepLinks(intent)
            if (!handled && navigator.isEmptyWelcomeStartDestination()) {
                // nothing to handle so if "welcome empty start" screen then switch "start" screen to login by navigating to it
                navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
            }
            return
        } else {
            val handled = viewModel.handleIntentsThatAreNotDeepLinks(intent)
            if (!handled) {
                viewModel.handleDeepLink(intent)
                intent.putExtra(HANDLED_DEEPLINK_FLAG, true)
            }
        }
    }

    private fun handleShakeShortcut(navigator: Navigator) {
        runOnUiThread {
            val currentRoute = navigator.navController.currentDestination?.route?.getBaseRoute()
            val shouldOpenDebugTools = BuildConfig.PRIVATE_BUILD && BuildConfig.DEBUG_SCREEN_ENABLED
            val targetRoute = if (shouldOpenDebugTools) {
                DebugScreenDestination.baseRoute
            } else {
                LogManagementScreenDestination.baseRoute
            }
            if (currentRoute == targetRoute) return@runOnUiThread
            val target = if (shouldOpenDebugTools) {
                DebugScreenDestination
            } else {
                LogManagementScreenDestination
            }
            navigator.navigate(NavigationCommand(target, BackStackMode.UPDATE_EXISTED))
        }
    }

    private fun Bundle?.getOriginalIntent(): Intent? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION") // API 33
            this?.getParcelable(ORIGINAL_SAVED_INTENT_FLAG)
        } else {
            this?.getParcelable(ORIGINAL_SAVED_INTENT_FLAG, Intent::class.java)
        }
    }

    companion object {
        private const val HANDLED_DEEPLINK_FLAG = "deeplink_handled_flag_key"
        private const val ORIGINAL_SAVED_INTENT_FLAG = "original_saved_intent"
        private const val TAG = "WireActivity"
    }
}

private data class WireActivityScopedViewModels(
    val callFeedbackViewModel: CallFeedbackViewModel,
    val featureFlagNotificationViewModel: FeatureFlagNotificationViewModel,
    val commonTopAppBarViewModel: CommonTopAppBarViewModel,
    val legalHoldRequestedViewModel: LegalHoldRequestedViewModel,
    val legalHoldDeactivatedViewModel: LegalHoldDeactivatedViewModel,
)

private data class WireActivityGraphContext(
    val graph: MetroViewModelGraph,
    val viewModelFactory: MetroViewModelFactory,
    val sessionGraph: AppSessionViewModelGraph?,
    val activityViewModels: WireActivityScopedViewModels?,
)

private class SessionGraphStoreViewModel(
    private val appGraph: WireApplicationGraph,
) : ViewModel() {
    private val sessionGraphs = mutableMapOf<UserId, AppSessionViewModelGraph>()

    fun graphFor(userId: UserId): AppSessionViewModelGraph =
        sessionGraphs.getOrPut(userId) {
            appLogger.i("WireActivity creating lifecycle-retained session graph for $userId")
            appGraph.createSessionViewModelGraph(userId)
        }
}

private data class ActiveGraphRequest(
    val authenticationViewModelGraph: AppAuthenticationViewModelGraph,
    val sessionGraph: AppSessionViewModelGraph?,
    val effectiveBaseRoute: String,
    val currentBaseRoute: String?,
    val usesAuthenticationGraph: Boolean,
    val usesNoSessionAuthenticationGraph: Boolean,
    val isSessionTransitionInProgress: Boolean,
)

private data class SessionNavigationState(
    val currentUserId: UserId?,
    val currentBaseRoute: String?,
    val isAuthenticationRoute: Boolean,
    val isUserUiBlocked: Boolean,
    val isSessionTransitionInProgress: Boolean,
)

private val loginContinuationAuthenticationRoutes = setOf(
    NewLoginPasswordScreenDestination.baseRoute,
    NewLoginVerificationCodeScreenDestination.baseRoute,
)

private val accountCreationAuthenticationRoutes = setOf(
    CreateAccountSelectorScreenDestination.baseRoute,
    CreateAccountDataDetailScreenDestination.baseRoute,
    CreateAccountVerificationCodeScreenDestination.baseRoute,
    CreatePersonalAccountOverviewScreenDestination.baseRoute,
    CreateTeamAccountOverviewScreenDestination.baseRoute,
    CreateAccountEmailScreenDestination.baseRoute,
    CreateAccountDetailsScreenDestination.baseRoute,
    CreateAccountCodeScreenDestination.baseRoute,
    CreateAccountSummaryScreenDestination.baseRoute,
    CreateAccountUsernameScreenDestination.baseRoute,
)

private val noSessionAuthenticationGraphRoutes = loginContinuationAuthenticationRoutes + accountCreationAuthenticationRoutes

private val sessionBackedAuthenticationGraphRoutes = setOf(
    RegisterDeviceScreenDestination.baseRoute,
    RemoveDeviceScreenDestination.baseRoute,
    E2EIEnrollmentScreenDestination.baseRoute,
)

private val authenticationGraphRoutes = setOf(
    WelcomeScreenDestination.baseRoute,
    NewWelcomeEmptyStartScreenDestination.baseRoute,
    LoginScreenDestination.baseRoute,
    NewLoginScreenDestination.baseRoute,
) + noSessionAuthenticationGraphRoutes + sessionBackedAuthenticationGraphRoutes

private fun Bundle.sessionBackedAuthenticationUserId(): UserId? {
    val value = getString(SessionBackedAuthenticationNavArgs.USER_ID_VALUE_KEY)
    val domain = getString(SessionBackedAuthenticationNavArgs.USER_ID_DOMAIN_KEY)
    return value?.let { userIdValue ->
        domain?.let { userIdDomain ->
            UserId(userIdValue, userIdDomain)
        }
    }
}

internal fun Navigator.shouldReplaceWelcomeLoginStartDestination(): Boolean {
    val firstDestinationBaseRoute = navController.startDestination()?.safeRoute()?.baseRoute
    val welcomeScreens = listOf(WelcomeScreenDestination, NewWelcomeEmptyStartScreenDestination)
    val loginScreens = listOf(LoginScreenDestination, NewLoginScreenDestination)
    val welcomeAndLoginBaseRoutes = (welcomeScreens + loginScreens).map { it.baseRoute }
    return welcomeAndLoginBaseRoutes.contains(firstDestinationBaseRoute)
}

internal fun Navigator.isEmptyWelcomeStartDestination(): Boolean {
    val firstDestinationBaseRoute = navController.startDestination()?.safeRoute()?.baseRoute
    return firstDestinationBaseRoute == NewWelcomeEmptyStartScreenDestination.baseRoute
}

val LocalActivity = staticCompositionLocalOf<AppCompatActivity> {
    error("No Activity provided")
}
