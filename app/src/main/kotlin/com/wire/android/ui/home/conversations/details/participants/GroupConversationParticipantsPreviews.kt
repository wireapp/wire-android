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

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationParticipants() = WireTheme {
    GroupConversationParticipants(
        onProfilePressed = {},
        groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
        developerFeaturesEnabled = true,
        lazyListState = rememberLazyListState(),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationParticipantsAdandonedOneOnOne() = WireTheme {
    GroupConversationParticipants(
        onProfilePressed = {},
        groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
        developerFeaturesEnabled = true,
        lazyListState = rememberLazyListState(),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMLSProgressIndicator() = WireTheme {
    MLSProgressIndicator(0.25F)
}
