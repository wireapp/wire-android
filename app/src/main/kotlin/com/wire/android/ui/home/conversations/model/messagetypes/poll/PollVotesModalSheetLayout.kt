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

package com.wire.android.ui.home.conversations.model.messagetypes.poll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.ModalSheetHeaderItem
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun PollVotesModalSheetLayout(
    sheetState: WireModalSheetState<UIMessageContent.Poll>,
    modifier: Modifier = Modifier
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        modifier = modifier,
        sheetContent = { poll ->
            PollVotesSheetContent(poll = poll)
        }
    )
}

@Composable
private fun PollVotesSheetContent(poll: UIMessageContent.Poll) {
    ModalSheetHeaderItem(
        header = MenuModalSheetHeader.Visible(title = stringResource(R.string.poll_votes_title))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing12x
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x)
    ) {
        if (poll.votes.isEmpty()) {
            Text(
                text = stringResource(R.string.poll_no_votes),
                color = MaterialTheme.wireColorScheme.secondaryText,
                style = MaterialTheme.wireTypography.body01
            )
        } else {
            poll.options.forEachIndexed { index, option ->
                PollVotesOptionSection(
                    option = option,
                    votes = poll.votes.filter { option.id in it.selectedOptionIds },
                    hideVoterNames = poll.hideVoterNames
                )
                if (index != poll.options.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.wireColorScheme.divider)
                }
            }
        }
    }
}

@Composable
private fun PollVotesOptionSection(
    option: UIMessageContent.Poll.Option,
    votes: List<UIMessageContent.Poll.Vote>,
    hideVoterNames: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.text,
                color = MaterialTheme.wireColorScheme.onSurface,
                style = MaterialTheme.wireTypography.body01,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = pluralStringResource(R.plurals.poll_votes_count, votes.size, votes.size),
                color = MaterialTheme.wireColorScheme.secondaryText,
                style = MaterialTheme.wireTypography.label01
            )
        }

        when {
            votes.isEmpty() -> PollVotesHint(text = stringResource(R.string.poll_no_votes))
            hideVoterNames -> PollVotesHint(text = stringResource(R.string.poll_votes_hidden))
            else -> votes.forEach { vote -> PollVoteRow(vote = vote) }
        }
    }
}

@Composable
private fun PollVotesHint(text: String) {
    Text(
        text = text,
        color = MaterialTheme.wireColorScheme.secondaryText,
        style = MaterialTheme.wireTypography.body02
    )
}

@Composable
private fun PollVoteRow(vote: UIMessageContent.Poll.Vote) {
    val displayName = vote.voterName ?: stringResource(R.string.poll_unknown_voter)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileAvatar(
            avatarData = vote.voterAvatarData ?: UserAvatarData(
                nameBasedAvatar = NameBasedAvatar(fullName = displayName, accentColor = -1)
            ),
            size = dimensions().spacing32x,
            padding = 0.dp,
            type = UserProfileAvatarType.WithoutIndicators
        )
        Text(
            text = displayName,
            color = MaterialTheme.wireColorScheme.onSurface,
            style = MaterialTheme.wireTypography.body01,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
