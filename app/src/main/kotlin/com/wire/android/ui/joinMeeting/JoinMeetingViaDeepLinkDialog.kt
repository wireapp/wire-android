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
package com.wire.android.ui.joinMeeting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wire.android.R
import com.wire.android.ui.common.DialogTextSuffixLink
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.common.R as CommonR

@Composable
fun JoinMeetingViaDeepLinkDialog(
    state: JoinMeetingDialogState,
    passwordTextState: TextFieldState,
    onJoin: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        JoinMeetingDialogState.Idle, JoinMeetingDialogState.Loading, JoinMeetingDialogState.Success -> Unit
        is JoinMeetingDialogState.Error -> JoinMeetingErrorDialog(
            state = state,
            onRetry = onRetry,
            onDismiss = onDismiss,
        )
        is JoinMeetingDialogState.PasswordRequired -> MeetingPasswordDialog(
            loading = state.loading,
            error = state.error,
            passwordTextState = passwordTextState,
            onJoin = onJoin,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun JoinMeetingErrorDialog(
    state: JoinMeetingDialogState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (state) {
        JoinMeetingDialogState.Error.NotFound -> R.string.join_meeting_not_found_title
        JoinMeetingDialogState.Error.Unavailable -> R.string.join_meeting_unavailable_title
        JoinMeetingDialogState.Error.Generic, JoinMeetingDialogState.Error.InvalidPassword -> R.string.join_meeting_title
    }
    WireDialog(
        title = stringResource(title),
        text = stringResource(state.descriptionResource()),
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = true,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.label_cancel),
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onRetry,
            text = stringResource(R.string.label_retry),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@Composable
private fun MeetingPasswordDialog(
    loading: Boolean,
    error: JoinMeetingDialogState.Error?,
    passwordTextState: TextFieldState,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val wrongPassword = error == JoinMeetingDialogState.Error.InvalidPassword
    val joinDisabled = loading || wrongPassword || passwordTextState.text.isBlank()
    WireDialog(
        title = stringResource(R.string.join_meeting_password_title),
        text = stringResource(R.string.join_meeting_password_description) + "\n\n",
        onDismiss = onDismiss,
        textSuffixLink = DialogTextSuffixLink(
            linkText = stringResource(R.string.join_meeting_learn_more),
            linkUrl = stringResource(CommonR.string.url_meeting_guest_links)
        ),
        buttonsHorizontalAlignment = true,
        dismissButtonProperties = WireDialogButtonProperties(text = stringResource(R.string.label_cancel), onClick = onDismiss),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onJoin,
            text = stringResource(R.string.join_conversation_dialog_button),
            type = WireDialogButtonType.Primary,
            state = if (joinDisabled) WireButtonState.Disabled else WireButtonState.Default,
            loading = loading,
        ),
        properties = wireDialogPropertiesBuilder(dismissOnBackPress = false, dismissOnClickOutside = false),
        content = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }
            WirePasswordTextField(
                textState = passwordTextState,
                labelText = stringResource(R.string.join_meeting_password_label),
                placeholderText = stringResource(R.string.join_meeting_password_title),
                trailingIcon = {
                    ClearInputIconButton(state = passwordTextState, enabled = !loading)
                },
                state = when {
                    loading -> WireTextFieldState.Disabled
                    error != null -> WireTextFieldState.Error(stringResource(error.descriptionResource()))
                    else -> WireTextFieldState.Default
                },
                keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                onKeyboardAction = { keyboardController?.hide() },
                modifier = Modifier.focusRequester(focusRequester),
                autoFill = false,
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    )
}

private fun JoinMeetingDialogState.Error.descriptionResource(): Int = when (this) {
    JoinMeetingDialogState.Error.InvalidPassword -> R.string.remove_device_invalid_password
    JoinMeetingDialogState.Error.NotFound -> R.string.join_meeting_not_found_description
    JoinMeetingDialogState.Error.Unavailable -> R.string.join_meeting_unavailable_description
    JoinMeetingDialogState.Error.Generic -> R.string.join_meeting_failure_description
}

@Composable
private fun ClearInputIconButton(state: TextFieldState, enabled: Boolean) {
    AnimatedVisibility(
        visible = state.text.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(
            modifier = Modifier
                .padding(start = dimensions().spacing12x)
                .size(size = dimensions().spacing48x),
            onClick = state::clearText,
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.ic_clear_search),
                contentDescription = stringResource(CommonR.string.content_description_clear_content),
            )
        }
    }
}
