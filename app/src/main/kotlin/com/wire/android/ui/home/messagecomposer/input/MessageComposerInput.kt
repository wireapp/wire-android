package com.wire.android.ui.home.messagecomposer.input

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.theme.wireTypography

@Composable
fun MessageComposerInput(
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    messageComposerInputState: MessageComposeInputState,
    onIsFocused: () -> Unit,
    onNotFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = wireTextFieldColors(
            borderColor = Color.Transparent,
            focusColor = Color.Transparent
        ),
        singleLine = messageComposerInputState == MessageComposeInputState.Enabled,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add a extra space so that the a cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onIsFocused()
                } else {
                    onNotFocused()
                }
            }
        )
    )
}
