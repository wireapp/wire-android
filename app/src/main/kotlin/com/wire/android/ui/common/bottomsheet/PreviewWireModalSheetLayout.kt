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
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.edit.ReactionOption
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar

@Preview
@Composable
fun PreviewMenuModalSheetContentWithoutHeader() {
    MenuModalSheetContent(
        MenuModalSheetHeader.Gone,
        listOf { ReactionOption({}) }
    )
}

@Preview
@Composable
fun PreviewMenuModalSheetContentWithHeader() {
    MenuModalSheetContent(
        MenuModalSheetHeader.Visible("Title", { GroupConversationAvatar(colorsScheme().primary) }, dimensions().spacing8x),
        listOf { ReactionOption({}) }
    )
}
