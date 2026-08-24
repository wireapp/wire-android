/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.authentication.BackendConfigSuccessContent
import com.wire.android.ui.authentication.MissingBackendConfigContent
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.sso.SsoIdentityChangedDialog
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialog
import com.wire.kalium.logic.configuration.server.ServerConfig

/** Navigation- and host-specific adapter for the feature-owned new-login content. */
@Composable
internal fun NewLoginRouteScreen(
    navArgs: LoginNavArgs,
    viewModel: AppNewLoginViewModel,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
    onAction: (AppNewLoginAction) -> Unit,
) {
    LaunchedEffect(navArgs.ssoCodeAutoLogin) {
        navArgs.ssoCodeAutoLogin?.let {
            viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(it.ssoCode)
            if (it.autoInitiateLogin) viewModel.onLoginStarted()
        }
    }
    (viewModel.state.flowState as? NewLoginFlowState.CustomConfigDialog<ServerConfig.Links>)?.let { dialogState ->
        CustomServerDetailsDialog(
            serverLinks = dialogState.serverLinks,
            onDismiss = viewModel::onDismissDialog,
            onConfirm = { viewModel.onCustomServerDialogConfirm(dialogState.serverLinks) },
        )
    }
    (viewModel.state.flowState as? AppNewLoginDialogError)?.let { dialogState ->
        LoginErrorDialog(dialogState.toLoginDialogErrorData(), viewModel::onDismissDialog)
    }
    if (viewModel.state.flowState == NewLoginFlowState.SsoIdentityChanged) {
        SsoIdentityChangedDialog(
            cancelLabel = stringResource(R.string.label_cancel),
            onDismiss = viewModel::onSsoIdentityChangeDismissed,
            onConfirm = viewModel::onSsoIdentityChangeConfirmed,
        )
    }

    NewLoginContent(
        presentation = viewModel.state.toPresentation(),
        userIdentifierState = viewModel.userIdentifierTextState,
        onNextClicked = viewModel::onLoginStarted,
        header = {
            NewLoginHeader(
                serverConfig = viewModel.serverConfig,
                presentation = viewModel.state.toPresentation(),
                canNavigateBack = canNavigateBack,
                navigateBack = navigateBack,
            )
        },
        topBar = {
            if (BuildConfig.PRIVATE_BUILD) {
                BackendSelectorDropDown(onNoBackendSelected = viewModel::onNoBackendSelected)
            }
        },
        configurationContent = {
            when (viewModel.state.flowState) {
                NewLoginFlowState.BackendConfigSuccess ->
                    BackendConfigSuccessContent(onContinue = viewModel::onBackendConfigSuccessContinue)

                else -> MissingBackendConfigContent(
                    errorText = if (viewModel.state.flowState == NewLoginFlowState.BackendConfigError) {
                        stringResource(R.string.missing_backend_config_error)
                    } else {
                        null
                    },
                    isLoading = viewModel.state.flowState == NewLoginFlowState.LoadingBackendConfig,
                    onConfigurationLinkEntered = viewModel::onBackendConfigLinkEntered,
                )
            }
        },
    )
    HandleActions(viewModel.actions, onAction)
}
