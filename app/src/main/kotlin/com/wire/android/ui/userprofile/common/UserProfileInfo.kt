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

package com.wire.android.ui.userprofile.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.MLSVerifiedIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.android.ui.common.banner.SecurityClassificationBannerForUser
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.ifNotEmpty
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Suppress("ComposeParameterOrder", "CyclomaticComplexMethod")
@Composable
fun UserProfileInfo(
    userId: UserId?,
    isLoading: Boolean,
    avatarAsset: UserAvatarAsset?,
    fullName: String,
    userName: String,
    teamName: String?,
    membership: Membership = Membership.None,
    onUserProfileClick: (() -> Unit)? = null,
    editableState: EditableState,
    modifier: Modifier = Modifier,
    connection: ConnectionState = ConnectionState.ACCEPTED,
    delayToShowPlaceholderIfNoAsset: Duration = 200.milliseconds,
    isProteusVerified: Boolean = false,
    isMLSVerified: Boolean = false,
    expiresAt: Instant? = null,
    onQrCodeClick: (() -> Unit)? = null,
    accentId: Int = -1,
    showQrCode: Boolean = true,
) {
    Column(
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = dimensions().spacing16x)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val userAvatarData = UserAvatarData(
                asset = avatarAsset,
                connectionState = connection,
                membership = membership,
                nameBasedAvatar = NameBasedAvatar(fullName, accentId)
            )
            val showPlaceholderIfNoAsset = remember { mutableStateOf(!delayToShowPlaceholderIfNoAsset.isPositive()) }
            val currentAssetIsNull = rememberUpdatedState(avatarAsset == null)
            if (delayToShowPlaceholderIfNoAsset.isPositive()) {
                LaunchedEffect(Unit) {
                    delay(delayToShowPlaceholderIfNoAsset)
                    showPlaceholderIfNoAsset.value = currentAssetIsNull.value // show placeholder if there is still no proper avatar data
                }
            }
            Crossfade(
                targetState = userAvatarData to showPlaceholderIfNoAsset.value,
                label = "UserProfileInfoAvatar"
            ) { (userAvatarData, showPlaceholderIfNoAsset) ->
                val onAvatarClickDescription = stringResource(R.string.content_description_change_it_label)
                val contentDescription = if (editableState is EditableState.IsEditable) {
                    stringResource(R.string.content_description_self_profile_avatar)
                } else {
                    null
                }
                UserProfileAvatar(
                    size = dimensions().avatarDefaultBigSize,
                    temporaryUserBorderWidth = dimensions().avatarBigTemporaryUserBorderWidth,
                    avatarData = userAvatarData,
                    clickable = remember(editableState) {
                        Clickable(
                            enabled = editableState is EditableState.IsEditable,
                            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                            onClickDescription = onAvatarClickDescription,
                        ) { onUserProfileClick?.invoke() }
                    },
                    showPlaceholderIfNoAsset = showPlaceholderIfNoAsset,
                    withCrossfadeAnimation = true,
                    type = expiresAt?.let { UserProfileAvatarType.WithIndicators.TemporaryUser(expiresAt) }
                        ?: UserProfileAvatarType.WithoutIndicators,
                    contentDescription = contentDescription
                )
            }
            this@Column.AnimatedVisibility(visible = isLoading) {
                Box(
                    Modifier
                        .padding(dimensions().avatarClickablePadding)
                        .clip(CircleShape)
                        .background(MaterialTheme.wireColorScheme.background.copy(alpha = 0.6f))
                ) {
                    WireCircularProgressIndicator(
                        size = dimensions().spacing32x,
                        strokeWidth = dimensions().spacing4x,
                        progressColor = MaterialTheme.wireColorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        ConstraintLayout(
            modifier = Modifier
                .padding(horizontal = dimensions().spacing32x)
                .then(modifier)
        ) {
            val (displayName, username, qrIcon) = createRefs()

            val (text, inlineContent: MutableMap<String, InlineTextContent>) =
                processFullName(
                    fullName = fullName,
                    isLoading = isLoading,
                    isProteusVerified = isProteusVerified,
                    isMLSVerified = isMLSVerified
                )

            val profileNameDescription =
                stringResource(R.string.content_description_self_profile_profile_name, fullName)
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing16x)
                    .constrainAs(displayName) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .semantics(mergeDescendants = true) { contentDescription = profileNameDescription },
                text = text,
                // TODO. replace with MIDDLE_ELLIPSIS when available see https://issuetracker.google.com/issues/185418980
                overflow = TextOverflow.Visible,
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.wireTypography.title02,
                color = if (fullName.isNotBlank()) MaterialTheme.colorScheme.onBackground else MaterialTheme.wireColorScheme.secondaryText,
                inlineContent = inlineContent
            )

            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.constrainAs(username) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(displayName.bottom)
                }
            ) {
                val usernameDescription =
                    stringResource(R.string.content_description_self_profile_username, userName)
                Text(
                    text = processUsername(userName, membership, expiresAt),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.wireTypography.body02,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = usernameDescription }
                )
                UserBadge(
                    membership = membership,
                    connectionState = connection,
                    topPadding = dimensions().spacing8x
                )
            }

            Column(
                Modifier
                    .padding(top = dimensions().spacing0x, start = dimensions().spacing4x)
                    .constrainAs(qrIcon) {
                        start.linkTo(if (fullName.length > userName.length) displayName.end else username.end)
                        end.linkTo(parent.end)
                        top.linkTo(displayName.top)
                        bottom.linkTo(displayName.bottom)
                    }
            ) {
                if (showQrCode && isLoading.not()) {
                    onQrCodeClick?.let { QRCodeIcon(it) }
                }
            }
        }
        val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current

        if (localFeatureVisibilityFlags.UserProfileEditIcon) {
            if (editableState is EditableState.IsEditable) {
                ManageMemberButton(
                    modifier = Modifier
                        .padding(start = dimensions().spacing16x),
                    onEditClick = editableState.onEditClick
                )
            }
        }

        if (teamName != null) {
            val teamDescription = stringResource(R.string.content_description_self_profile_team, teamName)
            TeamInformation(
                modifier = Modifier
                    .padding(top = dimensions().spacing8x)
                    .padding(horizontal = dimensions().spacing16x)
                    .semantics(mergeDescendants = true) { contentDescription = teamDescription },
                teamName = teamName
            )
        }

        userId?.also {
            SecurityClassificationBannerForUser(
                userId = it,
                modifier = Modifier.padding(top = dimensions().spacing8x)
            )
        }
    }
}

@Composable
private fun processFullName(
    fullName: String,
    isLoading: Boolean,
    isProteusVerified: Boolean,
    isMLSVerified: Boolean,
): Pair<AnnotatedString, MutableMap<String, InlineTextContent>> {
    val proteusIcon = "proteusIcon"
    val mlsIcon = "mlsIcon"
    val text = buildAnnotatedString {
        val processedFullName = createMiddleEllipsizeIfNeeded(fullName)
        append(
            processedFullName.ifBlank {
                if (isLoading) ""
                else UIText.StringResource(R.string.username_unavailable_label).asString()
            }
        )

        if (isProteusVerified) appendInlineContent(proteusIcon, "[icon1]")
        if (isMLSVerified) appendInlineContent(mlsIcon, "[icon2]")
    }
    val inlineContent: MutableMap<String, InlineTextContent> = mutableMapOf()
    if (isProteusVerified) {
        inlineContent[proteusIcon] = InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom
            )
        ) { ProteusVerifiedIcon() }
    }
    if (isMLSVerified) {
        inlineContent[mlsIcon] = InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom
            )
        ) { MLSVerifiedIcon() }
    }
    return Pair(text, inlineContent)
}

// TODO. replace with proper Ellipsize behavior to be fixed by https://issuetracker.google.com/issues/185418980
// TODO. We then can pass the fullName without any processing and remove this function
private fun createMiddleEllipsizeIfNeeded(
    fullName: String,
    maxCharsToEllipsis: Int = 40
): String {
    return when {
        fullName.length <= maxCharsToEllipsis -> fullName
        else -> {
            val firstPart = fullName.take(maxCharsToEllipsis)
            "$firstPart..."
        }
    }
}

@Composable
fun QRCodeIcon(
    onQrCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(id = R.string.user_profile_qr_code_share_link)
    val clickDescription = stringResource(id = R.string.content_description_share_label)
    WireSecondaryButton(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        leadingIcon = Icons.Filled.QrCode.Icon(),
        contentPadding = PaddingValues(0.dp),
        onClick = onQrCodeClick,
        onClickDescription = clickDescription,
        fillMaxWidth = false,
        minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    )
}

@Composable
private fun processUsername(userName: String, membership: Membership, expiresAt: Instant?): String {
    return when {
        expiresAt != null -> UIText.StringResource(R.string.temporary_user_label, userName).asString()
        membership == Membership.Service -> userName
        else -> userName.ifNotEmpty { "@$userName" }
    }
}

@Composable
private fun ManageMemberButton(onEditClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier,
        onClick = onEditClick,
        content = Icons.Filled.Edit.Icon()
    )
}

@Composable
private fun TeamInformation(teamName: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = teamName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.wireTypography.label01,
        color = MaterialTheme.colorScheme.onBackground
    )
}

sealed class EditableState {
    object NotEditable : EditableState()
    class IsEditable(val onEditClick: () -> Unit) : EditableState()
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileInfo() {
    WireTheme {
        UserProfileInfo(
            userId = UserId("value", "domain"),
            isLoading = false,
            editableState = EditableState.IsEditable {},
            userName = "userName",
            avatarAsset = null,
            fullName = "Juan Román Riquelme1 Riquelme3 Riquelme4",
            onUserProfileClick = {},
            teamName = "Wire",
            connection = ConnectionState.ACCEPTED,
            isProteusVerified = true,
            isMLSVerified = true,
            onQrCodeClick = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileInfoTempUser() {
    WireTheme {
        UserProfileInfo(
            userId = UserId("value", "domain"),
            isLoading = false,
            editableState = EditableState.IsEditable {},
            userName = UsernameMapper.fromOtherUser(
                OtherUser(
                    id = UserId("value", "domain"),
                    name = "fullName",
                    handle = "",
                    accentId = 1,
                    connectionStatus = ConnectionState.ACCEPTED,
                    userType = UserType.GUEST,
                    availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                    completePicture = null,
                    previewPicture = null,
                    expiresAt = Clock.System.now().plus(2.minutes),
                    botService = null,
                    isProteusVerified = true,
                    teamId = null,
                    deleted = false,
                    defederated = false,
                    supportedProtocols = null
                )
            ),
            avatarAsset = null,
            fullName = "fullName",
            onUserProfileClick = {},
            teamName = null,
            connection = ConnectionState.ACCEPTED,
            isProteusVerified = false,
            isMLSVerified = false,
            expiresAt = Clock.System.now().plus(1.hours),
            onQrCodeClick = {}
        )
    }
}
