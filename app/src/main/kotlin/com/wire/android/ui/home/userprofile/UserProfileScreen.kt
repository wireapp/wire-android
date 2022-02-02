package com.wire.android.ui.home.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.UserStatus
import com.wire.android.ui.common.UserStatusDot
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.Dimensions
import com.wire.android.ui.theme.body02
import com.wire.android.ui.theme.label01
import com.wire.android.ui.theme.subline01
import com.wire.android.ui.theme.title01
import com.wire.android.ui.theme.title02
import com.wire.android.ui.theme.title03

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel) {
    val uiState by viewModel.state.collectAsState()

    with(uiState) {
        renderUserProfileScreen(state = this, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun renderUserProfileScreen(state: UserProfileState, viewModel: UserProfileViewModel) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {

        val (topBar, avatar, name, userName, team, editBtn, statusRow, otherAccsHeader, otherAccsList, addAccBtn) = createRefs()

        CenterAlignedTopAppBar(
            modifier = Modifier.constrainAs(topBar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.close() },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.user_profile_close_description),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.user_profile_tile),
                    style = MaterialTheme.typography.title01,
                    color = MaterialTheme.colorScheme.onBackground,
                )

            },
            actions = {
                WireSecondaryButton(
                    modifier = Modifier.height(32.dp),
                    onClick = { viewModel.logout() },
                    text = stringResource(R.string.user_profile_logout),
                    fillMaxWidth = false,
                )
            }
        )

        UserProfileAvatar(
            modifier = Modifier
                .padding(top = 16.dp)
                .constrainAs(avatar) {
                    top.linkTo(topBar.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            size = Dimensions.userAvatarBig,
            avatarUrl = state.avatarUrl
        )

        Text(
            modifier = Modifier
                .padding(top = 16.dp)
                .constrainAs(name) {
                    top.linkTo(avatar.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = state.fullName,
            style = MaterialTheme.typography.title02,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            modifier = Modifier
                .constrainAs(userName) {
                    top.linkTo(name.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = state.userName,
            style = MaterialTheme.typography.body02,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .constrainAs(team) {
                    top.linkTo(userName.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = state.teamName,
            style = MaterialTheme.typography.label01,
            color = MaterialTheme.colorScheme.onBackground,
        )

        IconButton(
            modifier = Modifier
                .padding(start = 20.dp)
                .constrainAs(editBtn) {
                    top.linkTo(name.top)
                    bottom.linkTo(team.bottom)
                    start.linkTo(name.end)
                },
            onClick = {},
            content = Icons.Filled.Edit.Icon()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(32.dp)
                .constrainAs(statusRow) {
                    top.linkTo(team.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        ) {
            WireSecondaryButton(
                onClick = {},
                text = stringResource(R.string.user_profile_status_available),
                fillMaxWidth = false,
                state = if (state.status == UserStatus.AVAILABLE) WireButtonState.Selected else WireButtonState.Default,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                leadingIcon = {
                    UserStatusDot(
                        status = UserStatus.AVAILABLE,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                })
            WireSecondaryButton(
                onClick = {},
                text = stringResource(R.string.user_profile_status_busy),
                fillMaxWidth = false,
                state = if (state.status == UserStatus.BUSY) WireButtonState.Selected else WireButtonState.Default,
                shape = RoundedCornerShape(0.dp),
                leadingIcon = {
                    UserStatusDot(
                        status = UserStatus.BUSY,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                })
            WireSecondaryButton(
                onClick = {},
                text = stringResource(R.string.user_profile_status_available),
                fillMaxWidth = false,
                state = if (state.status == UserStatus.AWAY) WireButtonState.Selected else WireButtonState.Default,
                shape = RoundedCornerShape(0.dp),
                leadingIcon = {
                    UserStatusDot(
                        status = UserStatus.AWAY,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                })
            WireSecondaryButton(
                onClick = {},
                text = stringResource(R.string.user_profile_status_none),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                state = if (state.status == UserStatus.NONE) WireButtonState.Selected else WireButtonState.Default,
                leadingIcon = {
                    UserStatusDot(
                        status = UserStatus.NONE,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                })
        }

        Text(
            modifier = Modifier
                .padding(top = 14.dp, start = 16.dp, bottom = 4.dp)
                .constrainAs(otherAccsHeader) {
                    top.linkTo(statusRow.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = stringResource(id = R.string.user_profile_other_accs).uppercase(),
            style = MaterialTheme.typography.title03,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start
        )

        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 64.dp)
                .constrainAs(otherAccsList) {
                    top.linkTo(otherAccsHeader.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {

            items(
                items = state.otherAccounts,
                itemContent = { account -> OtherAccountItem(account) }
            )
        }

        Surface(
            modifier = Modifier
                .shadow(2.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .constrainAs(addAccBtn) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
            WirePrimaryButton(
                text = stringResource(R.string.user_profile_new_acc_text),
                onClick = {})
        }
    }
}

@Composable
private fun OtherAccountItem(account: OtherAccount, onClick: (String) -> Unit = {}) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(bottom = 1.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 2.dp)
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
            .padding(start = 8.dp)
            .constrainAs(data) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(avatar.end)
            }) {

            Text(text = account.fullName, style = MaterialTheme.typography.body02)

            if (account.teamName != null) {
                Text(text = account.teamName, style = MaterialTheme.typography.subline01)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
fun UserProfileScreenPreview() {
    renderUserProfileScreen(
        UserProfileState(
            "",
            UserStatus.BUSY,
            "Tester Tost",
            "@userName",
            "Best team ever",
            listOf(
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "New Name")
            )
        ),
        UserProfileViewModel(NavigationManager())
    )
}
