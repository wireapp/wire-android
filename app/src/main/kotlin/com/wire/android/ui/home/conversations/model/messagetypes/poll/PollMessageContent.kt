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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.highlighted
import com.wire.android.ui.home.conversations.messages.item.onBackground
import com.wire.android.ui.home.conversations.messages.item.surface
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.wireTypography

@Composable
fun PollMessageContent(
    poll: UIMessageContent.Poll,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    onOptionSelected: (List<String>) -> Unit = {}
) {
    val totalVoters = poll.votes.map { it.voterId }.distinct().size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing10x)
    ) {
        Text(
            text = poll.question,
            color = messageStyle.onBackground(),
            style = MaterialTheme.wireTypography.title03,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = stringResource(
                id = if (poll.allowMultipleAnswers) {
                    R.string.poll_select_one_or_more
                } else {
                    R.string.poll_select_one
                }
            ),
            color = messageStyle.textColor(),
            style = MaterialTheme.wireTypography.label02
        )

        Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing12x)) {
            poll.options.forEach { option ->
                val isSelected = option.id in poll.selectedOptionIds
                val selectedOptionIds = if (poll.allowMultipleAnswers) {
                    when {
                        isSelected -> poll.selectedOptionIds - option.id
                        else -> poll.selectedOptionIds + option.id
                    }
                } else {
                    listOf(option.id)
                }
                val voteCount = poll.votes.count { vote -> option.id in vote.selectedOptionIds }
                PollOptionResult(
                    text = option.text,
                    isSelected = isSelected,
                    voteCount = voteCount,
                    voteFraction = if (totalVoters == 0) 0f else voteCount.toFloat() / totalVoters.toFloat(),
                    messageStyle = messageStyle,
                    onClick = {
                        if (poll.allowMultipleAnswers || !isSelected) {
                            onOptionSelected(selectedOptionIds)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PollOptionResult(
    text: String,
    isSelected: Boolean,
    voteCount: Int,
    voteFraction: Float,
    messageStyle: MessageStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) messageStyle.highlighted() else messageStyle.textColor(),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = messageStyle.highlighted(),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.size(dimensions().spacing8x))

            Text(
                text = text,
                color = messageStyle.onBackground(),
                style = MaterialTheme.wireTypography.body01,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = voteCount.toString(),
                color = messageStyle.onBackground(),
                style = MaterialTheme.wireTypography.body01,
                fontWeight = FontWeight.SemiBold
            )
        }

        VerticalSpace.x4()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(messageStyle.surface().copy(alpha = 0.5f))
        ) {
            if (voteFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(voteFraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(messageStyle.highlighted())
                )
            }
        }
    }
}
