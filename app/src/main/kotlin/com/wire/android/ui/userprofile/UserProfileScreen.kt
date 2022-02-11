package com.wire.android.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.UserStatusIndicator
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel) {
    with(viewModel.userProfileState) {
        UserProfileScreen(
            state = this,
            onCloseClick = { viewModel.close() },
            onLogoutClick = { viewModel.logout() },
            onEditClick = { viewModel.editProfile() },
            onStatusClicked = { viewModel.changeStatusClick(it) },
            onAddAccountClick = { viewModel.addAccount() },
            dismissStatusDialog = { viewModel.dismissStatusDialog() },
            onStatusChange = { viewModel.changeStatus(it) },
            onNotShowRationaleAgainChange = { show -> viewModel.dialogCheckBoxStateChanged(show) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileScreen(
    state: SelfUserProfileState,
    onCloseClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onStatusClicked: (UserStatus) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    dismissStatusDialog: () -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {

        TopBar(onCloseClick, onLogoutClick)
        Header(state, onEditClick)
        StatusesRow(state.status, onStatusClicked)
        OtherAccountsList(state, onAddAccountClick)
    }

    ChangeStatusDialogContent(state.statusDialogData, dismissStatusDialog, onStatusChange, onNotShowRationaleAgainChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.TopBar(onCloseClick: () -> Unit, onLogoutClick: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.user_profile_close_description),
                )
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.user_profile_title),
                style = MaterialTheme.wireTypography.title01,
            )
        },
        actions = {
            WireSecondaryButton(
                onClick = onLogoutClick,
                text = stringResource(R.string.user_profile_logout),
                fillMaxWidth = false,
                minHeight = dimensions().userProfileLogoutBtnHeight,
                state = WireButtonState.Error
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.Header(state: SelfUserProfileState, onEditClick: () -> Unit) {
    UserProfileAvatar(
        modifier = Modifier
            .padding(top = dimensions().spacing16x)
            .align(Alignment.CenterHorizontally),
        size = dimensions().userAvatarDefaultBigSize,
        avatarUrl = state.avatarUrl,
        status = UserStatus.NONE
    )

    ConstraintLayout(modifier = Modifier.align(Alignment.CenterHorizontally)) {

        val (data, editBtn, team) = createRefs()

        Column(modifier = Modifier
            .padding(horizontal = dimensions().spacing64x)
            .constrainAs(data) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = state.fullName,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.wireTypography.title02,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = state.userName,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.wireTypography.body02,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        IconButton(
            modifier = Modifier
                .padding(start = dimensions().spacing16x)
                .constrainAs(editBtn) {
                    top.linkTo(data.top)
                    bottom.linkTo(data.bottom)
                    end.linkTo(data.end)
                },
            onClick = onEditClick,
            content = Icons.Filled.Edit.Icon()
        )

        Text(
            modifier = Modifier
                .padding(top = dimensions().spacing8x)
                .padding(horizontal = dimensions().spacing16x)
                .constrainAs(team) {
                    top.linkTo(data.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = state.teamName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.wireTypography.label01,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ColumnScope.StatusesRow(status: UserStatus, onStatusClicked: (UserStatus) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions().spacing16x),
    ) {
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.AVAILABLE) },
            text = stringResource(R.string.user_profile_status_available),
            fillMaxWidth = false,
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (status == UserStatus.AVAILABLE) WireButtonState.Selected else WireButtonState.Default,
            shape = RoundedCornerShape(topStart = dimensions().corner16x, bottomStart = dimensions().corner16x),
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.AVAILABLE,
                    modifier = Modifier.padding(end = dimensions().spacing4x)
                )
            })
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.BUSY) },
            text = stringResource(R.string.user_profile_status_busy),
            fillMaxWidth = false,
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (status == UserStatus.BUSY) WireButtonState.Selected else WireButtonState.Default,
            shape = RoundedCornerShape(0.dp),
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.BUSY,
                    modifier = Modifier.padding(end = dimensions().spacing4x)
                )
            })
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.AWAY) },
            text = stringResource(R.string.user_profile_status_away),
            fillMaxWidth = false,
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (status == UserStatus.AWAY) WireButtonState.Selected else WireButtonState.Default,
            shape = RoundedCornerShape(0.dp),
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.AWAY,
                    modifier = Modifier.padding(end = dimensions().spacing4x)
                )
            })
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.NONE) },
            text = stringResource(R.string.user_profile_status_none),
            shape = RoundedCornerShape(topEnd = dimensions().corner16x, bottomEnd = dimensions().corner16x),
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (status == UserStatus.NONE) WireButtonState.Selected else WireButtonState.Default,
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.NONE,
                    modifier = Modifier.padding(end = dimensions().spacing4x)
                )
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.OtherAccountsList(state: SelfUserProfileState, onAddAccountClick: () -> Unit) {
    Text(
        modifier = Modifier
            .padding(top = dimensions().spacing16x, start = dimensions().spacing16x, bottom = dimensions().spacing4x),
        text = stringResource(id = R.string.user_profile_other_accs).uppercase(),
        style = MaterialTheme.wireTypography.title03,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Start
    )

    LazyColumn(
        modifier = Modifier.Companion
            .weight(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        items(
            items = state.otherAccounts,
            itemContent = { account -> OtherAccountItem(account) }
        )
    }

    Surface(shadowElevation = 8.dp) {
        WirePrimaryButton(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(dimensions().spacing16x),
            text = stringResource(R.string.user_profile_new_account_text),
            onClick = onAddAccountClick
        )
    }
}

@Composable
private fun OtherAccountItem(account: OtherAccount, onClick: (String) -> Unit = {}) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().userProfileOtherAccItemHeight)
            .padding(bottom = 1.dp)
            .background(MaterialTheme.colorScheme.surface)
            .selectableBackground(true) { onClick(account.id) }
    ) {
        val (avatar, data) = createRefs()

        UserProfileAvatar(
            avatarUrl = account.avatarUrl,
            modifier = Modifier.constrainAs(avatar) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            }
        )

        Column(modifier = Modifier
            .padding(start = dimensions().spacing8x)
            .constrainAs(data) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(avatar.end)
            }) {

            Text(text = account.fullName, style = MaterialTheme.wireTypography.body02)

            if (account.teamName != null) {
                Text(text = account.teamName, style = MaterialTheme.wireTypography.subline01)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
private fun UserProfileScreenPreview() {
    UserProfileScreen(
        SelfUserProfileState(
            "",
            UserStatus.BUSY,
            "Tester Tost long lomng long logn long logn long lonf lonf",
            "@userName",
            "Best team ever long ",
            listOf(
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "New Name")
            ),
            statusDialogData = null
        )
    )
}
