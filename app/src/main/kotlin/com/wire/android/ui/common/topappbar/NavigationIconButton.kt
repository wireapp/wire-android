package com.wire.android.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.wire.android.R

@Composable
fun NavigationIconButton(iconType: NavigationIconType, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = iconType.icon, contentDescription = stringResource(iconType.contentDescription))
    }
}

@Composable
fun BackNavigationIconButton(onBackButtonClick: () -> Unit) { NavigationIconButton(NavigationIconType.Back, onBackButtonClick) }

enum class NavigationIconType(val icon: ImageVector, @StringRes val contentDescription: Int) {
    Back(Icons.Filled.ArrowBack, R.string.content_description_back_button),
    Close(Icons.Filled.Close, R.string.content_description_close_button),
    Menu(Icons.Filled.Menu, R.string.content_description_menu_button)
}
