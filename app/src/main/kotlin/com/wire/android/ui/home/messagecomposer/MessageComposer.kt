package com.wire.android.ui.home.messagecomposer


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.SwipeableState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class MessageComposerState(
    defaultText: TextFieldValue,
    defaultMessageComposerTextState: MessageComposerTextState,
    defaultSendButtonEnabledState: Boolean,
    defaultIsExpandedState: Boolean,
    private val coroutineScope: CoroutineScope
) : SwipeableState<MessageComposerValue>(
    initialValue = MessageComposerValue.Normal,
    animationSpec = SwipeableDefaults.AnimationSpec
) {

    var text by mutableStateOf(defaultText)
        private set

    var messageComposerTextState by mutableStateOf(defaultMessageComposerTextState)

    var sendButtonEnabled by mutableStateOf(defaultSendButtonEnabledState)

    var isExpanded by mutableStateOf(defaultIsExpandedState)

    fun onTextChanged(newText: TextFieldValue) {
        when (messageComposerTextState) {
            MessageComposerTextState.Enabled -> {
                text = newText
                messageComposerTextState = MessageComposerTextState.Active
            }
            MessageComposerTextState.Active -> {
                text = newText
                sendButtonEnabled = newText.text.filter { !it.isWhitespace() }.isNotBlank()
            }
            MessageComposerTextState.Expanded -> {
            }
        }
    }

    fun expand() {
        coroutineScope.launch { animateTo(MessageComposerValue.FullScreen) }
    }

    val isActive: Boolean
        get() = messageComposerTextState is MessageComposerTextState.Active
}


enum class MessageComposerValue {
    Normal,
    FullScreen
}

@Composable
fun rememberMessageComposerState(
    defaultText: TextFieldValue = TextFieldValue(""),
    defaultMessageComposerTextState: MessageComposerTextState = MessageComposerTextState.Enabled,
    defaultSendButtonEnabledState: Boolean = false,
    defaultIsExpandedState: Boolean = false,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember {
    MessageComposerState(
        defaultText,
        defaultMessageComposerTextState,
        defaultSendButtonEnabledState,
        defaultIsExpandedState,
        coroutineScope
    )
}

@Composable
fun MessageComposer() {
    val state = rememberMessageComposerState()

    MessageComposer(state)
}

@Composable
fun MessageComposer(state: MessageComposerState) {
    Column {
        Divider()
        if (state.isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OnDropDownIconButton { state.expand() }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.messageComposerTextState is MessageComposerTextState.Enabled) {
                AddButton()
            }
            MessageTextInput(
                text = state.text,
                onValueChange = { state.onTextChanged(it) },
                modifier = Modifier.weight(1f)
            )
            if (state.isActive) {
                SendButton(state.sendButtonEnabled)
            }
        }
    }
}

@Composable
fun AddButton() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_icon),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
        shape = RoundedCornerShape(size = 12.dp),
        contentPadding = PaddingValues(0.dp)
    )
}

@Composable
fun MessageTextInput(text: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.wireTypography.body01,
        modifier = modifier
    )
}

@Composable
fun SendButton(isEnabled: Boolean) {
    IconButton(
        onClick = { }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.wireColorScheme.onSecondaryButtonDisabled)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = stringResource(R.string.content_description_back_button),
                tint = MaterialTheme.wireColorScheme.surface
            )
        }
    }
}

sealed class MessageComposerTextState {
    object Enabled : MessageComposerTextState()
    object Active : MessageComposerTextState()
    object Expanded : MessageComposerTextState()
}
