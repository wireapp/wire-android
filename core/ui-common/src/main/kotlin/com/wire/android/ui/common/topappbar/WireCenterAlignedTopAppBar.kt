/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.common.topappbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.PreviewMultipleThemes
import kotlin.math.ceil

@Composable
fun WireCenterAlignedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.wireTypography.title01,
    maxLines: Int = 2,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
    onNavigationPressed: () -> Unit = {},
    navigationIconType: NavigationIconType? = NavigationIconType.Back(),
    elevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    titleContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    WireCenterAlignedTopAppBar(
        titleContent = {
            WireTopAppBarTitle(
                title = title,
                style = titleStyle,
                maxLines = maxLines,
                contentDescription = titleContentDescription
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
    modifier: Modifier = Modifier,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
    onNavigationPressed: () -> Unit = {},
    navigationIconType: NavigationIconType? = NavigationIconType.Back(),
    elevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    actions: @Composable RowScope.() -> Unit = {},
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
                actions = actions,
                modifier = Modifier
                    // TopAppBarHorizontalPadding is 4.dp so another 4.dp needs to be added to match 8.dp from designs
                    .padding(end = dimensions().spacing4x),
            )
            bottomContent()
        }
    }
}

@Composable
fun WireTopAppBarTitle(
    title: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    contentDescription: String? = null
) {
    // There's an ongoing issue about multiline text taking all width available instead of wrapping visible text.
    // https://issuetracker.google.com/issues/206039942
    // It's very noticeable on TopAppBar because due to that issue, the title is not centered, even if there are large enough empty spaces
    // on both sides and all lines of text are actually shorter and could fit at the center.
    // This workaround is based on this: https://stackoverflow.com/a/69947555, but instead of using SubcomposeLayout, we just measure text.
    BoxWithConstraints(modifier = modifier.padding(horizontal = dimensions().spacing6x)) {
        val textMeasurer = rememberTextMeasurer()
        val textLayoutResult: TextLayoutResult = textMeasurer.measure(
            text = title,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(
                minWidth = 0,
                minHeight = 0,
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight
            )
        )
        val width = with(LocalDensity.current) {
            (0 until textLayoutResult.lineCount).maxOf { line ->
                ceil(textLayoutResult.getLineRight(line) - textLayoutResult.getLineLeft(line)).toInt()
            }.toDp()
        }
        Text(
            modifier = Modifier
                .width(width)
                .semantics {
                    heading()
                    contentDescription?.let { this.contentDescription = it }
                },
            text = title,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCenterAlignedTopAppBarWithDefaultTitle() = WireTheme {
    Box(modifier = Modifier.width(400.dp)) {
        WireCenterAlignedTopAppBar(
            title = "This is title",
            titleStyle = MaterialTheme.wireTypography.title01
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCenterAlignedTopAppBarWithDefaultTwoLinesTitle() = WireTheme {
    Box(modifier = Modifier.width(400.dp)) {
        WireCenterAlignedTopAppBar(
            title = "This title is a quite long title another_line",
            titleStyle = MaterialTheme.wireTypography.title01
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCenterAlignedTopAppBarWithDefaultTwoLinesTooLongTitle() = WireTheme {
    Box(modifier = Modifier.width(400.dp)) {
        WireCenterAlignedTopAppBar(
            title = "This title is even longer than one before another_line",
            titleStyle = MaterialTheme.wireTypography.title01
        )
    }
}
