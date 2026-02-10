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
package com.wire.android.ui.settings.about

import com.wire.android.navigation.annotation.app.WireRootDestination
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@WireRootDestination
@Composable
fun AboutThisAppScreen(
    navigator: Navigator,
    viewModel: AboutThisAppViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    AboutThisAppContent(
        state = viewModel.state,
        onBackPressed = navigator::navigateBack,
        onItemClicked = remember {
            {
                it.direction.handleNavigation(
                    context = context,
                    handleOtherDirection = { navigator.navigate(NavigationCommand(it)) }
                )
            }
        }
    )
}

@Composable
private fun AboutThisAppContent(
    state: AboutThisAppState,
    onBackPressed: () -> Unit,
    onItemClicked: (SettingsItem.DirectionItem) -> Unit
) {
    val aboutThisAppContentState: AboutThisAppContentState = rememberAboutThisAppContentState()

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(id = R.string.about_app_screen_title),
                elevation = dimensions().spacing0x,
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onBackPressed
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(aboutThisAppContentState.scrollState)
                .padding(internalPadding)
        ) {
            SettingsItem(
                text = stringResource(id = R.string.settings_wire_website_label),
                trailingIcon = R.drawable.ic_arrow_right,
                onRowPressed = Clickable(
                    enabled = true,
                    onClick = {
                        onItemClicked(SettingsItem.WireWebsite)
                    }
                )
            )
            SettingsItem(
                text = stringResource(id = R.string.settings_terms_of_use_label),
                trailingIcon = R.drawable.ic_arrow_right,
                onRowPressed = Clickable(
                    enabled = true,
                    onClick = {
                        onItemClicked(SettingsItem.TermsOfUse)
                    }
                )
            )
            SettingsItem(
                text = stringResource(id = R.string.settings_privacy_policy_label),
                trailingIcon = R.drawable.ic_arrow_right,
                onRowPressed = Clickable(
                    enabled = true,
                    onClick = {
                        onItemClicked(SettingsItem.PrivacyPolicy)
                    }
                )
            )
            SettingsItem(
                text = stringResource(id = R.string.settings_licenses_settings_label),
                trailingIcon = R.drawable.ic_arrow_right,
                onRowPressed = Clickable(
                    enabled = true,
                    onClick = {
                        onItemClicked(SettingsItem.Licenses)
                    }
                )
            )
            SettingsItem(
                text = stringResource(id = R.string.settings_dependencies_label),
                trailingIcon = R.drawable.ic_arrow_right,
                onRowPressed = Clickable(
                    enabled = true,
                    onClick = {
                        onItemClicked(SettingsItem.Dependencies)
                    }
                )
            )
            SettingsItem(
                title = stringResource(R.string.label_code_commit_id),
                text = state.commitish,
                trailingIcon = R.drawable.ic_copy,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = {
                        aboutThisAppContentState.copyToClipboard(state.commitish)
                    }
                )
            )
            SettingsItem(
                title = stringResource(R.string.app_version),
                text = state.appName,
                trailingIcon = R.drawable.ic_copy,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = {
                        aboutThisAppContentState.copyToClipboard(state.appName)
                    }
                )
            )
            SettingsItem(
                title = stringResource(R.string.label_copyright),
                text = stringResource(id = R.string.label_copyright_value)
            )
        }
    }
}

@Composable
fun rememberAboutThisAppContentState(): AboutThisAppContentState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    return remember {
        AboutThisAppContentState(
            context = context,
            clipboardManager = clipboardManager,
            scrollState = scrollState
        )
    }
}

data class AboutThisAppContentState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val scrollState: ScrollState
) {
    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(
            context,
            context.getText(R.string.label_text_copied),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAboutThisAppScreen() = WireTheme {
    AboutThisAppContent(
        state = AboutThisAppState(commitish = "abcd-1234", appName = "4.1.9-1234-beta"),
        onBackPressed = { },
        onItemClicked = { }
    )
}
