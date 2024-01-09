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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.common.dimensions
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@Composable
internal fun MutingOptionsSheetContent(
    mutingConversationState: MutedConversationStatus,
    onMuteConversation: (MutedConversationStatus) -> Unit,
    onBackClick: () -> Unit,
) {
    MenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.label_notifications),
            leadingIcon = {
                ArrowLeftIcon(modifier = Modifier.clickable { onBackClick() })
                Spacer(modifier = Modifier.width(dimensions().spacing8x))
            },
        ),
        menuItems = listOf(
            {
                SelectableMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_all_allowed_title),
                    subLine = stringResource(id = R.string.muting_option_all_allowed_text),
                    onItemClick = Clickable { onMuteConversation(MutedConversationStatus.AllAllowed) },
                    state = if (mutingConversationState == MutedConversationStatus.AllAllowed) RichMenuItemState.SELECTED
                    else RichMenuItemState.DEFAULT
                )
            },
            {
                SelectableMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_only_mentions_title),
                    subLine = stringResource(id = R.string.muting_option_only_mentions_text),
                    onItemClick = Clickable { onMuteConversation(MutedConversationStatus.OnlyMentionsAndRepliesAllowed) },
                    state = if (mutingConversationState == MutedConversationStatus.OnlyMentionsAndRepliesAllowed)
                        RichMenuItemState.SELECTED else RichMenuItemState.DEFAULT
                )
            },
            {
                SelectableMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_all_muted_title),
                    subLine = stringResource(id = R.string.muting_option_all_muted_text),
                    onItemClick = Clickable { onMuteConversation(MutedConversationStatus.AllMuted) },
                    state = if (mutingConversationState == MutedConversationStatus.AllMuted) RichMenuItemState.SELECTED
                    else RichMenuItemState.DEFAULT
                )
            }
        )
    )
}
