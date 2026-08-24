/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireTypography

@Immutable
data class BackendConfigText(
    val title: String,
    val description: String,
    val inputLabel: String,
    val inputPlaceholder: String,
    val setupLabel: String,
    val continueLabel: String,
    val successTitle: String,
    val successDescription: String,
)

@Composable
fun BackendConfigFormContent(
    text: BackendConfigText,
    onConfigurationLinkEntered: ((String) -> Unit)?,
    onDefaultConfigurationLinkEntered: (String) -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
    centerText: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    errorText: String? = null,
    isLoading: Boolean = false,
) {
    val input = remember { TextFieldState() }
    val align = if (centerText) TextAlign.Center else TextAlign.Start
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        if (showTitle) {
            Text(
                text = text.title,
                style = MaterialTheme.wireTypography.title01,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpace.x16()
        }
        Text(
            text = text.description,
            style = typography().body01,
            color = colorsScheme().secondaryText,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace.x16()
        WireTextField(
            textState = input,
            placeholderText = text.inputPlaceholder,
            labelText = text.inputLabel,
            state = errorText?.let(WireTextFieldState::Error) ?: WireTextFieldState.Default,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.testTag("backendConfigInputField"),
            trailingIcon = trailingIcon,
            testTag = "backendConfigInput",
        )
        VerticalSpace.x8()
        WirePrimaryButton(
            text = text.setupLabel,
            fillMaxWidth = true,
            state = if (input.text.isBlank() || isLoading || onConfigurationLinkEntered == null) {
                WireButtonState.Disabled
            } else {
                WireButtonState.Default
            },
            onClick = {
                (onConfigurationLinkEntered ?: onDefaultConfigurationLinkEntered)(input.text.toString())
            },
            modifier = Modifier.testTag("backendConfigContinueButton"),
        )
    }
}

@Composable
fun BackendConfigSuccessContent(
    text: BackendConfigText,
    successIcon: @Composable () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxWidth(),
) {
    VerticalSpace.x16()
    Row(verticalAlignment = Alignment.CenterVertically) {
        successIcon()
        HorizontalSpace.x8()
        Text(
            text = text.successTitle,
            style = typography().body01,
            color = colorsScheme().onSurface,
        )
    }
    VerticalSpace.x8()
    Text(
        text = text.successDescription,
        style = typography().body01,
        color = colorsScheme().secondaryText,
    )
    VerticalSpace.x24()
    WirePrimaryButton(
        text = text.continueLabel,
        onClick = onContinue,
        fillMaxWidth = true,
        modifier = Modifier.testTag("backendConfigSuccessContinueButton"),
    )
}
