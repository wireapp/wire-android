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

import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class FakeAiModelStorage(
    tempDir: Path,
    private val descriptor: AiModelDescriptor
) : AiModelStorage {
    private val modelDirectory = tempDir.resolve(descriptor.localDirectoryName).toFile()
    val modelFile = File(modelDirectory, descriptor.localFileName)
    val tempModelFile = File(modelDirectory, "${descriptor.localFileName}.download")

    init {
        ensureModelDirectoryExists(descriptor)
    }

    override fun getModelFile(descriptor: AiModelDescriptor): File {
        check(descriptor == this.descriptor)
        return modelFile
    }

    override fun getTempModelFile(descriptor: AiModelDescriptor): File {
        check(descriptor == this.descriptor)
        return tempModelFile
    }

    override fun ensureModelDirectoryExists(descriptor: AiModelDescriptor) {
        check(descriptor == this.descriptor)
        modelDirectory.mkdirs()
    }

    override fun promoteTempFile(descriptor: AiModelDescriptor) {
        check(descriptor == this.descriptor)
        Files.move(
            tempModelFile.toPath(),
            modelFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}
