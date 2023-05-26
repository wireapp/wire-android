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

package com.wire.android.ui.home.conversationslist.common

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.toMessageId
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun ConnectionLabel(connectionInfo: UILastMessageContent.Connection) {
    if (connectionInfo.connectionState == ConnectionState.PENDING || connectionInfo.connectionState == ConnectionState.IGNORED) {
        Text(
            text = getConnectionStringLabel(labelId = connectionInfo.connectionState.toMessageId()),
            style = MaterialTheme.wireTypography.subline01.copy(
                color = MaterialTheme.wireColorScheme.secondaryText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun getConnectionStringLabel(@StringRes labelId: Int) =
    if (labelId == -1) "" else stringResource(id = labelId)
