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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.wire.android.feature.meetings.R
import com.wire.android.model.Contact
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.typography
import com.wire.android.util.ui.MarkdownTextStyle
import com.wire.android.util.ui.markdownText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.user.UserId

@Composable
internal fun meetingParticipantFailureText(participants: Map<UserId, CoreFailure>, contacts: List<Contact>): AnnotatedString {
    if (participants.isEmpty()) return AnnotatedString("")
    val failures = participants.entries
    val details = failures.groupBy { (userId, failure) ->
        val reason = failure.displayReason()
        reason to userId.domain.takeIf { reason == Reason.OfflineBackend }
    }
        .map { (group, failed) ->
            val names = failed.map { (userId, _) ->
                contacts.find { it.id == userId.value && it.domain == userId.domain }?.name ?: userId.value
            }
            val (singleRes, pluralRes) = group.first.detailResources()
            if (failures.size == 1) {
                stringResource(singleRes, names.single(), group.second.orEmpty())
            } else {
                pluralStringResource(pluralRes, names.size, names.dropLast(1).joinToString(", "), names.last(), group.second.orEmpty())
            }
        }
    val summary = if (failures.size > 1) stringResource(R.string.meeting_participants_failed_summary, failures.size) + "\n\n" else ""
    return markdownText(
        "\n\n" + summary + details.joinToString("\n\n"),
        MarkdownTextStyle(
            normalStyle = typography().body01,
            boldStyle = typography().body01.copy(fontWeight = FontWeight.Bold),
            normalColor = colorsScheme().onBackground,
            boldColor = colorsScheme().onBackground,
        ),
    )
}

private fun Reason.detailResources(): Pair<Int, Int> = when (this) {
    Reason.NonFederatingBackends -> R.string.meeting_participant_failed_federation to R.plurals.meeting_participants_failed_federation
    Reason.OfflineBackend -> R.string.meeting_participant_failed_offline to R.plurals.meeting_participants_failed_offline
    Reason.Unknown -> R.string.meeting_participant_failed_unknown to R.plurals.meeting_participants_failed_unknown
    Reason.NotMlsCapable -> R.string.meeting_participant_failed_mls to R.plurals.meeting_participants_failed_mls
}

private enum class Reason { NonFederatingBackends, OfflineBackend, Unknown, NotMlsCapable }

private fun CoreFailure.displayReason(): Reason = when (this) {
    is CoreFailure.MissingKeyPackages -> Reason.NotMlsCapable
    is NetworkFailure.FederatedBackendFailure.ConflictingBackends,
    is NetworkFailure.FederatedBackendFailure.FederationDenied,
    is NetworkFailure.FederatedBackendFailure.FederationNotEnabled -> Reason.NonFederatingBackends
    is NetworkFailure.FederatedBackendFailure.FailedDomains -> Reason.OfflineBackend
    else -> Reason.Unknown
}
