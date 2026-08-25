/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.code

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.user.UserId

@Composable
internal fun LegacyRegistrationCodeErrorDialog(
    result: LegacyRegistrationCodeState.Result<UserId, CoreFailure>,
    onDismiss: () -> Unit,
) {
    val dialogText = result.dialogText() ?: return
    WireDialog(
        title = dialogText.title,
        text = dialogText.annotatedMessage,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.label_ok),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@Composable
private fun LegacyRegistrationCodeState.Result<UserId, CoreFailure>.dialogText(): DialogErrorStrings? = when (this) {
    LegacyRegistrationCodeState.Result.AccountAlreadyExists -> dialogText(AuthenticationR.string.create_account_email_already_in_use_error)
    LegacyRegistrationCodeState.Result.Blacklisted -> dialogText(AuthenticationR.string.create_account_email_blacklisted_error)
    LegacyRegistrationCodeState.Result.DomainBlocked -> dialogText(AuthenticationR.string.create_account_email_domain_blocked_error)
    LegacyRegistrationCodeState.Result.InvalidEmail -> dialogText(AuthenticationR.string.create_account_email_invalid_error)
    LegacyRegistrationCodeState.Result.TeamMembersLimit ->
        dialogText(AuthenticationR.string.create_account_code_error_team_members_limit_reached)
    LegacyRegistrationCodeState.Result.CreationRestricted ->
        dialogText(AuthenticationR.string.create_account_code_error_personal_account_creation_restricted)
    LegacyRegistrationCodeState.Result.UserAlreadyExists -> DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")
    is LegacyRegistrationCodeState.Result.Generic -> failure.dialogErrorStrings(LocalContext.current.resources)
    else -> null
}

@Composable
private fun dialogText(messageId: Int): DialogErrorStrings = DialogErrorStrings(
    title = stringResource(AuthenticationR.string.create_account_code_error_title),
    message = stringResource(messageId),
)
