package com.wire.android.ui.common.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.Tint
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY

@Composable
internal fun WireTextField(
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
    val enabled = state !is WireTextFieldState.Disabled

    Column(modifier = modifier) {
        if (labelText != null)
            Label(labelText, labelMandatoryIcon, state, interactionSource, colors)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = colors.textColor(state = state).value),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor(state).value, shape = shape)
                .border(width = 1.dp, color = colors.borderColor(state, interactionSource).value, shape = shape),
            decorationBox = { innerTextField ->
                InnerText(innerTextField, value, leadingIcon, trailingIcon, placeholderText, state, placeholderTextStyle, inputMinHeight)
            },
        )
        val bottomText = when {
            state is WireTextFieldState.Error && state.errorText != null -> state.errorText
            !descriptionText.isNullOrEmpty() -> descriptionText
            else -> String.EMPTY
        }
        AnimatedVisibility(visible = bottomText.isNotEmpty()) {
            Text(
                text = bottomText,
                style = MaterialTheme.wireTypography.label04,
                textAlign = TextAlign.Start,
                color = colors.descriptionColor(state).value,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun Label(
    labelText: String,
    labelMandatoryIcon: Boolean,
    state: WireTextFieldState,
    interactionSource: InteractionSource,
    colors: WireTextFieldColors
) {
    Row {
        Text(
            text = labelText,
            style = MaterialTheme.wireTypography.label01,
            color = colors.labelColor(state, interactionSource).value,
            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
        )
        if (labelMandatoryIcon)
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_input_mandatory),
                tint = colors.labelMandatoryColor(state).value,
                contentDescription = "",
                modifier = Modifier.padding(top = 2.dp)
            )
    }
}

@Composable
private fun InnerText(
    innerTextField: @Composable () -> Unit,
    value: TextFieldValue,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    inputMinHeight: Dp = 48.dp,
    colors: WireTextFieldColors = wireTextFieldColors()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.heightIn(min = inputMinHeight)
    ) {

        val trailingOrStateIcon: @Composable (() -> Unit)? = when {
            trailingIcon != null -> trailingIcon
            else -> state.icon()?.Icon(Modifier.padding(horizontal = 16.dp))
        }
        if (leadingIcon != null)
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(state).value, content = leadingIcon)
            }
        Box(Modifier.weight(1f)) {
            val padding = Modifier.padding(
                start = if (leadingIcon == null) 16.dp else 0.dp,
                end = if (trailingOrStateIcon == null) 16.dp else 0.dp,
                top = 2.dp, bottom = 2.dp
            )
            if (value.text.isEmpty() && placeholderText != null)
                Text(
                    text = placeholderText,
                    style = placeholderTextStyle,
                    color = colors.placeholderColor(state).value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(padding)
                )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(padding),
                propagateMinConstraints = true
            ) {
                innerTextField()
            }
        }
        if (trailingOrStateIcon != null)
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(state).value, content = trailingOrStateIcon)
            }
    }
}

@Preview(name = "Default WireTextField")
@Composable
private fun WireTextFieldPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Default WireTextField with labels")
@Composable
private fun WireTextFieldLabelsPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        labelText = "label",
        labelMandatoryIcon = true,
        descriptionText = "description",
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Dense Search WireTextField")
@Composable
private fun WireTextFieldDenseSearchPreview() {
    WireTextField(
        value = TextFieldValue(""),
        placeholderText = "Search",
        leadingIcon = { IconButton(modifier = Modifier.height(40.dp), onClick = {}) { Icon(Icons.Filled.Search, "") } },
        trailingIcon = { IconButton(modifier = Modifier.height(40.dp), onClick = {}) { Icon(Icons.Filled.Close, "") } },
        onValueChange = {},
        inputMinHeight = 40.dp,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Disabled WireTextField")
@Composable
private fun WireTextFieldDisabledPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Disabled,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Error WireTextField")
@Composable
private fun WireTextFieldErrorPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Error("error"),
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Success WireTextField")
@Composable
private fun WireTextFieldSuccessPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Success,
        modifier = Modifier.padding(16.dp)
    )
}

sealed class WireTextFieldState {
    object Default : WireTextFieldState()
    data class Error(val errorText: String? = null) : WireTextFieldState()
    object Success : WireTextFieldState()
    object Disabled : WireTextFieldState()

    fun icon(): ImageVector? = when (this) {
        is Error -> Icons.Filled.ErrorOutline
        is Success -> Icons.Filled.Check
        else -> null
    }
}
