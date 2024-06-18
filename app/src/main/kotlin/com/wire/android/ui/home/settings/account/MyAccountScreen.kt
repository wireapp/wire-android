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

package com.wire.android.ui.home.settings.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.ChangeDisplayNameScreenDestination
import com.wire.android.ui.destinations.ChangeEmailScreenDestination
import com.wire.android.ui.destinations.ChangeHandleScreenDestination
import com.wire.android.ui.home.settings.account.AccountDetailsItem.DisplayName
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Domain
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Email
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Team
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Username
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountDialog
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountViewModel
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.toTitleCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@RootNavGraph
@Destination
@Composable
fun MyAccountScreen(
    navigator: Navigator,
    changeDisplayNameResultRecipient: ResultRecipient<ChangeDisplayNameScreenDestination, Boolean>,
    changeHandleResultRecipient: ResultRecipient<ChangeHandleScreenDestination, Boolean>,
    viewModel: MyAccountViewModel = hiltViewModel(),
    deleteAccountViewModel: DeleteAccountViewModel = hiltViewModel()
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    with(viewModel.myAccountState) {
        MyAccountContent(
            accountDetailItems = mapToUISections(
                state = this,
                navigateToChangeDisplayName = { navigator.navigate(NavigationCommand(ChangeDisplayNameScreenDestination)) },
                navigateToChangeHandle = { navigator.navigate(NavigationCommand(ChangeHandleScreenDestination)) },
                navigateToChangeEmail = { navigator.navigate(NavigationCommand(ChangeEmailScreenDestination)) }
            ),
            forgotPasswordUrl = this.changePasswordUrl,
            canDeleteAccount = viewModel.myAccountState.teamName.isNullOrBlank(),
            onDeleteAccountClicked = deleteAccountViewModel::onDeleteAccountClicked,
            onDeleteAccountConfirmed = deleteAccountViewModel::onDeleteAccountDialogConfirmed,
            onDeleteAccountDismissed = deleteAccountViewModel::onDeleteAccountDialogDismissed,
            startDeleteAccountFlow = deleteAccountViewModel.state.startDeleteAccountFlow,
            onNavigateBack = navigator::navigateBack
        )
    }
    val tryAgainSnackBarMessage = stringResource(id = R.string.error_unknown_message)
    val successDisplayNameSnackBarMessage = stringResource(id = R.string.settings_myaccount_display_name_updated)
    val successHandleSnackBarMessage = stringResource(id = R.string.settings_myaccount_handle_updated)
    handleNavResult(scope, changeDisplayNameResultRecipient, tryAgainSnackBarMessage, successDisplayNameSnackBarMessage, snackbarHostState)
    handleNavResult(scope, changeHandleResultRecipient, tryAgainSnackBarMessage, successHandleSnackBarMessage, snackbarHostState)
}

@Composable
private fun <T : DestinationSpec<*>> handleNavResult(
    scope: CoroutineScope,
    resultRecipient: ResultRecipient<T, Boolean>,
    tryAgainSnackBarMessage: String,
    successSnackBarMessage: String,
    snackbarHostState: SnackbarHostState
) {
    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args")
            }

            is NavResult.Value -> {
                scope.launch {
                    if (result.value) {
                        snackbarHostState.showSnackbar(successSnackBarMessage)
                    } else {
                        snackbarHostState.showSnackbar(tryAgainSnackBarMessage)
                    }
                }
            }
        }
    }
}

@Stable
private fun mapToUISections(
    state: MyAccountState,
    navigateToChangeDisplayName: () -> Unit,
    navigateToChangeHandle: () -> Unit,
    navigateToChangeEmail: () -> Unit
): ImmutableList<AccountDetailsItem> {
    return with(state) {
        listOfNotNull(
            if (fullName.isNotBlank()) {
                DisplayName(fullName, clickableActionIfPossible(!state.isEditNameAllowed, navigateToChangeDisplayName))
            } else {
                null
            },
            if (userName.isNotBlank()) {
                Username("@$userName", clickableActionIfPossible(!state.isEditHandleAllowed, navigateToChangeHandle))
            } else {
                null
            },
            if (email.isNotBlank()) Email(
                email,
                clickableActionIfPossible(!state.isEditEmailAllowed, navigateToChangeEmail)
            ) else null,
            if (!teamName.isNullOrBlank()) Team(teamName) else null,
            if (domain.isNotBlank()) Domain(domain) else null
        ).toImmutableList()
    }
}

private fun clickableActionIfPossible(shouldDisableAction: Boolean, action: () -> Unit) =
    if (shouldDisableAction) null else Clickable { action.invoke() }

@Composable
fun MyAccountContent(
    accountDetailItems: ImmutableList<AccountDetailsItem>,
    forgotPasswordUrl: String?,
    canDeleteAccount: Boolean,
    onDeleteAccountClicked: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    onDeleteAccountDismissed: () -> Unit,
    startDeleteAccountFlow: Boolean,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onNavigateBack,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.settings_your_account_label)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions().spacing16x)
            ) {

                if (!forgotPasswordUrl.isNullOrBlank()) {
                    WirePrimaryButton(
                        text = stringResource(R.string.settings_myaccount_reset_password).toTitleCase(),
                        onClick = { CustomTabsHelper.launchUrl(context, forgotPasswordUrl) })
                }

                if (canDeleteAccount) {
                    if (!forgotPasswordUrl.isNullOrBlank()) Spacer(modifier = Modifier.padding(dimensions().spacing8x))
                    WirePrimaryButton(
                        text = stringResource(R.string.settings_myaccount_logout).toTitleCase(),
                        onClick = onDeleteAccountClicked,
                        state = WireButtonState.Error
                    )
                }
            }
        }
    ) { internalPadding ->

        if (startDeleteAccountFlow) {
            DeleteAccountDialog(
                onDismiss = onDeleteAccountDismissed,
                onConfirm = onDeleteAccountConfirmed
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            folderWithElements(
                header = context.getString(R.string.settings_myaccount_title),
                items = accountDetailItems.associateBy { it.title.toString() },
                factory = { item: AccountDetailsItem ->
                    RowItemTemplate(
                        title = {
                            Text(
                                style = MaterialTheme.wireTypography.label01,
                                color = MaterialTheme.wireColorScheme.secondaryText,
                                text = item.title.asString(),
                                modifier = Modifier.padding(start = dimensions().spacing8x)
                            )
                            Text(
                                style = MaterialTheme.wireTypography.body01,
                                color = MaterialTheme.wireColorScheme.onBackground,
                                text = item.text,
                                modifier = Modifier.padding(start = dimensions().spacing8x)
                            )
                        },
                        actions = {
                            if (item.clickable?.enabled == true) {
                                Icons.Filled.ChevronRight.Icon().invoke()
                            }
                        },
                        clickable = item.clickable ?: Clickable(false)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyAccountScreen() {
    MyAccountContent(
        accountDetailItems = persistentListOf(
            DisplayName("Bob", Clickable(enabled = true) {}),
            Username("@bob_wire", Clickable(enabled = true) {}),
            Email("bob@wire.com", Clickable(enabled = true) {}),
            Team("Wire")
        ),
        forgotPasswordUrl = "http://wire.com",
        canDeleteAccount = true,
        { },
        { },
        {},
        false,
        { }
    )
}
