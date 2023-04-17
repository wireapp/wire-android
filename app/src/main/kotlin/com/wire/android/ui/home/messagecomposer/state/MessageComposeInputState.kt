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

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Stable
sealed class MessageComposeInputState {
    abstract val messageText: TextFieldValue
    abstract val inputFocused: Boolean

    @Stable
    data class Inactive(
        override val messageText: TextFieldValue = TextFieldValue(""),
        override val inputFocused: Boolean = false
    ) : MessageComposeInputState()

    @Stable
    data class Active(
        override val messageText: TextFieldValue = TextFieldValue(""),
        override val inputFocused: Boolean = false,
        val type: MessageComposeInputType = MessageComposeInputType.NewMessage(),
        val size: MessageComposeInputSize = MessageComposeInputSize.COLLAPSED,
    ) : MessageComposeInputState()

    fun toActive(messageText: TextFieldValue = this.messageText, inputFocused: Boolean = this.inputFocused) = when (this) {
        is Active -> Active(messageText, inputFocused, this.type, this.size)
        is Inactive -> Active(messageText, inputFocused)
    }

    fun toInactive(messageText: TextFieldValue = this.messageText, inputFocused: Boolean = this.inputFocused) =
        Inactive(messageText, inputFocused)

    fun copyCurrent(messageText: TextFieldValue = this.messageText, inputFocused: Boolean = this.inputFocused) = when (this) {
        is Active -> Active(messageText, inputFocused, this.type, this.size)
        is Inactive -> Inactive(messageText, inputFocused)
    }

    val isExpanded: Boolean
        get() = this is Active && this.size == MessageComposeInputSize.EXPANDED
    val attachmentOptionsDisplayed: Boolean
        get() = this is Active && this.type is MessageComposeInputType.NewMessage && this.type.attachmentOptionsDisplayed

    val isEditMessage: Boolean
        get() = this is Active && this.type is MessageComposeInputType.EditMessage
    val editSaveButtonEnabled: Boolean
        get() = this is Active && this.type is MessageComposeInputType.EditMessage && messageText.text.trim().isNotBlank()
                && messageText.text != this.type.originalText
    val sendButtonEnabled: Boolean
        get() = this is Active && this.type is MessageComposeInputType.NewMessage && messageText.text.trim().isNotBlank()

    val sendEphemeralMessageButtonEnabled: Boolean
        get() = this is Active && this.type is MessageComposeInputType.SelfDeletingMessage && messageText.text.trim().isNotBlank()

    val isEphemeral: Boolean
        get() = this is Active && this.type is MessageComposeInputType.SelfDeletingMessage
}

enum class MessageComposeInputSize {
    COLLAPSED, // wrap content
    EXPANDED; // fullscreen
}

@Stable
sealed class MessageComposeInputType {

    @Stable
    data class NewMessage(
        val attachmentOptionsDisplayed: Boolean = false,
    ) : MessageComposeInputType()

    @Stable
    data class EditMessage(
        val messageId: String,
        val originalText: String,
    ) : MessageComposeInputType()

    @Stable
    data class SelfDeletingMessage(
        val selfDeletionDuration: SelfDeletionDuration
    ) : MessageComposeInputType()
}

enum class SelfDeletionDuration(val value: Duration?, val label: String) {
    None(null, "Off"),
    TenSeconds(10.seconds, "10 seconds"),
    FiveMinutes(5.minutes, "5 minutes"),
    OneHour(1.hours, "1 hour"),
    OneDay(1.days, "1 day"),
    OneWeek(7.days, "1 week"),
    FourWeeks(28.days, "4 weeks")
}
