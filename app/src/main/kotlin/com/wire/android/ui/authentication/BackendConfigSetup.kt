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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.kalium.logic.configuration.server.ServerConfig
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import kotlinx.coroutines.launch

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
    val text = BackendConfigText(
        title = stringResource(R.string.missing_backend_config_title),
        description = stringResource(R.string.missing_backend_config_description),
        inputLabel = stringResource(R.string.missing_backend_config_input_label),
        inputPlaceholder = stringResource(R.string.missing_backend_config_input_placeholder),
        setupLabel = stringResource(R.string.missing_backend_config_button_setup),
        continueLabel = stringResource(R.string.label_continue),
        successTitle = stringResource(R.string.backend_config_success_title),
        successDescription = stringResource(R.string.backend_config_success_description),
    )
    BackendConfigFormContent(
        text = text,
        onConfigurationLinkEntered = onConfigurationLinkEntered,
        onDefaultConfigurationLinkEntered = context::openBackendConfig,
        trailingIcon = {
            IconButton(
                onClick = {
                    if (!context.openExternalCamera()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(noCameraAppMessage) }
                    }
                },
                modifier = Modifier.testTag("backendConfigCameraButton"),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = CommonR.drawable.ic_qr_code_scanner),
                    contentDescription = stringResource(R.string.content_description_backend_config_camera_button),
                )
            }
        },
        modifier = modifier,
        showTitle = showTitle,
        centerText = centerText,
        verticalArrangement = verticalArrangement,
        errorText = errorText,
        isLoading = isLoading,
    )
}

@Composable
fun BackendConfigSuccessContent(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit,
) {
    val text = BackendConfigText(
        title = "",
        description = "",
        inputLabel = "",
        inputPlaceholder = "",
        setupLabel = "",
        continueLabel = stringResource(R.string.label_continue),
        successTitle = stringResource(R.string.backend_config_success_title),
        successDescription = stringResource(R.string.backend_config_success_description),
    )
    com.wire.android.ui.authentication.BackendConfigSuccessContent(
        text = text,
        successIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_validation_check),
                tint = colorsScheme().positive,
                contentDescription = null,
                modifier = Modifier.size(dimensions().spacing16x),
            )
        },
        onContinue = onContinue,
        modifier = modifier,
    )
}

fun ServerConfig.Links.isConfigured() = api.isNotBlank()

@Suppress("ReturnCount")
fun String.toBackendConfigUrl(): String? {
    val sanitizedInput = trim().takeIf(String::isNotBlank) ?: return null

    if (!sanitizedInput.startsWith(WIRE_ACCESS_DEEPLINK_BASE)) {
        return sanitizedInput
    }

    return runCatching {
        URI.create(sanitizedInput)
            .rawQuery
            ?.split('&')
            ?.firstOrNull { it.substringBefore('=') == BACKEND_CONFIG_QUERY_PARAMETER }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.let { URLDecoder.decode(it, UTF_8.name()) }
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
