package com.wire.android.ui.common.textfield

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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import com.wire.android.R

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
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Start),
    placeHolderTextStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
    inputMinHeight: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(16.dp),
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
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor(state).value, shape = shape)
                .border(width = 1.dp, color = colors.borderColor(state, interactionSource).value, shape = shape),
            decorationBox = { innerTextField ->
                InnerText(innerTextField, value, leadingIcon, trailingIcon, placeholderText, state, placeHolderTextStyle, inputMinHeight)
            },
        )
        val bottomText = when {
            state is WireTextFieldState.Error -> state.errorText
            !descriptionText.isNullOrEmpty() -> descriptionText
            else -> null
        }
        if (bottomText != null)
            Text(
                text = bottomText,
                fontSize = MaterialTheme.typography.caption.fontSize,
                color = colors.descriptionColor(state).value,
                modifier = Modifier.padding(top = 4.dp)
            )
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
            style = MaterialTheme.typography.caption,
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
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Start),
    inputMinHeight: Dp = 48.dp,
    colors: WireTextFieldColors = wireTextFieldColors()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.heightIn(min = inputMinHeight)
    ) {

        val trailingOrStateIcon: @Composable (() -> Unit)? = when {
            trailingIcon != null -> trailingIcon
            else -> state.icon()?.Icon()
        }
        if (leadingIcon != null)
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(state).value, content = leadingIcon)
            }
        Box(Modifier.weight(1f)) {
            val padding = Modifier.padding(
                start = if(leadingIcon == null) 16.dp else 0.dp,
                end = if(trailingOrStateIcon == null) 16.dp else 0.dp,
                top = 2.dp, bottom = 2.dp
            )
            if (value.text.isEmpty() && placeholderText != null)
                Text(
                    text = placeholderText,
                    fontSize = textStyle.fontSize,
                    textAlign = textStyle.textAlign,
                    color = colors.placeholderColor(state).value,
                    modifier = Modifier.fillMaxWidth().then(padding)
                )
            Box(
                modifier = Modifier.fillMaxWidth().then(padding),
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

@Composable
private fun ImageVector.Icon(): @Composable (() -> Unit) =
    { Icon(imageVector = this, contentDescription = "", modifier = Modifier.padding(horizontal = 16.dp)) }

@Composable
private fun Tint(contentColor: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha, content = content)
    }
}

@Preview(name = "Default WireTextField")
@Composable
fun WireTextFieldPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Default WireTextField with labels")
@Composable
fun WireTextFieldLabelsPreview() {
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
fun WireTextFieldDenseSearchPreview() {
    WireTextField(
        value = TextFieldValue(""),
        placeholderText = "Search",
        leadingIcon = { IconButton(modifier = Modifier.height(40.dp), onClick = {}) { Icon(Icons.Filled.Search,"") } },
        trailingIcon = { IconButton(modifier = Modifier.height(40.dp), onClick = {}) { Icon(Icons.Filled.Close,"") } },
        onValueChange = {},
        inputMinHeight = 40.dp,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Disabled WireTextField")
@Composable
fun WireTextFieldDisabledPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Disabled,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Error WireTextField")
@Composable
fun WireTextFieldErrorPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Error("error"),
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Success WireTextField")
@Composable
fun WireTextFieldSuccessPreview() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Success,
        modifier = Modifier.padding(16.dp)
    )
}

sealed class WireTextFieldState {
    object Default : WireTextFieldState()
    data class Error(val errorText: String) : WireTextFieldState()
    object Success : WireTextFieldState()
    object Disabled : WireTextFieldState()

    fun icon(): ImageVector? = when (this) {
        is Error -> Icons.Filled.ErrorOutline
        is Success -> Icons.Filled.Check
        else -> null
    }
}
