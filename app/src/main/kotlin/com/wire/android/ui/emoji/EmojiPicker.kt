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

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.emoji2.emojipicker.EmojiPickerView
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.wire.android.R

@Composable
fun EmojiPickerBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit = {},
    onEmojiSelected: (emoji: String) -> Unit
) {
    val context = LocalContext.current
    val dialog = remember {
        HandleDraggableBottomSheetDialog(context).apply {
            setContentView(R.layout.view_emoji_picker).run {
                findViewById<View>(R.id.emoji_picker_back_button)?.setOnClickListener {
                    dismiss()
                    onDismiss.invoke()
                }
                findViewById<EmojiPickerView>(R.id.emoji_picker)?.setOnEmojiPickedListener { emojiViewItem ->
                    dismiss()
                    onEmojiSelected(emojiViewItem.emoji)
                }
                findViewById<BottomSheetDragHandleView>(R.id.handle)?.let { handle ->
                    getBehavior().dragHandle = handle
                }
            }
            setOnCancelListener { onDismiss.invoke() }
        }
    }
    if (isVisible) {
        dialog.show()
    } else {
        dialog.hide()
    }
}
