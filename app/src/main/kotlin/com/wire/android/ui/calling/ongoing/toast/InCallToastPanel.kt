/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.calling.ongoing.toast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun InCallToastPanel(
    items: Set<InCallToast>,
    onToastClick: (toastId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    val itemStates = remember { // Internal buffer to keep removed toasts in the UI during their exit animation
        mutableStateMapOf<String, InCallToast>().apply {
            putAll(items.associateBy { it.id }) // Initialize the map with current `items` immediately
        }
    }
    LaunchedEffect(items) { // Sync toasts into buffer map whenever set of 'items' changes
        itemStates.putAll(items.associateBy { it.id })
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions().spacing4x)
    ) {
        itemStates.values.sortedBy { it.time }.map { it.id }.forEach { key -> // Sort keys by the timestamp in buffer map
            val isPresent = items.find { it.id == key } != null // The item is "present" if it's still in the source 'items' set

            key(key) { // Use the toast key as the Compose key to maintain identity across recompositions
                val visibilityState = remember {
                    MutableTransitionState(isPreview).apply { // For preview, start visible, otherwise start hidden
                        targetState = true // Target 'true' immediately to guarantee 'Enter' animation plays for new items
                    }
                }
                // Now, sync with the real 'isPresent' logic. This only changes the target to 'false' when the item is actually removed
                visibilityState.targetState = isPresent
                // Animate visibility based on the presence of the item in the source set. This controls the enter/exit animations
                AnimatedVisibility(
                    visibleState = visibilityState,
                    enter = fadeIn() + scaleIn() + expandIn(expandFrom = Alignment.Center),
                    exit = fadeOut() + scaleOut() + shrinkOut(shrinkTowards = Alignment.Center),
                ) {
                    // Use the buffered toast to ensure data exists during the exit fade
                    val currentItem = itemStates[key] ?: return@AnimatedVisibility
                    // Animate internal changes (like a name update) for the same toast key, without re-running the enter/exit animation
                    AnimatedContent(targetState = currentItem) { target ->
                        InCallToast(
                            toast = target,
                            onClick = onToastClick,
                            modifier = Modifier.padding(dimensions().spacing4x)
                        )
                    }
                }
                // Cleanup: remove from buffer map once AnimatedVisibility is done
                if (!isPresent) {
                    LaunchedEffect(visibilityState.isIdle) {
                        if (visibilityState.isIdle && !visibilityState.targetState) {
                            itemStates.remove(key)
                        }
                    }
                }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun InCallToastPanelPreview() = WireTheme {
    InCallToastPanel(
        items = setOf(
            InCallToast.Fullscreen(0L, InCallToast.Fullscreen.Type.DoubleTapToOpen),
            InCallToast.ModerationAction(1L, "1", "Alice", InCallToast.ModerationAction.Type.Muted)
        ),
        onToastClick = {}
    )
}
