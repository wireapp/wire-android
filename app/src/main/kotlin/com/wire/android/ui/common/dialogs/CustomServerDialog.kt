/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.common.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun CustomServerDialog(
    serverLinksTitle: String,
    serverLinksApi: String,
    serverProxy: ServerConfig.ApiProxy?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    val text = if (serverProxy != null) {
        LocalContext.current.resources.stringWithStyledArgs(
            R.string.custom_backend_with_proxy_dialog_body,
            MaterialTheme.wireTypography.body01,
            MaterialTheme.wireTypography.body02,
            colorsScheme().onBackground,
            colorsScheme().onBackground,
            serverLinksTitle,
            serverLinksApi,
            serverProxy.host,
            serverProxy.needsAuthentication.toString()
        )
    } else {
        LocalContext.current.resources.stringWithStyledArgs(
            R.string.custom_backend_dialog_body,
            MaterialTheme.wireTypography.body01,
            MaterialTheme.wireTypography.body02,
            colorsScheme().onBackground,
            colorsScheme().onBackground,
            serverLinksTitle,
            serverLinksApi
        )
    }
    WireDialog(
        title = stringResource(R.string.custom_backend_dialog_title),
        text = text,

        buttonsHorizontalAlignment = true,
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismiss = onDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(id = R.string.label_proceed),
            type = WireDialogButtonType.Primary,
            state =
            WireButtonState.Default
        )
    )
}

data class CustomServerDialogState(val serverLinks: ServerConfig.Links)
