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

package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ObserveSelfUserUseCaseProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val userId: UserId
) {
    val observeSelfUser: ObserveSelfUserUseCase
        get() = coreLogic.getSessionScope(userId).users.observeSelfUser

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): ObserveSelfUserUseCaseProvider
    }
}
