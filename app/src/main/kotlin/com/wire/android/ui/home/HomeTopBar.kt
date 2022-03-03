package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.newconversation.SearchableWireCenterAlignedTopAppBar


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
        SearchableWireCenterAlignedTopAppBar(
            topBarTitle = topBarTitle,
            searchHint = stringResource(R.string.search_bar_hint, topBarTitle.lowercase()),
            scrollPosition = scrollPosition,
            onNavigationPressed = { onHamburgerMenuItemCLick() },
            navigationIconType = navigationIconType,
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
