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

package com.wire.android.ui.home.settings.multifactorauthentication

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay

private const val DUMMY_TURN_OFF_DELAY_MILLIS = 500L

/** Local prototype validation; no password or authentication code is sent to a backend. */
class TurnOffMultiFactorAuthenticationUseCase @Inject constructor() {
    suspend operator fun invoke(password: String, code: String): Boolean {
        require(password.isNotBlank())
        delay(DUMMY_TURN_OFF_DELAY_MILLIS)
        return code == "284913"
    }
}
