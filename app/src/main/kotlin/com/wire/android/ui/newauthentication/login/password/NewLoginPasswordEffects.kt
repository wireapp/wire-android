/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login.password

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.ui.authentication.login.AppLoginDialogError
import com.wire.android.ui.authentication.login.AppLoginState
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.common.dialogs.EmailAlreadyInUseClaimedDomainDialog
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.kalium.logic.data.user.UserId

/** Remains host-owned because actions carry concrete [UserId] values and drive route mutation. */
@Composable
internal fun LoginStateNavigationAndDialogs(
    viewModel: AppLoginEmailViewModel,
    onAction: (NewLoginPasswordScreenAction) -> Boolean,
) {
    val state = viewModel.loginState.flowState
    val claimedDomainDialog = rememberVisibilityState<DomainClaimedByOrg.Claimed>()
    val handleState: (AppLoginState) -> Boolean = { loginState ->
        when (val terminal = newLoginPasswordTerminal(loginState)) {
            is NewLoginPasswordTerminal.Success -> onAction(
                NewLoginPasswordScreenAction.Success(
                    terminal.syncCompleted,
                    terminal.e2eiRequired,
                    terminal.userId,
                ),
            )
            is NewLoginPasswordTerminal.RemoveDevice -> {
                val accepted = onAction(NewLoginPasswordScreenAction.RemoveDevice(terminal.userId))
                if (accepted) viewModel.clearLoginErrors()
                accepted
            }
            NewLoginPasswordTerminal.Canceled -> onAction(NewLoginPasswordScreenAction.Canceled)
            else -> false
        }
    }
    LaunchedEffect(state) {
        val claimed = viewModel.domainClaimedByOrg
        val completed = state is LoginState.Success<*> || state is LoginState.Error.TooManyDevicesError<*>
        if (completed && claimed is DomainClaimedByOrg.Claimed) claimedDomainDialog.show(claimed) else handleState(state)
    }
    if (state is LoginState.Error.DialogError<*, *>) {
        LoginErrorDialog((state as AppLoginDialogError).toLoginDialogErrorData(), viewModel::clearLoginErrors)
    }
    EmailAlreadyInUseClaimedDomainDialog(
        dialogState = claimedDomainDialog,
        onDismiss = {
            claimedDomainDialog.dismiss()
            handleState(state)
        },
    )
}
