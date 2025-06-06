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
package com.wire.android.ui.common.bottomsheet

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.edit.ReactionOption
import com.wire.android.ui.home.conversationslist.common.RegularGroupConversationAvatar
import com.wire.kalium.logic.data.id.ConversationId

@MultipleThemePreviews
@Composable
fun PreviewMenuModalSheetContentWithoutHeader() {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Gone,
        menuItems = listOf { ReactionOption({}) }
    )
}

@MultipleThemePreviews
@Composable
fun PreviewMenuModalSheetContentWithHeader() {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            "Title",
            {
                RegularGroupConversationAvatar(
                    conversationId = ConversationId("value", "domain")
                )
            },
            dimensions().spacing8x
        ),
        menuItems = listOf { ReactionOption({}) }
    )
}
