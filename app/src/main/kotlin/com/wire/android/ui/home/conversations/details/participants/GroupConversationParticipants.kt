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

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
fun GroupConversationParticipants(
    openFullListPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    onAddParticipantsPressed : () -> Unit,
    groupParticipantsState: GroupConversationParticipantsState,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(weight = 1f, fill = true)
        ) {
            item(key = "participants_list_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.wireColorScheme.surface)
                        .padding(MaterialTheme.wireDimensions.spacing16x)
                ) {
                    Text(
                        text = context.resources.stringWithStyledArgs(
                            R.string.conversation_details_participants_info,
                            MaterialTheme.wireTypography.body01,
                            MaterialTheme.wireTypography.body02,
                            MaterialTheme.wireColorScheme.onBackground,
                            MaterialTheme.wireColorScheme.onBackground,
                            groupParticipantsState.data.allCount.toString()
                        )
                    )
                    if (groupParticipantsState.data.isSelfAnAdmin)
                        WirePrimaryButton(
                            text = stringResource(R.string.conversation_details_group_participants_add),
                            fillMaxWidth = true,
                            onClick = onAddParticipantsPressed ,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MaterialTheme.wireDimensions.spacing16x),
                        )
                }
            }
            participantsFoldersWithElements(context, groupParticipantsState, onProfilePressed)
        }
        if (groupParticipantsState.showAllVisible)
            Surface(
                shadowElevation = lazyListState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    WireSecondaryButton(
                        text = stringResource(R.string.conversation_details_show_all_participants, groupParticipantsState.data.allCount),
                        onClick = openFullListPressed
                    )
                }
            }
    }
}

@Preview
@Composable
fun GroupConversationParticipantsPreview() {
    GroupConversationParticipants({}, {}, {}, GroupConversationParticipantsState.PREVIEW)
}
