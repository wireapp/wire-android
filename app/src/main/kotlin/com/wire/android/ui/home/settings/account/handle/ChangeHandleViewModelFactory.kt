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
package com.wire.android.ui.home.settings.account.handle

import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dev.zacsweers.metro.Inject

@Inject
class ChangeHandleViewModelFactory(
    private val updateHandle: SetUserHandleUseCase,
    private val validateHandle: ValidateUserHandleUseCase,
    private val getSelf: GetSelfUserUseCase,
) {
    fun create(): ChangeHandleViewModel = ChangeHandleViewModel(
        updateHandle = updateHandle,
        validateHandle = validateHandle,
        getSelf = getSelf,
    )
}
