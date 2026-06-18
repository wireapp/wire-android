/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.aiassistant.model

sealed interface AiModelSource {
    val id: String
    val displayName: String
    val promptCapability: AiPromptCapability

    data class OnDevice(val descriptor: AiModelDescriptor) : AiModelSource {
        override val id: String = descriptor.repositoryId
        override val displayName: String = descriptor.displayName
        override val promptCapability: AiPromptCapability = descriptor.promptCapability
    }

    data object WireLlm : AiModelSource {
        override val id: String = ID
        override val displayName: String = "Wire LLM"
        override val promptCapability: AiPromptCapability = AiPromptCapability.Capable

        const val ID = "wire/llm-http"
    }
}

sealed interface AiInferenceTarget {
    data class OnDevice(val modelPath: String) : AiInferenceTarget
    data class WireLlm(val serverIp: String) : AiInferenceTarget
}
