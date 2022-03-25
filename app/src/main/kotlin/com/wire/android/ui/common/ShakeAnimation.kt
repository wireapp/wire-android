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
