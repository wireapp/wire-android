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
package com.wire.android.ui.home.conversations.details

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.media.ConversationFilesButton
import com.wire.android.ui.home.conversations.media.ConversationMediaButton
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesButton
import com.wire.android.ui.theme.WireTheme

@Composable
fun SearchAndMediaRow(
    onSearchConversationMessagesClick: () -> Unit,
    onConversationMediaClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWireCellEnabled: Boolean = false,
) {
    Row(modifier = modifier.padding(horizontal = dimensions().spacing16x)) {
        SearchConversationMessagesButton(
            modifier = Modifier.weight(1F),
            onClick = onSearchConversationMessagesClick
        )
        HorizontalSpace.x8()
        if (isWireCellEnabled) {
            ConversationFilesButton(
                modifier = Modifier.weight(1F),
                onClick = onConversationMediaClick
            )
        } else {
            ConversationMediaButton(
                modifier = Modifier.weight(1F),
                onClick = onConversationMediaClick
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewSearchAndMediaRow() = WireTheme {
    SearchAndMediaRow({}, {})
}
