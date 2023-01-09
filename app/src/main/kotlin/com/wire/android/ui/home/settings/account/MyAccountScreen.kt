package com.wire.android.ui.home.settings.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.account.AccountDetailsItem.DisplayName
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Domain
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Email
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Team
import com.wire.android.ui.home.settings.account.AccountDetailsItem.Username
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.extension.folderWithElements

@Composable
fun MyAccountScreen(viewModel: MyAccountViewModel = hiltViewModel()) {
    with(viewModel.myAccountState) {
        MyAccountContent(
            accountDetailItems = mapToUISections(viewModel, this),
            forgotPasswordUrl = this.changePasswordUrl,
            onNavigateBack = viewModel::navigateBack
        )
    }
}

@Stable
private fun mapToUISections(viewModel: MyAccountViewModel, state: MyAccountState): List<AccountDetailsItem> {
    return with(state) {
        listOfNotNull(
            if (fullName.isNotBlank()) DisplayName(fullName, Clickable { viewModel.navigateToChangeDisplayName() }) else null,
            if (userName.isNotBlank()) Username("@$userName") else null,
            if (email.isNotBlank()) Email(email) else null,
            if (teamName.isNotBlank()) Team(teamName) else null,
            if (domain.isNotBlank()) Domain(domain) else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccountContent(
    accountDetailItems: List<AccountDetailsItem> = emptyList(),
    forgotPasswordUrl: String?,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
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
                factory = { item ->
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
                            if (item.clickable.enabled) {
                                Icons.Filled.ChevronRight.Icon().invoke()
                            }
                        },
                        clickable = item.clickable
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
            Username("@bob_wire"),
            Email("bob@wire.com"),
            Team("Wire")
        ),
        forgotPasswordUrl = "http://wire.com"
    )
}
