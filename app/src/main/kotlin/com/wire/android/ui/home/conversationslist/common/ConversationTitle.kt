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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.theme.wireTypography

@Composable
fun ConversationTitle(
    name: String,
    isLegalHold: Boolean = false,
    modifier: Modifier = Modifier,
    badges: @Composable () -> Unit = {},
    searchQuery: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (searchQuery.isEmpty()) {
            Text(
                text = name,
                style = MaterialTheme.wireTypography.body02,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(weight = 1f, fill = false)
            )
        } else {
            HighlightName(name = name, searchQuery = searchQuery)
        }
        badges()
        if (isLegalHold) {
            Spacer(modifier = Modifier.width(6.dp))
            LegalHoldIndicator()
        }
    }
}

@Preview(widthDp = 200)
@Composable
fun PreviewConversationTitle() {
    ConversationTitle("very very loooooooooooong name", true, searchQuery = "test")
}
