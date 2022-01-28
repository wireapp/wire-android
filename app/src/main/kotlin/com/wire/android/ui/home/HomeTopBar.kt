package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: HomeViewModel
) {
    val scope = rememberCoroutineScope()
    val titleText = stringResource(id = title ?: R.string.conversations_screen_title)

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, start = 4.dp),
            content = {
                ToolbarIconBtn(scope, drawerState)
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    text = titleText,
                    style = MaterialTheme.typography.title01
                )

                UserProfileAvatar(avatarUrl = "") {
                    scope.launch { viewModel.navigateToUserProfile() }
                }
            }
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
