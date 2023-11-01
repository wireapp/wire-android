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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CustomServerDialog
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.joinConversation.JoinConversationViaDeepLinkDialog
import com.wire.android.ui.joinConversation.JoinConversationViaInviteLinkError
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.MaxAccountReachedDialog
import com.wire.android.util.formatMediumDateTime
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase

@Composable
fun FileRestrictionDialog(
    isFileSharingEnabled: Boolean,
    hideDialogStatus: () -> Unit,
) {
    val text: String =
        stringResource(id = if (isFileSharingEnabled) R.string.sharing_files_enabled else R.string.sharing_files_disabled)

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = hideDialogStatus,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = hideDialogStatus,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun SelfDeletingMessagesDialog(
    areSelfDeletingMessagesEnabled: Boolean,
    enforcedTimeout: SelfDeletionDuration,
    hideDialogStatus: () -> Unit,
) {
    val formattedTimeout = enforcedTimeout.longLabel.asString()
    val text: String = when {
        areSelfDeletingMessagesEnabled && enforcedTimeout == SelfDeletionDuration.None -> {
            stringResource(id = R.string.self_deleting_messages_team_setting_enabled)
        }

        areSelfDeletingMessagesEnabled -> {
            stringResource(
                R.string.self_deleting_messages_team_setting_enabled_enforced_timeout,
                formattedTimeout
            )
        }

        else -> {
            stringResource(id = R.string.self_deleting_messages_team_setting_disabled)
        }
    }

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = hideDialogStatus,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = hideDialogStatus,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun GuestRoomLinkFeatureFlagDialog(
    isGuestRoomLinkEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val text: String =
        stringResource(id = if (isGuestRoomLinkEnabled) R.string.guest_room_link_enabled else R.string.guest_room_link_disabled)

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun TeamAppLockFeatureFlagDialog(
    isTeamAppLockEnabled: Boolean,
    onConfirm: () -> Unit,
) {
    val text: String =
        stringResource(id = if (isTeamAppLockEnabled) R.string.team_app_lock_enabled
        else R.string.team_app_lock_disabled)

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = {},
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun UpdateAppDialog(shouldShow: Boolean, onUpdateClick: () -> Unit) {
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
fun JoinConversationDialog(
    joinedDialogState: JoinConversationViaCodeState?,
    navigate: (NavigationCommand) -> Unit,
    onJoinConversationFlowCompleted: () -> Unit
) {
    joinedDialogState?.let { state ->

        val onComplete: (convId: ConversationId?) -> Unit = remember {
            {
                onJoinConversationFlowCompleted()
                it?.also {
                    navigate(
                        NavigationCommand(
                            ConversationScreenDestination(it),
                            BackStackMode.CLEAR_TILL_START
                        )
                    )
                }
            }
        }

        when (state) {
            is JoinConversationViaCodeState.Error -> JoinConversationViaInviteLinkError(
                errorState = state,
                onCancel = { onComplete(null) }
            )

            is JoinConversationViaCodeState.Show -> {
                JoinConversationViaDeepLinkDialog(
                    name = state.conversationName,
                    code = state.code,
                    domain = state.domain,
                    key = state.key,
                    requirePassword = state.passwordProtected,
                    onFlowCompleted = onComplete
                )
            }
        }
    }
}

@Composable
fun CustomBackendDialog(
    globalAppState: GlobalAppState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (globalAppState.customBackendDialog != null) {
        CustomServerDialog(
            serverLinksTitle = globalAppState.customBackendDialog.serverLinks.title,
            serverLinksApi = globalAppState.customBackendDialog.serverLinks.api,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
fun MaxAccountDialog(shouldShow: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (shouldShow) {
        MaxAccountReachedDialog(
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            buttonText = R.string.max_account_reached_dialog_button_open_profile
        )
    }
}

@Composable
fun AccountLoggedOutDialog(blockUserUI: CurrentSessionErrorState?, navigateAway: () -> Unit) {
    blockUserUI?.let {
        accountLoggedOutDialog(reason = it, navigateAway)
    }
}

@Composable
private fun accountLoggedOutDialog(reason: CurrentSessionErrorState, navigateAway: () -> Unit) {
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
fun NewClientDialog(
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

@PreviewMultipleThemes
@Composable
fun previewFileRestrictionDialog() {
    WireTheme {
        FileRestrictionDialog(true) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewGuestRoomLinkFeatureFlagDialog() {
    WireTheme {
        GuestRoomLinkFeatureFlagDialog(true) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewUpdateAppDialog() {
    WireTheme {
        UpdateAppDialog(true) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewJoinConversationDialogWithPassword() {
    WireTheme {
        JoinConversationDialog(
            JoinConversationViaCodeState.Show("convName", "code", "key", "domain", true),
            {}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewJoinConversationDialogWithoutPassword() {
    WireTheme {
        JoinConversationDialog(
            JoinConversationViaCodeState.Show("convName", "code", "key", "domain", false),
            {}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewJoinConversationDialogError() {
    WireTheme {
        JoinConversationDialog(
            JoinConversationViaCodeState.Error(CheckConversationInviteCodeUseCase.Result.Failure.InvalidCodeOrKey),
            {}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewCustomBackendDialog() {
    WireTheme {
        CustomBackendDialog(
            GlobalAppState(
                customBackendDialog = CustomServerDialogState(
                    ServerConfig.STAGING
                )
            ), {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun previewMaxAccountDialog() {
    WireTheme {
        MaxAccountDialog(true, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun previewAccountLoggedOutDialog() {
    WireTheme {
        AccountLoggedOutDialog(CurrentSessionErrorState.DeletedAccount) {}
    }
}
