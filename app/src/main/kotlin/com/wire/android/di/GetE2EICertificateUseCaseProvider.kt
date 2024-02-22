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

import android.content.Context
import com.wire.android.feature.e2ei.GetE2EICertificateUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

// TODO remove it
class GetE2EICertificateUseCaseProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @ApplicationContext private val applicationContext: Context,
    @Assisted private val userId: UserId,
    @Assisted private val dispatcherProvider: DispatcherProvider
) {

    val useCase: GetE2EICertificateUseCase
        get() = GetE2EICertificateUseCase(
            enrollE2EI = coreLogic.getSessionScope(userId).enrollE2EI,
            applicationContext = applicationContext,
            dispatcherProvider = dispatcherProvider
        )

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId, dispatcherProvider: DispatcherProvider): GetE2EICertificateUseCaseProvider
    }
}
