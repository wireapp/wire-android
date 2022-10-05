package com.wire.android.ui.home.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.isExternalRoute
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.debug.LocalFeatureVisibilityFlags

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
                header = context.getString(R.string.settings_general_group_title),
                items = buildList {
                    if (AppSettings) {
                        add(SettingsItem.AppSettings)
                    }
                    add(SettingsItem.ManageDevices)
                    add(SettingsItem.NetworkSettings)
                },
                onItemClicked = onItemClicked
            )

            if (BackUpSettings) {
                folderWithElements(
                    header = context.getString(R.string.settings_backups_group_title),
                    items = listOf(SettingsItem.BackupAndRestore),
                    onItemClicked = onItemClicked
                )
            }

            folderWithElements(
                header = context.getString(R.string.settings_other_group_title),
                items = listOf(SettingsItem.Support, SettingsItem.DebugSettings),
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
            item = settingsItem,
            clickable = remember { Clickable(enabled = true) { onItemClicked(settingsItem) } }
        )
    }
}

@Composable
private fun SettingsItem(item: SettingsItem, clickable: Clickable) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = item.title.asString(),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = Icons.Filled.ChevronRight.Icon(),
        clickable = clickable
    )
}


@Preview(showBackground = false)
@Composable
fun SettingsScreenPreview() {
    SettingsScreenContent {}
}
