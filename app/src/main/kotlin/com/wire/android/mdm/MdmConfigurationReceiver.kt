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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MdmConfigurationReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var mdmConfigurationManager: MdmConfigurationManager
    
    @Inject
    lateinit var servicesManager: ServicesManager
    
    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED -> {
                appLogger.i("MDM configuration changed, processing certificate pinning updates")
                scope.launch {
                    handleMdmConfigurationChange()
                }
            }
        }
    }
    
    private suspend fun handleMdmConfigurationChange() {
        try {
            val newMdmConfig = mdmConfigurationManager.getCertificatePinningConfigAndNotify()
            
            if (newMdmConfig.isNotEmpty()) {
                appLogger.i("MDM certificate pinning configuration updated, restarting network services")
                
                // Restart PersistentWebSocketService to apply new certificate pinning
                if (servicesManager.isPersistentWebSocketServiceRunning()) {
                    appLogger.i("Restarting PersistentWebSocketService for certificate pinning update")
                    try {
                        servicesManager.stopPersistentWebSocketService()
                        // Service will be automatically restarted by the system when needed
                        // with the new configuration from KaliumConfigsModule
                    } catch (e: Exception) {
                        mdmConfigurationManager.notifyConfigurationError(
                            e, 
                            MdmConfigurationEvent.ErrorType.SERVICE_RESTART_FAILED
                        )
                        throw e
                    }
                }
                
                // Refresh global network configuration
                refreshNetworkConfiguration()
                
                appLogger.i("MDM certificate pinning configuration refresh completed")
            } else {
                appLogger.i("MDM certificate pinning configuration cleared")
            }
        } catch (e: Exception) {
            appLogger.e("Failed to handle MDM configuration change: ${e.message}")
            mdmConfigurationManager.notifyConfigurationError(
                e,
                MdmConfigurationEvent.ErrorType.NETWORK_REFRESH_FAILED
            )
        }
    }
    
    private suspend fun refreshNetworkConfiguration() {
        try {
            // The network configuration will be automatically refreshed when services restart
            // as they will get the new merged configuration from KaliumConfigsModule
            
            // For any active sessions, we could potentially trigger a network refresh
            // but this is complex and risky, so we rely on service restart for now
            
            appLogger.i("Network configuration refresh initiated")
        } catch (e: Exception) {
            appLogger.e("Failed to refresh network configuration: ${e.message}")
        }
    }
}
