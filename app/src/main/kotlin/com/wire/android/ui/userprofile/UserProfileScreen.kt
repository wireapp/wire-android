package com.wire.android.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.UserStatusIndicator
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.UserProfileViewModel.ErrorCodes
import com.wire.android.ui.userprofile.UserProfileViewModel.ErrorCodes.DownloadUserInfoError

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel = hiltViewModel()) {

    UserProfileContent(
        state = viewModel.userProfileState,
        onCloseClick = { viewModel.navigateBack() },
        onLogoutClick = { viewModel.logout() },
        onChangeUserProfilePicture = { viewModel.onChangeProfilePictureClicked() },
        onEditClick = { viewModel.editProfile() },
        onStatusClicked = { viewModel.changeStatusClick(it) },
        onAddAccountClick = { viewModel.addAccount() },
        dismissStatusDialog = { viewModel.dismissStatusDialog() },
        onStatusChange = { viewModel.changeStatus(it) },
        onNotShowRationaleAgainChange = { show -> viewModel.dialogCheckBoxStateChanged(show) },
        onMessageShown = { viewModel.clearErrorMessage() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun UserProfileContent(
    state: SelfUserProfileState,
    onCloseClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onChangeUserProfilePicture: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onStatusClicked: (UserStatus) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    dismissStatusDialog: () -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {},
    onMessageShown: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Error handling
    state.errorMessageCode?.let { errorCode ->
        val errorMessage = mapErrorCodeToString(errorCode)
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            UserProfileTopBar(
                onCloseClick = onCloseClick,
                onLogoutClick = onLogoutClick
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        with(state) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                UserProfileInfo(
                    isLoading = state.isAvatarLoading,
                    avatarAssetByteArray = state.avatarAssetByteArray,
                    fullName = fullName,
                    userName = userName,
                    teamName = teamName,
                    onUserProfileClick = onChangeUserProfilePicture,
                    onEditClick = onEditClick
                )
                CurrentUserStatus(
                    userStatus = status,
                    onStatusClicked = onStatusClicked
                )
                if (state.otherAccounts.isNotEmpty()) {
                    OtherAccountsList(
                        otherAccounts = otherAccounts,
                        onAddAccountClick = onAddAccountClick,
                    )
                }
            }
            ChangeStatusDialogContent(
                data = statusDialogData,
                dismiss = dismissStatusDialog,
                onStatusChange = onStatusChange,
                onNotShowRationaleAgainChange = onNotShowRationaleAgainChange
            )
        }
    }
}

@Composable
private fun mapErrorCodeToString(errorCode: ErrorCodes): String {
    return when (errorCode) {
        DownloadUserInfoError -> stringResource(R.string.error_downloading_user_info)
        // Add more future errors for a more granular error handling
        else -> stringResource(R.string.error_unknown_title)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileTopBar(
    onCloseClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = stringResource(id = R.string.user_profile_title),
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
private fun ColumnScope.UserProfileInfo(
    isLoading: Boolean,
    avatarAssetByteArray: ByteArray?,
    fullName: String,
    userName: String,
    teamName: String?,
    onUserProfileClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        Modifier
            .wrapContentSize()
            .padding(top = dimensions().spacing16x)
            .align(Alignment.CenterHorizontally)
    ) {
        UserProfileAvatar(
            onClick = onUserProfileClick,
            isEnabled = !isLoading,
            size = dimensions().userAvatarDefaultBigSize,
            avatarAssetByteArray = avatarAssetByteArray,
            status = UserStatus.NONE,
        )
        if (isLoading) {
            Box(
                Modifier
                    .matchParentSize()
                    .align(Alignment.Center)
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
    ConstraintLayout(modifier = Modifier.align(Alignment.CenterHorizontally)) {
        val (userDescription, editButton, teamDescription) = createRefs()

        Column(
            modifier = Modifier
                .padding(horizontal = dimensions().spacing64x)
                .constrainAs(userDescription) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = fullName,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.wireTypography.title02,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "@$userName",
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.wireTypography.body02,
                maxLines = 1,
                color = MaterialTheme.wireColorScheme.labelText,
            )
        }
        IconButton(
            modifier = Modifier
                .padding(start = dimensions().spacing16x)
                .constrainAs(editButton) {
                    top.linkTo(userDescription.top)
                    bottom.linkTo(userDescription.bottom)
                    end.linkTo(userDescription.end)
                },
            onClick = onEditClick,
            content = Icons.Filled.Edit.Icon()
        )

        if (teamName != null) {
            Text(
                modifier = Modifier
                    .padding(top = dimensions().spacing8x)
                    .padding(horizontal = dimensions().spacing16x)
                    .constrainAs(teamDescription) {
                        top.linkTo(userDescription.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                text = teamName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.wireTypography.label01,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun CurrentUserStatus(
    userStatus: UserStatus,
    onStatusClicked: (UserStatus) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions().spacing16x),
    ) {
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.AVAILABLE) },
            text = stringResource(R.string.user_profile_status_available),
            fillMaxWidth = false,
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (userStatus == UserStatus.AVAILABLE) WireButtonState.Selected else WireButtonState.Default,
            shape = RoundedCornerShape(
                topStart = dimensions().corner16x,
                bottomStart = dimensions().corner16x
            ),
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.AVAILABLE,
                    modifier = Modifier.padding(end = dimensions().spacing4x).testTag("Available")
                )
            }
        )
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.BUSY) },
            text = stringResource(R.string.user_profile_status_busy),
            fillMaxWidth = false,
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (userStatus == UserStatus.BUSY) WireButtonState.Selected else WireButtonState.Default,
            shape = RoundedCornerShape(0.dp),
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.BUSY,
                    modifier = Modifier.padding(end = dimensions().spacing4x).testTag("Busy")
                )
            }
        )
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.AWAY) },
            text = stringResource(R.string.user_profile_status_away),
            fillMaxWidth = false,
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (userStatus == UserStatus.AWAY) WireButtonState.Selected else WireButtonState.Default,
            shape = RoundedCornerShape(0.dp),
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.AWAY,
                    modifier = Modifier.padding(end = dimensions().spacing4x).testTag("Away")
                )
            }
        )
        WireSecondaryButton(
            onClick = { onStatusClicked(UserStatus.NONE) },
            text = stringResource(R.string.user_profile_status_none),
            fillMaxWidth = false,
            shape = RoundedCornerShape(
                topEnd = dimensions().corner16x,
                bottomEnd = dimensions().corner16x
            ),
            minHeight = dimensions().userProfileStatusBtnHeight,
            state = if (userStatus == UserStatus.NONE) WireButtonState.Selected else WireButtonState.Default,
            leadingIcon = {
                UserStatusIndicator(
                    status = UserStatus.NONE,
                    modifier = Modifier.padding(end = dimensions().spacing4x).testTag("None")
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.OtherAccountsList(
    otherAccounts: List<OtherAccount>,
    onAddAccountClick: () -> Unit
) {
    Text(
        modifier = Modifier
            .padding(
                top = dimensions().spacing16x,
                start = dimensions().spacing16x,
                bottom = dimensions().spacing4x
            ),
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
            items = otherAccounts,
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
private fun OtherAccountItem(
    account: OtherAccount,
    onClick: (String) -> Unit = {}
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().userProfileOtherAccItemHeight)
            .padding(start = dimensions().spacing8x, bottom = 1.dp)
            .background(MaterialTheme.colorScheme.surface)
            .selectableBackground(true) { onClick(account.id) }
    ) {
        val (avatar, data) = createRefs()

        UserProfileAvatar(
            modifier = Modifier.constrainAs(avatar) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            }
        )

        Column(
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .constrainAs(data) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(avatar.end)
                }
        ) {

            Text(
                text = account.fullName,
                style = MaterialTheme.wireTypography.body02
            )

            if (account.teamName != null) {
                Text(
                    text = account.teamName,
                    style = MaterialTheme.wireTypography.subline01
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
private fun UserProfileScreenPreview() {
    UserProfileContent(
        SelfUserProfileState(
            status = UserStatus.BUSY,
            fullName = "Tester Tost_long_long_long long  long  long  long  long  long ",
            userName = "@userName_long_long_long_long_long_long_long_long_long_long",
            teamName = "Best team ever long  long  long  long  long  long  long  long  long ",
            otherAccounts = listOf(
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "New Name")
            ),
            statusDialogData = null
        ),
    )
}
