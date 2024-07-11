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
package com.wire.android.ui.calling.ongoing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun EmojiFlowAnimator(name: String, emoji: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val emojis = remember { mutableStateListOf<AnimatedEmoji>() }
        val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        LaunchedEffect(emoji, name) {
            emojis.add(
                AnimatedEmoji(
                    name = name,
                    emoji = emoji,
                    startX = Random.nextFloat() * maxWidthPx
                )
            )
        }

        emojis.forEach { animatedEmoji ->
            EmojiAnimator(
                emoji = animatedEmoji.emoji,
                name = animatedEmoji.name,
                startX = animatedEmoji.startX,
                screenHeightPx = maxHeightPx
            )
        }
    }
}

data class AnimatedEmoji(
    val name: String,
    val emoji: String,
    val startX: Float
)

@Composable
fun EmojiAnimator(
    emoji: String,
    name: String,
    startX: Float,
    screenHeightPx: Float
) {
    val translateY = remember { androidx.compose.animation.core.Animatable(screenHeightPx) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            translateY.animateTo(
                targetValue = -100f,
                animationSpec = tween(
                    durationMillis = 5000,
                    easing = LinearEasing
                )
            )
        }
    }

    Box(modifier = Modifier
        .graphicsLayer {
            translationY = translateY.value
            translationX = startX
        }
        .padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 48.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )
            Text(
                text = name,
                style = MaterialTheme.wireTypography.label01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
