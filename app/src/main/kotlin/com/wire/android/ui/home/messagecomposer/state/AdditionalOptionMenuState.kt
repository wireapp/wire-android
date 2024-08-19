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
    None,
    RecordAudio,
}

enum class AdditionalOptionSelectItem {
    RichTextEditing,
//    DrawingMode, TODO KBX remove

    // it's only used to show keyboard after self deleting bottom sheet collapses
    SelfDeleting,
    AttachFile,
    None,
}

class AdditionalOptionStateHolder {

    var selectedOption by mutableStateOf(AdditionalOptionSelectItem.None)

    var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
        AdditionalOptionSubMenuState.None
    )
        private set

    var additionalOptionState: AdditionalOptionMenuState by mutableStateOf(AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu)
        private set

    fun showAdditionalOptionsMenu() {
        println("KBX showAdditionalOptionsMenu")
        selectedOption = AdditionalOptionSelectItem.AttachFile
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.None // KBX Do we need this if it's only triggered by closing audio recording
    }

    fun unselectAdditionalOptionsMenu() {
        println("KBX unselectAdditionalOptionsMenu")
        selectedOption = AdditionalOptionSelectItem.None
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.None // KBX same here
    }

    fun toAudioRecording() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.RecordAudio
    }

//    fun toLocationPicker() { // TODO KBX check
//        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.Location
//        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
//    }

    fun toInitialAttachmentOptionsMenu() {
        additionalOptionsSubMenuState = AdditionalOptionSubMenuState.None
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
    }

    fun toRichTextEditing() {
        additionalOptionState = AdditionalOptionMenuState.RichTextEditing
    }

    fun toAttachmentAndAdditionalOptionsMenu() {
        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
        unselectAdditionalOptionsMenu()
    }

    fun toSelfDeletingOptionsMenu() {
        selectedOption = AdditionalOptionSelectItem.SelfDeleting
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
