package com.wire.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.home.conversationslist.common.EventBadgeFactory
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.theme.DEFAULT_WEIGHT

@Composable
fun RowItemTemplate(
    leadingIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItem(
        onRowItemClick = onRowItemClicked,
        onRowItemLongClick = onRowItemLongClicked,
        modifier = modifier
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensions().spacing8x)
        ) {
            title()
            subtitle()
        }
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = dimensions().spacing8x)
        ) {
            actions()
        }
    }
}

@Composable
fun RowItemTemplate(
    leadingIcon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    eventType: EventType? = null,
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    trailingIcon: @Composable () -> Unit = { },
    modifier: Modifier = Modifier
) {
    RowItem(
        onRowItemClick = onRowItemClicked,
        onRowItemLongClick = onRowItemLongClicked,
        modifier = modifier
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .weight(DEFAULT_WEIGHT),
        ) {
            title()
            subTitle()
        }
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = dimensions().spacing8x)
        ) {
            if (eventType != null) {
                EventBadgeFactory(eventType = eventType, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        trailingIcon()
    }
}
