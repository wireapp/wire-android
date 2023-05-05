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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.config.CustomUiConfigurationProvider
import com.wire.android.config.LocalCustomUiConfigurationProvider
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
import com.wire.android.util.formatMediumDateTime
import com.wire.android.util.ui.updateScreenSettings
import com.wire.kalium.logic.data.user.UserId
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

        viewModel.observePersistentConnectionStatus()
        val startDestination = viewModel.startNavigationRoute()
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
        startDestination: String,
        onComplete: () -> Unit
    ) {
        setContent {
            if (viewModel.globalAppState.jailBreakDetected) {
                JailBreakDetectedDialog()
            } else {
                CompositionLocalProvider(
                    LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
                    LocalSyncStateObserver provides SyncStateObserver(viewModel.observeSyncFlowState),
                    LocalCustomUiConfigurationProvider provides CustomUiConfigurationProvider
                ) {
                    WireTheme {
                        setUpNavigationGraph(
                            startDestination = startDestination,
                            onComplete = onComplete
                        )
                        handleDialogs()
                    }
                }
            }
        }
    }

    @Composable
    private fun setUpNavigationGraph(
        startDestination: String,
        onComplete: () -> Unit
    ) {
        val navController = rememberTrackingAnimatedNavController { NavigationItem.fromRoute(it)?.itemName }
        val scope = rememberCoroutineScope()
        NavigationGraph(
            navController = navController,
            startDestination = startDestination,
            onComplete = onComplete
        )
        // This setup needs to be done after the navigation graph is created, because building the graph takes some time,
        // and if any NavigationCommand is executed before the graph is fully built, it will cause a NullPointerException.
        setUpNavigation(navController, scope)
    }

    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        val currentKeyboardController by rememberUpdatedState(LocalSoftwareKeyboardController.current)
        val currentNavController by rememberUpdatedState(navController)
        LaunchedEffect(scope) {
            navigationManager.navigateState.onEach { command ->
                if (command == null) return@onEach
                currentKeyboardController?.hide()
                currentNavController.navigateToItem(command)
            }.launchIn(scope)

            navigationManager.navigateBack.onEach {
                if (!currentNavController.popWithArguments(it)) finish()
            }.launchIn(scope)
        }

        DisposableEffect(navController) {
            val updateScreenSettingsListener = NavController.OnDestinationChangedListener { controller, _, _ ->
                currentKeyboardController?.hide()
                updateScreenSettings(controller)
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
    private fun handleDialogs() {
        UpdateAppDialog(
            onUpdateClick = ::updateTheApp,
            shouldShow = viewModel.globalAppState.updateAppDialog
        )
        JoinConversationDialog(
            viewModel.globalAppState.conversationJoinedDialog
        )
        CustomBackendDialog(
            viewModel.globalAppState.customBackendDialog.shouldShowDialog
        )
        MaxAccountDialog(
            onConfirm = viewModel::openProfile,
            onDismiss = viewModel::dismissMaxAccountDialog,
            shouldShow = viewModel.globalAppState.maxAccountDialog
        )
        AccountLoggedOutDialog(
            blockUserUI = viewModel.globalAppState.blockUserUI
        )
        NewClientDialog(
            data = viewModel.globalAppState.newClientDialog,
            openDeviceManager = viewModel::openDeviceManager,
            switchAccount = viewModel::switchAccount,
            dismiss = viewModel::dismissNewClientDialog
        )
    }

    @Composable
    private fun JailBreakDetectedDialog() {
        WireDialog(
            title = stringResource(R.string.label_jailbreak_detected_dialog_title),
            text = stringResource(R.string.label_jailbreak_detected_dialog_text),
            onDismiss = { },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = true
            )
        )
    }

    @Composable
    private fun UpdateAppDialog(onUpdateClick: () -> Unit, shouldShow: Boolean) {
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
    private fun JoinConversationDialog(joinedDialogState: JoinConversationViaCodeState?) {
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
    private fun CustomBackendDialog(shouldShow: Boolean) {
        if (shouldShow) {
            CustomBEDeeplinkDialog(viewModel)
        }
    }

    @Composable
    private fun MaxAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, shouldShow: Boolean) {
        if (shouldShow) {
            MaxAccountReachedDialog(
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                buttonText = R.string.max_account_reached_dialog_button_open_profile
            )
        }
    }

    @Composable
    private fun AccountLoggedOutDialog(blockUserUI: CurrentSessionErrorState?) {
        blockUserUI?.let { AccountLoggedOutDialog(it, viewModel::navigateToNextAccountOrWelcome) }
    }

    @Composable
    fun AccountLoggedOutDialog(reason: CurrentSessionErrorState, navigateAway: () -> Unit) {
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
    private fun NewClientDialog(
        data: NewClientData?,
        openDeviceManager: () -> Unit,
        switchAccount: (UserId) -> Unit,
        dismiss: () -> Unit
    ) {
        data?.let {
            val date = data.date.formatMediumDateTime() ?: ""
            val title: String
            val text: String
            val btnText: String
            val btnAction: () -> Unit
            when (data) {
                is NewClientData.OtherUser -> {
                    title = stringResource(R.string.new_device_dialog_other_user_title, data.userName ?: "", data.userHandle ?: "")
                    text = stringResource(R.string.new_device_dialog_other_user_message, date, data.deviceInfo.asString())
                    btnText = stringResource(R.string.new_device_dialog_other_user_btn)
                    btnAction = { switchAccount(data.userId) }
                }

                is NewClientData.CurrentUser -> {
                    title = stringResource(R.string.new_device_dialog_current_user_title)
                    text = stringResource(R.string.new_device_dialog_current_user_message, date, data.deviceInfo.asString())
                    btnText = stringResource(R.string.new_device_dialog_current_user_btn)
                    btnAction = openDeviceManager
                }
            }
            WireDialog(
                title = title,
                text = text,
                onDismiss = dismiss,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = {
                        dismiss()
                        btnAction()
                    },
                    text = btnText,
                    type = WireDialogButtonType.Secondary
                ),
                optionButton2Properties = WireDialogButtonProperties(
                    text = stringResource(id = R.string.label_ok),
                    onClick = dismiss,
                    type = WireDialogButtonType.Primary
                ),
                properties = DialogProperties(usePlatformDefaultWidth = true)
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
