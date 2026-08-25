/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.newauthentication.login

fun <LinksT, FailureT, SsoFailureT> NewLoginScreenState<LinksT, FailureT, SsoFailureT>.toPresentation(): NewLoginContentPresentation =
    NewLoginContentPresentation(
        mode = when (flowState) {
            NewLoginFlowState.MissingBackendConfig,
            NewLoginFlowState.LoadingBackendConfig,
            NewLoginFlowState.BackendConfigError -> NewLoginContentMode.BackendConfiguration
            NewLoginFlowState.BackendConfigSuccess -> NewLoginContentMode.BackendConfigurationSuccess
            else -> NewLoginContentMode.Identifier
        },
        nextEnabled = nextEnabled,
        loading = flowState is NewLoginFlowState.Loading,
        invalidIdentifier = flowState is NewLoginFlowState.Error.TextFieldError.InvalidValue,
    )
