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
package com.wire.android.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.toTimeLongLabelUiText
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlin.time.Duration.Companion.seconds

@Composable
fun FileRestrictionDialog(
    isFileSharingEnabled: Boolean,
    hideDialogStatus: () -> Unit,
) {
    val text: String = stringResource(id = if (isFileSharingEnabled) R.string.sharing_files_enabled else R.string.sharing_files_disabled)

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
            stringResource(R.string.self_deleting_messages_team_setting_enabled_enforced_timeout, formattedTimeout)
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
fun WelcomeNewUserDialog(
    dismissDialog: () -> Unit,
    context: Context = LocalContext.current
) {
    val welcomeToNewAndroidUrl = stringResource(id = R.string.url_welcome_to_new_android)
    WireDialog(
        title = stringResource(id = R.string.welcome_migration_dialog_title),
        text = stringResource(id = R.string.welcome_migration_dialog_content),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                dismissDialog.invoke()
                CustomTabsHelper.launchUrl(context, welcomeToNewAndroidUrl)
            },
            text = stringResource(id = R.string.label_learn_more),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.welcome_migration_dialog_continue),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun E2EIRequiredDialog(
    result: FeatureFlagState.E2EIRequired,
    getCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit,
) {
    when (result) {
        FeatureFlagState.E2EIRequired.NoGracePeriod -> E2EIdRequiredNoSnoozeDialog(getCertificate = getCertificate)
        is FeatureFlagState.E2EIRequired.WithGracePeriod -> E2EIdRequiredWithSnoozeDialog(
            result = result,
            getCertificate = getCertificate,
            snoozeDialog = snoozeDialog
        )
    }
}

@Composable
fun E2EIdRequiredWithSnoozeDialog(
    result: FeatureFlagState.E2EIRequired.WithGracePeriod,
    getCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_required_dialog_text),
        onDismiss = { snoozeDialog(result) },
        optionButton1Properties = WireDialogButtonProperties(
            onClick = getCertificate,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_positive_button),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = { snoozeDialog(result) },
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_snooze_button),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun E2EIdRequiredNoSnoozeDialog(getCertificate: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_required_dialog_text_no_snooze),
        onDismiss = getCertificate,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = getCertificate,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_positive_button),
            type = WireDialogButtonType.Primary,
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun E2EIdSnoozeDialog(
    state: FeatureFlagState.E2EISnooze,
    dismissDialog: () -> Unit
) {
    val timeText = state.timeLeft.toTimeLongLabelUiText().asString()
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_snooze_dialog_text, timeText),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdRequiredWithSnoozeDialog() {
    WireTheme {
        E2EIdRequiredWithSnoozeDialog(FeatureFlagState.E2EIRequired.WithGracePeriod(2.seconds), {}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdRequiredNoSnoozeDialog() {
    WireTheme {
        E2EIdRequiredNoSnoozeDialog {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdSnoozeDialog() {
    WireTheme {
        E2EIdSnoozeDialog(FeatureFlagState.E2EISnooze(2.seconds)) {}
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
fun previewWelcomeNewUserDialog() {
    WireTheme {
        WelcomeNewUserDialog({})
    }
}
