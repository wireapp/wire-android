package com.wire.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireTertiaryButton
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    confirmButtonProperties: WireDialogButtonProperties,
    dismissButtonProperties: WireDialogButtonProperties? = null,
    buttonsHorizontalAlignment: Boolean = true,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding),
    content: @Composable (() -> Unit)? = null
) {
        Dialog(onDismissRequest = onDismiss) {
            WireDialogContent(
                confirmButtonProperties = confirmButtonProperties,
                dismissButtonProperties = dismissButtonProperties,
                buttonsHorizontalAlignment = buttonsHorizontalAlignment,
                modifier = modifier,
                shape = shape,
                contentPadding = contentPadding,
                title = title,
                text = text,
                content = content
            )
        }
}

@Composable
private fun WireDialogContent(
    title: String,
    text: String,
    confirmButtonProperties: WireDialogButtonProperties,
    dismissButtonProperties: WireDialogButtonProperties? = null,
    buttonsHorizontalAlignment: Boolean = true,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding),
    content: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)) {
            Text(
                text = title,
                style = MaterialTheme.wireTypography.title02,
                modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.dialogTextsSpacing)
            )
            Text(
                text = text,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.dialogTextsSpacing)
            )
            content?.let {
                Box(modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.dialogButtonsSpacing)) {
                    it.invoke()
                }
            }

            if (buttonsHorizontalAlignment)
                Row(Modifier.padding(top = MaterialTheme.wireDimensions.dialogButtonsSpacing)) {
                    dismissButtonProperties.getButton(Modifier.weight(1f))
                    if (dismissButtonProperties != null)
                        Spacer(Modifier.width(MaterialTheme.wireDimensions.dialogButtonsSpacing))
                    confirmButtonProperties.getButton(Modifier.weight(1f))
                }
            else
                Column(Modifier.padding(top = MaterialTheme.wireDimensions.dialogButtonsSpacing)) {
                    dismissButtonProperties.getButton()
                    if (dismissButtonProperties != null)
                        Spacer(Modifier.height(MaterialTheme.wireDimensions.dialogButtonsSpacing))
                    confirmButtonProperties.getButton()
                }
        }
    }
}

@Composable
private fun WireDialogButtonProperties?.getButton(modifier: Modifier = Modifier) {
    this?.let {
        Box(modifier = modifier) {
            when (type) {
                WireDialogButtonType.Primary ->
                    WirePrimaryButton(onClick = onClick, text = text, state = state, modifier = modifier)
                WireDialogButtonType.Secondary ->
                    WireSecondaryButton(onClick = onClick, text = text, state = state, modifier = modifier)
                WireDialogButtonType.Tertiary ->
                    WireTertiaryButton(onClick = onClick, text = text, state = state, modifier = modifier)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WireDialogPreview() {
    var password by remember { mutableStateOf(TextFieldValue("")) }
    WireTheme(useDarkColors = false, isPreview = true) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            WireDialogContent(
                confirmButtonProperties = WireDialogButtonProperties(
                    text = "OK",
                    onClick = { },
                    type = WireDialogButtonType.Primary,
                    state = if (password.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Error,
                ),
                dismissButtonProperties = WireDialogButtonProperties(
                    text = "Cancel",
                    onClick = { }
                ),
                title = "title",
                text = "text",
            ) {
                WirePasswordTextField(
                    value = password,
                    onValueChange = { password = it })
            }
        }
    }
}

enum class WireDialogButtonType { Primary, Secondary, Tertiary }

data class WireDialogButtonProperties(
    val text: String,
    val onClick: () -> Unit,
    val state: WireButtonState = WireButtonState.Default,
    val type: WireDialogButtonType = WireDialogButtonType.Secondary
)
