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
package com.wire.android.feature.aiassistant.model

object DefaultAiModelDescriptor {
    val gemma3_270M_IT = AiModelDescriptor(
        displayName = "Gemma 3 270M IT",
        repositoryId = "litert-community/gemma-3-270m-it",
        artifactPath = "gemma3-270m-it-q8.litertlm",
        localDirectoryName = "gemma-3-270m-it",
        localFileName = "model.litertlm",
        promptCapability = AiPromptCapability.Weak
    )
    val gemma3_1B_IT_int4 = AiModelDescriptor(
        displayName = "Gemma 3 1B IT int4",
        repositoryId = "litert-community/Gemma3-1B-IT",
        artifactPath = "gemma3-1b-it-int4.litertlm",
        localDirectoryName = "gemma-3-1b-it",
        localFileName = "model.litertlm",
        promptCapability = AiPromptCapability.Capable
    )
    val gemma3nE2bIt = AiModelDescriptor(
        displayName = "Gemma 3n E2B IT",
        repositoryId = "google/gemma-3n-E2B-it-litert-lm",
        artifactPath = "gemma-3n-E2B-it-int4.litertlm",
        localDirectoryName = "gemma-3n-e2b-it",
        localFileName = "model.litertlm",
        promptCapability = AiPromptCapability.Capable
    )

    val allModels: List<AiModelDescriptor> = listOf(
        gemma3_270M_IT,
        gemma3_1B_IT_int4,
        gemma3nE2bIt
    )
}
