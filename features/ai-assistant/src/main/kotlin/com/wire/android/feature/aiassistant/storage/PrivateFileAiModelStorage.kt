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
package com.wire.android.feature.aiassistant.storage

import android.content.Context
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

class PrivateFileAiModelStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AiModelStorage {
    override fun getModelFile(descriptor: AiModelDescriptor): File =
        File(getModelDirectory(descriptor), descriptor.localFileName)

    override fun getTempModelFile(descriptor: AiModelDescriptor): File =
        File(getModelDirectory(descriptor), "${descriptor.localFileName}$TEMP_FILE_SUFFIX")

    override fun ensureModelDirectoryExists(descriptor: AiModelDescriptor) {
        getModelDirectory(descriptor).mkdirs()
    }

    override fun promoteTempFile(descriptor: AiModelDescriptor) {
        Files.move(
            getTempModelFile(descriptor).toPath(),
            getModelFile(descriptor).toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    private fun getModelDirectory(descriptor: AiModelDescriptor): File =
        File(File(context.filesDir, MODELS_DIRECTORY), descriptor.localDirectoryName)

    private companion object {
        const val MODELS_DIRECTORY = "ai-assistant-models"
        const val TEMP_FILE_SUFFIX = ".download"
    }
}
