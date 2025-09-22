/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.newauthentication.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.forceLowercase

@Composable
internal fun BackendSelectorDropDown() {

    val context = LocalContext.current
    var showCustomBackendDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions().spacing12x),
        horizontalAlignment = Alignment.End,
    ) {
        WireDropDown(
            modifier = Modifier.alpha(0.5f),
            items = backendConfigs.map { it.first },
            label = null,
            autoUpdateSelection = false,
            placeholder = "Change application backend",
            onSelected = { index ->
                val backend = backendConfigs[index].second
                if (backend.isNotEmpty()) {
                    openConfigUrl(context, backend)
                } else {
                    showCustomBackendDialog = true
                }
            },
        )
    }

    if (showCustomBackendDialog) {
        EnterBackendNameDialog(
            onConfirm = { backendName ->
                if (backendName.isNotEmpty()) {
                    openConfigUrl(context, "https://nginz-https.$backendName.wire.link/deeplink.json")
                }
                showCustomBackendDialog = false
            },
            onDismiss = {
                showCustomBackendDialog = false
            }
        )
    }
}

@Composable
private fun EnterBackendNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var textState by remember { mutableStateOf(TextFieldState("")) }

    WireDialog(
        title = "Enter custom backend name",
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { onDismiss() },
            text = stringResource(R.string.label_cancel),
            type = WireDialogButtonType.Secondary
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = { onConfirm(textState.text.toString()) },
            text = stringResource(R.string.label_confirm),
            type = WireDialogButtonType.Primary
        ),
        onDismiss = onDismiss
    ) {
        WireTextField(
            modifier = Modifier.focusRequester(focusRequester),
            textState = textState,
            placeholderText = "test-qa-column-1",
            inputTransformation = InputTransformation
                .forceLowercase()
                .replaceSpaces(),
        )

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

private fun InputTransformation.replaceSpaces(): InputTransformation =
    this.then({
        val currentText = asCharSequence().toString()
        val transformedText = currentText.replace(" ", "-")
        if (currentText != transformedText) {
            replace(0, length, transformedText)
        }
    })

private fun openConfigUrl(context: Context, configUrl: String) {
    context.startActivity(
        Intent(context, WireActivity::class.java).apply {
            data = Uri.parse("wire://access/?config=$configUrl")
        }
    )
}

private val backendConfigs = listOf(
    "Production" to "https://prod-nginz-https.wire.com/deeplink.json",
    "Staging" to "https://staging-nginz-https.zinfra.io/deeplink.json",
    "Anta" to "https://nginz-https.anta.wire.link/deeplink.json",
    "Bella" to "https://nginz-https.bella.wire.link/deeplink.json",
    "Chala" to "https://nginz-https.chala.wire.link/deeplink.json",
    "Elna" to "https://nginz-https.elna.wire.link/deeplink.json",
    "Foma" to "https://nginz-https.foma.wire.link/deeplink.json",
    "Imai" to "https://nginz-https.imai.wire.link/deeplink.json",
    "Fulu" to "https://nginz-https.fulu.wire.link/deeplink.json",
    "Custom" to "",
)
