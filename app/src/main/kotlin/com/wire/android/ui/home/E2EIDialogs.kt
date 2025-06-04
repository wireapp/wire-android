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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.toTimeLongLabelUiText
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@Composable
fun E2EIRequiredDialog(
    e2EIRequired: FeatureFlagState.E2EIRequired,
    isE2EILoading: Boolean,
    getCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit,
) {
    when (e2EIRequired) {
        FeatureFlagState.E2EIRequired.NoGracePeriod.Create -> E2EIRequiredNoSnoozeDialog(
            isLoading = isE2EILoading,
            getCertificate = getCertificate
        )

        FeatureFlagState.E2EIRequired.NoGracePeriod.Renew -> E2EIRenewNoSnoozeDialog(
            isLoading = isE2EILoading,
            updateCertificate = getCertificate
        )

        is FeatureFlagState.E2EIRequired.WithGracePeriod.Create -> E2EIRequiredWithSnoozeDialog(
            isLoading = isE2EILoading,
            getCertificate = getCertificate,
            snoozeDialog = { snoozeDialog(e2EIRequired) }
        )

        is FeatureFlagState.E2EIRequired.WithGracePeriod.Renew -> E2EIRenewWithSnoozeDialog(
            isLoading = isE2EILoading,
            updateCertificate = getCertificate,
            snoozeDialog = { snoozeDialog(e2EIRequired) }
        )
    }
}

@Composable
fun E2EIResultDialog(
    result: FeatureFlagState.E2EIResult,
    isE2EILoading: Boolean,
    updateCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit,
    openCertificateDetails: (String) -> Unit,
    dismissSuccessDialog: () -> Unit
) {
    when (result) {
        is FeatureFlagState.E2EIResult.Failure -> E2EIRenewErrorDialog(
            e2EIRequired = result.e2EIRequired,
            isE2EILoading = isE2EILoading,
            updateCertificate = updateCertificate,
            snoozeDialog = snoozeDialog
        )

        is FeatureFlagState.E2EIResult.Success -> E2EISuccessDialog(
            openCertificateDetails = { openCertificateDetails(result.certificate) },
            dismissDialog = dismissSuccessDialog
        )
    }
}

@Composable
fun E2EIRenewErrorDialog(
    e2EIRequired: FeatureFlagState.E2EIRequired,
    isE2EILoading: Boolean,
    updateCertificate: () -> Unit,
    snoozeDialog: (FeatureFlagState.E2EIRequired.WithGracePeriod) -> Unit,
) {
    when (e2EIRequired) {
        is FeatureFlagState.E2EIRequired.NoGracePeriod -> E2EIErrorNoSnoozeDialog(
            isE2EILoading = isE2EILoading,
            updateCertificate = updateCertificate
        )

        is FeatureFlagState.E2EIRequired.WithGracePeriod -> E2EIErrorWithSnoozeDialog(
            updateCertificate = updateCertificate,
            isE2EILoading = isE2EILoading,
            snoozeDialog = { snoozeDialog(e2EIRequired) }
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
    )
}

@Composable
fun E2EISuccessDialog(
    openCertificateDetails: () -> Unit,
    dismissDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireDialog(
        modifier = modifier,
        title = stringResource(id = R.string.end_to_end_identity_renew_success_dialog_title),
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
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing16x))

                Image(
                    modifier = Modifier
                        .width(MaterialTheme.wireDimensions.spacing64x)
                        .height(MaterialTheme.wireDimensions.spacing64x),
                    painter = painterResource(id = R.drawable.ic_certificate_valid_mls),
                    contentDescription = "",
                )

                Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing16x))

                Text(
                    text = stringResource(id = R.string.end_to_end_identity_renew_success_dialog_text),
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = MaterialTheme.wireDimensions.dialogTextsSpacing),
                )
            }
        }
    )
}

@Composable
fun E2EIUpdateErrorWithDismissDialog(
    isE2EILoading: Boolean,
    updateCertificate: () -> Unit,
    onDismiss: () -> Unit
) {
    E2EIErrorWithDismissDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_text),
        isE2EILoading = isE2EILoading,
        updateCertificate = updateCertificate,
        onDismiss = onDismiss
    )
}

@Composable
fun E2EIEnrollmentErrorWithDismissDialog(
    isE2EILoading: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    E2EIErrorWithDismissDialog(
        title = stringResource(id = R.string.end_to_end_identity_enrollment_error_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_enrollment_error_dialog_text),
        isE2EILoading = isE2EILoading,
        updateCertificate = onClick,
        onDismiss = onDismiss
    )
}

@Composable
private fun E2EIErrorWithDismissDialog(
    title: String,
    text: String,
    isE2EILoading: Boolean,
    updateCertificate: () -> Unit,
    onDismiss: () -> Unit
) {
    WireDialog(
        title = title,
        text = text,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.label_retry),
            type = WireDialogButtonType.Primary,
            loading = isE2EILoading
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
    )
}

@Composable
private fun E2EIErrorWithSnoozeDialog(
    isE2EILoading: Boolean,
    updateCertificate: () -> Unit,
    snoozeDialog: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_error_dialog_text),
        onDismiss = snoozeDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.label_retry),
            type = WireDialogButtonType.Primary,
            loading = isE2EILoading
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = snoozeDialog,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
    )
}

@Composable
private fun E2EIErrorNoSnoozeDialog(
    isE2EILoading: Boolean,
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
            loading = isE2EILoading
        ),
        properties = wireDialogPropertiesBuilder(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
        )
    )
}

@Composable
private fun E2EIRequiredWithSnoozeDialog(
    isLoading: Boolean,
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
            loading = isLoading
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = snoozeDialog,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_snooze_button),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
    )
}

@Composable
private fun E2EIRequiredNoSnoozeDialog(isLoading: Boolean, getCertificate: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_required_dialog_text_no_snooze),
        onDismiss = getCertificate,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = getCertificate,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_positive_button),
            type = WireDialogButtonType.Primary,
            loading = isLoading
        ),
        buttonsHorizontalAlignment = false,
        properties = wireDialogPropertiesBuilder(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
        )
    )
}

@Composable
private fun E2EIRenewWithSnoozeDialog(
    isLoading: Boolean,
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
            loading = isLoading
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = snoozeDialog,
            text = stringResource(id = R.string.end_to_end_identity_required_dialog_snooze_button),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
    )
}

@Composable
private fun E2EIRenewNoSnoozeDialog(isLoading: Boolean, updateCertificate: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_renew_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_renew_dialog_text_no_snooze),
        onDismiss = updateCertificate,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = updateCertificate,
            text = stringResource(id = R.string.end_to_end_identity_renew_dialog_positive_button),
            type = WireDialogButtonType.Primary,
            loading = isLoading
        ),
        buttonsHorizontalAlignment = false,
        properties = wireDialogPropertiesBuilder(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
        )
    )
}

@Composable
fun E2EICertificateRevokedDialog(
    onLogout: () -> Unit,
    onContinue: () -> Unit
) {
    WireDialog(
        title = stringResource(id = R.string.end_to_end_identity_certificate_revoked_dialog_title),
        text = stringResource(id = R.string.end_to_end_identity_certificate_revoked_dialog_description),
        onDismiss = onContinue,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onLogout,
            text = stringResource(id = R.string.end_to_end_identity_certificate_revoked_dialog_button_logout),
            type = WireDialogButtonType.Primary
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onContinue,
            text = stringResource(id = R.string.end_to_end_identity_certificate_revoked_dialog_button_continue),
            type = WireDialogButtonType.Secondary,
        ),
        buttonsHorizontalAlignment = false,
        properties = wireDialogPropertiesBuilder(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
        )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EICertificateRevokedDialog() {
    E2EICertificateRevokedDialog({}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIdRequiredWithSnoozeDialog() {
    WireTheme {
        E2EIRequiredWithSnoozeDialog(false, {}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIdRequiredNoSnoozeDialog() {
    WireTheme {
        E2EIRequiredNoSnoozeDialog(false) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIdRenewRequiredWithSnoozeDialog() {
    WireTheme {
        E2EIRenewWithSnoozeDialog(false, {}) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIdRenewRequiredNoSnoozeDialog() {
    WireTheme {
        E2EIRenewNoSnoozeDialog(false) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIdSnoozeDialog() {
    WireTheme {
        E2EISnoozeDialog(2.seconds) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIRenewErrorDialogNoGracePeriod() {
    WireTheme {
        E2EIRenewErrorDialog(FeatureFlagState.E2EIRequired.NoGracePeriod.Renew, false, { }) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIRenewErrorDialogWithGracePeriod() {
    WireTheme {
        E2EIRenewErrorDialog(FeatureFlagState.E2EIRequired.WithGracePeriod.Renew(2.days), false, { }) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EISuccessDialog() {
    WireTheme {
        E2EISuccessDialog({}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIRenewErrorNoSnoozeDialog() {
    WireTheme {
        E2EIErrorNoSnoozeDialog(false) { }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIRenewErrorWithSnoozeDialog() {
    WireTheme {
        E2EIErrorWithSnoozeDialog(isE2EILoading = false, updateCertificate = {}) { }
    }
}
