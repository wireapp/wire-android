package com.wire.android.ui.common.topappbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireCenterAlignedTopAppBar(
    title: String,
    titleStyle: TextStyle = MaterialTheme.wireTypography.title01,
    maxLines: Int = 2,
    titleStartPadding: Dp = 0.dp,
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
                title = {
                    WireTopAppBarTitle(
                        title = title,
                        style = titleStyle,
                        maxLines = maxLines,
                        titleStartPadding = titleStartPadding
                    )
                },
                navigationIcon = { navigationIconType?.let { NavigationIconButton(iconType = it) { onNavigationPressed() } } },
                colors = colors,
                actions = actions
            )
            bottomContent()
        }
    }
}

@Composable
fun WireTopAppBarTitle(
    title: String,
    style: TextStyle,
    maxLines: Int = 2,
    titleStartPadding: Dp = 0.dp
) {
    Text(
        modifier = Modifier.padding(start = titleStartPadding),
        text = title,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
