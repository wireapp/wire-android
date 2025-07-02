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

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import com.wire.android.mdm.model.MdmCertificatePinningConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MdmConfigurationManager @Inject constructor(
    private val context: Context,
    private val json: Json
) {
    
    private val restrictionsManager: RestrictionsManager by lazy {
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    }
    
    private val _configurationEvents = MutableSharedFlow<MdmConfigurationEvent>()
    val configurationEvents: SharedFlow<MdmConfigurationEvent> = _configurationEvents.asSharedFlow()
    
    private var lastKnownConfig: Map<String, List<String>> = emptyMap()
    
    fun getCertificatePinningConfig(): Map<String, List<String>> {
        val restrictions = restrictionsManager.applicationRestrictions
        
        if (restrictions == null || restrictions.isEmpty) {
            return emptyMap()
        }
        
        val certPinningJson = restrictions.getString(KEY_CERTIFICATE_PINNING_CONFIG)
        
        return if (!certPinningJson.isNullOrEmpty()) {
            try {
                val config = json.decodeFromString<Map<String, List<String>>>(certPinningJson)
                config
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
    
    suspend fun getCertificatePinningConfigAndNotify(): Map<String, List<String>> {
        val currentConfig = getCertificatePinningConfig()
        
        if (currentConfig != lastKnownConfig) {
            val event = if (currentConfig.isEmpty() && lastKnownConfig.isNotEmpty()) {
                MdmConfigurationEvent.CertificatePinningCleared
            } else {
                MdmConfigurationEvent.CertificatePinningChanged(
                    newConfig = currentConfig,
                    previousConfig = lastKnownConfig
                )
            }
            
            lastKnownConfig = currentConfig
            _configurationEvents.emit(event)
        }
        
        return currentConfig
    }
    
    suspend fun notifyConfigurationError(error: Throwable, errorType: MdmConfigurationEvent.ErrorType) {
        _configurationEvents.emit(
            MdmConfigurationEvent.ConfigurationError(error, errorType)
        )
    }
    
    fun mergeCertificatePinningConfigs(
        defaultConfig: Map<String, List<String>>,
        mdmConfig: Map<String, List<String>>
    ): Map<String, List<String>> {
        val mergedConfig = defaultConfig.toMutableMap()
        
        mdmConfig.forEach { (hash, domains) ->
            val existingDomains = mergedConfig[hash] ?: emptyList()
            val mergedDomains = (existingDomains + domains).distinct()
            mergedConfig[hash] = mergedDomains
        }
        
        return mergedConfig
    }
    
    companion object {
        private const val KEY_CERTIFICATE_PINNING_CONFIG = "certificate_pinning_config"
    }
}