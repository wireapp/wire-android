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

package com.wire.android.ui.authentication.create.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import java.net.URL

@Composable
fun ServerTitle(
    serverLinks: ServerConfig.Links,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.wireTypography.title01,
    textColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    infoIconColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    @StringRes titleResId: Int? = null,
) {
    var serverFullDetailsDialogState: Boolean by remember { mutableStateOf(false) }
    val host = URL(serverLinks.api).host
    val infoIconId = "info"
    val text = titleResId?.let { stringResource(it, host) } ?: host
    val annotatedText = buildAnnotatedString {
        append(text)
        append(" ")
        appendInlineContent(infoIconId, "[info]")
    }
    val iconSizeDp = MaterialTheme.wireDimensions.wireIconButtonSize
    val iconSizeSp = with(LocalDensity.current) {
        iconSizeDp.toSp()
    }
    val inlineContent = mapOf(
        "info" to InlineTextContent(
            Placeholder(
                width = iconSizeSp,
                height = iconSizeSp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = stringResource(R.string.more_information_about_this_server),
                modifier = Modifier
                    .size(iconSizeDp)
                    .clickable(Clickable(true, onClick = { serverFullDetailsDialogState = true })),
                tint = infoIconColor,
            )
        }
    )

    Text(
        text = annotatedText,
        style = style,
        color = textColor,
        maxLines = if (titleResId != null) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent,
        modifier = modifier,
    )

    if (serverFullDetailsDialogState) {
        ServerEnrollmentDialogContent(
            serverLinks = serverLinks,
            onClick = { serverFullDetailsDialogState = false },
            onDismiss = { serverFullDetailsDialogState = false }
        )
    }
}

@Composable
private fun ServerEnrollmentDialogContent(
    serverLinks: ServerConfig.Links,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val text = if (serverLinks.apiProxy == null) {
        LocalContext.current.resources.stringWithStyledArgs(
            R.string.server_details_dialog_body,
            MaterialTheme.wireTypography.body02,
            MaterialTheme.wireTypography.body02,
            normalColor = colorsScheme().secondaryText,
            argsColor = colorsScheme().onBackground,
            serverLinks.title,
            serverLinks.api
        )
    } else {
        LocalContext.current.resources.stringWithStyledArgs(
            R.string.server_details_dialog_body_with_proxy,
            MaterialTheme.wireTypography.body02,
            MaterialTheme.wireTypography.body02,
            normalColor = colorsScheme().secondaryText,
            argsColor = colorsScheme().onBackground,
            serverLinks.title,
            serverLinks.api,
            serverLinks.apiProxy!!.host,
            serverLinks.apiProxy!!.needsAuthentication.toString()
        )
    }
    WireDialog(
        title = stringResource(id = R.string.server_details_dialog_title),
        text = text,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            stringResource(id = R.string.label_ok),
            onClick = onClick,
            type = WireDialogButtonType.Primary
        )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewServerTitle() = WireTheme {
    Box(modifier = Modifier.background(colorsScheme().surface)) {
        ServerTitle(serverLinks = ServerConfig.DEFAULT)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewServerTitleEnterprise() = WireTheme {
    Box(modifier = Modifier.background(colorsScheme().surface)) {
        ServerTitle(
            serverLinks = ServerConfig.DEFAULT,
            titleResId = R.string.enterprise_login_on_prem_welcome_title,
            textColor = MaterialTheme.wireColorScheme.onSurface,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewServerEnrollmentDialog() = WireTheme {
    ServerEnrollmentDialogContent(
        serverLinks = ServerConfig.DEFAULT,
        onClick = { },
        onDismiss = { }
    )
}
