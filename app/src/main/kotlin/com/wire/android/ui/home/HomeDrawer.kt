package com.wire.android.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.NavigationItem
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.selectableBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDrawer(
    drawerState: DrawerState,
    currentRoute: String?,
    homeNavController: NavController,
    topItems: List<HomeNavigationItem>,
    scope: CoroutineScope,
    viewModel: HomeViewModel
) {
    val bottomItems = listOf(NavigationItem.Settings, NavigationItem.Support)

    Column(
        modifier = Modifier
            .padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 16.dp)

    ) {
        Logo()

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )

        topItems.forEach { item ->
            DrawerItem(data = item.getDrawerData(),
                selected = currentRoute == item.route,
                onItemClick = {
                    navigateToItemInHome(homeNavController, item)
                    scope.launch { drawerState.close() }
                })
        }

        Spacer(modifier = Modifier.weight(1f))

        bottomItems.forEach { item ->
            DrawerItem(data = item.getDrawerData(),
                selected = currentRoute == item.route,
                onItemClick = {
                    scope.launch {
                        viewModel.navigateTo(item)
                        drawerState.close()
                    }
                })
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
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor)
            .selectableBackground(selected) { onItemClick() }
    ) {
        Image(
            painter = painterResource(id = data.icon!!),
            contentDescription = stringResource(data.title!!),
            colorFilter = ColorFilter.tint(contentColor),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
        Text(
            text = stringResource(id = data.title!!),
            fontSize = 15.sp,
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}

data class DrawerItemData(@StringRes val title: Int?, @DrawableRes val icon: Int?)

@ExperimentalMaterial3Api
private fun Any.getDrawerData(): DrawerItemData =
    when (this) {
        is HomeNavigationItem.Vault -> DrawerItemData(R.string.vault_screen_title, R.drawable.ic_vault)
        is HomeNavigationItem.Conversations -> DrawerItemData(R.string.conversations_screen_title, R.drawable.ic_conversation)
        is HomeNavigationItem.Archive -> DrawerItemData(R.string.archive_screen_title, R.drawable.ic_archive)
        is NavigationItem.Settings -> DrawerItemData(R.string.settings_screen_title, R.drawable.ic_settings)
        is NavigationItem.Support -> DrawerItemData(R.string.support_screen_title, R.drawable.ic_support)
        else -> DrawerItemData(null, null)
    }
