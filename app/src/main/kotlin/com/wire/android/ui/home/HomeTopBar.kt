package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.SearchBarUI
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.theme.title01
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun HomeTopBar(
    @StringRes title: Int?,
    isSearchable: Boolean,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: HomeViewModel
) {
    val titleText = stringResource(id = title ?: R.string.conversations_screen_title)

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            navigationIcon = { ToolbarIconBtn(scope, drawerState) },
            title = { Text(text = titleText, style = MaterialTheme.typography.title01) },
            actions = {
                UserProfileAvatar(avatarUrl = "") {
                    scope.launch { viewModel.navigateToUserProfile() }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (isSearchable) {
            SearchBarUI(placeholderText = stringResource(R.string.search_bar_hint, titleText.lowercase()))
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun ToolbarIconBtn(scope: CoroutineScope, drawerState: DrawerState) {

    IconButton(
        onClick = { scope.launch { drawerState.open() } },
        modifier = Modifier.height(40.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = stringResource(R.string.home_open_drawer_description),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
