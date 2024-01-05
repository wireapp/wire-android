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

package com.wire.android.ui.home.conversationslist.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallLabel(callInfo: ConversationLastEvent.Call) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TimeLabel(callTime = callInfo.callTime)
        Spacer(modifier = Modifier.width(6.dp))
        CallEventIcon(callEvent = callInfo.callEvent)
    }
}

@Composable
private fun CallEventIcon(callEvent: CallEvent, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = callEvent.drawableResourceId),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
private fun TimeLabel(callTime: CallTime) {
    Text(text = callTime.toLabel(), style = MaterialTheme.wireTypography.subline01, color = MaterialTheme.wireColorScheme.secondaryText)
}
