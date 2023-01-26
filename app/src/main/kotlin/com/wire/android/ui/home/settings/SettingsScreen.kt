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

package com.wire.android.ui.home.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.isExternalRoute
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.extension.folderWithElements
import com.wire.android.BuildConfig

@Composable
fun SettingsScreen(
    lazyListState: LazyListState = rememberLazyListState(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    SettingsScreenContent(
        lazyListState = lazyListState,
        onItemClicked = remember {
            {
                when (it.navigationItem.isExternalRoute()) {
                    true -> CustomTabsHelper.launchUrl(context, it.navigationItem.getRouteWithArgs())
                    false -> viewModel.navigateTo(it.navigationItem)
                }
            }
        })
}

@Composable
fun SettingsScreenContent(
    lazyListState: LazyListState = rememberLazyListState(),
    onItemClicked: (SettingsItem) -> Unit
) {
    val context = LocalContext.current

    val featureVisibilityFlags = LocalFeatureVisibilityFlags.current

    with(featureVisibilityFlags) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {

            folderWithElements(
                header = context.getString(R.string.settings_account_settings_label),
                items = buildList {
                    add(SettingsItem.YourAccount)
                    add(SettingsItem.PrivacySettings)
                    add(SettingsItem.ManageDevices)
                    if (BackUpSettings) {
                        add(SettingsItem.BackupAndRestore)
                    }
                },
                onItemClicked = onItemClicked
            )

            folderWithElements(
                header = context.getString(R.string.app_settings_screen_title),
                items = buildList {
                    if (AppSettings) {
                        add(SettingsItem.AppSettings)
                    }
                    add(SettingsItem.NetworkSettings)
                },
                onItemClicked = onItemClicked
            )

            folderWithElements(
                header = context.getString(R.string.settings_other_group_title),
                items = buildList {
                    add(SettingsItem.Support)
                    if (BuildConfig.DEBUG_SCREEN_ENABLED) {
                        add(SettingsItem.DebugSettings)
                    }
                },
                onItemClicked = onItemClicked
            )
        }
    }
}

private fun LazyListScope.folderWithElements(
    header: String,
    items: List<SettingsItem>,
    onItemClicked: (SettingsItem) -> Unit
) {
    folderWithElements(
        header = header.uppercase(),
        items = items.associateBy { it.id }
    ) { settingsItem ->
        SettingsItem(
            title = settingsItem.title.asString(),
            onRowPressed = remember { Clickable(enabled = true) { onItemClicked(settingsItem) } }
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    @DrawableRes trailingIcon: Int? = null,
    onRowPressed: Clickable = Clickable(false),
    onIconPressed: Clickable = Clickable(false)
) {
    RowItemTemplate(
        title = {
            Row {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = title,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
        },
        actions = {
            trailingIcon?.let {
                Icon(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = "",
                    tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                    modifier = Modifier
                        .defaultMinSize(80.dp)
                        .clickable(onIconPressed)
                )
            } ?: Icons.Filled.ChevronRight
        },
        clickable = onRowPressed
    )
}

@Preview(showBackground = false)
@Composable
fun SettingsScreenPreview() {
    SettingsScreenContent {}
}
