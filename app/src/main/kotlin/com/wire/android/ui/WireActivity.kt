/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.spec.Route
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.config.CustomUiConfigurationProvider
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.navigateToItem
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.calling.ProximitySensorManager
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CustomServerDialog
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.ImportMediaScreenDestination
import com.wire.android.ui.destinations.IncomingCallScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.destinations.MigrationScreenDestination
import com.wire.android.ui.destinations.OngoingCallScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfDevicesScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.joinConversation.JoinConversationViaDeepLinkDialog
import com.wire.android.ui.joinConversation.JoinConversationViaInviteLinkError
import com.wire.android.ui.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.MaxAccountReachedDialog
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.LocalSyncStateObserver
import com.wire.android.util.SyncStateObserver
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.formatMediumDateTime
import com.wire.android.util.ui.updateScreenSettings
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
@Suppress("TooManyFunctions")
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    private val viewModel: WireActivityViewModel by viewModels()

    private val commonTopAppBarViewModel: CommonTopAppBarViewModel by viewModels()

    val navigationCommands: MutableSharedFlow<NavigationCommand> = MutableSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        proximitySensorManager.initialize()
        lifecycle.addObserver(currentScreenManager)

        viewModel.observePersistentConnectionStatus()
        val startDestination = when (viewModel.initialAppState) {
            InitialAppState.NOT_MIGRATED -> MigrationScreenDestination
            InitialAppState.NOT_LOGGED_IN -> WelcomeScreenDestination
            InitialAppState.LOGGED_IN -> HomeScreenDestination
        }
        setComposableContent(startDestination) {
            handleDeepLink(intent, savedInstanceState)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (viewModel.isSharingIntent(intent)) {
            setIntent(intent)
        }
        handleDeepLink(intent)
        super.onNewIntent(intent)
    }

    private fun setComposableContent(
        startDestination: Route,
        onComplete: () -> Unit
    ) {
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            CompositionLocalProvider(
                LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
                LocalSyncStateObserver provides SyncStateObserver(viewModel.observeSyncFlowState),
                LocalCustomUiConfigurationProvider provides CustomUiConfigurationProvider,
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    Column {
                        val navigator = rememberNavigator(this@WireActivity::finish)
                        val scope = rememberCoroutineScope()

                        CommonTopAppBar(
                            connectivityUIState = commonTopAppBarViewModel.connectivityState,
                            onReturnToCallClick = { establishedCall ->
                                navigator.navigate(NavigationCommand(OngoingCallScreenDestination(establishedCall.conversationId)))
                            }
                        )
                        NavigationGraph(
                            navigator = navigator,
                            startDestination = startDestination
                        )
                        // This setup needs to be done after the navigation graph is created, because building the graph takes some time,
                        // and if any NavigationCommand is executed before the graph is fully built, it will cause a NullPointerException.
                        setUpNavigation(navigator.navController, onComplete, scope)
                        handleScreenshotCensoring()
                        handleDialogs(navigator::navigate)
                    }
                }
            }
        }
    }

    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        onComplete: () -> Unit,
        scope: CoroutineScope
    ) {
        val currentKeyboardController by rememberUpdatedState(LocalSoftwareKeyboardController.current)
        val currentNavController by rememberUpdatedState(navController)
        LaunchedEffect(scope) {
            navigationCommands
                .onSubscription { onComplete() }
                .onEach { command ->
                    currentKeyboardController?.hide()
                    currentNavController.navigateToItem(command)
                }.launchIn(scope)
        }

        DisposableEffect(navController) {
            val updateScreenSettingsListener = NavController.OnDestinationChangedListener { _, navDestination, _ ->
                currentKeyboardController?.hide()
                updateScreenSettings(navDestination)
            }
            navController.addOnDestinationChangedListener(updateScreenSettingsListener)
            navController.addOnDestinationChangedListener(currentScreenManager)

            onDispose {
                navController.removeOnDestinationChangedListener(updateScreenSettingsListener)
                navController.removeOnDestinationChangedListener(currentScreenManager)
            }
        }
    }

    @Composable
    private fun handleScreenshotCensoring() {
        LaunchedEffect(viewModel.globalAppState.screenshotCensoringEnabled) {
            if (viewModel.globalAppState.screenshotCensoringEnabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    @Composable
    private fun handleDialogs(navigate: (NavigationCommand) -> Unit) {
        updateAppDialog({ updateTheApp() }, viewModel.globalAppState.updateAppDialog)
        joinConversationDialog(
            viewModel.globalAppState.conversationJoinedDialog,
            navigate,
            viewModel::onJoinConversationFlowCompleted
        )
        customBackendDialog(navigate)
        maxAccountDialog(
            onConfirm = {
                viewModel.dismissMaxAccountDialog()
                navigate(NavigationCommand(SelfUserProfileScreenDestination))
            },
            onDismiss = viewModel::dismissMaxAccountDialog,
            shouldShow = viewModel.globalAppState.maxAccountDialog
        )
        accountLoggedOutDialog(viewModel.globalAppState.blockUserUI, navigate)
        newClientDialog(
            viewModel.globalAppState.newClientDialog,
            { navigate(NavigationCommand(SelfDevicesScreenDestination)) },
            {
                viewModel.switchAccount(
                    userId = it,
                    actions = NavigationSwitchAccountActions(navigate),
                    onComplete = { navigate(NavigationCommand(SelfDevicesScreenDestination)) })
            },
            viewModel::dismissNewClientsDialog
        )
    }

    @Composable
    private fun updateAppDialog(onUpdateClick: () -> Unit, shouldShow: Boolean) {
        if (shouldShow) {
            WireDialog(
                title = stringResource(id = R.string.update_app_dialog_title),
                text = stringResource(id = R.string.update_app_dialog_body),
                onDismiss = { },
                optionButton1Properties = WireDialogButtonProperties(
                    text = stringResource(id = R.string.update_app_dialog_button),
                    onClick = onUpdateClick,
                    type = WireDialogButtonType.Primary
                ),
                properties = wireDialogPropertiesBuilder(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = true
                )
            )
        }
    }

    @Composable
    private fun joinConversationDialog(
        joinedDialogState: JoinConversationViaCodeState?,
        navigate: (NavigationCommand) -> Unit,
        onJoinConversationFlowCompleted: () -> Unit
    ) {
        joinedDialogState?.let {

            val onComplete: (convId: ConversationId?) -> Unit = remember {
                {
                    onJoinConversationFlowCompleted()
                    it?.also {
                        appLogger.d("Join conversation via code dialog completed, navigating to conversation screen")
                        navigate(
                            NavigationCommand(
                                ConversationScreenDestination(it),
                                BackStackMode.CLEAR_TILL_START
                            )
                        )
                    }
                }
            }

            when (it) {
                is JoinConversationViaCodeState.Error -> JoinConversationViaInviteLinkError(
                    errorState = it,
                    onCancel = { onComplete(null) }
                )

                is JoinConversationViaCodeState.Show -> {
                    JoinConversationViaDeepLinkDialog(
                        name = it.conversationName,
                        code = it.code,
                        domain = it.domain,
                        key = it.key,
                        requirePassword = it.passwordProtected,
                        onFlowCompleted = onComplete
                    )
                }
            }
        }
    }

    @Composable
    private fun customBackendDialog(navigate: (NavigationCommand) -> Unit) {
        with(viewModel) {
            if (globalAppState.customBackendDialog != null) {
                CustomServerDialog(
                    serverLinksTitle = globalAppState.customBackendDialog!!.serverLinks.title,
                    serverLinksApi = globalAppState.customBackendDialog!!.serverLinks.api,
                    onDismiss = this::dismissCustomBackendDialog,
                    onConfirm = { customBackendDialogProceedButtonClicked { navigate(NavigationCommand(WelcomeScreenDestination)) } }
                )
            }
        }
    }

    @Composable
    private fun maxAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, shouldShow: Boolean) {
        if (shouldShow) {
            MaxAccountReachedDialog(
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                buttonText = R.string.max_account_reached_dialog_button_open_profile
            )
        }
    }

    @Composable
    private fun accountLoggedOutDialog(blockUserUI: CurrentSessionErrorState?, navigate: (NavigationCommand) -> Unit) {
        blockUserUI?.let {
            accountLoggedOutDialog(reason = it) { viewModel.tryToSwitchAccount(NavigationSwitchAccountActions(navigate)) }
        }
    }

    @Composable
    fun accountLoggedOutDialog(reason: CurrentSessionErrorState, navigateAway: () -> Unit) {
        appLogger.e("AccountLongedOutDialog: $reason")
        val (@StringRes title: Int, text: String) = when (reason) {
            CurrentSessionErrorState.SessionExpired -> {
                if (BuildConfig.WIPE_ON_COOKIE_INVALID) {
                    R.string.session_expired_error_title to (
                            stringResource(id = R.string.session_expired_error_message)
                                    + "\n\n"
                                    + stringResource(id = R.string.conversation_history_wipe_explanation)
                            )
                } else {
                    R.string.session_expired_error_title to stringResource(id = R.string.session_expired_error_message)
                }
            }

            CurrentSessionErrorState.RemovedClient -> {
                if (BuildConfig.WIPE_ON_DEVICE_REMOVAL) {
                    R.string.removed_client_error_title to (
                            stringResource(id = R.string.removed_client_error_message)
                                    + "\n\n"
                                    + stringResource(id = R.string.conversation_history_wipe_explanation)
                            )
                } else {
                    R.string.removed_client_error_title to stringResource(R.string.removed_client_error_message)
                }
            }

            CurrentSessionErrorState.DeletedAccount -> {
                R.string.deleted_user_error_title to stringResource(R.string.deleted_user_error_message)
            }
        }
        WireDialog(
            title = stringResource(id = title),
            text = text,
            onDismiss = remember { { } },
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_ok),
                onClick = navigateAway,
                type = WireDialogButtonType.Primary
            )
        )
    }

    @Composable
    private fun newClientDialog(
        data: NewClientsData?,
        openDeviceManager: () -> Unit,
        switchAccountAndOpenDeviceManager: (UserId) -> Unit,
        dismiss: (UserId) -> Unit
    ) {
        data?.let {
            val title: String
            val text: String
            val btnText: String
            val btnAction: () -> Unit
            val dismissAction: () -> Unit = { dismiss(data.userId) }
            val devicesList = data.clientsInfo.map {
                stringResource(
                    R.string.new_device_dialog_message_defice_info,
                    it.date.formatMediumDateTime() ?: "",
                    it.deviceInfo.asString()
                )
            }.joinToString("")
            when (data) {
                is NewClientsData.OtherUser -> {
                    title = stringResource(
                        R.string.new_device_dialog_other_user_title,
                        data.userName ?: "",
                        data.userHandle ?: ""
                    )
                    text = stringResource(R.string.new_device_dialog_other_user_message, devicesList)
                    btnText = stringResource(R.string.new_device_dialog_other_user_btn)
                    btnAction = { switchAccountAndOpenDeviceManager(data.userId) }
                }

                is NewClientsData.CurrentUser -> {
                    title = stringResource(R.string.new_device_dialog_current_user_title)
                    text = stringResource(R.string.new_device_dialog_current_user_message, devicesList)
                    btnText = stringResource(R.string.new_device_dialog_current_user_btn)
                    btnAction = openDeviceManager
                }
            }
            WireDialog(
                title = title,
                text = text,
                onDismiss = dismissAction,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = {
                        dismissAction()
                        btnAction()
                    },
                    text = btnText,
                    type = WireDialogButtonType.Secondary
                ),
                optionButton2Properties = WireDialogButtonProperties(
                    text = stringResource(id = R.string.label_ok),
                    onClick = dismissAction,
                    type = WireDialogButtonType.Primary
                )
            )
        }
    }

    private fun updateTheApp() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.UPDATE_APP_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        proximitySensorManager.registerListener()
    }

    override fun onPause() {
        super.onPause()
        proximitySensorManager.unRegisterListener()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HANDLED_DEEPLINK_FLAG, true)
        super.onSaveInstanceState(outState)
    }

    @Suppress("ComplexCondition")
    private fun handleDeepLink(
        intent: Intent?,
        savedInstanceState: Bundle? = null
    ) {
        if (intent == null
            || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
            || savedInstanceState?.getBoolean(HANDLED_DEEPLINK_FLAG, false) == true
            || intent.getBooleanExtra(HANDLED_DEEPLINK_FLAG, false)
        ) {
            return
        } else {
            val navigate: (NavigationCommand) -> Unit = { lifecycleScope.launch { navigationCommands.emit(it) } }
            viewModel.handleDeepLink(
                intent = intent,
                onResult = ::handleDeepLinkResult,
                onOpenConversation = { navigate(NavigationCommand(ConversationScreenDestination(it), BackStackMode.CLEAR_TILL_START)) },
                onIsSharingIntent = { navigate(NavigationCommand(ImportMediaScreenDestination, BackStackMode.UPDATE_EXISTED)) }
            )
            intent.putExtra(HANDLED_DEEPLINK_FLAG, true)
        }
    }

    private fun handleDeepLinkResult(result: DeepLinkResult) {
        val navigate: (NavigationCommand) -> Unit = { lifecycleScope.launch { navigationCommands.emit(it) } }
        when (result) {
            is DeepLinkResult.SSOLogin -> {
                navigate(NavigationCommand(LoginScreenDestination(ssoLoginResult = result), BackStackMode.UPDATE_EXISTED))
            }

            is DeepLinkResult.MigrationLogin -> {
                navigate(NavigationCommand(LoginScreenDestination(result.userHandle), BackStackMode.UPDATE_EXISTED))
            }

            is DeepLinkResult.CustomServerConfig -> {
                // do nothing, already handled in ViewModel
            }

            is DeepLinkResult.IncomingCall -> {
                if (result.switchedAccount) navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
                navigate(NavigationCommand(IncomingCallScreenDestination(result.conversationsId)))
            }

            is DeepLinkResult.OngoingCall -> {
                navigate(NavigationCommand(OngoingCallScreenDestination(result.conversationsId)))
            }

            is DeepLinkResult.OpenConversation -> {
                if (result.switchedAccount) navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
                navigate(NavigationCommand(ConversationScreenDestination(result.conversationsId), BackStackMode.UPDATE_EXISTED))
            }

            is DeepLinkResult.OpenOtherUserProfile -> {
                if (result.switchedAccount) navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
                navigate(NavigationCommand(OtherUserProfileScreenDestination(result.userId), BackStackMode.UPDATE_EXISTED))
            }

            is DeepLinkResult.JoinConversation -> {
                // do nothing, already handled in ViewModel
            }

            is DeepLinkResult.Unknown -> {
                appLogger.e("unknown deeplink result $result")
            }
        }
    }

    companion object {
        private const val HANDLED_DEEPLINK_FLAG = "deeplink_handled_flag_key"
    }
}

val LocalActivity = staticCompositionLocalOf<Activity> {
    error("No Activity provided")
}
