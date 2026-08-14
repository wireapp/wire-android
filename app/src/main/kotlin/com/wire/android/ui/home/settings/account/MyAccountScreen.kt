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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.account.AccountDetailsItem.DisplayName
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Domain
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Email
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Team
import com.wire.android.ui.home.settings.account.AccountDetailsItem.UserColor
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Username
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountDialog
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountViewModel
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.resourceId
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.toTitleCase
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.sectionWithElements
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal enum class MyAccountUpdateKind {
    DISPLAY_NAME,
    HANDLE,
    USER_COLOR,
}

internal data class MyAccountUpdateNotification(
    val requestId: String,
    val kind: MyAccountUpdateKind,
    val successful: Boolean,
)

@Composable
internal fun MyAccountRouteScreen(
    viewModel: MyAccountViewModel,
    deleteAccountViewModel: DeleteAccountViewModel,
    onNavigateBack: () -> Unit,
    onChangeDisplayName: () -> Unit,
    onChangeHandle: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangeUserColor: () -> Unit,
    updateNotification: MyAccountUpdateNotification?,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val tryAgainMessage = stringResource(id = R.string.error_unknown_message)
    val displayNameSuccessMessage = stringResource(id = R.string.settings_myaccount_display_name_updated)
    val handleSuccessMessage = stringResource(id = R.string.settings_myaccount_handle_updated)
    val userColorSuccessMessage = stringResource(id = R.string.settings_myaccount_user_color_updated)

    LaunchedEffect(updateNotification) {
        updateNotification?.let { notification ->
            snackbarHostState.showSnackbar(
                if (!notification.successful) {
                    tryAgainMessage
                } else {
                    when (notification.kind) {
                        MyAccountUpdateKind.DISPLAY_NAME -> displayNameSuccessMessage
                        MyAccountUpdateKind.HANDLE -> handleSuccessMessage
                        MyAccountUpdateKind.USER_COLOR -> userColorSuccessMessage
                    }
                }
            )
        }
    }

    with(viewModel.myAccountState) {
        MyAccountContent(
            accountDetailItems = mapToUISections(
                state = this,
                navigateToChangeDisplayName = onChangeDisplayName,
                navigateToChangeHandle = onChangeHandle,
                navigateToChangeEmail = onChangeEmail,
                navigateToChangeColor = onChangeUserColor,
            ),
            forgotPasswordUrl = changePasswordUrl,
            canDeleteAccount = canDeleteAccount,
            onDeleteAccountClicked = deleteAccountViewModel::onDeleteAccountClicked,
            onDeleteAccountConfirmed = deleteAccountViewModel::onDeleteAccountDialogConfirmed,
            onDeleteAccountDismissed = deleteAccountViewModel::onDeleteAccountDialogDismissed,
            startDeleteAccountFlow = deleteAccountViewModel.state.startDeleteAccountFlow,
            onNavigateBack = onNavigateBack,
        )
    }
}

@Stable
private fun mapToUISections(
    state: MyAccountState,
    navigateToChangeDisplayName: () -> Unit,
    navigateToChangeHandle: () -> Unit,
    navigateToChangeEmail: () -> Unit,
    navigateToChangeColor: () -> Unit
): ImmutableList<AccountDetailsItem> {
    return with(state) {
        listOfNotNull(
            if (fullName.isNotBlank()) {
                DisplayName(
                    UIText.DynamicString(fullName),
                    clickableActionIfPossible(!state.isEditNameAllowed, navigateToChangeDisplayName)
                )
            } else {
                null
            },
            if (userName.isNotBlank()) {
                Username(UIText.DynamicString("@$userName"), clickableActionIfPossible(!state.isEditHandleAllowed, navigateToChangeHandle))
            } else {
                null
            },
            if (email.isNotBlank()) {
                Email(
                    UIText.DynamicString(email),
                    clickableActionIfPossible(!state.isEditEmailAllowed, navigateToChangeEmail)
                )
            } else {
                null
            },
            if (accent != null) {
                UserColor(
                    UIText.StringResource(accent.resourceId()),
                    Clickable(onClick = navigateToChangeColor),
                    accent
                )
            } else {
                null
            },
            if (!teamName.isNullOrBlank()) Team(UIText.DynamicString(teamName)) else null,
            if (domain.isNotBlank()) Domain(UIText.DynamicString(domain)) else null
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
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    WireScaffold(
        modifier = modifier,
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
                        onClick = { CustomTabsHelper.launchUrl(context, forgotPasswordUrl) }
                    )
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
            sectionWithElements(
                header = UIText.StringResource(R.string.settings_myaccount_title),
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
                                text = item.text.asString(),
                                modifier = Modifier.padding(start = dimensions().spacing8x)
                            )
                        },
                        actions = {
                            Row {
                                if (item is UserColor && item.accent != Accent.Unknown) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = dimensions().spacing12x)
                                            .size(dimensions().spacing24x)
                                            .background(
                                                color = colorsScheme().wireAccentColors.getOrDefault(
                                                    item.accent,
                                                    colorsScheme().primary,
                                                ),
                                                shape = RoundedCornerShape(MaterialTheme.wireDimensions.groupAvatarCornerRadius)
                                            )
                                    ) {}
                                }
                                if (item.clickable?.enabled == true) {
                                    Icon(
                                        painter = painterResource(commonR.drawable.ic_chevron_right),
                                        contentDescription = null,
                                    )
                                }
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
            DisplayName(UIText.DynamicString("Bob"), Clickable(enabled = true) {}),
            Username(UIText.DynamicString("@bob_wire"), Clickable(enabled = true) {}),
            Email(UIText.DynamicString("bob@wire.com"), Clickable(enabled = true) {}),
            Team(UIText.DynamicString("Wire"))
        ),
        forgotPasswordUrl = "http://wire.com",
        canDeleteAccount = true,
        { },
        { },
        {},
        false,
        Modifier,
        { }
    )
}
