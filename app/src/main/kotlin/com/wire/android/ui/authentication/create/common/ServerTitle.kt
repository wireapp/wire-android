/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.create.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
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
    val resources = LocalContext.current.resources
    val host = URL(serverLinks.api).host
    val detailsBody = if (serverLinks.apiProxy == null) {
        resources.stringWithStyledArgs(
            R.string.server_details_dialog_body,
            MaterialTheme.wireTypography.body02,
            MaterialTheme.wireTypography.body02,
            normalColor = colorsScheme().secondaryText,
            argsColor = colorsScheme().onBackground,
            serverLinks.title,
            serverLinks.api,
        )
    } else {
        resources.stringWithStyledArgs(
            R.string.server_details_dialog_body_with_proxy,
            MaterialTheme.wireTypography.body02,
            MaterialTheme.wireTypography.body02,
            normalColor = colorsScheme().secondaryText,
            argsColor = colorsScheme().onBackground,
            serverLinks.title,
            serverLinks.api,
            serverLinks.apiProxy!!.host,
            serverLinks.apiProxy!!.needsAuthentication.toString(),
        )
    }
    ServerTitleContent(
        presentation = ServerTitlePresentation(
            text = titleResId?.let { stringResource(it, host) } ?: host,
            showFullText = titleResId != null,
            infoContentDescription = stringResource(R.string.more_information_about_this_server),
            detailsTitle = stringResource(R.string.server_details_dialog_title),
            detailsBody = detailsBody,
            confirmLabel = stringResource(R.string.label_ok),
        ),
        modifier = modifier,
        style = style,
        textColor = textColor,
        infoIconColor = infoIconColor,
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
