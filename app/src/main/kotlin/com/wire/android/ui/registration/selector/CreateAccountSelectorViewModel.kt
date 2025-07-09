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
package com.wire.android.ui.registration.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.config.orDefault
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountSelectorViewModel @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val navArgs: CreateAccountSelectorNavArgs = savedStateHandle.navArgs()
    val serverConfig: ServerConfig.Links = navArgs.customServerConfig.orDefault()
    val email: String = navArgs.email.orEmpty()
    val teamAccountCreationUrl = serverConfig.teams

    fun onPageLoaded() = viewModelScope.launch {
        println("ym. resetting analytics state")
        globalDataStore.setAnonymousRegistrationEnabled(false)
    }
}
