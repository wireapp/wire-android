/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login.email

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.ui.authentication.login.AppLoginDialogError
import com.wire.android.ui.authentication.login.AppLoginState
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.common.dialogs.EmailAlreadyInUseClaimedDomainDialog
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.kalium.logic.data.user.UserId

@Composable
internal fun LoginEmailStateNavigationAndDialogs(
    state: AppLoginState,
    domainClaimedByOrg: DomainClaimedByOrg?,
    onClearLoginErrors: () -> Unit,
    onSuccess: (Boolean, Boolean, UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
) {
    val dialog = rememberVisibilityState<DomainClaimedByOrg.Claimed>()
    val navigate: (AppLoginState) -> Unit = { loginState ->
        when (val effect = loginTerminalEffect(loginState, null)) {
            is LoginTerminalEffect.Success -> onSuccess(effect.syncCompleted, effect.e2eiRequired, effect.userId)
            is LoginTerminalEffect.RemoveDevice -> {
                onClearLoginErrors()
                onRemoveDeviceNeeded(effect.userId)
            }
            else -> Unit
        }
    }
    LaunchedEffect(state) {
        when (val effect = loginTerminalEffect(state, domainClaimedByOrg as? DomainClaimedByOrg.Claimed)) {
            is LoginTerminalEffect.ShowClaimedDomain -> dialog.show(effect.domain)
            else -> navigate(state)
        }
    }
    if (state is LoginState.Error.DialogError<*, *>) {
        LoginErrorDialog((state as AppLoginDialogError).toLoginDialogErrorData(), onClearLoginErrors)
    }
    EmailAlreadyInUseClaimedDomainDialog(dialogState = dialog) {
        dialog.dismiss()
        navigate(state)
    }
}
