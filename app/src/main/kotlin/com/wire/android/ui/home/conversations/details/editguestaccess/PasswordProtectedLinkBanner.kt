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
package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.ui.common.R as commonR

@Composable
fun PasswordProtectedLinkBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        Divider(
            color = colorsScheme().outline,
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(bottom = dimensions().spacing4x)
            ) {
                Text(
                    text = stringResource(id = R.string.password_protected_link_banner_title),
                    style = MaterialTheme.wireTypography.title02,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = commonR.drawable.ic_shield_holo),
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp),
                    contentDescription = null,
                    tint = colorsScheme().onBackground
                )
            }
            Text(
                text = stringResource(id = R.string.password_protected_link_banner_description),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewPasswordProtectedLinkBanner() = WireTheme {
    PasswordProtectedLinkBanner()
}
