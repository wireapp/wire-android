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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.common

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.nonScaledSp
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.hours

const val MINUTES_IN_DAY = 60 * 24
const val LEGAL_HOLD_INDICATOR_TEST_TAG = "legal_hold_indicator"
const val TEMP_USER_INDICATOR_TEST_TAG = "temp_user_indicator"

sealed class UserProfileAvatarType {

    /**
     * This avatar has indicators in the form of borders around the avatar.
     */
    sealed class WithIndicators : UserProfileAvatarType() {
        // this will take the indicators into account when calculating avatar size so the composable itself will be larger by the borders
        data class LegalHold(val legalHoldIndicatorVisible: Boolean) : WithIndicators()
        data class TemporaryUser(val expiresAt: Instant) : WithIndicators()
    }

    /**
     * This will not take the indicators into account when calculating avatar size so the avatar itself will be exactly as specified size
     */
    data object WithoutIndicators : UserProfileAvatarType()
}

/**
 * @param avatarData data for the avatar
 * @param modifier modifier for the avatar composable
 * @param size size of the inner avatar itself, without any padding or indicators borders, if [UserProfileAvatarType.WithIndicators] then
 * composable will be larger than this specified size by the indicators borders widths, if padding is specified it will also be added to
 * the final composable size
 * @param padding padding around the avatar and indicator borders
 * @param avatarBorderSize border of the avatar to override as base
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
    avatarBorderSize: Dp = MaterialTheme.wireDimensions.avatarLegalHoldIndicatorBorderSize,
    clickable: Clickable? = null,
    showPlaceholderIfNoAsset: Boolean = true,
    withCrossfadeAnimation: Boolean = false,
    type: UserProfileAvatarType = UserProfileAvatarType.WithIndicators.LegalHold(legalHoldIndicatorVisible = false),
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
        UserAvatar(avatarData, showPlaceholderIfNoAsset, withCrossfadeAnimation, type, size, avatarBorderSize)
        if (type is UserProfileAvatarType.WithIndicators.LegalHold) {
            val avatarWithLegalHoldRadius = (size.value / 2f) + avatarBorderSize.value
            val statusRadius = (dimensions().userAvatarStatusSize - dimensions().avatarStatusBorderSize).value / 2f
            // calculated using the trigonometry so that the status is always in the right place according to the avatar
            val paddingToAlignWithAvatar = ((sqrt(2f) - 1f) * avatarWithLegalHoldRadius + (1f - sqrt(2f)) * statusRadius) / sqrt(2f)
            UserStatusIndicator(
                status = avatarData.availabilityStatus,
                modifier = Modifier
                    // on designs the status border extends beyond the avatar's perimeter so we need to subtract it's size from the padding
                    .padding(paddingToAlignWithAvatar.dp - dimensions().avatarStatusBorderSize)
                    .align(Alignment.BottomEnd)
                    .testTag(LEGAL_HOLD_INDICATOR_TEST_TAG)
            )
        }
        if (type is UserProfileAvatarType.WithIndicators.TemporaryUser) {
            CircularProgressIndicator(
                progress = (type.expiresAt.minus(Clock.System.now()).inWholeMinutes.toFloat() / MINUTES_IN_DAY.toFloat()).absoluteValue,
                color = colorsScheme().wireAccentColors.getOrDefault(Accent.Blue, Color.Transparent),
                strokeWidth = avatarBorderSize,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .scale(scaleX = -1f, scaleY = 1f)
                    .testTag(TEMP_USER_INDICATOR_TEST_TAG)
            )
        }
    }
}

@Composable
private fun UserAvatar(
    avatarData: UserAvatarData,
    showPlaceholderIfNoAsset: Boolean,
    withCrossfadeAnimation: Boolean,
    type: UserProfileAvatarType,
    size: Dp,
    avatarBorderSize: Dp
) {
    if (avatarData.shouldPreferNameBasedAvatar()) {
        DefaultInitialsAvatar(avatarData.nameBasedAvatar!!, type, avatarBorderSize, size)
        return
    }
    val painter = painter(avatarData, showPlaceholderIfNoAsset, withCrossfadeAnimation)
    Image(
        painter = painter,
        contentDescription = stringResource(R.string.content_description_user_avatar),
        modifier = Modifier
            .withAvatarSize(size, avatarBorderSize, dimensions().avatarStatusBorderSize, type)
            .let { withAvatarBorders(type, avatarBorderSize, it) }
            .clip(CircleShape)
            .testTag("User avatar"),
        contentScale = ContentScale.Crop
    )
}

@SuppressLint("ComposeModifierMissing")
@Composable
private fun DefaultInitialsAvatar(
    nameBasedAvatar: NameBasedAvatar,
    type: UserProfileAvatarType,
    avatarBorderSize: Dp,
    size: Dp = MaterialTheme.wireDimensions.avatarDefaultSize
) {
    val contentDescription = stringResource(R.string.content_description_user_avatar)
    Box(
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .withAvatarSize(size, avatarBorderSize, dimensions().avatarStatusBorderSize, type)
            .let { withAvatarBorders(type, avatarBorderSize, it) }
            .clip(CircleShape)
            .background(
                if (type is UserProfileAvatarType.WithIndicators.TemporaryUser) {
                    colorsScheme().wireAccentColors.getOrDefault(Accent.Unknown, colorsScheme().secondaryText)
                } else {
                    colorsScheme().wireAccentColors.getOrDefault(
                        Accent.fromAccentId(nameBasedAvatar.accentColor),
                        colorsScheme().secondaryText
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nameBasedAvatar.initials,
            color = MaterialTheme.wireColorScheme.onPrimaryButtonSelected,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            fontSize = (size.value.sp / 2.5).nonScaledSp
        )
    }
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

/**
 * Calculate the avatar borders based on the type of the avatar.
 * If the legal hold indicator is visible then the avatar will have two borders
 */
@Composable
private fun withAvatarBorders(type: UserProfileAvatarType, avatarBorderSize: Dp, it: Modifier = Modifier) =
    if (type is UserProfileAvatarType.WithIndicators.LegalHold) {
        if (type.legalHoldIndicatorVisible) {
            it
                .border(
                    width = avatarBorderSize / 2,
                    shape = CircleShape,
                    color = colorsScheme().error.copy(alpha = 0.3f)
                )
                .padding(avatarBorderSize / 2)
                .border(
                    width = avatarBorderSize / 2,
                    shape = CircleShape,
                    color = colorsScheme().error.copy(alpha = 1.0f)
                )
                .padding(avatarBorderSize / 2)
        } else {
            it
                // this is to make the border of the avatar to be the same size as with the legal hold indicator
                .padding(avatarBorderSize - dimensions().spacing1x)
                .border(
                    width = dimensions().spacing1x,
                    shape = CircleShape,
                    color = colorsScheme().outline
                )
                .padding(dimensions().spacing1x)
        }
    } else it

/**
 * Calculate the size of the avatar based on the type (legal hold enabled or not) of the avatar and the size of the avatar itself
 */
private fun Modifier.withAvatarSize(
    size: Dp,
    avatarBorderSize: Dp,
    avatarStatusBorderSize: Dp,
    type: UserProfileAvatarType
): Modifier {
    return size(
        when (type) {
            is UserProfileAvatarType.WithIndicators.LegalHold -> {
                // indicator borders need to be taken into account, the avatar itself will be smaller by the borders widths
                size + (max(avatarBorderSize, avatarStatusBorderSize) * 2)
            }

            is UserProfileAvatarType.WithIndicators.TemporaryUser,
            is UserProfileAvatarType.WithoutIndicators -> {
                // indicator borders don't need to be taken into account, the avatar itself will take all available space
                size
            }
        }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatar() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                nameBasedAvatar = NameBasedAvatar("Jon Doe", -1)
            ),
            type = UserProfileAvatarType.WithIndicators.LegalHold(legalHoldIndicatorVisible = false)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithLegalHold() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
            type = UserProfileAvatarType.WithIndicators.LegalHold(legalHoldIndicatorVisible = true)
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
            type = UserProfileAvatarType.WithIndicators.LegalHold(legalHoldIndicatorVisible = true)
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

@PreviewMultipleThemes
@Composable
fun PreviewTempUserCustomIndicators() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(),
            padding = 4.dp,
            size = dimensions().avatarDefaultBigSize,
            type = UserProfileAvatarType.WithIndicators.TemporaryUser(expiresAt = Clock.System.now().plus(1.hours)),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTempUserSmallAvatarCustomIndicators() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(),
            modifier = Modifier.padding(
                start = dimensions().spacing8x
            ),
            avatarBorderSize = 2.dp,
            type = UserProfileAvatarType.WithIndicators.TemporaryUser(expiresAt = Clock.System.now().plus(10.hours)),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithInitialsBig() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(nameBasedAvatar = NameBasedAvatar("Juan Roman Riquelme", -1)),
            padding = 4.dp,
            size = dimensions().avatarDefaultBigSize,
            type = UserProfileAvatarType.WithoutIndicators,
        )
    }
}

@Preview(fontScale = 3f)
@Composable
fun PreviewUserProfileAvatarSmallest() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(nameBasedAvatar = NameBasedAvatar("Juan", -1)),
            padding = 4.dp,
            size = dimensions().spacing16x,
            type = UserProfileAvatarType.WithoutIndicators,
        )
    }
}
