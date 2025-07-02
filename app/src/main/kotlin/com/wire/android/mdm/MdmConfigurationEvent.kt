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

package com.wire.android.mdm

import com.wire.android.mdm.model.MdmServerConfig

sealed class MdmConfigurationEvent {
    data class CertificatePinningChanged(
        val newConfig: Map<String, List<String>>,
        val previousConfig: Map<String, List<String>>
    ) : MdmConfigurationEvent()
    
    data object CertificatePinningCleared : MdmConfigurationEvent()
    
    data class ServerConfigChanged(
        val newConfig: MdmServerConfig,
        val previousConfig: MdmServerConfig?
    ) : MdmConfigurationEvent()
    
    data object ServerConfigCleared : MdmConfigurationEvent()
    
    data class ConfigurationError(
        val error: Throwable,
        val errorType: ErrorType
    ) : MdmConfigurationEvent()
    
    enum class ErrorType {
        INVALID_JSON,
        NETWORK_REFRESH_FAILED,
        SERVICE_RESTART_FAILED,
        INVALID_SERVER_CONFIG
    }
}