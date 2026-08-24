package com.wire.android.ui.authentication.create.code

import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType

enum class CreateAccountCodeDialogKind {
    ACCOUNT_EXISTS,
    BLACKLISTED,
    DOMAIN_BLOCKED,
    INVALID_EMAIL,
    TEAM_LIMIT,
    PERSONAL_CREATION_RESTRICTED,
    TEAM_CREATION_RESTRICTED,
    USER_EXISTS,
    GENERIC,
}

fun CreateAccountCodeResult.Error.DialogError<*>.dialogKind(
    flowType: CreateAccountRouteFlowType,
): CreateAccountCodeDialogKind = when (this) {
    CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError -> CreateAccountCodeDialogKind.ACCOUNT_EXISTS
    CreateAccountCodeResult.Error.DialogError.BlackListedError -> CreateAccountCodeDialogKind.BLACKLISTED
    CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError -> CreateAccountCodeDialogKind.DOMAIN_BLOCKED
    CreateAccountCodeResult.Error.DialogError.InvalidEmailError -> CreateAccountCodeDialogKind.INVALID_EMAIL
    CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError -> CreateAccountCodeDialogKind.TEAM_LIMIT
    CreateAccountCodeResult.Error.DialogError.CreationRestrictedError -> if (flowType == CreateAccountRouteFlowType.PERSONAL) {
        CreateAccountCodeDialogKind.PERSONAL_CREATION_RESTRICTED
    } else {
        CreateAccountCodeDialogKind.TEAM_CREATION_RESTRICTED
    }
    CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError -> CreateAccountCodeDialogKind.USER_EXISTS
    is CreateAccountCodeResult.Error.DialogError.GenericError<*> -> CreateAccountCodeDialogKind.GENERIC
}
