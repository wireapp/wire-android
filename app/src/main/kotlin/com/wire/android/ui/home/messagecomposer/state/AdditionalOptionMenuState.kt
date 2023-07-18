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

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize

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

class AdditionalOptionStateHolder(
    ininitialSelectedOption : AdditionalOptionSelectItem= AdditionalOptionSelectItem.None,
    initialOptionsSubMenuState : AdditionalOptionSubMenuState= AdditionalOptionSubMenuState.Hidden,
    initialOptionStateHolder: AdditionalOptionMenuState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
) {

    var selectedOption by mutableStateOf(ininitialSelectedOption)

    var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
        initialOptionsSubMenuState
    )
        private set

    var additionalOptionState: AdditionalOptionMenuState by mutableStateOf(initialOptionStateHolder)
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

    fun hideAudioRecording() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.AttachFile
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
    }

    fun toRichTextEditing() {
        additionalOptionState = AdditionalOptionMenuState.RichTextEditing
    }

    fun toAttachmentAndAdditionalOptionsMenu() {
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
    }

//    companion object {
//        fun saver(): Saver<AdditionalOptionStateHolder, *> = Saver(
//            save = {
//                listOf(
//                    it.selectedOption,
//                    it.additionalOptionsSubMenuState,
//                    it.additionalOptionState
//                )
//            },
//            restore = {
//                AdditionalOptionStateHolder().apply {
//                    selectedOption = it[0] as AdditionalOptionSelectItem
//                    additionalOptionsSubMenuState = it[1] as AdditionalOptionSubMenuState
//                    additionalOptionState = it[2] as AdditionalOptionMenuState
//                }
//            }
//        )
//    }
}
