/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.authentication

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.android.ui.common.R as CommonR
import kotlinx.coroutines.launch

@Composable
fun MissingBackendConfigContent(
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
    centerText: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val noCameraAppMessage = stringResource(R.string.no_camera_app)
    val backendConfigTextState = remember { TextFieldState() }
    val isConfigInputEmpty = backendConfigTextState.text.isBlank()
    val textAlign = if (centerText) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
    ) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.missing_backend_config_title),
                style = MaterialTheme.wireTypography.title01,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpace.x16()
        }
        Text(
            text = stringResource(R.string.missing_backend_config_description),
            style = typography().body01,
            color = colorsScheme().secondaryText,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace.x16()
        WireTextField(
            textState = backendConfigTextState,
            placeholderText = stringResource(R.string.missing_backend_config_input_placeholder),
            labelText = stringResource(R.string.missing_backend_config_input_label),
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.testTag("backendConfigInput"),
            testTag = "backendConfigInput",
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (!context.openExternalCamera()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(noCameraAppMessage)
                            }
                        }
                    },
                    modifier = Modifier.testTag("backendConfigCameraButton")
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = CommonR.drawable.ic_qr_code_scanner),
                        contentDescription = stringResource(R.string.content_description_backend_config_camera_button),
                    )
                }
            },
        )
        VerticalSpace.x8()
        WirePrimaryButton(
            text = stringResource(R.string.label_continue),
            fillMaxWidth = true,
            state = if (isConfigInputEmpty) WireButtonState.Disabled else WireButtonState.Default,
            onClick = { context.openBackendConfig(backendConfigTextState.text.toString()) },
            modifier = Modifier.testTag("backendConfigContinueButton"),
        )
    }
}

@Composable
fun BackendConfigSuccessContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    onContinue: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_tick),
                contentDescription = null,
                tint = MaterialTheme.wireColorScheme.positive,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.backend_config_success_title),
                style = typography().body01,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        VerticalSpace.x32()
        Text(
            text = stringResource(R.string.backend_config_success_description),
            style = typography().body01,
            color = colorsScheme().secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace.x32()
        WirePrimaryButton(
            text = stringResource(R.string.label_continue),
            fillMaxWidth = true,
            onClick = onContinue,
            modifier = Modifier.testTag("backendConfigSuccessContinueButton"),
        )
    }
}

fun ServerConfig.Links.isConfigured() = api.isNotBlank()

fun Context.openBackendConfig(input: String) {
    val sanitizedInput = input.trim()
    if (sanitizedInput.isEmpty()) return

    val deepLinkUri = if (sanitizedInput.startsWith(WIRE_ACCESS_DEEPLINK_PREFIX)) {
        Uri.parse(sanitizedInput)
    } else {
        Uri.parse("$WIRE_ACCESS_DEEPLINK_PREFIX${Uri.encode(sanitizedInput)}")
    }

    startActivity(
        Intent(this, WireActivity::class.java).apply {
            data = deepLinkUri
        }
    )
}

fun Context.openExternalCamera(): Boolean {
    return try {
        startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private const val WIRE_ACCESS_DEEPLINK_PREFIX = "wire://access/?config="
