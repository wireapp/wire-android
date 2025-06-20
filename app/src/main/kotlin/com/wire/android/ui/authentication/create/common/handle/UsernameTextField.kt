/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.authentication.create.common.handle

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.animation.ShakeAnimation
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.forceLowercase
import com.wire.android.ui.common.textfield.maxLengthWithCallback
import com.wire.android.ui.common.textfield.patternWithCallback
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.Patterns

@Composable
fun UsernameTextField(
    errorState: HandleUpdateErrorState,
    username: TextFieldState,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (errorState is HandleUpdateErrorState.DialogError.GenericError) {
        CoreFailureErrorDialog(errorState.coreFailure, onErrorDismiss)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    ShakeAnimation { animate ->
        WireTextField(
            textState = username,
            placeholderText = stringResource(R.string.create_account_username_placeholder),
            labelText = stringResource(R.string.create_account_username_label),
            inputTransformation = InputTransformation
                .forceLowercase()
                .patternWithCallback(Patterns.HANDLE, animate)
                .maxLengthWithCallback(255, animate),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mention),
                    contentDescription = stringResource(R.string.content_description_mention_icon),
                    modifier = Modifier.padding(
                        start = MaterialTheme.wireDimensions.spacing16x,
                        end = MaterialTheme.wireDimensions.spacing8x
                    )
                )
            },
            state = if (errorState is HandleUpdateErrorState.TextFieldError) when (errorState) {
                HandleUpdateErrorState.TextFieldError.UsernameTakenError ->
                    WireTextFieldState.Error(stringResource(id = R.string.create_account_username_taken_error))

                HandleUpdateErrorState.TextFieldError.UsernameInvalidError ->
                    WireTextFieldState.Error(stringResource(id = R.string.create_account_username_description))
            } else WireTextFieldState.Default,
            descriptionText = stringResource(id = R.string.create_account_username_description),
            keyboardOptions = KeyboardOptions.DefaultEmailDone,
            onKeyboardAction = { keyboardController?.hide() },
            modifier = modifier.then(Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x))
        )
    }
}
