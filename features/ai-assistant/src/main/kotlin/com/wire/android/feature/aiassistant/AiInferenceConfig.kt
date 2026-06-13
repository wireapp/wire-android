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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.aiassistant

import kotlinx.coroutines.flow.Flow

enum class AiInferenceBackend {
    CPU,
    GPU
}

data class AiInferenceConfig(
    val backend: AiInferenceBackend = AiInferenceBackend.CPU,
    val cpuThreads: Int? = null
) {
    init {
        require(cpuThreads == null || cpuThreads in CPU_THREADS_RANGE) {
            "CPU threads must be null or in $CPU_THREADS_RANGE"
        }
    }

    companion object {
        val DEFAULT = AiInferenceConfig()
        val CPU_THREADS_RANGE = 1..8
    }
}

interface AiInferenceConfigStore {
    fun observeConfig(): Flow<AiInferenceConfig>
    suspend fun setConfig(config: AiInferenceConfig)
}
