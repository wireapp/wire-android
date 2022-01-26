package com.wire.android.ui.drawer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationType
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.selectableBackground
import kotlinx.coroutines.launch

@Composable
fun WireDrawer(
    currentRoute: String?,
    navigationType: NavigationType?,
    viewModel: DrawerViewModel = hiltViewModel()
): @Composable (ColumnScope.() -> Unit)? =
    if (navigationType != null && navigationType is NavigationType.WithTopBar.WithDrawer) {
        { HomeDrawerCompose(currentRoute, viewModel) }
    } else {
        null
    }

@Composable
private fun HomeDrawerCompose(
    currentRoute: String?,
    viewModel: DrawerViewModel
) {
    val scope = rememberCoroutineScope()
    val topItems = listOf(NavigationItem.Conversations, NavigationItem.Archive, NavigationItem.Vault)
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
                    scope.launch { viewModel.navigateTo(item) }
                })
        }

        Spacer(modifier = Modifier.weight(1f))

        bottomItems.forEach { item ->
            DrawerItem(data = item.getDrawerData(),
                selected = currentRoute == item.route,
                onItemClick = {
                    scope.launch { viewModel.navigateTo(item) }
                })
        }
    }
}

@Composable
fun DrawerItem(data: DrawerItemData, selected: Boolean, onItemClick: () -> Unit) {
    val backgroundColor = if (selected) MaterialTheme.colors.secondary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colors.onSecondary else MaterialTheme.colors.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
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
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = stringResource(id = data.title!!),
            fontSize = 14.sp,
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}

data class DrawerItemData(@StringRes val title: Int?, @DrawableRes val icon: Int?)

private fun NavigationItem.getDrawerData(): DrawerItemData =
    when (this) {
        is NavigationItem.Vault -> DrawerItemData(R.string.vault_screen_title, R.drawable.ic_vault)
        is NavigationItem.Conversations -> DrawerItemData(R.string.conversations_screen_title, R.drawable.ic_conversation)
        is NavigationItem.Archive -> DrawerItemData(R.string.archive_screen_title, R.drawable.ic_archive)
        is NavigationItem.Settings -> DrawerItemData(R.string.settings_screen_title, R.drawable.ic_settings)
        is NavigationItem.Support -> DrawerItemData(R.string.support_screen_title, R.drawable.ic_support)
        else -> DrawerItemData(null, null)
    }

@Preview(showBackground = false)
@Composable
fun DrawerItemPreview() {
    DrawerItem(data = NavigationItem.Conversations.getDrawerData(), selected = false, onItemClick = {})
}

@Preview(showBackground = false)
@Composable
fun DrawerItemSelectedPreview() {
    DrawerItem(data = NavigationItem.Conversations.getDrawerData(), selected = true, onItemClick = {})
}

@Preview(showBackground = true)
@Composable
fun DrawerPreview() {
    WireDrawer(
        currentRoute = "scope",
        navigationType = NavigationType.WithTopBar.WithDrawer(
            title = R.string.conversations_screen_title,
            isSearchable = true,
            hasUserAvatar = true
        ) as NavigationType
    )
}
