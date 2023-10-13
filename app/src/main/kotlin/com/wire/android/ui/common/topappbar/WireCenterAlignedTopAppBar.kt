/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.common.topappbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireCenterAlignedTopAppBar(
    title: String,
    titleStyle: TextStyle = MaterialTheme.wireTypography.title01,
    maxLines: Int = 2,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
    onNavigationPressed: () -> Unit = {},
    navigationIconType: NavigationIconType? = NavigationIconType.Back,
    elevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    WireCenterAlignedTopAppBar(
        titleContent = {
            WireTopAppBarTitle(
                title = title,
                style = titleStyle,
                maxLines = maxLines
            )
        },
        subtitleContent = subtitleContent,
        onNavigationPressed = onNavigationPressed,
        navigationIconType = navigationIconType,
        elevation = elevation,
        actions = actions,
        modifier = modifier,
        bottomContent = bottomContent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireCenterAlignedTopAppBar(
    titleContent: @Composable ColumnScope.() -> Unit,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
    onNavigationPressed: () -> Unit = {},
    navigationIconType: NavigationIconType? = NavigationIconType.Back,
    elevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shadowElevation = elevation,
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        titleContent()
                        subtitleContent()
                    }
                },
                navigationIcon = { navigationIconType?.let { NavigationIconButton(iconType = it, onClick = onNavigationPressed) } },
                colors = wireTopAppBarColors(),
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
    maxLines: Int = 2
) {
    Text(
        modifier = Modifier.padding(
            start = dimensions().spacing6x,
            end = dimensions().spacing6x
        ),
        text = title,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
