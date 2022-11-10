package com.wire.android.ui.userprofile.self

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.UserStatusIndicator
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.HighlightSubtitle
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes.DownloadUserInfoError
import com.wire.android.ui.userprofile.self.dialog.ChangeStatusDialogContent
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.ui.userprofile.self.model.OtherAccount
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfUserProfileScreen(viewModelSelf: SelfUserProfileViewModel = hiltViewModel()) {
    SelfUserProfileContent(
        state = viewModelSelf.userProfileState,
        onCloseClick = viewModelSelf::navigateBack,
        logout = viewModelSelf::logout,
        onChangeUserProfilePicture = viewModelSelf::onChangeProfilePictureClicked,
        onEditClick = viewModelSelf::editProfile,
        onStatusClicked = viewModelSelf::changeStatusClick,
        onAddAccountClick = viewModelSelf::addAccount,
        dismissStatusDialog = viewModelSelf::dismissStatusDialog,
        onStatusChange = viewModelSelf::changeStatus,
        onNotShowRationaleAgainChange = viewModelSelf::dialogCheckBoxStateChanged,
        onMessageShown = viewModelSelf::clearErrorMessage,
        onMaxAccountReachedDialogDismissed = viewModelSelf::onMaxAccountReachedDialogDismissed,
        onOtherAccountClick = viewModelSelf::switchAccount,
        isUserInCall = viewModelSelf::isUserInCall
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SelfUserProfileContent(
    state: SelfUserProfileState,
    onCloseClick: () -> Unit = {},
    logout: (Boolean) -> Unit = {},
    onChangeUserProfilePicture: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onStatusClicked: (UserAvailabilityStatus) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    dismissStatusDialog: () -> Unit = {},
    onStatusChange: (UserAvailabilityStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {},
    onMessageShown: () -> Unit = {},
    onMaxAccountReachedDialogDismissed: () -> Unit = {},
    onOtherAccountClick: (UserId) -> Unit = {},
    isUserInCall: () -> Boolean,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    state.errorMessageCode?.let { errorCode ->
        val errorMessage = mapErrorCodeToString(errorCode)
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage)
            onMessageShown()
        }
    }
    val scrollState = rememberScrollState()
    val logoutOptionsDialogState = rememberVisibilityState<LogoutOptionsDialogState>()

    Scaffold(
        topBar = {
            SelfUserProfileTopBar(
                onCloseClick = onCloseClick,
                onLogoutClick = remember {
                    { logoutOptionsDialogState.show(logoutOptionsDialogState.savedState ?: LogoutOptionsDialogState()) }
                }
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { internalPadding ->
        with(state) {
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .scrollable(state = scrollState, orientation = Orientation.Vertical)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(internalPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .scrollable(state = scrollState, orientation = Orientation.Vertical)
                ) {
                    stickyHeader {
                        UserProfileInfo(
                            isLoading = state.isAvatarLoading,
                            avatarAsset = state.avatarAsset,
                            fullName = fullName,
                            userName = userName,
                            teamName = teamName,
                            onUserProfileClick = onChangeUserProfilePicture,
                            editableState = EditableState.IsEditable(onEditClick),
                        )
                    }
                    stickyHeader {
                        CurrentSelfUserStatus(
                            userStatus = status,
                            onStatusClicked = onStatusClicked
                        )
                    }
                    if (state.otherAccounts.isNotEmpty()) {
                        stickyHeader {
                            OtherAccountsHeader()
                        }
                        items(
                            items = otherAccounts,
                            itemContent = { account ->
                                OtherAccountItem(
                                    account,
                                    clickable = remember {
                                        Clickable(enabled = true, onClick = {
                                            if (isUserInCall()) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.cant_switch_account_in_call),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                onOtherAccountClick(account.id)
                                            }
                                        })
                                    })
                            }
                        )
                    }
                }
                NewTeamButton(onAddAccountClick, isUserInCall, context)
            }
            ChangeStatusDialogContent(
                data = statusDialogData,
                dismiss = dismissStatusDialog,
                onStatusChange = onStatusChange,
                onNotShowRationaleAgainChange = onNotShowRationaleAgainChange
            )

            if (state.maxAccountsReached) {
                MaxAccountReachedDialog(
                    onConfirm = onMaxAccountReachedDialogDismissed,
                    onDismiss = onMaxAccountReachedDialogDismissed,
                    buttonText = R.string.label_ok
                )
            }

            LogoutOptionsDialog(
                dialogState = logoutOptionsDialogState,
                logout = logout
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mapErrorCodeToString(errorCode: ErrorCodes): String {
    return when (errorCode) {
        DownloadUserInfoError -> stringResource(R.string.error_downloading_user_info)
        // Add more future errors for a more granular error handling
        else -> stringResource(R.string.error_unknown_title)
    }
}

@Composable
private fun SelfUserProfileTopBar(
    onCloseClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = stringResource(id = R.string.user_profile_title),
        navigationIconType = NavigationIconType.Close,
        elevation = 0.dp,
        actions = {
            WireSecondaryButton(
                onClick = onLogoutClick,
                text = stringResource(R.string.user_profile_logout),
                fillMaxWidth = false,
                minHeight = dimensions().userProfileLogoutBtnHeight,
                state = WireButtonState.Error,
                blockUntilSynced = true
            )
        }
    )
}

@Composable
private fun CurrentSelfUserStatus(
    userStatus: UserAvailabilityStatus,
    onStatusClicked: (UserAvailabilityStatus) -> Unit
) {
    val items = listOf(
        UserAvailabilityStatus.AVAILABLE,
        UserAvailabilityStatus.BUSY,
        UserAvailabilityStatus.AWAY,
        UserAvailabilityStatus.NONE
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        FolderHeader("Availability")

        WireDropDown(
            items = items.map {
                when (it) {
                    UserAvailabilityStatus.AVAILABLE -> stringResource(R.string.user_profile_status_available)
                    UserAvailabilityStatus.BUSY -> stringResource(R.string.user_profile_status_busy)
                    UserAvailabilityStatus.AWAY -> stringResource(R.string.user_profile_status_away)
                    UserAvailabilityStatus.NONE -> stringResource(R.string.user_profile_status_none)
                }
            },
            defaultItemIndex = items.indexOf(userStatus),
            label = null,
            modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
            autoUpdateSelection = false,
            showDefaultTextIndicator = false,
            leadingCompose = { index -> UserStatusIndicator(items[index]) }
        ) { selectedIndex ->
            onStatusClicked(items[selectedIndex])
        }
    }
}

@Composable
private fun OtherAccountsHeader() {
    FolderHeader(stringResource(id = R.string.user_profile_other_accs))
}

@Composable
private fun NewTeamButton(
    onAddAccountClick: () -> Unit,
    isUserIdCall: () -> Boolean,
    context: Context
) {
    Surface(shadowElevation = dimensions().spacing8x) {
        WirePrimaryButton(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(dimensions().spacing16x)
                .testTag("New Team or Account"),
            text = stringResource(R.string.user_profile_new_account_text),
            onClick = remember {
                {
                    if (isUserIdCall()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.cant_switch_account_in_call),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onAddAccountClick()
                    }
                }
            }
        )
    }
}

@Composable
private fun OtherAccountItem(
    account: OtherAccount,
    clickable: Clickable = Clickable(enabled = true) {}
) {
    RowItemTemplate(
        leadingIcon = { UserProfileAvatar(account.avatarData) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightName(
                    name = account.fullName,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
            }

        },
        subtitle = {
            if (account.teamName != null)
                HighlightSubtitle(subTitle = account.teamName, suffix = "")
        },
        actions = {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = MaterialTheme.wireDimensions.spacing8x)
            ) {
                ArrowRightIcon(Modifier.align(Alignment.TopEnd))
            }
        },
        clickable = clickable,
        modifier = Modifier.padding(start = dimensions().spacing8x)
    )
}

@Preview(widthDp = 400, heightDp = 800)
@Preview(widthDp = 800)
@Composable
private fun SelfUserProfileScreenPreview() {
    SelfUserProfileContent(
        SelfUserProfileState(
            status = UserAvailabilityStatus.BUSY,
            fullName = "Tester Tost_long_long_long long  long  long  long  long  long ",
            userName = "userName_long_long_long_long_long_long_long_long_long_long",
            teamName = "Best team ever long  long  long  long  long  long  long  long  long ",
            otherAccounts = listOf(
                OtherAccount(id = UserId("id1", "domain"), fullName = "Other Name", teamName = "team A"),
                OtherAccount(id = UserId("id2", "domain"), fullName = "New Name")
            ),
            statusDialogData = null
        ),
        isUserInCall = { false }
    )
}

@Preview(widthDp = 800)
@Preview(widthDp = 400)
@Composable
private fun CurrentSelfUserStatusPreview() {
    CurrentSelfUserStatus(UserAvailabilityStatus.AVAILABLE, onStatusClicked = {})
}
