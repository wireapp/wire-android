package com.wire.android.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import kotlin.math.absoluteValue

@Composable
fun WireTabRow(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    onTabChange: (Int) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.background,
    divider: @Composable () -> Unit = @Composable { TabRowDefaults.Divider() },
    modifier: Modifier = Modifier
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
            Tab(
                enabled = true,
                text = {
                    Text(
                        text = stringResource(id = tabItem.titleResId),
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

@OptIn(ExperimentalPagerApi::class)
fun PagerState.calculateCurrentTab() = // change the tab if we go over half the offset
    if(this.currentPageOffset.absoluteValue > 0.5f) this.targetPage else this.currentPage

interface TabItem {
    @get:StringRes
    val titleResId: Int
}
