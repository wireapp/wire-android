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
package com.wire.android.ui.home.newconversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.inject.Inject

@HiltViewModel
class SendConnectionRequestViewModel @Inject constructor(
    private val sendConnectionRequest: SendConnectionRequestUseCase,
) : ViewModel() {

    fun addContact(userId: UserId): Deferred<Boolean> =
        viewModelScope.async {
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Success -> {
                    true
                }

                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user ${userId.value.obfuscateId()}"))
                    false
                }
            }
        }
}
