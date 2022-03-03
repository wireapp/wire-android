package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.SearchBar
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.newconversation.SearchTopBar


@ExperimentalMaterial3Api
@Composable
fun HomeTopBar(
    @StringRes title: Int,
    isSearchable: Boolean,
    scrollPosition: Int,
    onUserProfileClick: () -> Unit,
    onHamburgerMenuItemCLick: () -> Unit,
) {
    val topBarTitle = stringResource(id = title)
    val navigationIconType = NavigationIconType.Menu

    if (isSearchable) {
        SearchTopBar(
            topBarTitle = topBarTitle,
            scrollPosition = scrollPosition,
            onNavigationPressed = { onHamburgerMenuItemCLick() },
            navigationIconType = navigationIconType,
            searchBar = {
                SearchBar(
                    placeholderText = stringResource(R.string.search_bar_hint, topBarTitle.lowercase()),
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                )
            },
            actions = {
                UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
                    onUserProfileClick()
                }
            }
        )
    } else {
        WireCenterAlignedTopAppBar(
            title = topBarTitle,
            onNavigationPressed = { onHamburgerMenuItemCLick() },
            navigationIconType = navigationIconType,
            actions = {
                UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
                    onUserProfileClick()
                }
            }
        )
    }
}
