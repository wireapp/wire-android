package com.wire.android.ui.common.topappbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireCenterAlignedTopAppBar(
    title: String,
    onNavigationPressed: () -> Unit = {},
    navigationIconType: NavigationIconType? = NavigationIconType.Back,
    elevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = wireTopAppBarColors(),
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        shadowElevation = elevation,
        color = colors.containerColor(scrollFraction = 0f).value
    ) {
        Column {
            CenterAlignedTopAppBar(
                title = { WireTopAppBarTitle(title = title) },
                navigationIcon = { navigationIconType?.let { NavigationIconButton(iconType = it) { onNavigationPressed() } } },
                colors = colors,
                actions = actions
            )
            bottomContent()
        }
    }
}

@Composable
fun WireTopAppBarTitle(title: String) { Text(text = title, style = MaterialTheme.wireTypography.title01) }
