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
package com.wire.android.feature.meetings.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingLink
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.common.R as CommonR

@Composable
fun MeetingInvitationCard(meetingLink: MeetingLink, modifier: Modifier = Modifier, onClose: () -> Unit = {}) {
    Surface(
        modifier = modifier
            .widthIn(max = dimensions().callingControlPanelMaxWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(dimensions().spacing12x),
        color = colorsScheme().background,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = dimensions().spacing16x, top = dimensions().spacing8x),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = dimensions().spacing16x, end = dimensions().spacing2x)
            ) {
                Text(
                    text = stringResource(R.string.meeting_invite_guests_title),
                    style = typography().title01.copy(textAlign = TextAlign.Start),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = CommonR.drawable.ic_close),
                        contentDescription = stringResource(R.string.meeting_invite_guests_close_content_description)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
                modifier = Modifier.padding(horizontal = dimensions().spacing16x)
            ) {
                MeetingLinkSurface(link = meetingLink.url, maxLines = 1)
                MeetingLinkCopyButton(link = meetingLink.url, onLinkCopied = onClose, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun MeetingInvitationPreview() = WireTheme {
    MeetingInvitationCard(meetingLink = MeetingLink("https://account.wire.com/meeting-join/?key=example&code=example&domain=wire.com"))
}
