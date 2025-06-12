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

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.Clickable
import com.wire.android.util.LocalSyncStateObserver

@Composable
fun Tint(contentColor: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
}

@Composable
fun ImageVector.Icon(modifier: Modifier = Modifier): @Composable (() -> Unit) =
    { androidx.compose.material3.Icon(imageVector = this, contentDescription = "", modifier = modifier) }

@Composable
fun rememberClickBlockAction(clickBlockParams: ClickBlockParams, clickAction: () -> Unit): () -> Unit {
    val syncStateObserver = LocalSyncStateObserver.current
    val context = LocalContext.current
    val clickerHandler = remember { SingleClickHandler() }
    return remember(clickBlockParams, syncStateObserver, clickAction) {
        {
            when {
                clickBlockParams.blockWhenConnecting && syncStateObserver.isConnecting ->
                    Toast.makeText(context, context.getString(R.string.label_wait_until_connected), Toast.LENGTH_SHORT).show()

                clickBlockParams.blockWhenSyncing && syncStateObserver.isSyncing ->
                    Toast.makeText(context, context.getString(R.string.label_wait_until_synchronised), Toast.LENGTH_SHORT).show()

                else -> clickerHandler.ensureSingleClick { clickAction() }
            }
        }
    }
}

@SuppressLint("ComposeComposableModifier")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.clickable(clickable: Clickable?) = clickable?.let {
    if (clickable.enabled) {
        val onClick = rememberClickBlockAction(clickable.clickBlockParams, clickable.onClick)
        val onLongClick = clickable.onLongClick?.let { onLongClick ->
            rememberClickBlockAction(clickable.clickBlockParams, onLongClick)
        }
        this.combinedClickable(
            enabled = clickable.enabled,
            onClick = onClick,
            onLongClick = onLongClick,
            onClickLabel = clickable.onClickDescription,
            onLongClickLabel = clickable.onLongClickDescription
        )
    } else {
        // even though element is disabled we want to merge all inner elements into one for TalkBack
        this.semantics(mergeDescendants = true) { }
    }
} ?: this

private class SingleClickHandler {

    private companion object {
        private const val CLICK_THRESHOLD = 500L
    }

    private val now: Long
        get() = System.currentTimeMillis()

    private var lastEventTimeMs: Long = 0

    fun ensureSingleClick(block: () -> Unit) {
        if (now - lastEventTimeMs >= CLICK_THRESHOLD) {
            block()
        }
        lastEventTimeMs = now
    }
}
