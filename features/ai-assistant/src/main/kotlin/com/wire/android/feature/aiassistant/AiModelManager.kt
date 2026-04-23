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

import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AiModelManager {
    val availableModels: List<AiModelDescriptor>
    val selectedModel: StateFlow<AiModelDescriptor>
    fun selectModel(descriptor: AiModelDescriptor)
    fun observeModelStatus(): Flow<AiModelStatus>
    fun downloadModel(): Flow<AiModelDownloadState>
}
