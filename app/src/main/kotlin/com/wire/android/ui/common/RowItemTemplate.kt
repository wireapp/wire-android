package com.wire.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversationslist.common.EventBadgeFactory
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.theme.DEFAULT_WEIGHT

@Composable
fun RowItemTemplate(
    leadingIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    clickable: Clickable = Clickable(false) {},
    modifier: Modifier = Modifier
) {
    RowItem(
        clickable = clickable,
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
    eventType: BadgeEventType = BadgeEventType.None,
    clickable: Clickable,
    trailingIcon: @Composable () -> Unit = { },
    modifier: Modifier = Modifier
) {
    RowItem(
        clickable = clickable,
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
            if (eventType != BadgeEventType.None) {
                EventBadgeFactory(eventType = eventType, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        trailingIcon()
    }
}
