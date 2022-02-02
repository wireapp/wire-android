package com.wire.android.ui.home.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.wire.android.ui.theme.wireTypography

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {

        renderTopBar(viewModel)
        renderHeader(state, viewModel)
        renderStatusesRow(state)
        renderOtherAccountsList(state, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.renderOtherAccountsList(state: UserProfileState, viewModel: UserProfileViewModel) {
    Text(
        modifier = Modifier
            .padding(top = 14.dp, start = 16.dp, bottom = 4.dp),
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

    Box(
        modifier = Modifier
            .shadow(2.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        WirePrimaryButton(
            text = stringResource(R.string.user_profile_new_acc_text),
            onClick = { viewModel.addAccount() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.renderTopBar(viewModel: UserProfileViewModel) {
    CenterAlignedTopAppBar(
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
                style = MaterialTheme.wireTypography.title01,
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
}

@Composable
private fun ColumnScope.renderStatusesRow(state: UserProfileState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(32.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.renderHeader(state: UserProfileState, viewModel: UserProfileViewModel) {
    UserProfileAvatar(
        modifier = Modifier
            .padding(top = 16.dp)
            .align(Alignment.CenterHorizontally),
        size = 64.dp,
        avatarUrl = state.avatarUrl
    )

    ConstraintLayout(modifier = Modifier.align(Alignment.CenterHorizontally)) {

        val (data, editBtn) = createRefs()

        Column(modifier = Modifier.constrainAs(data) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = state.fullName,
                style = MaterialTheme.wireTypography.title02,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = state.userName,
                style = MaterialTheme.wireTypography.body02,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = state.teamName,
                style = MaterialTheme.wireTypography.label01,
                color = MaterialTheme.colorScheme.onBackground,
            )

        }

        IconButton(
            modifier = Modifier
                .padding(start = 20.dp)
                .constrainAs(editBtn) {
                    top.linkTo(data.top)
                    bottom.linkTo(data.bottom)
                    start.linkTo(data.end)
                },
            onClick = { viewModel.editProfile() },
            content = Icons.Filled.Edit.Icon()
        )
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
                OtherAccount("someId", "", "New Name")
            )
        ),
        UserProfileViewModel(NavigationManager())
    )
}
