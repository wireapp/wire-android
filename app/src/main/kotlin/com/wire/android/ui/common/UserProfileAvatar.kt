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

package com.wire.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlin.math.sqrt

/**
 * @param avatarData data for the avatar
 * @param modifier modifier for the avatar composable
 * @param size size of the inner avatar itself, without any padding or indicators borders, if [UserProfileAvatarType.WithIndicators] then
 * composable will be larger than this specified size by the indicators borders widths, if padding is specified it will also be added to
 * the final composable size
 * @param padding padding around the avatar and indicator borders
 * @param clickable clickable callback for the avatar
 * @param showPlaceholderIfNoAsset if true, will show default avatar if asset is null
 * @param withCrossfadeAnimation if true, will animate the avatar change
 * @param type type of the avatar, if [UserProfileAvatarType.WithIndicators] then composable will be larger by the indicators borders
 */
@Composable
fun UserProfileAvatar(
    avatarData: UserAvatarData,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.wireDimensions.avatarDefaultSize,
    padding: Dp = MaterialTheme.wireDimensions.avatarClickablePadding,
    clickable: Clickable? = null,
    showPlaceholderIfNoAsset: Boolean = true,
    withCrossfadeAnimation: Boolean = false,
    type: UserProfileAvatarType = UserProfileAvatarType.WithIndicators(legalHoldIndicatorVisible = false),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .let {
                if (clickable != null) it
                    .clip(CircleShape)
                    .clickable(clickable) else it
            }
            .padding(padding)
    ) {
        val painter = painter(avatarData, showPlaceholderIfNoAsset, withCrossfadeAnimation)
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.content_description_user_avatar),
            modifier = Modifier
                .size(
                    when (type) {
                        is UserProfileAvatarType.WithIndicators -> {
                            // indicator borders need to be taken into account, the avatar itself will be smaller by the borders widths
                            size + (max(dimensions().avatarStatusBorderSize, dimensions().avatarLegalHoldIndicatorBorderSize) * 2)
                        }
                        UserProfileAvatarType.WithoutIndicators -> {
                            // indicator borders don't need to be taken into account, the avatar itself will take all available space
                            size
                        }
                    }
                )
                .let {
                    if (type is UserProfileAvatarType.WithIndicators) {
                        if (type.legalHoldIndicatorVisible) {
                            it
                                .border(
                                    width = dimensions().avatarLegalHoldIndicatorBorderSize / 2,
                                    shape = CircleShape,
                                    color = colorsScheme().error.copy(alpha = 0.3f)
                                )
                                .padding(dimensions().avatarLegalHoldIndicatorBorderSize / 2)
                                .border(
                                    width = dimensions().avatarLegalHoldIndicatorBorderSize / 2,
                                    shape = CircleShape,
                                    color = colorsScheme().error.copy(alpha = 1.0f)
                                )
                                .padding(dimensions().avatarLegalHoldIndicatorBorderSize / 2)
                        } else {
                            it
                                // this is to make the border of the avatar to be the same size as with the legal hold indicator
                                .padding(dimensions().avatarLegalHoldIndicatorBorderSize - dimensions().spacing1x)
                                .border(
                                    width = dimensions().spacing1x,
                                    shape = CircleShape,
                                    color = colorsScheme().outline
                                )
                                .padding(dimensions().spacing1x)
                        }
                    } else it
                }
                .clip(CircleShape)
                .testTag("User avatar"),
            contentScale = ContentScale.Crop
        )
        if (type is UserProfileAvatarType.WithIndicators) {
            val avatarWithLegalHoldRadius = (size.value / 2f) + dimensions().avatarLegalHoldIndicatorBorderSize.value
            val statusRadius = (dimensions().userAvatarStatusSize - dimensions().avatarStatusBorderSize).value / 2f
            // calculated using the trigonometry so that the status is always in the right place according to the avatar
            val paddingToAlignWithAvatar = ((sqrt(2f) - 1f) * avatarWithLegalHoldRadius + (1f - sqrt(2f)) * statusRadius) / sqrt(2f)
            UserStatusIndicator(
                status = avatarData.availabilityStatus,
                modifier = Modifier
                    // on designs the status border extends beyond the avatar's perimeter so we need to subtract it's size from the padding
                    .padding(paddingToAlignWithAvatar.dp - dimensions().avatarStatusBorderSize)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

sealed class UserProfileAvatarType {

    // this will take the indicators into account when calculating avatar size so the composable itself will be larger by the borders
    data class WithIndicators(val legalHoldIndicatorVisible: Boolean) : UserProfileAvatarType()

    // this will not take the indicators into account when calculating avatar size so the avatar itself will be exactly as specified size
    data object WithoutIndicators : UserProfileAvatarType()
}

/**
 * Workaround to have profile avatar available for preview
 * @see [painter] https://developer.android.com/jetpack/compose/tooling
 */
@Composable
private fun painter(
    data: UserAvatarData,
    showPlaceholderIfNoAsset: Boolean = true,
    withCrossfadeAnimation: Boolean = false,
): Painter = when {
    LocalInspectionMode.current -> {
        getDefaultAvatar(membership = data.membership)
    }

    data.connectionState == ConnectionState.BLOCKED -> {
        painterResource(id = R.drawable.ic_blocked_user_avatar)
    }

    data.asset == null -> {
        if (showPlaceholderIfNoAsset) getDefaultAvatar(membership = data.membership)
        else ColorPainter(Color.Transparent)
    }

    else -> {
        data.asset.paint(getDefaultAvatarResourceId(membership = data.membership), withCrossfadeAnimation)
    }
}

@Composable
private fun getDefaultAvatar(membership: Membership): Painter =
    painterResource(id = getDefaultAvatarResourceId(membership))

@Composable
private fun getDefaultAvatarResourceId(membership: Membership): Int =
    if (membership == Membership.Service) {
        R.drawable.ic_default_service_avatar
    } else {
        R.drawable.ic_default_user_avatar
    }

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatar() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithLegalHold() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
            type = UserProfileAvatarType.WithIndicators(legalHoldIndicatorVisible = true)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLargeUserProfileAvatarWithLegalHold() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
            size = 48.dp,
            type = UserProfileAvatarType.WithIndicators(legalHoldIndicatorVisible = true)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithoutIndicators() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(),
            padding = 0.dp,
            size = 48.dp,
            type = UserProfileAvatarType.WithoutIndicators,
        )
    }
}
