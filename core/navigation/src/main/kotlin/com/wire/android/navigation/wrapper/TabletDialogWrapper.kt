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
package com.wire.android.navigation.wrapper

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.scope.DestinationScope
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.ramcosta.composedestinations.wrapper.DestinationWrapper

object TabletDialogWrapper : DestinationWrapper {

    @Composable
    override fun <T> DestinationScope<T>.Wrap(screenContent: @Composable () -> Unit) {
        val movableContent = remember(screenContent) { movableContentOf(screenContent) }
        val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= TABLET_MIN_SCREEN_WIDTH_DP
        val shouldWrapAsDialog = destination.style is DestinationStyle.Dialog ||
            (isTablet && shouldWrapTabletRouteAsDialog(destination.route))
        if (shouldWrapAsDialog) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .imePadding()
            ) {
                movableContent()
            }
        } else {
            movableContent()
        }
    }
}

fun setTabletDialogRouteMatcher(routeMatcher: (String) -> Boolean) {
    shouldWrapTabletRouteAsDialog = routeMatcher
}

@Volatile
private var shouldWrapTabletRouteAsDialog: (String) -> Boolean = { false }

private const val TABLET_MIN_SCREEN_WIDTH_DP = 600
