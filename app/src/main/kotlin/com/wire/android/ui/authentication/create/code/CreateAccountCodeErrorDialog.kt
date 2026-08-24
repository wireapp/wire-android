package com.wire.android.ui.authentication.create.code

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.authentication.create.code.CreateAccountCodeDialogKind.*
import com.wire.android.ui.authentication.create.code.dialogKind
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings

@Composable
fun CreateAccountCodeResult.Error.DialogError<com.wire.kalium.common.error.CoreFailure>.dialogResources(
    type: CreateAccountRouteFlowType,
): DialogErrorStrings = when (dialogKind(type)) {
    ACCOUNT_EXISTS -> dialog(R.string.create_account_email_already_in_use_error)
    BLACKLISTED -> dialog(R.string.create_account_email_blacklisted_error)
    DOMAIN_BLOCKED -> dialog(R.string.create_account_email_domain_blocked_error)
    INVALID_EMAIL -> dialog(R.string.create_account_email_invalid_error)
    TEAM_LIMIT -> dialog(R.string.create_account_code_error_team_members_limit_reached)
    PERSONAL_CREATION_RESTRICTED -> dialog(R.string.create_account_code_error_personal_account_creation_restricted)
    TEAM_CREATION_RESTRICTED -> dialog(R.string.create_account_code_error_team_creation_restricted)
    USER_EXISTS -> DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")
    GENERIC -> (this as CreateAccountCodeResult.Error.DialogError.GenericError).failure
        .dialogErrorStrings(LocalContext.current.resources)
}

@Composable
private fun dialog(message: Int): DialogErrorStrings = DialogErrorStrings(
    stringResource(R.string.create_account_code_error_title),
    stringResource(message),
)
