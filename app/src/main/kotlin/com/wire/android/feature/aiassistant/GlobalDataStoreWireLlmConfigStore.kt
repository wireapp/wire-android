/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.aiassistant

import com.wire.android.datastore.GlobalDataStore
import kotlinx.coroutines.flow.Flow

class GlobalDataStoreWireLlmConfigStore(
    private val globalDataStore: GlobalDataStore
) : WireLlmConfigStore {
    override fun observeServerIp(): Flow<String?> = globalDataStore.observeWireLlmServerIp()

    override suspend fun setServerIp(serverIp: String) {
        globalDataStore.setWireLlmServerIp(serverIp)
    }
}
