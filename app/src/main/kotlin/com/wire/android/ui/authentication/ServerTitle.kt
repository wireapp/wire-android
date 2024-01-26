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

package com.wire.android.ui.authentication

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import java.net.URL

@Composable
fun ServerTitle(
    serverLinks: ServerConfig.Links,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.wireTypography.title01
) {
    ConstraintLayout(
        modifier = Modifier
            .padding(horizontal = dimensions().spacing32x)
            .fillMaxWidth()
            .then(modifier)
    ) {
        val (serverTitle, infoIcon) = createRefs()

        var serverFullDetailsDialogState: Boolean by remember { mutableStateOf(false) }

        Text(
            modifier = Modifier.constrainAs(serverTitle) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            text = URL(serverLinks.api).host,
            style = style,
            color = MaterialTheme.wireColorScheme.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Icon(painter = painterResource(id = R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier
                .constrainAs(infoIcon) {
                    start.linkTo(serverTitle.end)
                    centerVerticallyTo(serverTitle)
                }
                .padding(start = dimensions().spacing8x)
                .size(MaterialTheme.wireDimensions.wireIconButtonSize)
                .clickable(Clickable(true, onClick = { serverFullDetailsDialogState = true })),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )

        if (serverFullDetailsDialogState) {
            val bodyText = if (serverLinks.apiProxy != null) {
                LocalContext.current.resources.stringWithStyledArgs(
                    R.string.server_details_with_proxy_dialog_body,
                    MaterialTheme.wireTypography.body02,
                    MaterialTheme.wireTypography.body02,
                    normalColor = colorsScheme().secondaryText,
                    argsColor = colorsScheme().onBackground,
                    serverLinks.title,
                    serverLinks.api,
                    serverLinks.apiProxy!!.host,
                    serverLinks.apiProxy!!.needsAuthentication.toString()
                )
            } else {
                LocalContext.current.resources.stringWithStyledArgs(
                    R.string.server_details_dialog_body,
                    MaterialTheme.wireTypography.body02,
                    MaterialTheme.wireTypography.body02,
                    normalColor = colorsScheme().secondaryText,
                    argsColor = colorsScheme().onBackground,
                    serverLinks.title,
                    serverLinks.api
                )
            }

            WireDialog(
                title = stringResource(id = R.string.server_details_dialog_title),
                text = bodyText,
                onDismiss = { serverFullDetailsDialogState = false },
                optionButton1Properties = WireDialogButtonProperties(
                    stringResource(id = R.string.label_ok),
                    onClick = { serverFullDetailsDialogState = false },
                    type = WireDialogButtonType.Primary
                )
            )
        }
    }
}
