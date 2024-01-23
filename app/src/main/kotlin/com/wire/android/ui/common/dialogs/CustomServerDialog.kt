/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.common.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun CustomServerDialog(
    serverLinks: ServerConfig.Links,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    WireDialog(
        title = stringResource(R.string.custom_backend_dialog_title),
        text = stringResource(R.string.custom_backend_dialog_body),
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
        ),
        content = {
            Column {
                CustomServerPropertyInfo(
                    title = stringResource(id = R.string.custom_backend_dialog_body_backend_name),
                    value = serverLinks.title
                )
                CustomServerPropertyInfo(
                    title = stringResource(id = R.string.custom_backend_dialog_body_backend_api),
                    value = serverLinks.api
                )
                if (showDetails) {
                    CustomServerPropertyInfo(
                        title = stringResource(id = R.string.custom_backend_dialog_body_backend_websocket),
                        value = serverLinks.webSocket
                    )
                    CustomServerPropertyInfo(
                        title = stringResource(id = R.string.custom_backend_dialog_body_backend_blacklist),
                        value = serverLinks.blackList
                    )
                    CustomServerPropertyInfo(
                        title = stringResource(id = R.string.custom_backend_dialog_body_backend_teams),
                        value = serverLinks.teams
                    )
                    CustomServerPropertyInfo(
                        title = stringResource(id = R.string.custom_backend_dialog_body_backend_accounts),
                        value = serverLinks.accounts
                    )
                    CustomServerPropertyInfo(
                        title = stringResource(id = R.string.custom_backend_dialog_body_backend_website),
                        value = serverLinks.website
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensions().spacing8x)
                ) {
                    Text(
                        text = stringResource(id = if (showDetails) R.string.label_hide_details else R.string.label_show_details),
                        style = MaterialTheme.wireTypography.body02.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showDetails = !showDetails }
                            )
                    )
                }
            }
        }
    )
}

@Composable
private fun CustomServerPropertyInfo(
    title: String,
    value: String
) {
    Text(
        text = title,
        style = MaterialTheme.wireTypography.body01,
        color = colorsScheme().onBackground,
    )
    VerticalSpace.x4()
    Text(
        text = value,
        style = MaterialTheme.wireTypography.body02,
        color = colorsScheme().onBackground,
    )
    VerticalSpace.x16()
}

data class CustomServerDialogState(val serverLinks: ServerConfig.Links)

@PreviewMultipleThemes
@Composable
fun PreviewCustomServerDialog() = WireTheme {
    CustomServerDialog(
        serverLinks = ServerConfig.DEFAULT,
        onConfirm = { },
        onDismiss = { }
    )
}
