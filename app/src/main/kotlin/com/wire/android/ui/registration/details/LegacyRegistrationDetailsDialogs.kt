/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.common.error.CoreFailure

@Composable
internal fun LegacyRegistrationDetailsDialogs(
    state: LegacyRegistrationDetailsState<CoreFailure>,
    tosUrl: String,
    onTermsDismiss: () -> Unit,
    onTermsAccept: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    if (state.termsDialogVisible) {
        val context = LocalContext.current
        LegacyRegistrationTermsDialog(
            onDismiss = onTermsDismiss,
            onAccept = onTermsAccept,
            onViewPolicy = { CustomTabsHelper.launchUrl(context, tosUrl) },
        )
    }
    val error = state.error as? LegacyRegistrationDetailsState.DetailsError.GenericError
    val failure = error?.failure as? CoreFailure
    if (failure != null) CoreFailureErrorDialog(failure, onErrorDismiss)
}

@Composable
private fun LegacyRegistrationTermsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onViewPolicy: () -> Unit,
) {
    WireDialog(
        title = stringResource(AuthenticationR.string.create_account_email_terms_dialog_title),
        text = stringResource(AuthenticationR.string.create_account_email_terms_dialog_text),
        onDismiss = onDismiss,
    ) {
        Column {
            WireSecondaryButton(
                text = stringResource(R.string.label_cancel),
                onClick = onDismiss,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = androidx.compose.material3.MaterialTheme.wireDimensions.spacing8x)
                    .testTag("cancelButton"),
            )
            WireSecondaryButton(
                text = stringResource(AuthenticationR.string.create_account_email_terms_dialog_view_policy),
                onClick = onViewPolicy,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = androidx.compose.material3.MaterialTheme.wireDimensions.spacing8x)
                    .testTag("viewTC"),
            )
            WirePrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = onAccept,
                fillMaxWidth = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
