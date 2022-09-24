package com.wire.android.ui.userprofile.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.ifNotEmpty
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun UserProfileInfo(
    isLoading: Boolean,
    avatarAsset: UserAvatarAsset?,
    fullName: String,
    userName: String,
    teamName: String?,
    membership: Membership = Membership.None,
    onUserProfileClick: (() -> Unit)? = null,
    editableState: EditableState,
    modifier: Modifier = Modifier,
    connection: ConnectionState = ConnectionState.ACCEPTED
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
            UserProfileAvatar(
                size = dimensions().userAvatarDefaultBigSize,
                avatarData = UserAvatarData(asset = avatarAsset, connectionState = connection),
                clickable = remember { Clickable(enabled = editableState is EditableState.IsEditable) { onUserProfileClick?.invoke() } }
            )
            if (isLoading) {
                Box(
                    Modifier
                        .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
                        .clip(CircleShape)
                        .background(MaterialTheme.wireColorScheme.onBackground.copy(alpha = 0.7f))
                ) {
                    WireCircularProgressIndicator(
                        progressColor = MaterialTheme.wireColorScheme.surface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        ConstraintLayout(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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
                    text = fullName,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.wireTypography.title02,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = userName.ifNotEmpty { "@$userName" },
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.wireTypography.body02,
                    maxLines = 1,
                    color = MaterialTheme.wireColorScheme.labelText,
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
        color = MaterialTheme.colorScheme.onBackground,
    )
}

sealed class EditableState {
    object NotEditable : EditableState()
    class IsEditable(val onEditClick: () -> Unit) : EditableState()
}


@Preview
@Composable
private fun UserProfileInfoPreview() {
    UserProfileInfo(
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
