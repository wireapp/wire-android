/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.config.CustomUiConfigurationProvider
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.metro.AppAuthenticationViewModelGraph
import com.wire.android.di.metro.WireApplicationGraph
import com.wire.android.di.metro.WireViewModelDiagnostics
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.conversation.config.LocalConversationHostConfiguration
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import com.wire.android.navigation.navigation3.clearWireViewModelStoreOwner
import com.wire.android.navigation.navigation3.rememberWireSharedViewModelStoreOwner
import com.wire.android.navigation.navigation3.rememberWireNavigation3Runtime
import com.wire.android.navigation.navigation3.rememberWireViewModelStoreProvider
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.AuthenticationNavigationTransition
import com.wire.android.navigation.routes.auth.NewLoginPasswordRoute
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.auth.toAuthenticationServerLinks
import com.wire.android.navigation.runtime.MetroWireEntryEnvironment
import com.wire.android.navigation.runtime.MetroWireEntryGraphResolver
import com.wire.android.navigation.runtime.ProvideViewModelGraph
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel
import com.wire.android.navigation.runtime.wireSessionGraphStoreViewModel
import com.wire.android.navigation.runtime.WireActivityGraphContext
import com.wire.android.navigation.runtime.WireActivityIntentCoordinator
import com.wire.android.navigation.runtime.WireActivityIntentRequest
import com.wire.android.navigation.runtime.WireNavigation3ActivityCallbacks
import com.wire.android.navigation.runtime.WireNavigation3ActivityEffects
import com.wire.android.navigation.runtime.WireNavigation3ActivityPolicy
import com.wire.android.navigation.runtime.WireNavigation3Contributions
import com.wire.android.navigation.runtime.WireNavigation3ProductionActions
import com.wire.android.navigation.runtime.WireNavigation3ProductionHost
import com.wire.android.navigation.runtime.WireNavigation3SwitchAccountActions
import com.wire.android.navigation.runtime.WireNavigationDiagnostics
import com.wire.android.navigation.runtime.noOtherAccountNavigationCommand
import com.wire.android.navigation.runtime.rememberWireNavigation3ActivityGraphContext
import com.wire.android.navigation.runtime.rememberWireNavigation3ProductionActions
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.navigation.runtime.startup.toWireSessionId
import com.wire.android.navigation.runtime.toKaliumUserId
import com.wire.android.navigation.style.BackgroundType
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.CommonTopAppBarState
import com.wire.android.ui.common.topappbar.WireTopAppBar
import com.wire.android.ui.home.conversations.config.AppConversationHostConfiguration
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.LocalSyncStateObserver
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.SyncStateObserver
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.stableKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

internal data class WireActivityHostDependencies(
    val activity: AppCompatActivity,
    val viewModel: WireActivityViewModel,
    val appGraph: WireApplicationGraph,
    val loginTypeSelector: LoginTypeSelector,
    val intentCoordinator: WireActivityIntentCoordinator,
    val currentScreenManager: CurrentScreenManager,
    val switchAccountObserver: SwitchAccountObserver,
    val shakeEvents: Flow<Unit>,
    val onIntentRequest: suspend (
        WireNavigation3Runtime,
        AuthenticationNavigation3Router,
        WireActivityIntentRequest,
    ) -> Unit,
    val onShake: (WireNavigation3Runtime) -> Unit,
    val onStartTeamAppLock: () -> Unit,
    val onUpdateApp: () -> Unit,
    val onScreenshotCensoringChanged: (Boolean) -> Unit,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WireActivityNavigation3Host(
    startDestination: WireRoute,
    dependencies: WireActivityHostDependencies,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val authenticationViewModelGraph = remember(dependencies.appGraph) {
        dependencies.appGraph.authenticationViewModelGraph
    }
    val sessionGraphStore = wireSessionGraphStoreViewModel(
        appGraph = dependencies.appGraph,
        owner = dependencies.activity,
    )
    val sharedViewModelStoreProvider = rememberWireViewModelStoreProvider()

    CompositionLocalProvider(
        LocalMetroViewModelFactory provides dependencies.appGraph.metroViewModelFactory,
        LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
        LocalConversationHostConfiguration provides AppConversationHostConfiguration,
        LocalSyncStateObserver provides SyncStateObserver(dependencies.viewModel.observeSyncFlowState),
        LocalCustomUiConfigurationProvider provides CustomUiConfigurationProvider,
        LocalSnackbarHostState provides snackbarHostState,
        LocalActivity provides dependencies.activity,
    ) {
        HandleWireActivityThemeChanges(dependencies.viewModel.globalAppState.themeOption)
        WireTheme(accent = dependencies.viewModel.globalAppState.userAccent) {
            WireActivityNavigation3ThemedHost(
                startDestination = startDestination,
                dependencies = dependencies,
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraphStore = sessionGraphStore,
                sharedViewModelStoreProvider = sharedViewModelStoreProvider,
            )
        }
    }
}

@Composable
private fun WireActivityNavigation3ThemedHost(
    startDestination: WireRoute,
    dependencies: WireActivityHostDependencies,
    authenticationViewModelGraph: AppAuthenticationViewModelGraph,
    sessionGraphStore: SessionGraphStoreViewModel,
    sharedViewModelStoreProvider: ViewModelStoreProvider,
) {
    val context = LocalContext.current
    val viewModel = dependencies.viewModel
    val isUserUiBlocked = viewModel.globalAppState.blockUserUI != null
    val currentSessionProvider: () -> WireSessionId? = remember {
        {
            viewModel.globalAppState.currentUserId?.toWireSessionId()
        }
    }
    val runtime = rememberWireNavigation3Runtime(
        startRoute = startDestination,
        resultTypes = WireNavigation3Contributions.resultTypes(),
        canNavigate = { command ->
            command.destination !is NewLoginRoute || viewModel.checkNumberOfSessions()
        },
        onNavigationChanged = WireNavigationDiagnostics::navigation,
    )
    val authenticationRouter = remember(runtime) {
        AuthenticationNavigation3Router(runtime)
    }
    val currentRoute = runtime.navigator.currentRoute
    val currentUserId = viewModel.globalAppState.currentUserId
    val isSessionTransitionInProgress = viewModel.globalAppState.isSessionTransitionInProgress
    val sessionTransitionReason = viewModel.globalAppState.sessionTransitionReason
    val shouldInvalidateRetainedSessionGraph = shouldInvalidateWireActivitySessionGraph(
        isUserUiBlocked = isUserUiBlocked,
        sessionTransitionReason = sessionTransitionReason,
    )
    val currentRouteSessionId = (currentRoute as? com.wire.navigation.SessionRoute)?.sessionId
    val appliedSessionGeneration = rememberConfirmedSessionGraphAvailability(
        confirmedSessionGeneration = viewModel.globalAppState.confirmedSessionGeneration,
        currentUserId = currentUserId,
        currentRouteSessionId = currentRouteSessionId,
        invalidated = shouldInvalidateRetainedSessionGraph,
        sessionGraphStore = sessionGraphStore,
    )
    val graphResolver = remember(
        dependencies.appGraph,
        authenticationViewModelGraph,
        sessionGraphStore,
        appliedSessionGeneration,
    ) {
        MetroWireEntryGraphResolver(
            appGraph = dependencies.appGraph,
            authenticationGraph = authenticationViewModelGraph,
            sessionGraphStore = sessionGraphStore,
        )
    }
    val graphContext = rememberWireNavigation3ActivityGraphContext(
        route = currentRoute,
        graphResolver = graphResolver,
        isUserUiBlocked = isUserUiBlocked,
    )
    val sessionLifecycle = rememberWireActivitySessionLifecycle(
        routeSessionId = currentRouteSessionId,
        graphContext = graphContext,
        invalidated = shouldInvalidateRetainedSessionGraph,
        sessionGraphStore = sessionGraphStore,
        provider = sharedViewModelStoreProvider,
    )
    val rootSwitchActions = remember(runtime, dependencies.loginTypeSelector) {
        WireNavigation3SwitchAccountActions(
            onSwitchedToAnotherAccount = {
                currentSessionProvider()?.let {
                    authenticationRouter.navigate(
                        AuthenticationNavigationTransition.ACCOUNT_SWITCH_TO_HOME,
                        HomeRoute(it),
                        WireBackStackMode.CLEAR_WHOLE,
                    )
                }
            },
            onNoOtherAccountToSwitch = {
                noOtherAccountNavigationCommand(
                    currentRoute = runtime.navigator.currentRoute,
                    useNewLogin = dependencies.loginTypeSelector.canUseNewLogin(),
                    recoveryServerLinks = dependencies.viewModel
                        .consumeSessionRecoveryServerLinks()
                        ?.toAuthenticationServerLinks(),
                )?.let {
                    authenticationRouter.navigate(
                        AuthenticationNavigationTransition.ACCOUNT_SWITCH_TO_LOGIN,
                        it,
                    )
                }
            },
        )
    }
    val activityEffects = remember(dependencies.activity) {
        WireNavigation3ActivityEffects(dependencies.activity)
    }
    var pendingTeamAccountReturnRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val teamAccountLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        pendingTeamAccountReturnRoute?.let { encodedReturnRoute ->
            authenticationRouter.navigate(
                AuthenticationNavigationTransition.TEAM_WEB_FLOW_TO_PASSWORD,
                Json.decodeFromString<NewLoginPasswordRoute>(encodedReturnRoute),
                WireBackStackMode.UPDATE_EXISTING,
            )
        }
        pendingTeamAccountReturnRoute = null
    }
    val activityCallbacks = remember(runtime, activityEffects, teamAccountLauncher) {
        WireNavigation3ActivityCallbacks(
            finish = dependencies.activity::finish,
            openUrl = activityEffects::openUrl,
            openIntent = activityEffects::openExternal,
            openTeamAccountWebFlow = { request ->
                activityEffects.teamAccountWebLaunch(request).let { launch ->
                    pendingTeamAccountReturnRoute = Json.encodeToString(launch.returnRoute)
                    teamAccountLauncher.launch(launch.intent)
                }
            },
            completeAppLock = {
                if (!runtime.navigator.goBack()) dependencies.activity.finish()
            },
            cancelAppLock = {
                if (!runtime.navigator.goBack()) dependencies.activity.finish()
            },
            hardLogout = {
                viewModel.doHardLogout(
                    clearUserData = { userId -> UserDataStore(context, userId) },
                    switchAccountActions = rootSwitchActions,
                )
            },
            restartAfterLogout = activityEffects::restartAfterLogout,
            moveTaskToBackground = { dependencies.activity.moveTaskToBack(true) },
            completeSessionBackedAuthenticationCancellation = { sessionId ->
                teardownWireActivitySession(
                    sessionId = sessionId,
                    markInvalidating = sessionGraphStore::markInvalidating,
                    clearOwner = { ownerKey ->
                        clearWireViewModelStoreOwner(sharedViewModelStoreProvider, ownerKey) {
                            WireViewModelDiagnostics.ownerCleared(ownerKey)
                        }
                    },
                    markRemoved = sessionGraphStore::markRemoved,
                )
            },
        )
    }
    val actions = rememberWireNavigation3ProductionActions(
        runtime = runtime,
        activity = activityCallbacks,
        currentSessionId = currentSessionProvider,
        loginTypeSelector = dependencies.loginTypeSelector,
        authenticationRouter = authenticationRouter,
    )
    val entryEnvironment = remember(
        dependencies.appGraph,
        authenticationViewModelGraph,
        sessionGraphStore,
        rootSwitchActions,
    ) {
        MetroWireEntryEnvironment(
            appGraph = dependencies.appGraph,
            authenticationGraph = authenticationViewModelGraph,
            sessionGraphStore = sessionGraphStore,
            logoutAction = { wipeData ->
                viewModel.doHardLogout(
                    clearUserData = { userId -> UserDataStore(context, userId) },
                    switchAccountActions = rootSwitchActions,
                    wipeData = wipeData,
                )
            },
        )
    }
    val backgroundType = WireNavigation3ActivityPolicy.backgroundType(currentRoute)

    HandleNavigation3SessionEffects(
        runtime = runtime,
        authenticationRouter = authenticationRouter,
        currentUserId = currentSessionProvider(),
        isUserUiBlocked = isUserUiBlocked,
        isSessionTransitionInProgress = isSessionTransitionInProgress,
        isSelfLogoutTransition =
            viewModel.globalAppState.sessionTransitionReason == SessionTransitionReason.SELF_LOGOUT,
        finishSessionTransition = viewModel::finishSessionTransition,
        resolveMissingCurrentSession = {
            viewModel.resolveMissingCurrentSession(rootSwitchActions)
        },
    )
    WireActivityNavigation3MainContent(
        runtime = runtime,
        actions = actions,
        authenticationRouter = authenticationRouter,
        entryEnvironment = entryEnvironment,
        graphContext = sessionLifecycle.contentGraphContext,
        backgroundType = backgroundType,
        dependencies = dependencies,
        switchAccountActions = rootSwitchActions,
        activitySessionViewModelStoreOwner = sessionLifecycle.sessionViewModelStoreOwner,
        sharedViewModelStoreProvider = sharedViewModelStoreProvider,
    )
}

@Composable
private fun rememberConfirmedSessionGraphAvailability(
    confirmedSessionGeneration: Long,
    currentUserId: UserId?,
    currentRouteSessionId: WireSessionId?,
    invalidated: Boolean,
    sessionGraphStore: SessionGraphStoreViewModel,
): Long {
    var appliedGeneration by remember(sessionGraphStore) { mutableLongStateOf(-1L) }
    LaunchedEffect(
        confirmedSessionGeneration,
        currentUserId,
        currentRouteSessionId,
        invalidated,
        sessionGraphStore,
    ) {
        val generationCanBeApplied = confirmedSessionGeneration > appliedGeneration && !invalidated
        val confirmedUserId = currentUserId?.takeIf {
            it.toWireSessionId() == currentRouteSessionId
        }
        if (generationCanBeApplied && confirmedUserId != null) {
            sessionGraphStore.markAvailable(confirmedUserId)
            appliedGeneration = confirmedSessionGeneration
        }
    }
    return appliedGeneration
}

private data class WireActivitySessionLifecycle(
    val contentGraphContext: WireActivityGraphContext?,
    val sessionViewModelStoreOwner: ViewModelStoreOwner?,
)

@Composable
private fun rememberWireActivitySessionLifecycle(
    routeSessionId: WireSessionId?,
    graphContext: WireActivityGraphContext?,
    invalidated: Boolean,
    sessionGraphStore: SessionGraphStoreViewModel,
    provider: ViewModelStoreProvider,
): WireActivitySessionLifecycle {
    var retainedRouteSessionId by remember { mutableStateOf<WireSessionId?>(null) }
    val activeSessionId = graphContext
        ?.takeIf { it.sessionGraph != null }
        ?.sessionId
    val sessionOwnerIdentity = activeSessionId?.let(WireViewModelOwner::Session)
    val teardownSessionId = routeSessionId ?: retainedRouteSessionId

    SideEffect {
        if (!invalidated && activeSessionId != null) {
            retainedRouteSessionId = activeSessionId
        }
    }

    LaunchedEffect(invalidated, teardownSessionId) {
        if (invalidated && teardownSessionId != null) {
            // The successful composition above has already stopped rendering the session host and
            // disposed its entry effects. The remaining teardown is deliberately serialized:
            // shared ViewModels first, then the Metro graph. A tombstone rejects stale entries.
            teardownWireActivitySession(
                sessionId = teardownSessionId,
                markInvalidating = sessionGraphStore::markInvalidating,
                clearOwner = { ownerKey ->
                    clearWireViewModelStoreOwner(provider, ownerKey) {
                        WireViewModelDiagnostics.ownerCleared(ownerKey)
                    }
                },
                markRemoved = sessionGraphStore::markRemoved,
            )
            retainedRouteSessionId = null
        }
    }

    val owner = sessionOwnerIdentity
        ?.takeUnless { invalidated }
        ?.let {
            rememberWireSharedViewModelStoreOwner(
                key = it.stableKey(),
                provider = provider,
            )
        }
    if (sessionOwnerIdentity != null && owner != null) {
        DisposableEffect(sessionOwnerIdentity, owner) {
            val ownerKey = sessionOwnerIdentity.stableKey()
            WireViewModelDiagnostics.ownerAvailable(owner, ownerKey)
            onDispose {
                WireViewModelDiagnostics.ownerReleased(owner, ownerKey)
            }
        }
    }
    val contentGraphContext = graphContext?.takeUnless { invalidated }
    return WireActivitySessionLifecycle(contentGraphContext, owner)
}

internal fun teardownWireActivitySession(
    sessionId: WireSessionId,
    markInvalidating: (UserId) -> Unit,
    clearOwner: (String) -> Unit,
    markRemoved: (UserId) -> Unit,
) {
    val userId = sessionId.toKaliumUserId()
    markInvalidating(userId)
    clearOwner(WireViewModelOwner.Session(sessionId).stableKey())
    markRemoved(userId)
}

internal fun shouldInvalidateWireActivitySessionGraph(
    isUserUiBlocked: Boolean,
    sessionTransitionReason: SessionTransitionReason?,
): Boolean = isUserUiBlocked || sessionTransitionReason != null

@Composable
private fun WireActivityNavigation3MainContent(
    runtime: WireNavigation3Runtime,
    actions: WireNavigation3ProductionActions,
    authenticationRouter: AuthenticationNavigation3Router,
    entryEnvironment: MetroWireEntryEnvironment,
    graphContext: WireActivityGraphContext?,
    backgroundType: BackgroundType,
    dependencies: WireActivityHostDependencies,
    switchAccountActions: SwitchAccountActions,
    activitySessionViewModelStoreOwner: ViewModelStoreOwner?,
    sharedViewModelStoreProvider: ViewModelStoreProvider,
) {
    val viewModel = dependencies.viewModel
    val context = LocalContext.current
    val dialogActions = navigation3DialogActions(
        runtime = runtime,
        switchAccountActions = switchAccountActions,
        dependencies = WireActivityDialogActionDependencies(
            context = context,
            viewModel = viewModel,
            updateApp = dependencies.onUpdateApp,
            startTeamAppLock = dependencies.onStartTeamAppLock,
        ),
    )
    if (backgroundType == BackgroundType.Auth) {
        WireAuthBackgroundLayout()
    }
    if (graphContext == null) {
        WireActivityDialogs(
            viewModel = viewModel,
            activityViewModels = null,
            actions = dialogActions,
        )
        return
    }
    graphContext.ProvideViewModelGraph(
        logoutAction = { wipeData ->
            viewModel.doHardLogout(
                clearUserData = { userId -> UserDataStore(context, userId) },
                switchAccountActions = switchAccountActions,
                wipeData = wipeData,
            )
        }
    ) {
        val activityViewModels = if (
            graphContext.sessionGraph != null && activitySessionViewModelStoreOwner != null
        ) {
            wireActivityScopedViewModels(activitySessionViewModelStoreOwner)
        } else {
            null
        }
        activityViewModels?.let {
            LaunchedEffect(it.legalHoldRequestedViewModel) {
                it.legalHoldRequestedViewModel.observeLegalHoldRequest()
            }
        }
        Column(
            modifier = Modifier.semantics { testTagsAsResourceId = true }
        ) {
            WireTopAppBar(
                commonTopAppBarState =
                    activityViewModels?.commonTopAppBarViewModel?.state ?: CommonTopAppBarState(),
                backgroundType = backgroundType,
            )
            WireNavigation3ProductionHost(
                runtime = runtime,
                actions = actions,
                authenticationRouter = authenticationRouter,
                entryEnvironment = entryEnvironment,
                onRootBack = dependencies.activity::finish,
                sharedViewModelStoreProvider = sharedViewModelStoreProvider,
                modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars),
            )
            WireActivityNavigation3Setup(
                runtime = runtime,
                intentCoordinator = dependencies.intentCoordinator,
                currentScreenManager = dependencies.currentScreenManager,
                switchAccountObserver = dependencies.switchAccountObserver,
                switchAccountActions = switchAccountActions,
                shakeEvents = dependencies.shakeEvents,
                onIntentRequest = {
                    dependencies.onIntentRequest(runtime, authenticationRouter, it)
                },
                onShake = { dependencies.onShake(runtime) },
            )
            HandleWireActivityScreenshotCensoring(dependencies)
            WireActivityDialogs(
                viewModel = viewModel,
                activityViewModels = activityViewModels,
                actions = dialogActions,
            )
            HandleNavigation3ViewActions(
                actions = viewModel.actions,
                runtime = runtime,
                authenticationRouter = authenticationRouter,
                loginTypeSelector = dependencies.loginTypeSelector,
                currentSessionId = {
                    viewModel.globalAppState.currentUserId?.toWireSessionId()
                },
            )
        }
    }
}

@Composable
private fun HandleWireActivityThemeChanges(themeOption: ThemeOption) {
    LaunchedEffect(themeOption) {
        val themeNightMode = when (themeOption) {
            ThemeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeOption.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        if (themeNightMode != AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.setDefaultNightMode(themeNightMode)
        }
    }
}

@Composable
private fun HandleWireActivityScreenshotCensoring(
    dependencies: WireActivityHostDependencies,
) {
    val screenshotCensoringEnabled =
        dependencies.viewModel.globalAppState.screenshotCensoringEnabled
    LaunchedEffect(screenshotCensoringEnabled) {
        dependencies.onScreenshotCensoringChanged(screenshotCensoringEnabled)
    }
}
