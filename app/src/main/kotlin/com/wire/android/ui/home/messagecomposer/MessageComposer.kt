package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
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


class MessageComposerState(
    defaultText: TextFieldValue,
    defaultMessageComposeStaticState: MessageComposeStaticState,
    val coroutineScope: CoroutineScope
) {

    var messageText by mutableStateOf(defaultText)

    var messageComposeVisibilityState by mutableStateOf(defaultMessageComposeStaticState)
        private set

    val sendButtonEnabledState: Boolean
        @Composable get() = messageText.text.filter { !it.isWhitespace() }.isNotBlank()

    fun toActive() {
        messageComposeVisibilityState = MessageComposeStaticState.Active
    }

    fun toEnabled() {
        messageComposeVisibilityState = MessageComposeStaticState.Enabled
    }

    fun toggleFullScreen() {
        messageComposeVisibilityState = if (messageComposeVisibilityState == MessageComposeStaticState.Active) {
            MessageComposeStaticState.FullScreen
        } else {
            MessageComposeStaticState.Active
        }
    }

    fun isEnabled(): Boolean = messageComposeVisibilityState == MessageComposeStaticState.Enabled

}

@Composable
fun rememberMessageComposerState(
    defaultText: TextFieldValue = TextFieldValue(""),
    defaultMessageComposeStaticState: MessageComposeStaticState = MessageComposeStaticState.Enabled,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember {
    MessageComposerState(
        defaultText,
        defaultMessageComposeStaticState,
        coroutineScope
    )
}

sealed class MessageComposeStaticState(
    val sendButtonVisibleState: Boolean,
    val dropDownButtonVisibleState: Boolean,
    val dropDownButtonRotation: DropDownButtonRotation,
    val addButtonVisible: Boolean,
) {
    object Enabled : MessageComposeStaticState(false, false, DropDownButtonRotation.Up, true)

    object Active : MessageComposeStaticState(true, true, DropDownButtonRotation.Up, false)

    object FullScreen : MessageComposeStaticState(true, true, DropDownButtonRotation.Down, false)
}

enum class DropDownButtonRotation(val rotationDegree: Float) {
    Up(180f), Down(0f)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposer(
    content: @Composable () -> Unit
) {
    val state = rememberMessageComposerState()

    MessageComposComposer(
        content = content,
        state
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MessageComposComposer(
    content: @Composable () -> Unit,
    state: MessageComposerState
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (state.isEnabled()) {
        focusManager.clearFocus()
    }

    BoxWithConstraints {
        val fullHeight = constraints.maxHeight.toFloat()
        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
        Surface(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    sheetHeightState.value = it.size.height.toFloat()
                }
        ) {
            Column {
                Box(
                    Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            state.toEnabled()
                        }) {
                    content()
                }

                val transition = updateTransition(state.messageComposeVisibilityState, label = "")

                val messageTextInputSize by transition.animateDp(label = "", transitionSpec = {
                    spring(stiffness = StiffnessLow)
                }) { state ->
                    when (state) {
                        MessageComposeStaticState.Enabled -> 56.dp
                        MessageComposeStaticState.Active -> 90.dp
                        MessageComposeStaticState.FullScreen -> fullHeight.dp
                    }
                }

                val onDropDownButtonRotationDegree by transition.animateFloat(label = "", transitionSpec = {
                    spring(stiffness = StiffnessLow)
                }) { state ->
                    state.dropDownButtonRotation.rotationDegree
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(messageTextInputSize)
                ) {
                    Column {
                        Divider()
                        AnimatedVisibility(visible = state.messageComposeVisibilityState.dropDownButtonVisibleState) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                OnDropDownIconButton(
                                    onDropDownClick = { state.toggleFullScreen() },
                                    modifier = Modifier.rotate(degrees = onDropDownButtonRotationDegree)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = state.messageComposeVisibilityState.addButtonVisible) {
                                AddButton()
                            }
                            MessageTextInput(
                                text = state.messageText,
                                onValueChange = {
                                    state.messageText = it
                                },
                                onIsFocused = {
                                    state.toActive()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                            )
                            AnimatedVisibility(visible = state.messageComposeVisibilityState.sendButtonVisibleState) {
                                SendButton(isEnabled = state.sendButtonEnabledState)
                            }
                        }
                    }
                }
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
fun MessageTextInput(
    text: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onIsFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.wireTypography.body01,
        modifier = modifier.then(
            Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onIsFocused()
                }
            }
        )
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


