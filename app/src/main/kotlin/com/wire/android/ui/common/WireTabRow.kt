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

package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import kotlin.math.absoluteValue

@Composable
fun WireTabRow(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    divider: @Composable () -> Unit = @Composable {
        HorizontalDivider(
            color = colorsScheme().outline,
            thickness = dimensions().dividerThickness
        )
    },
    upperCaseTitles: Boolean = true
) {
    TabRow(
        containerColor = containerColor,
        selectedTabIndex = selectedTabIndex,
        divider = divider,
        indicator = @Composable { tabPositions: List<TabPosition> ->
            WireIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]))
        },
        modifier = modifier
    ) {
        tabs.forEachIndexed { index, tabItem ->
            val selected = selectedTabIndex == index
            val text = tabItem.title.asString().let {
                if (upperCaseTitles) it.uppercase() else it
            }
            val selectText = stringResource(id = com.wire.android.R.string.content_description_select_label)

            Tab(
                modifier = Modifier.semantics { onClick(selectText) { false } },
                enabled = true,
                text = {
                    Text(
                        text = text,
                        style = MaterialTheme.wireTypography.title03
                    )
                },
                selectedContentColor = MaterialTheme.wireColorScheme.onSecondaryButtonSelected,
                unselectedContentColor = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
                selected = selected,
                onClick = { onTabChange(index) }
            )
        }
    }
}

@Composable
fun LoadingWireTabRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(dimensions().spacing48x)
            .fillMaxWidth()
            .background(color = colorsScheme().background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Spacer(
                modifier = Modifier
                    .padding(start = dimensions().spacing8x, end = dimensions().spacing8x)
                    .height(dimensions().spacing14x)
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .background(
                        color = colorsScheme().defaultSelectedItemInLoadingState,
                        shape = RoundedCornerShape(dimensions().corner16x)
                    )
                    .shimmerPlaceholder(
                        visible = true,
                        color = colorsScheme().defaultSelectedItemInLoadingState,
                        shape = RoundedCornerShape(dimensions().corner16x)
                    )
            )

            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(color = colorsScheme().primary)
            )
        }
        Spacer(
            Modifier
                .weight(1f)
                .height(dimensions().spacing14x)
                .padding(start = dimensions().spacing8x, end = dimensions().spacing8x)
                .background(color = colorsScheme().primaryButtonDisabled, shape = RoundedCornerShape(dimensions().corner16x))
                .shimmerPlaceholder(
                    visible = true,
                    color = colorsScheme().primaryButtonDisabled,
                    shape = RoundedCornerShape(dimensions().corner16x)
                )
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLoadingWireTabRow() {
    WireTheme {
        LoadingWireTabRow()
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewWireTabRow() {
    WireTheme {
        WireTabRow(
            tabs = listOf(
                object : TabItem {
                    override val title: UIText = UIText.StringResource(com.wire.android.R.string.conversation_details_options_tab)
                },
                object : TabItem {
                    override val title: UIText = UIText.StringResource(com.wire.android.R.string.conversation_details_participants_tab)
                }
            ),
            selectedTabIndex = 0,
            onTabChange = {}
        )
    }
}

@Composable
private fun WireIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                color = MaterialTheme.wireColorScheme.primary,
                shape = RoundedCornerShape(1.dp)
            )
    )
}

@Suppress("MagicNumber")
fun PagerState.calculateCurrentTab() = // change the tab if we go over half the offset
    if (this.currentPageOffsetFraction.absoluteValue > 0.5f) this.targetPage else this.currentPage

interface TabItem {
    val title: UIText
}
