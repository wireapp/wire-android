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
import com.wire.android.config.orDefault
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateAccountSelectorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val navArgs: CreateAccountOverviewNavArgs = savedStateHandle.navArgs()
    val serverConfig: ServerConfig.Links = navArgs.customServerConfig.orDefault()
}
