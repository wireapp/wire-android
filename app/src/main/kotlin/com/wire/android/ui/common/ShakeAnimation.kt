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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ShakeAnimation(offset: Dp = 8.dp, duration: Int = 160, animateContent: @Composable (() -> Unit) -> Unit) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val animate: () -> Unit = {
        coroutineScope.launch {
            launch {
                repeat(3) {
                    offsetX.animateTo(
                        targetValue = 0f, // return to the starting position
                        animationSpec = keyframes {
                            durationMillis = duration
                            offset.value at duration / 4        // max right offset after 25% of animation time
                            -offset.value at duration * 3 / 4   // max left offset after 75% of animation time
                        }
                    )
                }
            }
        }
    }
    Box(modifier = Modifier.offset(x = offsetX.value.dp)) { animateContent(animate) }
}
