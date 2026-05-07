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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ParticipantItem(
    participant: UICallParticipant,
    modifier: Modifier = Modifier,
) {
    RowItemTemplate(
        leadingIcon = {
            UserProfileAvatar(
                UserAvatarData(asset = participant.avatar, nameBasedAvatar = NameBasedAvatar(participant.name, participant.accentId)),
            )
        },
        titleStartPadding = dimensions().spacing0x,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing6x)
            ) {
                Text(
                    text = participant.name.orEmpty(),
                    style = typography().title02,
                    color = colorsScheme().onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MembershipQualifierLabel(membership = participant.membership)
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
            ) {
                if (participant.isSharingScreen) {
                    ActionIcon(
                        icon = R.drawable.ic_screen_share,
                        contentDescription = R.string.content_description_calling_screen_share_on,
                    )
                }
                if (participant.isCameraOn) {
                    ActionIcon(
                        icon = R.drawable.ic_camera_on,
                        contentDescription = R.string.content_description_calling_camera_on,
                    )
                }
                if (participant.isMuted) {
                    ActionIcon(
                        icon = R.drawable.ic_microphone_off,
                        contentDescription = R.string.content_description_calling_microphone_off,
                    )
                } else {
                    ActionIcon(
                        icon = R.drawable.ic_microphone_on,
                        contentDescription = R.string.content_description_calling_microphone_on,
                        active = participant.isSpeaking,
                    )
                }
            }
        },
        modifier = modifier.padding(start = dimensions().spacing8x),
    )
}

@Composable
private fun ActionIcon(
    @DrawableRes icon: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(contentDescription),
        tint = if (active) colorsScheme().primary else colorsScheme().onSurface,
        modifier = modifier
            .let {
                if (active) {
                    it
                        .border(
                            color = colorsScheme().primary,
                            width = dimensions().spacing1x,
                            shape = RoundedCornerShape(dimensions().spacing3x)
                        )
                        .background(
                            color = colorsScheme().primaryVariant,
                            shape = RoundedCornerShape(dimensions().spacing3x)
                        )
                } else {
                    it
                }
            }
            .padding(dimensions().spacing3x)
    )
}

private val previewParticipant = buildPreviewParticipantsList(1).first()

@PreviewMultipleThemes
@Composable
fun PreviewParticipantItem_Muted() = WireTheme {
    ParticipantItem(participant = previewParticipant.copy(isMuted = true))
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantItem_NotMuted() = WireTheme {
    ParticipantItem(participant = previewParticipant.copy(isMuted = false))
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantItem_Speaking() = WireTheme {
    ParticipantItem(participant = previewParticipant.copy(isMuted = false, isSpeaking = true))
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantItem_NotMutedWithCamera() = WireTheme {
    ParticipantItem(participant = previewParticipant.copy(isMuted = false, isCameraOn = true))
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantItem_NotMutedWithScreenShare() = WireTheme {
    ParticipantItem(participant = previewParticipant.copy(isMuted = false, isSharingScreen = true))
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantItem_MutedGuest() = WireTheme {
    ParticipantItem(participant = previewParticipant.copy(isMuted = true, membership = Membership.Guest))
}
