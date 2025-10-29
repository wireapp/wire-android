/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.common.avatar

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.google.common.primitives.Floats.min
import com.wire.android.model.Clickable
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.R
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.nonScaledSp
import com.wire.android.util.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.hours

const val MINUTES_IN_DAY = 60 * 24
const val STATUS_INDICATOR_TEST_TAG = "status_indicator"
const val UNREAD_INFO_TEST_TAG = "status_indicator"
const val LEGAL_HOLD_INDICATOR_TEST_TAG = "legal_hold_indicator"
const val TEMP_USER_INDICATOR_TEST_TAG = "temp_user_indicator"
const val USER_AVATAR_TEST_TAG = "User avatar"

sealed class UserProfileAvatarType {

    /**
     * This avatar has indicators in the form of borders around the avatar.
     */
    sealed class WithIndicators : UserProfileAvatarType() {
        data class RegularUser(val legalHoldIndicatorVisible: Boolean = false) : WithIndicators()
        data class TemporaryUser(val expiresAt: Instant) : WithIndicators()
    }

    data object WithoutIndicators : UserProfileAvatarType()
}

/**
 * @param avatarData data for the avatar
 * @param modifier modifier for the avatar composable
 * @param size size of the inner avatar itself, without any padding or indicators borders, if [UserProfileAvatarType.WithIndicators] then
 * composable will be larger than this specified size by the indicators borders widths or padding, depending what is larger
 * @param padding padding around the avatar, any indication borders will be drawn on the padding area outside the avatar itself,
 * @param legalHoldBorderWidth width of the legal hold border around outside the avatar
 * @param temporaryUserBorderWidth width of the temporary user border around inside the avatar
 * @param statusBorderWidth width of the status indicator border - cutout around the status
 * @param statusSize size, diameter of the status indicator
 * @param avatarBorderWidth width of the border around the avatar
 * @param avatarBorderColor color of the border around the avatar
 * @param clickable clickable callback for the avatar
 * @param showPlaceholderIfNoAsset if true, will show default avatar if asset is null
 * @param withCrossfadeAnimation if true, will animate the avatar change
 * @param type type of the avatar, if [UserProfileAvatarType.WithIndicators] then composable will be larger by the indicators borders
 */
@Composable
fun UserProfileAvatar(
    avatarData: UserAvatarData,
    modifier: Modifier = Modifier,
    size: Dp = dimensions().avatarDefaultSize,
    padding: Dp = dimensions().avatarClickablePadding,
    legalHoldBorderWidth: Dp = dimensions().avatarLegalHoldIndicatorBorderWidth,
    temporaryUserBorderWidth: Dp = dimensions().avatarTemporaryUserBorderWidth,
    statusBorderWidth: Dp = dimensions().avatarStatusBorderWidth,
    statusSize: Dp = dimensions().avatarStatusSize,
    unReadIndicatorSize: Dp = dimensions().unReadIndicatorSize,
    avatarBorderWidth: Dp = dimensions().avatarBorderWidth,
    avatarBorderColor: Color = colorsScheme().outline,
    clickable: Clickable? = null,
    showPlaceholderIfNoAsset: Boolean = true,
    shouldShowCreateTeamUnreadIndicator: Boolean = false,
    withCrossfadeAnimation: Boolean = false,
    contentDescription: String? = null,
    type: UserProfileAvatarType = UserProfileAvatarType.WithIndicators.RegularUser(
        legalHoldIndicatorVisible = false
    ),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .clickable(clickable)
    ) {
        var userStatusIndicatorParams by remember { mutableStateOf(Size.Zero to Offset.Zero) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .drawWithContent { // this cuts out the user status circle from the avatar and borders
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)
                        drawContent()
                        drawOval(
                            color = Color.Black,
                            size = userStatusIndicatorParams.first,
                            topLeft = userStatusIndicatorParams.second,
                            blendMode = BlendMode.Clear,
                        )
                        restoreToCount(checkPoint)
                    }
                }
        ) {
            UserAvatar(
                avatarData = avatarData,
                showPlaceholderIfNoAsset = showPlaceholderIfNoAsset,
                withCrossfadeAnimation = withCrossfadeAnimation,
                type = type,
                size = size,
                contentDescription = contentDescription,
                modifier = Modifier
                    .padding(padding)
                    .clip(CircleShape)
                    .border(
                        width = avatarBorderWidth,
                        shape = CircleShape,
                        color = avatarBorderColor
                    )
                    .size(size)
                    .testTag(USER_AVATAR_TEST_TAG),
            )
            if (type is UserProfileAvatarType.WithIndicators.RegularUser && type.legalHoldIndicatorVisible) {
                LegalHoldIndicator(
                    borderWidth = legalHoldBorderWidth,
                    innerSize = size,
                    modifier = Modifier.testTag(LEGAL_HOLD_INDICATOR_TEST_TAG)
                )
            }
            if (type is UserProfileAvatarType.WithIndicators.TemporaryUser) {
                CircularProgressIndicator(
                    progress = (type.expiresAt.minus(Clock.System.now()).inWholeMinutes.toFloat() / MINUTES_IN_DAY.toFloat()).absoluteValue,
                    color = colorsScheme().wireAccentColors.getOrDefault(
                        Accent.Blue,
                        colorsScheme().primary
                    ),
                    strokeWidth = temporaryUserBorderWidth,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .scale(scaleX = -1f, scaleY = 1f)
                        .testTag(TEMP_USER_INDICATOR_TEST_TAG)
                )
            }
        }

        if (type is UserProfileAvatarType.WithIndicators.RegularUser) {
            // calculated using the trigonometry so that the status is always in the right place according to the avatar
            val exactPointOnAvatarBorder =
                sqrt(2f) / 2f * ((size.value / 2f) + avatarBorderWidth.value)
            val maxOffset = (size.value / 2f) - (statusSize.value / 2f) - -statusBorderWidth.value
            val maxOffsetUnreadIndicator = (size.value / 2f) - (unReadIndicatorSize.value / 3f)
            val offsetToAlignWithAvatar = min(maxOffset, exactPointOnAvatarBorder)
            val offsetToAlignUnreadIndicatorWithAvatar =
                min(maxOffsetUnreadIndicator, exactPointOnAvatarBorder)

            if (shouldShowCreateTeamUnreadIndicator) {
                UnreadInfoIndicator(
                    modifier = Modifier
                        .offset(
                            x = offsetToAlignUnreadIndicatorWithAvatar.dp,
                            y = -offsetToAlignUnreadIndicatorWithAvatar.dp
                        )
                        .testTag(UNREAD_INFO_TEST_TAG)
                )
            }

            UserStatusIndicator(
                status = avatarData.availabilityStatus,
                size = statusSize,
                borderWidth = statusBorderWidth,
                borderColor = Color.Transparent,
                modifier = Modifier
                    .offset(x = offsetToAlignWithAvatar.dp, y = offsetToAlignWithAvatar.dp)
                    .clip(CircleShape)
                    .onGloballyPositioned {
                        userStatusIndicatorParams = it.size.toSize() to it.positionInParent()
                    }
                    .testTag(STATUS_INDICATOR_TEST_TAG)
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
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.content_description_user_avatar)
) {
    if (avatarData.shouldPreferNameBasedAvatar()) {
        DefaultInitialsAvatar(
            nameBasedAvatar = avatarData.nameBasedAvatar!!,
            type = type,
            size = size,
            modifier = modifier,
            contentDescription = contentDescription
        )
    } else {
        val painter = painter(avatarData, showPlaceholderIfNoAsset, withCrossfadeAnimation)
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
private fun DefaultInitialsAvatar(
    nameBasedAvatar: NameBasedAvatar,
    type: UserProfileAvatarType,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (type is UserProfileAvatarType.WithIndicators.TemporaryUser) {
                    colorsScheme().wireAccentColors.getOrDefault(
                        Accent.Unknown,
                        colorsScheme().outline
                    )
                } else {
                    colorsScheme().wireAccentColors.getOrDefault(
                        Accent.fromAccentId(nameBasedAvatar.accentColor),
                        colorsScheme().secondaryText
                    )
                }
            )
            .then(semantics)
    ) {
        Text(
            text = nameBasedAvatar.initials,
            color = if (type is UserProfileAvatarType.WithIndicators.TemporaryUser) {
                colorsScheme().onSurface
            } else {
                colorsScheme().inverseOnSurface
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            fontSize = (size.value.sp / 2.5).nonScaledSp,
        )
    }
}

@Composable
private fun LegalHoldIndicator(borderWidth: Dp, innerSize: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(
                width = borderWidth / 2,
                shape = CircleShape,
                color = colorsScheme().error.copy(alpha = 0.3f)
            )
            .padding(borderWidth / 2)
            .border(
                width = borderWidth / 2,
                shape = CircleShape,
                color = colorsScheme().error.copy(alpha = 1.0f)
            )
            .padding(borderWidth / 2)
            .size(innerSize)
    )
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
        if (showPlaceholderIfNoAsset) {
            getDefaultAvatar(membership = data.membership)
        } else {
            ColorPainter(Color.Transparent)
        }
    }

    else -> {
        data.asset.paint(
            getDefaultAvatarResourceId(membership = data.membership),
            withCrossfadeAnimation
        )
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
            avatarData = UserAvatarData(
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                nameBasedAvatar = NameBasedAvatar("Jon Doe", -1)
            ),
            type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = false)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithLegalHold() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
            type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = true)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithInfoUnreadIndicator() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
            type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = false),
            shouldShowCreateTeamUnreadIndicator = true
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLargeUserProfileAvatarWithLegalHold() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
            size = 60.dp,
            type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = true)
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
            size = 60.dp,
            type = UserProfileAvatarType.WithoutIndicators,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTempUserBig() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(
                nameBasedAvatar = NameBasedAvatar(
                    "Juan Roman Riquelme",
                    -1
                )
            ),
            padding = 4.dp,
            size = dimensions().avatarDefaultBigSize,
            temporaryUserBorderWidth = dimensions().avatarBigTemporaryUserBorderWidth,
            type = UserProfileAvatarType.WithIndicators.TemporaryUser(
                expiresAt = Clock.System.now().plus(1.hours)
            ),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTempUserSmall() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(
                nameBasedAvatar = NameBasedAvatar(
                    "Juan Roman Riquelme",
                    -1
                )
            ),
            size = dimensions().spacing24x,
            padding = dimensions().spacing0x,
            type = UserProfileAvatarType.WithIndicators.TemporaryUser(
                expiresAt = Clock.System.now().plus(10.hours)
            ),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarWithInitialsBig() {
    WireTheme {
        UserProfileAvatar(
            avatarData = UserAvatarData(
                nameBasedAvatar = NameBasedAvatar(
                    "Juan Roman Riquelme",
                    -1
                )
            ),
            padding = 4.dp,
            size = dimensions().avatarDefaultBigSize,
            temporaryUserBorderWidth = dimensions().avatarBigTemporaryUserBorderWidth,
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
