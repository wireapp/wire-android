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

package com.wire.android.ui.common.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions
import kotlinx.coroutines.launch

@Composable
fun ShakeAnimation(
    modifier: Modifier = Modifier,
    offset: Dp = dimensions().spacing12x,
    duration: Int = 160,
    animateContent: @Composable (() -> Unit) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val animate: () -> Unit = {
        coroutineScope.launch {
            offsetX.animateTo(
                targetValue = 0f, // return to the starting position
                animationSpec = keyframes {
                    durationMillis = duration
                    offset.value at duration * 1 / 8 // max right offset after 12.5% of animation time
                    -offset.value at duration * 3 / 8 // max left offset after 37.5% of animation time (passes 0 at 25% of animation time)
                    offset.value at duration * 5 / 8 // max right offset after 62.5% of animation time (passes 0 at 50% of animation time)
                    -offset.value at duration * 7 / 8 // max left offset after 87.5% of animation time (passes 0 at 75% of animation time)
                }
            )
        }
    }
    Box(modifier = modifier.graphicsLayer { translationX = offsetX.value }) { animateContent(animate) }
}
