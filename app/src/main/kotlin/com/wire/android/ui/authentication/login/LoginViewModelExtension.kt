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
package com.wire.android.ui.authentication.login

import com.wire.android.BuildConfig
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.kalium.logic.data.client.ClientCapability
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import kotlinx.coroutines.flow.first

class LoginViewModelExtension(
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val userDataStoreProvider: UserDataStoreProvider,
) {

    suspend fun registerClient(
        userId: UserId,
        password: String?,
        secondFactorVerificationCode: String? = null,
        capabilities: List<ClientCapability>? = null,
    ): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        return clientScope.getOrRegister(
            RegisterClientParam(
                password = password,
                capabilities = capabilities,
                secondFactorVerificationCode = secondFactorVerificationCode,
                modelPostfix = if (BuildConfig.PRIVATE_BUILD) " [${BuildConfig.FLAVOR}_${BuildConfig.BUILD_TYPE}]" else null
            )
        )
    }

    internal suspend fun isInitialSyncCompleted(userId: UserId): Boolean =
        userDataStoreProvider.getOrCreate(userId).initialSyncCompleted.first()
}
