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

package com.wire.android.ui.home.conversationslist.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.rememberCurrentTimeProvider
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toLocalDateTime
import java.util.Date

@Composable
internal fun DiscussionSearchResultCard(
    discussion: DiscussionClusterSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentTimeProvider: CurrentTimeProvider = rememberCurrentTimeProvider()
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorsScheme().surface),
        border = BorderStroke(dimensions().spacing1x, colorsScheme().divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions().spacing16x)
        ) {
            Text(
                modifier = Modifier.align(Alignment.End),
                text = discussion.firstMessageDate.toDisplayDate(currentTimeProvider),
                style = MaterialTheme.wireTypography.subline01,
                color = colorsScheme().onSurfaceVariant
            )
            discussion.topic?.takeIf { it.isNotBlank() }?.let { topic ->
                Text(
                    text = topic,
                    modifier = Modifier.padding(top = dimensions().spacing8x),
                    style = MaterialTheme.wireTypography.title02,
                    fontWeight = FontWeight.Normal,
                    color = colorsScheme().onBackground
                )
            }
            Text(
                text = discussion.conversationName,
                modifier = Modifier.padding(top = dimensions().spacing8x),
                style = MaterialTheme.wireTypography.body01,
                fontWeight = FontWeight.Normal,
                color = colorsScheme().onBackground
            )
            Text(
                text = discussion.participantFirstNames().joinToString(separator = ", "),
                modifier = Modifier.padding(top = dimensions().spacing4x),
                style = MaterialTheme.wireTypography.subline01,
                fontWeight = FontWeight.Normal,
                color = colorsScheme().onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Instant.toDisplayDate(currentTimeProvider: CurrentTimeProvider): String {
    val timeZone = TimeZone.currentSystemDefault()
    val currentDate = currentTimeProvider().toLocalDateTime(timeZone).date
    return when (toDiscussionDateLabel(currentDate, timeZone)) {
        DiscussionDateLabel.Today -> stringResource(R.string.search_results_discussions_today)
        DiscussionDateLabel.Yesterday -> stringResource(R.string.search_results_discussions_yesterday)
        DiscussionDateLabel.Exact -> DateAndTimeParsers.toMediumOnlyDateTime(Date.from(toJavaInstant()))
    }
}

internal fun DiscussionClusterSummary.participantFirstNames(): List<String> =
    participants.mapNotNull { name ->
        name.trim()
            .substringBefore(' ')
            .takeIf { it.isNotBlank() }
    }

internal fun Instant.toDiscussionDateLabel(
    currentDate: LocalDate,
    timeZone: TimeZone
): DiscussionDateLabel = when (toLocalDateTime(timeZone).date) {
    currentDate -> DiscussionDateLabel.Today
    currentDate.minus(1, DateTimeUnit.DAY) -> DiscussionDateLabel.Yesterday
    else -> DiscussionDateLabel.Exact
}

internal enum class DiscussionDateLabel {
    Today,
    Yesterday,
    Exact
}

@PreviewMultipleThemes
@Composable
private fun PreviewDiscussionSearchResultCard() = WireTheme {
    val previewDate = Instant.parse("2026-06-18T10:00:00Z")

    DiscussionSearchResultCard(
        discussion = DiscussionClusterSummary(
            topic = "Release planning",
            conversationId = ConversationId("conversation", "example.com"),
            firstMessageId = "first-cluster-message",
            conversationName = "Project Alpha",
            firstMessageDate = previewDate,
            lastMessageDate = Instant.parse("2026-06-18T11:00:00Z"),
            participants = listOf("Alice Adams", "Bob Brown", "Charlie Clark")
        ),
        onClick = {},
        modifier = Modifier.padding(dimensions().spacing16x),
        currentTimeProvider = CurrentTimeProvider { previewDate }
    )
}
