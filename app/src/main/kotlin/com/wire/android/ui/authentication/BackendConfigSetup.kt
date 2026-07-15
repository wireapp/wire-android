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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as CommonR

@Composable
fun MissingBackendConfigContent(
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
    centerText: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    errorText: String? = null,
    isLoading: Boolean = false,
    onConfigurationLinkEntered: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val noCameraAppMessage = stringResource(CommonR.string.no_camera_app)
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
            state = errorText?.let(WireTextFieldState::Error) ?: WireTextFieldState.Default,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.testTag("backendConfigInputField"),
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
            text = stringResource(R.string.missing_backend_config_button_setup),
            fillMaxWidth = true,
            state = if (isConfigInputEmpty || isLoading || onConfigurationLinkEntered == null) {
                WireButtonState.Disabled
            } else {
                WireButtonState.Default
            },
            onClick = { (onConfigurationLinkEntered ?: context::openBackendConfig)(backendConfigTextState.text.toString()) },
            modifier = Modifier.testTag("backendConfigContinueButton"),
        )
    }
}

@Composable
fun BackendConfigSuccessContent(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        VerticalSpace.x16()
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_validation_check),
                tint = colorsScheme().positive,
                contentDescription = null,
                modifier = Modifier.size(dimensions().spacing16x)
            )
            HorizontalSpace.x8()
            Text(
                text = stringResource(R.string.backend_config_success_title),
                style = typography().body01,
                color = colorsScheme().onSurface,
            )
        }
        VerticalSpace.x8()
        Text(
            text = stringResource(R.string.backend_config_success_description),
            style = typography().body01,
            color = colorsScheme().secondaryText,
        )
        VerticalSpace.x24()
        WirePrimaryButton(
            text = stringResource(R.string.label_continue),
            onClick = onContinue,
            fillMaxWidth = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backendConfigSuccessContinueButton")
        )
    }
}

fun ServerConfig.Links.isConfigured() = api.isNotBlank()

@Suppress("ReturnCount")
fun String.toBackendConfigUrl(): String? {
    val sanitizedInput = trim().takeIf(String::isNotBlank) ?: return null

    if (!sanitizedInput.startsWith(WIRE_ACCESS_DEEPLINK_BASE)) {
        return sanitizedInput
    }

    return runCatching {
        Uri.parse(sanitizedInput)
            .getQueryParameter(BACKEND_CONFIG_QUERY_PARAMETER)
            ?.takeIf(String::isNotBlank)
    }.getOrNull()
}

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

private const val WIRE_ACCESS_DEEPLINK_BASE = "wire://access/"
private const val BACKEND_CONFIG_QUERY_PARAMETER = "config"
private const val WIRE_ACCESS_DEEPLINK_PREFIX = "$WIRE_ACCESS_DEEPLINK_BASE?$BACKEND_CONFIG_QUERY_PARAMETER="
