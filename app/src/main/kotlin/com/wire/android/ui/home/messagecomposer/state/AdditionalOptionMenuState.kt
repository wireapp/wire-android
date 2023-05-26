/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class AdditionalOptionMenuState {
    abstract var additionalOptionsSubMenuState: AdditionalOptionSubMenuState

    class AttachmentAndAdditionalOptionsMenu : AdditionalOptionMenuState() {

        override var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
            AdditionalOptionSubMenuState.None
        )

        fun toggleAttachmentMenu() {
            additionalOptionsSubMenuState =
                if (additionalOptionsSubMenuState == AdditionalOptionSubMenuState.AttachFile) {
                    AdditionalOptionSubMenuState.None
                } else {
                    AdditionalOptionSubMenuState.AttachFile
                }
        }

        fun toggleGifMenu() {
            additionalOptionsSubMenuState =
                if (additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Gif) {
                    AdditionalOptionSubMenuState.None
                } else {
                    AdditionalOptionSubMenuState.Gif
                }
        }

    }

    object RichTextEditing : AdditionalOptionMenuState() {
        override var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
            AdditionalOptionSubMenuState.None
        )
    }
}
