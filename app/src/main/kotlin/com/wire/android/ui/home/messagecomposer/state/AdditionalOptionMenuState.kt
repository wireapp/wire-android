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

enum class AdditionalOptionMenuState {
    AttachmentAndAdditionalOptionsMenu,
    RichTextEditing,
    Hidden
}

enum class AdditionalOptionSubMenuState {
    Hidden,
    RecordAudio,
    AttachFile,
    AttachImage,
    Emoji,
    Gif;
}

enum class AdditionalOptionSelectItem {
    RichTextEditing,
    SelfDeleting,
    AttachFile,
    None,
}

class AdditionalOptionStateHolder {

    var selectedOption by mutableStateOf(AdditionalOptionSelectItem.None)

    var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
        AdditionalOptionSubMenuState.Hidden
    )
        private set

    var additionalOptionState: AdditionalOptionMenuState by mutableStateOf(AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu)
        private set

    fun showAdditionalOptionsMenu() {
        selectedOption = AdditionalOptionSelectItem.AttachFile
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.AttachFile
    }

    fun hideAdditionalOptionsMenu() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.Hidden
        selectedOption = AdditionalOptionSelectItem.None
    }

    fun toAudioRecording() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.RecordAudio
        additionalOptionState = AdditionalOptionMenuState.Hidden
    }

    fun toRichTextEditing() {
        additionalOptionState = AdditionalOptionMenuState.RichTextEditing
    }

    fun toAttachmentAndAdditionalOptionsMenu() {
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
    }

}

