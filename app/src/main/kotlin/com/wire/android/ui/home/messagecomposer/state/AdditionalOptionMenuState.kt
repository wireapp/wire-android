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

package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

enum class AdditionalOptionMenuState {
    AttachmentAndAdditionalOptionsMenu,
    RichTextEditing
}

enum class AdditionalOptionSubMenuState {
    Default,
    RecordAudio,
}

enum class AdditionalOptionSelectItem {
    RichTextEditing,
    None,
}

class AdditionalOptionStateHolder {

    var selectedOption by mutableStateOf(AdditionalOptionSelectItem.None)

    var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
        AdditionalOptionSubMenuState.Default
    )
        private set

    var additionalOptionState: AdditionalOptionMenuState by mutableStateOf(AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu)
        private set

    fun unselectAdditionalOptionsMenu() {
        selectedOption = AdditionalOptionSelectItem.None
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.Default
    }

    fun toAudioRecording() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.RecordAudio
    }

    fun toInitialAttachmentOptionsMenu() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.Default
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
    }

    fun toRichTextEditing() {
        additionalOptionState = AdditionalOptionMenuState.RichTextEditing
    }

    fun toAttachmentAndAdditionalOptionsMenu() {
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
        unselectAdditionalOptionsMenu()
    }

    companion object {
        fun saver(): Saver<AdditionalOptionStateHolder, *> = Saver(
            save = {
                listOf(
                    it.selectedOption,
                    it.additionalOptionsSubMenuState,
                    it.additionalOptionState
                )
            },
            restore = {
                AdditionalOptionStateHolder().apply {
                    selectedOption = it[0] as AdditionalOptionSelectItem
                    additionalOptionsSubMenuState = it[1] as AdditionalOptionSubMenuState
                    additionalOptionState = it[2] as AdditionalOptionMenuState
                }
            }
        )
    }
}
