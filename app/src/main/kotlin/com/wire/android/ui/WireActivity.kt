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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.spec.Route
import com.ramcosta.composedestinations.utils.destination
import com.ramcosta.composedestinations.utils.route
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.config.CustomUiConfigurationProvider
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.MainNavHost
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.getBaseRoute
import com.wire.android.navigation.rememberNavigator
import com.wire.android.navigation.startDestination
import com.wire.android.navigation.style.BackgroundStyle
import com.wire.android.navigation.style.BackgroundType
import com.wire.android.notification.broadcastreceivers.DynamicReceiversManager
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.calling.getIncomingCallIntent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarState
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.E2eiCertificateDetailsScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.destinations.NewLoginScreenDestination
import com.wire.android.ui.destinations.NewWelcomeEmptyStartScreenDestination
import com.wire.android.ui.destinations.SelfDevicesScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateUI
import com.wire.android.ui.home.E2EICertificateRevokedDialog
import com.wire.android.ui.home.E2EIRequiredDialog
import com.wire.android.ui.home.E2EIResultDialog
import com.wire.android.ui.home.E2EISnoozeDialog
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.appLock.LockCodeTimeManager
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
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.LocalSyncStateObserver
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.SyncStateObserver
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.deeplink.LoginType
import com.wire.android.util.launchUpdateTheApp
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
@Suppress("TooManyFunctions", "LargeClass")
class WireActivity : AppCompatActivity() {

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

    private val viewModel: WireActivityViewModel by viewModels()
    private val featureFlagNotificationViewModel: FeatureFlagNotificationViewModel by viewModels()
    private val callFeedbackViewModel: CallFeedbackViewModel by viewModels()

    private val commonTopAppBarViewModel: CommonTopAppBarViewModel by viewModels()
    private val legalHoldRequestedViewModel: LegalHoldRequestedViewModel by viewModels()
    private val legalHoldDeactivatedViewModel: LegalHoldDeactivatedViewModel by viewModels()

    private val newIntents = Channel<Pair<Intent, Bundle?>>(Channel.UNLIMITED) // keep new intents until subscribed but do not replay them

    // This flag is used to keep the splash screen open until the first screen is drawn.
    private var shouldKeepSplashOpen = true

    override fun onCreate(savedInstanceState: Bundle?) {

        appLogger.i("$TAG splash install")
        // We need to keep the splash screen open until the first screen is drawn.
        // Otherwise a white screen is displayed.
        // It's an API limitation, at some point we may need to remove it
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { shouldKeepSplashOpen }

        enableEdgeToEdge()
        setupOrientationForDevice()

        lifecycleScope.launch {

            appLogger.i("$TAG persistent connection status")
            viewModel.observePersistentConnectionStatus()

            appLogger.i("$TAG legal hold requested status")
            legalHoldRequestedViewModel.observeLegalHoldRequest()

            appLogger.i("$TAG init login type selector")

            appLogger.i("$TAG start destination")
            val startDestination = when (viewModel.initialAppState()) {
                InitialAppState.NOT_LOGGED_IN -> when (loginTypeSelector.canUseNewLogin()) {
                    true -> NewWelcomeEmptyStartScreenDestination
                    false -> WelcomeScreenDestination
                }

                InitialAppState.ENROLL_E2EI -> E2EIEnrollmentScreenDestination
                InitialAppState.LOGGED_IN -> HomeScreenDestination
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
                managedConfigurationsManager.refreshRemoteBackupURLConfig()
            }
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

    @Suppress("LongMethod")
    private fun setComposableContent(startDestination: Route) {
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            HandleThemeChanges(viewModel.globalAppState.themeOption)

            CompositionLocalProvider(
                LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
                LocalSyncStateObserver provides SyncStateObserver(viewModel.observeSyncFlowState),
                LocalCustomUiConfigurationProvider provides CustomUiConfigurationProvider,
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme(accent = viewModel.globalAppState.userAccent) {
                    val navigator = rememberNavigator(
                        finish = this@WireActivity::finish,
                        isAllowedToNavigate = { navigationCommand ->
                            when {
                                navigationCommand.destination.route.getBaseRoute() == NewLoginScreenDestination.route.getBaseRoute() -> {
                                    /**
                                     * This is a case when the app tries to open the "enterprise login" screen so first it needs to verify
                                     * whether it's possible to have another session, if not then do not navigate and show proper dialog.
                                     */
                                    viewModel.checkNumberOfSessions()
                                }

                                else -> true
                            }
                        }
                    )
                    val currentBackStackEntryState = navigator.navController.currentBackStackEntryAsState()
                    val backgroundType by remember {
                        derivedStateOf {
                            currentBackStackEntryState.value?.destination()?.style.let {
                                (it as? BackgroundStyle)?.backgroundType() ?: BackgroundType.Default
                            }
                        }
                    }
                    if (backgroundType == BackgroundType.Auth) {
                        WireAuthBackgroundLayout()
                    }
                    Column(
                        modifier = Modifier
                            .semantics { testTagsAsResourceId = true }
                    ) {
                        WireTopAppBar(
                            commonTopAppBarState = commonTopAppBarViewModel.state,
                            backgroundType = backgroundType,
                        )
                        MainNavHost(
                            navigator = navigator,
                            loginTypeSelector = loginTypeSelector,
                            startDestination = startDestination,
                            modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars)
                        )

                        // This setup needs to be done after the navigation graph is created, because building the graph takes some time,
                        // and if any NavigationCommand is executed before the graph is fully built, it will cause a NullPointerException.
                        SetUpNavigation(navigator)
                        HandleScreenshotCensoring()
                        HandleDialogs(navigator)
                        HandleViewActions(viewModel.actions, navigator, loginTypeSelector)
                    }
                }
            }
        }
    }

    @Composable
    private fun WireTopAppBar(
        commonTopAppBarState: CommonTopAppBarState,
        backgroundType: BackgroundType,
        modifier: Modifier = Modifier,
    ) {
        CommonTopAppBar(
            modifier = modifier,
            commonTopAppBarState = commonTopAppBarState,
            backgroundType = backgroundType,
            onReturnToCallClick = { establishedCall ->
                getOngoingCallIntent(
                    context = this@WireActivity,
                    conversationId = establishedCall.conversationId.toString(),
                    userId = establishedCall.userId.toString(),
                ).run {
                    startActivity(this)
                }
            },
            onReturnToIncomingCallClick = {
                getIncomingCallIntent(
                    context = this@WireActivity,
                    conversationId = it.conversationId.toString(),
                    userId = it.userId.toString(),
                ).run {
                    startActivity(this)
                }
            },
            onReturnToOutgoingCallClick = {
                getOutgoingCallIntent(
                    context = this@WireActivity,
                    conversationId = it.conversationId.toString(),
                    userId = it.userId.toString(),
                ).run {
                    startActivity(this)
                }
            }
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
                        handleDeepLink(currentNavigator, intent, savedInstanceState)
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
    private fun HandleDialogs(navigator: Navigator) {
        val navigate: (NavigationCommand) -> Unit = { navigator.navigate(it) }
        val context = LocalContext.current
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
                            val isUserAppLockSet =
                                featureFlagNotificationViewModel.isUserAppLockSet()
                            // No need to setup another app lock if the user already has one
                            if (!isUserAppLockSet) {
                                Intent(this@WireActivity, AppLockActivity::class.java)
                                    .apply {
                                        putExtra(AppLockActivity.SET_TEAM_APP_LOCK, true)
                                    }.also {
                                        startActivity(it)
                                    }
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
                                    E2eiCertificateDetailsScreenDestination(
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
                    onConfirm = { loginType ->
                        viewModel.customBackendDialogProceedButtonClicked { serverLinks ->
                            lifecycleScope.launch {
                                val destination = when (loginType) {
                                    LoginType.New -> NewLoginScreenDestination(loginPasswordPath = LoginPasswordPath(serverLinks))
                                    LoginType.Old -> WelcomeScreenDestination(customServerConfig = serverLinks)
                                    LoginType.Default -> when (loginTypeSelector.canUseNewLogin(serverLinks)) {
                                        true -> NewLoginScreenDestination(loginPasswordPath = LoginPasswordPath(serverLinks))
                                        false -> WelcomeScreenDestination(customServerConfig = serverLinks)
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    navigate(
                                        NavigationCommand(
                                            destination = destination,
                                            // if "welcome empty start" screen then switch "start" screen to proper one
                                            backStackMode = when (navigator.shouldReplaceWelcomeLoginStartDestination()) {
                                                true -> BackStackMode.CLEAR_WHOLE
                                                else -> BackStackMode.UPDATE_EXISTED
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    },
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

        lifecycleScope.launch {
            lockCodeTimeManager.get().observeAppLock()
                // Listen to one flow in a lifecycle-aware manner using flowWithLifecycle
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .first().let {
                    if (it) {
                        withContext(Dispatchers.Main) {
                            startActivity(
                                Intent(this@WireActivity, AppLockActivity::class.java)
                            )
                        }
                    }
                }
        }
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
    private fun handleDeepLink(
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
            || intent.action == Intent.ACTION_MAIN // This is the case when the app is opened from launcher so no deep link to handle
            || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
            || originalIntent == intent // This is the case when the activity is recreated and already handled
            || intent.getBooleanExtra(HANDLED_DEEPLINK_FLAG, false)
        ) {
            if (navigator.isEmptyWelcomeStartDestination()) {
                // no deep link to handle so if "welcome empty start" screen then switch "start" screen to login by navigating to it
                navigator.navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
            }
            return
        } else {
            viewModel.handleDeepLink(intent)
            intent.putExtra(HANDLED_DEEPLINK_FLAG, true)
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

internal fun Navigator.shouldReplaceWelcomeLoginStartDestination(): Boolean {
    val firstDestinationBaseRoute = navController.startDestination()?.route()?.route?.getBaseRoute()
    val welcomeScreens = listOf(WelcomeScreenDestination, NewWelcomeEmptyStartScreenDestination)
    val loginScreens = listOf(LoginScreenDestination, NewLoginScreenDestination)
    val welcomeAndLoginBaseRoutes = (welcomeScreens + loginScreens).map { it.route.getBaseRoute() }
    return welcomeAndLoginBaseRoutes.contains(firstDestinationBaseRoute)
}

internal fun Navigator.isEmptyWelcomeStartDestination(): Boolean {
    val firstDestinationBaseRoute = navController.startDestination()?.route()?.route?.getBaseRoute()
    return firstDestinationBaseRoute == NewWelcomeEmptyStartScreenDestination.route.getBaseRoute()
}

val LocalActivity = staticCompositionLocalOf<AppCompatActivity> {
    error("No Activity provided")
}
