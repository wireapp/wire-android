/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.search.widget.HighlightName
import com.wire.android.search.widget.HighlightSubtitle
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemTemplate

@Composable
internal fun EveryoneItemToMention(
    searchQuery: String,
    clickable: Clickable,
    modifier: Modifier = Modifier,
    backgroundColor: Color = colorsScheme().surface,
) {
    RowItemTemplate(
        leadingIcon = {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_mention), contentDescription = null)
            }
        },
        titleStartPadding = dimensions().spacing0x,
        title = { HighlightName(name = "@all", searchQuery = searchQuery) },
        subtitle = {
            HighlightSubtitle(
                subTitle = stringResource(R.string.mention_everyone_description),
                searchQuery = searchQuery,
            )
        },
        actions = {},
        clickable = clickable,
        modifier = modifier.padding(start = dimensions().spacing8x),
        backgroundColor = backgroundColor,
    )
}
