/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.publiclink.settings.expiration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.datetime.TimePickerResult
import com.wire.android.ui.common.datetime.asTimePickerResult
import com.wire.android.util.uiLinkExpirationDate
import com.wire.android.util.uiLinkExpirationTime
import com.wire.kalium.cells.domain.usecase.publiclink.SetPublicLinkExpirationUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
internal class PublicLinkExpirationScreenViewModel @Inject constructor(
    val setExpiration: SetPublicLinkExpirationUseCase,
    val savedStateHandle: SavedStateHandle,
) : ActionsViewModel<PublicLinkExpirationScreenAction>() {

    private val navArgs: PublicLinkExpirationScreenNavArgs = savedStateHandle.navArgs()

    var isExpirationSet: Boolean = navArgs.expiresAt != null
        private set

    private var selectedDate: Long? = navArgs.expiresAt
    private var selectedTime: TimePickerResult? = selectedDate?.asTimePickerResult()

    private val _state = MutableStateFlow(
        PublicLinkExpirationScreenViewState(
            isEnabled = isExpirationSet,
            isValidExpirationDate = isValidExpirationDate(),
            date = selectedDate?.uiLinkExpirationDate(),
            time = selectedDate?.uiLinkExpirationTime(),
        )
    )

    val state = _state.asStateFlow()

    fun onEnableClick() {
        val enabled = !_state.value.isEnabled
        updateState { copy(isEnabled = enabled) }

        if (!enabled && isExpirationSet) {
            removeExpiration()
        }
    }

    fun onDateSelected(selectedDate: Long) {
        this.selectedDate = selectedDate
        val isValid = isValidExpirationDate()
        updateState {
            copy(
                date = selectedDate.uiLinkExpirationDate(),
                isSetButtonEnabled = isValid && selectedTime != null,
                isValidExpirationDate = isValid
            )
        }
    }

    fun onTimeSelected(result: TimePickerResult) {
        this.selectedTime = result
        val isValid = isValidExpirationDate()
        updateState {
            copy(
                time = result.asTimestamp().uiLinkExpirationTime(),
                isSetButtonEnabled = isValid && selectedDate != null,
                isValidExpirationDate = isValid
            )
        }
    }

    private fun isValidExpirationDate(): Boolean {
        val expiresAt = getExpirationTimestamp() ?: return true
        return expiresAt > System.currentTimeMillis()
    }

    fun onDateClick() {
        sendAction(ShowDatePicker(selectedDate))
    }

    fun onTimeClick() {
        sendAction(ShowTimePicker(selectedTime))
    }

    fun setExpiration() = viewModelScope.launch {
        updateState { copy(showProgress = true) }
        getExpirationTimestamp()?.let { expiresAt ->
            setExpiration(navArgs.linkUuid, expiresAt)
                .onSuccess {
                    isExpirationSet = true
                    sendAction(
                        CloseScreen(
                            PublicLinkExpirationResult.Enabled(expiresAt)
                        )
                    )
                }
                .onFailure {
                    updateState { copy(showProgress = false) }
                    sendAction(ShowError(ExpirationError.SetFailure))
                }
        }
    }

    fun removeExpiration() = viewModelScope.launch {
        updateState { copy(showProgress = true) }
        setExpiration(navArgs.linkUuid, null)
            .onSuccess {
                isExpirationSet = false
                selectedDate = null
                selectedTime = null
                updateState {
                    copy(
                        isEnabled = false,
                        showProgress = false,
                    )
                }
            }
            .onFailure {
                updateState {
                    copy(
                        isEnabled = true,
                        showProgress = false,
                    )
                }
                sendAction(ShowError(ExpirationError.RemoveFailure))
            }
    }

    fun getResult(): PublicLinkExpirationResult {
        return if (isExpirationSet) {
            PublicLinkExpirationResult.Enabled(navArgs.expiresAt ?: 0)
        } else {
            PublicLinkExpirationResult.Disabled()
        }
    }

    @Suppress("ReturnCount")
    private fun getExpirationTimestamp(): Long? {
        val localDate = selectedDate?.let {
            val instant = Instant.fromEpochMilliseconds(it)
            instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        } ?: return null

        val localTime = selectedTime?.let {
            LocalTime(it.hour, it.minute, 0, 0)
        } ?: return null

        return LocalDateTime(localDate, localTime).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    private fun TimePickerResult.asTimestamp(): Long {
        val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val localTime = LocalTime(hour, minute, 0, 0)
        return LocalDateTime(localDate, localTime).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    fun retryError(error: ExpirationError) {
        when (error) {
            ExpirationError.RemoveFailure -> removeExpiration()
            ExpirationError.SetFailure -> setExpiration()
        }
    }

    private fun updateState(block: PublicLinkExpirationScreenViewState.() -> PublicLinkExpirationScreenViewState) {
        _state.update(block)
    }
}

internal sealed class ExpirationError(
    val title: Int? = null,
    val message: Int? = null,
) {
    internal object SetFailure : ExpirationError(
        R.string.public_link_expiration_create_failure_dialog_title,
        R.string.public_link_common_failure_dialog_message,
    )
    internal object RemoveFailure : ExpirationError()
}

internal data class PublicLinkExpirationScreenViewState(
    val date: String? = null,
    val time: String? = null,
    val isEnabled: Boolean = false,
    val isSetButtonEnabled: Boolean = false,
    val isValidExpirationDate: Boolean = true,
    val showProgress: Boolean = false,
)

internal sealed interface PublicLinkExpirationScreenAction
internal data class ShowDatePicker(val selectedDate: Long?) : PublicLinkExpirationScreenAction
internal data class ShowTimePicker(val selectedTime: TimePickerResult?) : PublicLinkExpirationScreenAction
internal data class CloseScreen(val result: PublicLinkExpirationResult) : PublicLinkExpirationScreenAction
internal data class ShowError(val error: ExpirationError) : PublicLinkExpirationScreenAction
