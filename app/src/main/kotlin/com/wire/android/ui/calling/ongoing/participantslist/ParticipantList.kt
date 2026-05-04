/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.calling.ongoing.participantslist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.collections.immutable.PersistentList

@Composable
fun ParticipantList(
    participants: PersistentList<UICallParticipant>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
    ) {
        items(
            items = participants,
            key = { it.id.value + it.clientId },
        ) { participant ->
            ParticipantItem(participant)
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantList() = WireTheme {
    ParticipantList(participants = buildPreviewParticipantsList(3))
}
