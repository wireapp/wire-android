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

package com.wire.android.ui.userprofile.self.status

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.R
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfTextStatusUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class SelfUserStatusViewModel @Inject constructor(
    @CurrentAccount selfUserId: UserId,
    userDataStoreProvider: UserDataStoreProvider,
    private val observeSelfUserWithTeam: ObserveSelfUserWithTeamUseCase,
    private val updateAvailabilityStatus: UpdateSelfAvailabilityStatusUseCase,
    private val updateTextStatus: UpdateSelfTextStatusUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val dataStore = userDataStoreProvider.getOrCreate(selfUserId)
    private val _confirmationMessage = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val confirmationMessage = _confirmationMessage.asSharedFlow()

    var state by mutableStateOf(SelfUserStatusState())
        private set

    init {
        observeSelfUser()
    }

    fun onEmojiSelected(emoji: String) {
        state = state.copy(emoji = emoji)
    }

    fun dismissStatusDialog() {
        state = state.copy(statusDialogData = null)
    }

    fun dialogCheckBoxStateChanged(isChecked: Boolean) {
        state = state.copy(statusDialogData = state.statusDialogData?.changeCheckBoxState(isChecked))
    }

    fun changeAvailabilityStatusClick(status: UserAvailabilityStatus) {
        if (state.availabilityStatus == status) return
        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                state = state.copy(statusDialogData = status.toDialogData())
            } else {
                changeAvailabilityStatus(status)
            }
        }
    }

    fun changeAvailabilityStatus(status: UserAvailabilityStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        viewModelScope.launch { updateAvailabilityStatus(status) }
        dismissStatusDialog()
    }

    fun updateCustomStatus(message: String) {
        if (!state.isTeamMember) return
        val bundledStatus = buildTextStatus(state.emoji, message)
        viewModelScope.launch {
            state = state.copy(isSaving = true)
            updateTextStatus(bundledStatus).fold(
                fnL = { },
                fnR = { _confirmationMessage.tryEmit(R.string.user_profile_status_updated) }
            )
            state = state.copy(isSaving = false)
        }
    }

    fun clearCustomStatus() {
        if (!state.isTeamMember) return
        state = state.copy(emoji = null, message = "")
        viewModelScope.launch {
            state = state.copy(isSaving = true)
            updateTextStatus(" ").fold(
                fnL = { },
                fnR = { _confirmationMessage.tryEmit(R.string.user_profile_status_updated) }
            )
            state = state.copy(isSaving = false)
        }
    }

    private fun observeSelfUser() {
        viewModelScope.launch {
            observeSelfUserWithTeam()
                .flowOn(dispatchers.io())
                .collect { (selfUser, team) ->
                    val parsedStatus = parseTextStatus(selfUser.textStatus)
                    state = state.copy(
                        availabilityStatus = selfUser.availabilityStatus,
                        emoji = parsedStatus.emoji,
                        message = parsedStatus.message,
                        isTeamMember = team != null,
                    )
                }
        }
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserAvailabilityStatus) {
        if (state.statusDialogData?.isCheckBoxChecked == true) {
            viewModelScope.launch { dataStore.dontShowStatusRationaleAgain(status) }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserAvailabilityStatus): Boolean =
        dataStore.shouldShowStatusRationaleFlow(status).first()

    private fun UserAvailabilityStatus.toDialogData(): StatusDialogData = when (this) {
        UserAvailabilityStatus.AVAILABLE -> StatusDialogData.StateAvailable()
        UserAvailabilityStatus.BUSY -> StatusDialogData.StateBusy()
        UserAvailabilityStatus.AWAY -> StatusDialogData.StateAway()
        UserAvailabilityStatus.NONE -> StatusDialogData.StateNone()
    }
}

data class ParsedTextStatus(val emoji: String?, val message: String)

fun parseTextStatus(textStatus: String?): ParsedTextStatus {
    val status = textStatus.orEmpty().trim()
    if (status.isBlank()) return ParsedTextStatus(null, "")

    val emojiEnd = emojiClusterEnd(status)
    return if (emojiEnd > 0) {
        ParsedTextStatus(
            emoji = status.substring(0, emojiEnd),
            message = status.substring(emojiEnd).trim().take(MAX_STATUS_TEXT_LENGTH)
        )
    } else {
        ParsedTextStatus(DEFAULT_STATUS_EMOJI, status.take(MAX_STATUS_TEXT_LENGTH))
    }
}

private fun emojiClusterEnd(value: String): Int {
    val firstCodePoint = value.codePointAt(0)
    if (!isEmojiLike(firstCodePoint)) return 0

    var index = Character.charCount(firstCodePoint)
    while (index < value.length) {
        val codePoint = value.codePointAt(index)
        val codePointLength = Character.charCount(codePoint)
        when {
            codePoint == VARIATION_SELECTOR || isEmojiModifier(codePoint) -> index += codePointLength
            codePoint == ZERO_WIDTH_JOINER && index + codePointLength < value.length -> {
                val nextCodePoint = value.codePointAt(index + codePointLength)
                if (!isEmojiLike(nextCodePoint)) return index
                index += codePointLength + Character.charCount(nextCodePoint)
            }
            else -> return index
        }
    }
    return index
}

private fun isEmojiLike(codePoint: Int): Boolean =
    Character.getType(codePoint) == Character.OTHER_SYMBOL.toInt() ||
            codePoint in MISC_SYMBOLS_AND_PICTOGRAPHS ||
            codePoint in EMOTICONS ||
            codePoint in TRANSPORT_AND_MAP ||
            codePoint in SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS

private fun isEmojiModifier(codePoint: Int): Boolean = codePoint in EMOJI_MODIFIERS

private const val ZERO_WIDTH_JOINER = 0x200D
private const val VARIATION_SELECTOR = 0xFE0F
private val EMOJI_MODIFIERS = 0x1F3FB..0x1F3FF
private val EMOTICONS = 0x1F600..0x1F64F
private val MISC_SYMBOLS_AND_PICTOGRAPHS = 0x1F300..0x1F5FF
private val TRANSPORT_AND_MAP = 0x1F680..0x1F6FF
private val SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS = 0x1F900..0x1F9FF
