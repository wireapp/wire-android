/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.settings.mfa.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.mfa.MFACodeInput
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.kalium.logic.data.mfa.MFAConstraints.AUTHENTICATION_CODE_LENGTH
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun MFACredentialsScreen(
    title: String,
    heading: String,
    description: String,
    password: TextFieldState,
    code: TextFieldState,
    isBusy: Boolean,
    actionAvailable: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    isCodeInvalid: Boolean = false,
) {
    BackHandler(onBack = onBack)
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = title,
                onNavigationPressed = onBack,
                elevation = dimensions().spacing0x,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            Text(
                text = heading,
                style = MaterialTheme.wireTypography.title01,
            )
            Text(
                text = description,
                style = MaterialTheme.wireTypography.body01,
            )
            val fieldState = if (isBusy) WireTextFieldState.Disabled else WireTextFieldState.Default
            WirePasswordTextField(textState = password, state = fieldState)
            MFACodeInput(
                code = code,
                isBusy = isBusy,
                isInvalid = isCodeInvalid,
                canSubmit = actionAvailable && password.text.isNotBlank(),
                onCodeComplete = onConfirm,
            )
            errorText?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.error,
                )
            }
        }
    }
}

internal fun canSubmitMFACredentials(password: CharSequence, code: CharSequence): Boolean =
    password.isNotBlank() && code.length == AUTHENTICATION_CODE_LENGTH && code.all { it in '0'..'9' }
