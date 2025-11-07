/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.meetings.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator

@Composable
fun MeetingShowAllFooter(onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    WireSecondaryButton(
        text = stringResource(R.string.meeting_button_show_all),
        onClick = onShowAll,
        fillMaxWidth = true,
        minSize = dimensions().buttonSmallMinSize,
        minClickableSize = dimensions().buttonMinClickableSize,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x),
    )
}

@Composable
fun MeetingLoadMoreFooter(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensions().spacing8x)
    ) {
        WireCircularProgressIndicator(progressColor = colorsScheme().onSurface)
    }
}
