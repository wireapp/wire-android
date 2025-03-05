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
package com.wire.android.ui.authentication.login.sso

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.DefaultServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.serialization.Serializable

interface SSOUrlConfigHolder {
    fun get(): SSOUrlConfig? = SSOUrlConfig(DefaultServerConfig)
    fun set(data: SSOUrlConfig) {}
}

object SSOUrlConfigHolderPreview : SSOUrlConfigHolder

class SSOUrlConfigHolderImpl(private val savedStateHandle: SavedStateHandle) : SSOUrlConfigHolder {
    override fun get(): SSOUrlConfig? = savedStateHandle.get<Bundle>(SSO_URL_CONFIG_KEY)?.let {
        Bundlizer.unbundle(SSOUrlConfig.serializer(), it)
    }

    override fun set(data: SSOUrlConfig) =
        savedStateHandle.set(SSO_URL_CONFIG_KEY, Bundlizer.bundle(SSOUrlConfig.serializer(), data))
}

private const val SSO_URL_CONFIG_KEY: String = "sso-url-config"

@Serializable
data class SSOUrlConfig(val serverConfig: ServerConfig.Links, val ssoCode: String = "")
