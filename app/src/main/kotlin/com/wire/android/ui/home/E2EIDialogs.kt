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
 */
package com.wire.android.ui.home

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.toTimeLongLabelUiText
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun E2EIRequiredDialog(
    result: FeatureFlagState.E2EIRequired,
    getCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit,
) {
    when (result) {
        FeatureFlagState.E2EIRequired.NoGracePeriod.Create -> E2EIRequiredNoSnoozeDialog(getCertificate = getCertificate)
        FeatureFlagState.E2EIRequired.NoGracePeriod.Renew -> E2EIRenewNoSnoozeDialog(updateCertificate = getCertificate)
        is FeatureFlagState.E2EIRequired.WithGracePeriod.Create -> E2EIRequiredWithSnoozeDialog(
            getCertificate = getCertificate,
            snoozeDialog = { snoozeDialog(result) }
        )

        is FeatureFlagState.E2EIRequired.WithGracePeriod.Renew -> E2EIRenewWithSnoozeDialog(
            updateCertificate = getCertificate,
            snoozeDialog = { snoozeDialog(result) }
        )
    }
}

@Composable
fun E2EIRenewErrorDialog(
    result: FeatureFlagState.E2EIRequired,
    updateCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit,
) {
    when (result) {
        is FeatureFlagState.E2EIRequired.NoGracePeriod -> E2EIErrorNoSnoozeDialog(updateCertificate = updateCertificate)
        is FeatureFlagState.E2EIRequired.WithGracePeriod -> E2EIErrorWithSnoozeDialog(
            updateCertificate = updateCertificate,
            snoozeDialog = { snoozeDialog(result) }
        )
    }
}

@Composable
fun E2EISnoozeDialog(
    timeLeft: Duration,
    dismissDialog: () -> Unit
) {
    val timeText = timeLeft.toTimeLongLabelUiText().asString()
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_snooze_dialog_text, timeText),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        ),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun E2EISuccessDialog(
    openCertificateDetails: () -> Unit,
    dismissDialog: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_dialog_text),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = openCertificateDetails,
            text = stringResource(id = R.string.end_to_end_identity_renew_success_dialog_second_button),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
        centerContent = true,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Image(
                painter = painterResource(id = R.drawable.ic_certificate_valid_mls),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier
            )
        }
    )
}

@Composable
private fun E2EIErrorWithSnoozeDialog(
    updateCertificate: () -> Unit,
    snoozeDialog: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_text),
        onDismiss = updateCertificate,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.label_retry),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = snoozeDialog,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun E2EIErrorNoSnoozeDialog(
    updateCertificate: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_text_no_snooze),
        onDismiss = updateCertificate,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.label_retry),
            type = WireDialogButtonType.Primary,
        ),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
private fun E2EIRequiredWithSnoozeDialog(
    getCertificate: () -> Unit,
    snoozeDialog: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_required_dialog_text),
        onDismiss = snoozeDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = getCertificate,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_positive_button),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = snoozeDialog,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_snooze_button),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun E2EIRequiredNoSnoozeDialog(getCertificate: () -> Unit) {
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
private fun E2EIRenewWithSnoozeDialog(
    updateCertificate: () -> Unit,
    snoozeDialog: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_dialog_text),
        onDismiss = snoozeDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.end_to_end_identity_renew_dialog_positive_button),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = snoozeDialog,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_snooze_button),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun E2EIRenewNoSnoozeDialog(updateCertificate: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_dialog_text_no_snooze),
        onDismiss = updateCertificate,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.end_to_end_identity_renew_dialog_positive_button),
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

@PreviewMultipleThemes
@Composable
fun previewE2EIdRequiredWithSnoozeDialog() {
    WireTheme {
        E2EIRequiredWithSnoozeDialog({}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdRequiredNoSnoozeDialog() {
    WireTheme {
        E2EIRequiredNoSnoozeDialog {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdRenewRequiredWithSnoozeDialog() {
    WireTheme {
        E2EIRenewWithSnoozeDialog({}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdRenewRequiredNoSnoozeDialog() {
    WireTheme {
        E2EIRenewNoSnoozeDialog {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIdSnoozeDialog() {
    WireTheme {
        E2EISnoozeDialog(2.seconds) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EISuccessDialog() {
    WireTheme {
        E2EISuccessDialog({ }) {}
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIRenewErrorNoSnoozeDialog() {
    WireTheme {
        E2EIErrorNoSnoozeDialog { }
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIRenewErrorWithSnoozeDialog() {
    WireTheme {
        E2EIErrorWithSnoozeDialog(updateCertificate = {}) { }
    }
}
