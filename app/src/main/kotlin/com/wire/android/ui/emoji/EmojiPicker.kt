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
package com.wire.android.ui.emoji

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import com.wire.android.R
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.ModalSheetHeaderItem
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun <T : Any> EmojiPickerBottomSheet(
    sheetState: WireModalSheetState<T>,
    onEmojiSelected: (emoji: String, stateParameter: T) -> Unit,
    modifier: Modifier = Modifier,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        containerColor = colorsScheme().surface
    ) { stateParameter: T ->
        Column(modifier = modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ModalSheetHeaderItem(
                    header = MenuModalSheetHeader.Visible(
                        title = stringResource(R.string.emoji_picker_select_reaction),
                        customVerticalPadding = dimensions().spacing8x,
                        leadingIcon = {
                            IconButton(onClick = { sheetState.hide() }) {
                                ArrowLeftIcon()
                            }
                        },
                        includeDivider = false,
                    )
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colorsScheme().surfaceContainer)
            ) {
                val emojiSize = dimensions().inCallReactionButtonSize
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        TouchWrappingLayout(it).apply {
                            addView(
                                EmojiPickerView(ContextThemeWrapper(it, R.style.EmojiPickerViewStyle)).apply {
                                    emojiGridColumns = (maxWidth / emojiSize).toInt()
                                    setOnEmojiPickedListener { emojiViewItem -> onEmojiSelected(emojiViewItem.emoji, stateParameter) }
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

private class TouchWrappingLayout(context: Context) : FrameLayout(context) {
    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.onInterceptTouchEvent(e)
    }
}
