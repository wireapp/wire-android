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
package com.wire.android.ui.debug.securityproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.Provider

class SecurityProvidersViewModel @Inject constructor(
    private val appPathsProvider: AppPathsProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityProvidersViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { current -> current.copy(appPaths = appPathsProvider()) }
        }
    }
}

/**
 * `Provider.getVersionStr()` needs API 28 and `Provider.getVersion()` is deprecated, so read the version
 * straight out of the provider's own property map, where it is registered under this key.
 */
private const val PROVIDER_VERSION_PROPERTY = "Provider.id version"

private fun Provider.versionString(): String = getProperty(PROVIDER_VERSION_PROPERTY).orEmpty()

data class SecurityProvidersViewState(
    val appPaths: List<AppPathEntry> = emptyList(),
    val providers: List<SecurityProvider>? = null,
)
