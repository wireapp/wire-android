/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.e2eiEnrollment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.wire.android.R
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.common.TextWithLearnMore
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentResult.Failure
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentResult.Success
import com.wire.android.ui.home.E2EIEnrollmentErrorWithDismissDialog
import com.wire.android.ui.home.E2EISuccessDialog
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.SupportPage
import com.wire.android.util.supportUrlResource
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult

/** Thin host adapter for session cancellation, shared dialogs and Kalium/OAuth result mapping. */
@Composable
internal fun E2EIEnrollmentRouteScreen(
    viewModel: E2EIEnrollmentViewModel,
    clearSessionViewModel: ClearSessionViewModel,
    switchAccountActions: SwitchAccountActions,
    onInitialSyncRequired: () -> Unit,
    onOpenCertificateDetails: (String) -> Unit,
) {
    val clearSessionState = clearSessionViewModel.state
    E2EIEnrollmentContent(
        state = viewModel.state,
        text = E2EIEnrollmentText(
            title = stringResource(R.string.end_to_end_identity_required_dialog_title),
            message = stringResource(R.string.end_to_end_identity_required_dialog_text_no_snooze),
            enrollLabel = stringResource(R.string.end_to_end_identity_required_dialog_positive_button),
            learnMoreUrl = supportUrlResource(SupportPage.E2EE_IDENTITY),
        ),
        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
        onEnroll = viewModel::enrollE2EICertificate,
        cancelDialog = {
            CancelEnrollmentDialog(
                state = clearSessionState,
                onCancel = { clearSessionViewModel.onCancelLoginClicked(switchAccountActions) },
                onProceed = clearSessionViewModel::onProceedLoginClicked,
            )
        },
        enrollmentRequest = { onResult ->
            GetE2EICertificateUI(
                enrollmentResultHandler = { result -> onResult(result.toFeatureResult()) },
                isNewClient = true,
            )
        },
        errorDialog = { onRetry, onDismiss ->
            E2EIEnrollmentErrorWithDismissDialog(
                isE2EILoading = viewModel.state.isLoading,
                onClick = onRetry,
                onDismiss = onDismiss,
            )
        },
        successDialog = { _, onOpenDetails, onDismiss, isFinalizing ->
            E2EISuccessDialog(
                openCertificateDetails = onOpenDetails,
                dismissDialog = onDismiss,
                isLoading = isFinalizing,
            )
        },
        learnMoreContent = { message, url -> E2EIEnrollmentLearnMoreMessage(message, url) },
        onEnrollmentResult = viewModel::handleE2EIEnrollmentResult,
        onDismissError = viewModel::dismissErrorDialog,
        onOpenCertificateDetails = onOpenCertificateDetails,
        onDismissSuccess = { viewModel.finalizeMLSClient(onInitialSyncRequired) },
    )
}

@Composable
private fun E2EIEnrollmentLearnMoreMessage(message: String, url: String) {
    val text = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = colorsScheme().onBackground,
                fontWeight = androidx.compose.material3.MaterialTheme.wireTypography.body01.fontWeight,
                fontSize = androidx.compose.material3.MaterialTheme.wireTypography.body01.fontSize,
                fontFamily = androidx.compose.material3.MaterialTheme.wireTypography.body01.fontFamily,
                fontStyle = androidx.compose.material3.MaterialTheme.wireTypography.body01.fontStyle,
            ),
        ) { append(message) }
    }
    TextWithLearnMore(textAnnotatedString = text, learnMoreLink = url)
}

@Composable
private fun CancelEnrollmentDialog(
    state: ClearSessionState,
    onCancel: () -> Unit,
    onProceed: () -> Unit,
) {
    val dialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(dialogState, onCancel, onProceed)
    if (state.showCancelLoginDialog) dialogState.show(dialogState.savedState ?: CancelLoginDialogState)
    else dialogState.dismiss()
}

private fun FinalizeEnrollmentResult.toFeatureResult(): E2EIEnrollmentResult = when (this) {
    is FinalizeEnrollmentResult.Success -> Success(certificate)
    is FinalizeEnrollmentResult.Failure -> Failure
}
