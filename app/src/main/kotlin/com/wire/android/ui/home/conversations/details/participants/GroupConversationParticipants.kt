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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.BuildConfig
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.home.conversations.details.participants.model.ParticipantsExpansionState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.SupportedProtocol

@Composable
fun GroupConversationParticipants(
    onProfilePressed: (UIParticipant) -> Unit,
    groupParticipantsState: GroupConversationParticipantsState,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val participantsExpansionState = remember { ParticipantsExpansionState() }
    Column(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (BuildConfig.DEVELOPER_FEATURES_ENABLED) {
                item(key = "participants_list_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.wireColorScheme.surface)
                            .padding(MaterialTheme.wireDimensions.spacing16x)
                    ) {
                        val groupParticipants = groupParticipantsState.data.allParticipants
                        MLSProgressIndicator(
                            mlsProgress = (groupParticipants)
                                .filter { it.supportedProtocolList.contains(SupportedProtocol.MLS) }
                                .size / (groupParticipantsState.data.allCount).toFloat(),
                            modifier = Modifier
                                .background(MaterialTheme.wireColorScheme.surface)
                        )
                    }
                }
            }
            participantsFoldersWithElements(context, groupParticipantsState, onProfilePressed, participantsExpansionState)
        }
    }
}

@Composable
fun MLSProgressIndicator(
    mlsProgress: Float,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(targetValue = mlsProgress)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${SupportedProtocol.MLS.name} (${String.format("%.2f", mlsProgress * 100)}%)",
            textAlign = TextAlign.Start,
            style = MaterialTheme.wireTypography.title03,
            color = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensions().spacing4x)
        )
        WireLinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationParticipants() = WireTheme {
    GroupConversationParticipants({}, GroupConversationParticipantsState.PREVIEW, rememberLazyListState())
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationParticipantsAdandonedOneOnOne() = WireTheme {
    GroupConversationParticipants({}, GroupConversationParticipantsState.PREVIEW, rememberLazyListState())
}

@PreviewMultipleThemes
@Composable
fun PreviewMLSProgressIndicator() = WireTheme {
    MLSProgressIndicator(0.25F)
}
