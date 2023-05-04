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

package com.wire.android.ui.home.settings.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.account.AccountDetailsItem.DisplayName
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Domain
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Email
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Team
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Username
import com.wire.android.ui.home.settings.account.MyAccountViewModel.SettingsOperationResult
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.extension.folderWithElements
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun MyAccountScreen(
    backNavArgs: ImmutableMap<String, Any> = persistentMapOf(),
    viewModel: MyAccountViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs)
) {
    with(viewModel.myAccountState) {
        MyAccountContent(
            accountDetailItems = mapToUISections(viewModel, this),
            forgotPasswordUrl = this.changePasswordUrl,
            checkPendingSnackBarMessages = viewModel::checkForPendingMessages,
            onNavigateBack = viewModel::navigateBack
        )
    }
}

@Stable
private fun mapToUISections(viewModel: MyAccountViewModel, state: MyAccountState): List<AccountDetailsItem> {
    return with(state) {
        listOfNotNull(
            if (fullName.isNotBlank()) {
                DisplayName(fullName, clickableActionIfPossible(state.isReadOnlyAccount) { viewModel.navigateToChangeDisplayName() })
            } else {
                null
            },
            if (userName.isNotBlank()) {
                Username("@$userName", clickableActionIfPossible(!state.isEditHandleAllowed) { viewModel.navigateToChangeHandle() })
            } else {
                null
            },
            if (email.isNotBlank()) Email(
                email,
                clickableActionIfPossible(!state.isEditEmailAllowed) { viewModel.navigateToChangeEmail() }) else null,
            if (teamName.isNotBlank()) Team(teamName) else null,
            if (domain.isNotBlank()) Domain(domain) else null
        )
    }
}

private fun clickableActionIfPossible(shouldDisableAction: Boolean, action: () -> Unit) =
    if (shouldDisableAction) null else Clickable { action.invoke() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccountContent(
    accountDetailItems: List<AccountDetailsItem> = emptyList(),
    forgotPasswordUrl: String?,
    checkPendingSnackBarMessages: () -> SettingsOperationResult = { SettingsOperationResult.None },
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        val result = checkPendingSnackBarMessages()
        if (result is SettingsOperationResult.Result) {
            snackbarHostState.showSnackbar(result.message.asString(context.resources))
        }
    }

    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onNavigateBack,
                elevation = 0.dp,
                title = stringResource(id = R.string.settings_your_account_label)
            )
        },
        bottomBar = {
            if (forgotPasswordUrl?.isNotBlank() == true) {
                WirePrimaryButton(
                    text = stringResource(R.string.settings_myaccount_reset_password),
                    onClick = { CustomTabsHelper.launchUrl(context, forgotPasswordUrl) },
                    modifier = Modifier.padding(dimensions().spacing16x)
                )
            }
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { internalPadding ->
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
        accountDetailItems = listOf(
            DisplayName("Bob", Clickable(enabled = true) {}),
            Username("@bob_wire", Clickable(enabled = true) {}),
            Email("bob@wire.com", Clickable(enabled = true) {}),
            Team("Wire")
        ),
        forgotPasswordUrl = "http://wire.com"
    )
}
