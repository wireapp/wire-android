package com.wire.android.ui.authentication.devices.remove

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.formatMediumDateTime

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RemoveDeviceDialog(
    errorState: RemoveDeviceError,
    state: RemoveDeviceDialogState.Visible,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveConfirm: () -> Unit
) {
    var keyboardController: SoftwareKeyboardController? = null
    val onDialogDismissHideKeyboard: () -> Unit = {
        keyboardController?.hide()
        onDialogDismiss()
    }
    WireDialog(
        title = stringResource(R.string.remove_device_dialog_title),
        text = state.device.name.asString() + "\n" +
            stringResource(
                R.string.remove_device_id_and_time_label,
                state.device.clientId.value,
                state.device.registrationTime?.formatMediumDateTime() ?: ""
            ),
        onDismiss = onDialogDismissHideKeyboard,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismissHideKeyboard,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                keyboardController?.hide()
                onRemoveConfirm()
            },
            text = stringResource(id = if (state.loading) R.string.label_removing else R.string.label_remove),
            type = WireDialogButtonType.Primary,
            loading = state.loading,
            state = if (state.removeEnabled) WireButtonState.Error else WireButtonState.Disabled
        ),
        content = {
            // keyboard controller from outside the Dialog doesn't work inside its content so we have to pass the state
            // to the dialog's content and use keyboard controller from there
            keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }
            WirePasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                state = when {
                    errorState is RemoveDeviceError.InvalidCredentialsError ->
                        WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))

                    state.loading -> WireTextFieldState.Disabled
                    else -> WireTextFieldState.Default
                },
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .testTag("remove device password field"),
                autofill = true
            )
            LaunchedEffect(Unit) { // executed only once when showing the dialog
                focusRequester.requestFocus()
            }
        }
    )
}
