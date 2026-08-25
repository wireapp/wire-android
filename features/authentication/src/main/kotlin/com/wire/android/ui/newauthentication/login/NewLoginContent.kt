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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState

@Immutable
data class NewLoginContentPresentation(
    val mode: NewLoginContentMode,
    val nextEnabled: Boolean,
    val loading: Boolean,
    val invalidIdentifier: Boolean,
)

enum class NewLoginContentMode {
    Identifier,
    BackendConfiguration,
    BackendConfigurationSuccess,
}

/**
 * Feature-owned shell and identifier form. The app provides the header and backend
 * configuration bodies because those depend on host configuration and dialogs.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewLoginContent(
    presentation: NewLoginContentPresentation,
    userIdentifierState: TextFieldState,
    onNextClicked: () -> Unit,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    configurationContent: @Composable ColumnScope.() -> Unit = {},
) {
    NewAuthContainer(
        header = header,
        modifier = modifier,
        topBar = topBar,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = dimensions().spacing16x)
                    .semantics { testTagsAsResourceId = true },
            ) {
                if (presentation.mode == NewLoginContentMode.Identifier) {
                    EmailOrSSOCodeInput(
                        userIdentifierState = userIdentifierState,
                        error = presentation.invalidIdentifier.then {
                            stringResource(R.string.enterprise_login_error_invalid_user_identifier)
                        },
                    )
                    VerticalSpace.x8()
                    LoginNextButton(
                        loading = presentation.loading,
                        enabled = presentation.nextEnabled,
                        onClick = onNextClicked,
                    )
                } else {
                    configurationContent()
                }
            }
        }
    }
}

@Composable
fun LoginNextButton(
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = stringResource(R.string.enterprise_login_next),
            onClick = onClick,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginButton"),
        )
    }
}

@Composable
fun EmailOrSSOCodeInput(
    userIdentifierState: TextFieldState,
    error: String?,
    modifier: Modifier = Modifier,
) {
    WireTextField(
        autoFillType = WireAutoFillType.Login,
        textState = userIdentifierState,
        placeholderText = stringResource(R.string.enterprise_login_user_identifier_label_placeholder),
        labelText = stringResource(R.string.enterprise_login_user_identifier_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        semanticDescription = stringResource(R.string.content_description_enterprise_login_email_field),
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = modifier.testTag("emailField"),
        testTag = "userIdentifierInput",
    )
}

private inline fun Boolean.then(value: () -> String): String? = if (this) value() else null
