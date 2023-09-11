/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.userprofile.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.banner.SecurityClassificationBanner
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.ifNotEmpty
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
                membership = membership
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
                UserProfileAvatar(
                    size = dimensions().avatarDefaultBigSize,
                    avatarData = userAvatarData,
                    clickable = remember(editableState) {
                        Clickable(
                            enabled = editableState is EditableState.IsEditable,
                            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                        ) { onUserProfileClick?.invoke() }
                    },
                    showPlaceholderIfNoAsset = showPlaceholderIfNoAsset,
                    withCrossfadeAnimation = true,
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
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize()
        ) {
            val (userDescription, editButton, teamDescription) = createRefs()

            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing64x)
                    .wrapContentSize()
                    .constrainAs(userDescription) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                Text(
                    text = fullName.ifBlank {
                        if (isLoading) ""
                        else UIText.StringResource(R.string.username_unavailable_label).asString()
                    },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.wireTypography.title02,
                    color = if (fullName.isNotBlank()) MaterialTheme.colorScheme.onBackground else MaterialTheme.wireColorScheme.labelText
                )
                Text(
                    text = if (membership == Membership.Service) userName else userName.ifNotEmpty { "@$userName" },
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.wireTypography.body02,
                    maxLines = 1,
                    color = MaterialTheme.wireColorScheme.labelText
                )
                UserBadge(membership, connection, topPadding = dimensions().spacing8x)
            }
            val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current

            if (localFeatureVisibilityFlags.UserProfileEditIcon) {
                if (editableState is EditableState.IsEditable) {
                    ManageMemberButton(
                        modifier = Modifier
                            .padding(start = dimensions().spacing16x)
                            .constrainAs(editButton) {
                                top.linkTo(userDescription.top)
                                bottom.linkTo(userDescription.bottom)
                                end.linkTo(userDescription.end)
                            },
                        onEditClick = editableState.onEditClick
                    )
                }
            }

            if (teamName != null) {
                TeamInformation(
                    modifier = Modifier
                        .padding(top = dimensions().spacing8x)
                        .padding(horizontal = dimensions().spacing16x)
                        .constrainAs(teamDescription) {
                            top.linkTo(userDescription.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                    teamName = teamName
                )
            }
        }
        SecurityClassificationBanner(
            userId = userId,
            modifier = Modifier.padding(top = dimensions().spacing8x)
        )
    }
}

@Composable
private fun ManageMemberButton(modifier: Modifier, onEditClick: () -> Unit) {
    IconButton(
        modifier = modifier,
        onClick = onEditClick,
        content = Icons.Filled.Edit.Icon()
    )
}

@Composable
private fun TeamInformation(modifier: Modifier, teamName: String) {
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

@Preview
@Composable
fun PreviewUserProfileInfo() {
    UserProfileInfo(
        userId = UserId("value", "domain"),
        isLoading = true,
        editableState = EditableState.IsEditable {},
        userName = "userName",
        avatarAsset = null,
        fullName = "fullName",
        onUserProfileClick = {},
        teamName = "Wire",
        connection = ConnectionState.ACCEPTED
    )
}
