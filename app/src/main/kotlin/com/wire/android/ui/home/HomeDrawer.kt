package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DrawerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.navigation.HomeNavigationItem.Conversations
import com.wire.android.navigation.HomeNavigationItem.Settings
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationItem.Debug
import com.wire.android.navigation.NavigationItem.Support
import com.wire.android.navigation.isExternalRoute
import com.wire.android.navigation.navigateToItemInHome
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.EMPTY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeDrawer(
    drawerState: DrawerState,
    currentRoute: String?,
    homeNavController: NavController,
    topItems: List<HomeNavigationItem>,
    scope: CoroutineScope,
    viewModel: HomeViewModel
) {
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    Column(
        modifier = Modifier
            .padding(
                start = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                end = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                bottom = MaterialTheme.wireDimensions.homeDrawerBottomPadding
            )

    ) {
        Logo()

        fun navigateAndCloseDrawer(item: Any) = scope.launch {
            when (item) {
                is HomeNavigationItem -> navigateToItemInHome(homeNavController, item)
                is NavigationItem -> when (item.isExternalRoute()) {
                    true -> CustomTabsHelper.launchUrl(homeNavController.context, item.getRouteWithArgs())
                    false -> viewModel.navigateTo(item)
                }
                else -> {}
            }
            drawerState.close()
        }

        topItems.forEach { item ->
            DrawerItem(
                data = item.getDrawerData(),
                selected = currentRoute == item.route(),
                onItemClick = remember { { navigateAndCloseDrawer(item) } }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val bottomItems = listOf(Settings, Support, Debug)
        bottomItems.forEach { item ->
            DrawerItem(
                data = item.getDrawerData(),
                selected = currentRoute == item.route(),
                onItemClick = remember { { navigateAndCloseDrawer(item) } }
            )
        }

        Text(
            text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun DrawerItem(data: DrawerItemData, selected: Boolean, onItemClick: () -> Unit) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor)
            .selectableBackground(selected) { onItemClick() },
    ) {
        Image(
            painter = painterResource(id = data.icon!!),
            contentDescription = stringResource(data.title!!),
            colorFilter = ColorFilter.tint(contentColor),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
        Text(
            style = MaterialTheme.wireTypography.button02,
            text = stringResource(id = data.title),
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}

data class DrawerItemData(@StringRes val title: Int?, @DrawableRes val icon: Int?)

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
private fun Any.getDrawerData(): DrawerItemData =
    when (this) {
        // TODO: Re-enable when we have Vault & Archive
//        Vault -> DrawerItemData(R.string.vault_screen_title, R.drawable.ic_vault)
//        Archive -> DrawerItemData(R.string.archive_screen_title, R.drawable.ic_archive)
        Conversations -> DrawerItemData(R.string.conversations_screen_title, R.drawable.ic_conversation)
        Settings -> DrawerItemData(R.string.settings_screen_title, R.drawable.ic_settings)
        Support -> DrawerItemData(R.string.support_screen_title, R.drawable.ic_support)
        Debug -> DrawerItemData(R.string.debug_screen_title, R.drawable.ic_bug)
        else -> DrawerItemData(null, null)
    }

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
fun Any.route() = when (this) {
    is HomeNavigationItem -> this.route
    is NavigationItem -> this.getRouteWithArgs()
    else -> null
}
