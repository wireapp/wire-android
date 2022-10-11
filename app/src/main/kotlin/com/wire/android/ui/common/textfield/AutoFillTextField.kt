package com.wire.android.ui.common.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AutoFillTextField(
    autofillTypes: List<AutofillType>,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    labelText: String? = null,
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    modifier: Modifier = Modifier
) {
    val autofillNode = AutofillNode(
        autofillTypes = autofillTypes,
        onFill = { onValueChange(TextFieldValue(it, TextRange(it.length))) }
    )
    val autofill = LocalAutofill.current

    LocalAutofillTree.current += autofillNode

    WireTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        placeholderText = placeholderText,
        labelText = labelText,
        labelMandatoryIcon = labelMandatoryIcon,
        descriptionText = descriptionText,
        state = state,
        interactionSource = interactionSource,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
        placeholderTextStyle = placeholderTextStyle,
        inputMinHeight = inputMinHeight,
        shape = shape,
        colors = colors,
        modifier = modifier
            .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
            .onFocusChanged { focusState ->
                autofill?.run {
                    if (focusState.isFocused) {
                        requestAutofillForNode(autofillNode)
                    } else {
                        cancelAutofillForNode(autofillNode)
                    }
                }
            }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun clearAutofillTree() {
    LocalAutofillTree.current.children.clear()
}
