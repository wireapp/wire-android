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

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.navigation.popWithArguments
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.ui.calling.ProximitySensorManager
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CustomBEDeeplinkDialog
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.joinConversation.JoinConversationViaDeepLinkDialog
import com.wire.android.ui.joinConversation.JoinConversationViaInviteLinkError
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.MaxAccountReachedDialog
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.LocalSyncStateObserver
import com.wire.android.util.SyncStateObserver
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.ui.updateScreenSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalCoroutinesApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
@Suppress("TooManyFunctions")
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    val viewModel: WireActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        proximitySensorManager.initialize()
        lifecycle.addObserver(currentScreenManager)

        handleDeepLink(intent, savedInstanceState)
        viewModel.observePersistentConnectionStatus()
        val startDestination = viewModel.startNavigationRoute()
        setComposableContent(startDestination)
    }

    override fun onNewIntent(intent: Intent?) {
        if (viewModel.isSharingIntent(intent)) {
            setIntent(intent)
        }
        handleDeepLink(intent)
        super.onNewIntent(intent)
    }

    private fun setComposableContent(startDestination: String) {
        setContent {
            CompositionLocalProvider(
                LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
                LocalSyncStateObserver provides SyncStateObserver(viewModel.observeSyncFlowState)
            ) {
                WireTheme {
                    val scope = rememberCoroutineScope()
                    val navController = rememberTrackingAnimatedNavController { NavigationItem.fromRoute(it)?.itemName }
                    setUpNavigationGraph(startDestination, navController, scope)
                    handleDialogs()
                }
            }
        }
    }

    @Composable
    fun setUpNavigationGraph(startDestination: String, navController: NavHostController, scope: CoroutineScope) {
        Scaffold {
            NavigationGraph(navController = navController, startDestination)
        }
        setUpNavigation(navController, scope)
    }

    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        // with the static key here we're sure that this effect wouldn't be canceled or restarted
        LaunchedEffect(Unit) {
            navigationManager.navigateState.onEach { command ->
                if (command == null) return@onEach
                keyboardController?.hide()
                navController.navigateToItem(command)
            }.launchIn(scope)

            navigationManager.navigateBack.onEach {
                if (!navController.popWithArguments(it)) finish()
            }.launchIn(scope)

            navController.addOnDestinationChangedListener { controller, _, _ ->
                keyboardController?.hide()
                updateScreenSettings(controller)
            }

            navController.addOnDestinationChangedListener(currentScreenManager)
        }
    }

    @Composable
    private fun handleDialogs() {
        updateAppDialog({ updateTheApp() }, viewModel.globalAppState.updateAppDialog)
        joinConversationDialog(viewModel.globalAppState.conversationJoinedDialog)
        customBackendDialog(viewModel.globalAppState.customBackendDialog.shouldShowDialog)
        maxAccountDialog(viewModel::openProfile, viewModel::dismissMaxAccountDialog, viewModel.globalAppState.maxAccountDialog)
        accountLoggedOutDialog(viewModel.globalAppState.blockUserUI)
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
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = true
                )
            )
        }
    }

    @Composable
    private fun joinConversationDialog(joinedDialogState: JoinConversationViaCodeState?) {
        joinedDialogState?.let {
            when (it) {
                is JoinConversationViaCodeState.Error -> JoinConversationViaInviteLinkError(
                    errorState = it,
                    onCancel = viewModel::cancelJoinConversation
                )

                is JoinConversationViaCodeState.Show -> JoinConversationViaDeepLinkDialog(
                    it,
                    false,
                    onCancel = viewModel::cancelJoinConversation,
                    onJoinClick = viewModel::joinConversationViaCode
                )
            }
        }
    }

    @Composable
    private fun customBackendDialog(shouldShow: Boolean) {
        if (shouldShow) {
            CustomBEDeeplinkDialog(viewModel)
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
    private fun accountLoggedOutDialog(blockUserUI: CurrentSessionErrorState?) {
        blockUserUI?.let { accountLoggedOutDialog(it, viewModel::navigateToNextAccountOrWelcome) }
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

    private fun handleDeepLink(
        intent: Intent?,
        savedInstanceState: Bundle? = null
    ) {
        if (intent == null
            || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
            || savedInstanceState?.getBoolean(HANDLED_DEEPLINK_FLAG, false) == true
        ) {
            return
        }

        viewModel.handleDeepLink(intent)
    }

    companion object {
        private const val HANDLED_DEEPLINK_FLAG = "deeplink_handled_flag_key"
    }
}
