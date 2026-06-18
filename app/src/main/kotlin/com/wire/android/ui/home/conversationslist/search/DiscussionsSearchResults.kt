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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesEmptyScreen
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNoResultsScreen
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.rememberCurrentTimeProvider
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toJavaInstant
import java.util.Date

@Composable
fun DiscussionsSearchResults(
    state: DiscussionsSearchState,
    onDiscussionClick: (DiscussionClusterSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        DiscussionsSearchState.EmptyQuery -> SearchConversationMessagesEmptyScreen(modifier)
        DiscussionsSearchState.Loading -> LoadingListContent(modifier)
        DiscussionsSearchState.NoResults -> SearchConversationMessagesNoResultsScreen(modifier)
        is DiscussionsSearchState.Success -> DiscussionsSearchResultsList(
            discussions = state.discussions,
            onDiscussionClick = onDiscussionClick,
            modifier = modifier
        )
    }
}

@Composable
private fun DiscussionsSearchResultsList(
    discussions: List<DiscussionClusterSummary>,
    onDiscussionClick: (DiscussionClusterSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = dimensions().spacing16x,
            vertical = dimensions().spacing8x
        )
    ) {
        items(discussions) { discussion ->
            DiscussionSearchResultCard(
                discussion = discussion,
                onClick = { onDiscussionClick(discussion) },
                modifier = Modifier.padding(vertical = dimensions().spacing4x)
            )
        }
    }
}

@Composable
private fun DiscussionSearchResultCard(
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
            modifier = Modifier.padding(dimensions().spacing16x)
        ) {
            Text(
                text = discussion.firstMessageDate.toDisplayDate(currentTimeProvider),
                style = MaterialTheme.wireTypography.body02,
                color = colorsScheme().onSurfaceVariant
            )
            discussion.topic?.takeIf { it.isNotBlank() }?.let { topic ->
                Text(
                    text = topic,
                    modifier = Modifier.padding(top = dimensions().spacing8x),
                    style = MaterialTheme.wireTypography.body01,
                    color = colorsScheme().onBackground
                )
            }
            Text(
                text = discussion.conversationName,
                modifier = Modifier.padding(top = dimensions().spacing8x),
                style = MaterialTheme.wireTypography.body02,
                color = colorsScheme().onBackground
            )
            Text(
                text = discussion.participantFirstNames().joinToString(separator = ", "),
                modifier = Modifier.padding(top = dimensions().spacing4x),
                style = MaterialTheme.wireTypography.body02,
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
