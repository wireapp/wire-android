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
package com.wire.android.ui.authentication.create.overview

import androidx.lifecycle.ViewModel
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = CreateAccountOverviewViewModel.Factory::class)
class CreateAccountOverviewViewModel @AssistedInject constructor(
    @Assisted val navArgs: CreateAccountOverviewNavArgs,
    defaultServerConfig: ServerConfig.Links
) : ViewModel() {
    val serverConfig: ServerConfig.Links = navArgs.customServerConfig ?: defaultServerConfig
    fun learnMoreUrl(): String = serverConfig.pricing

    @AssistedFactory
    interface Factory {
        fun create(args: CreateAccountOverviewNavArgs): CreateAccountOverviewViewModel
    }
}
