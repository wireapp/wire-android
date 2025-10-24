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

package com.wire.android.ui.authentication

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class LoginViewModelTest {

    @MockK
    private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var userDataStoreProvider: UserDataStoreProvider

    @MockK
    private lateinit var coreLogic: CoreLogic

    private lateinit var loginViewModel: LoginViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns QualifiedID("", "")
        every { savedStateHandle.navArgs<LoginNavArgs>() } returns LoginNavArgs(
            loginPasswordPath = LoginPasswordPath(ServerConfig.STAGING)
        )
        loginViewModel = LoginViewModel(
            savedStateHandle,
            clientScopeProviderFactory,
            userDataStoreProvider,
            coreLogic,
            ServerConfig.STAGING
        )
    }
}
