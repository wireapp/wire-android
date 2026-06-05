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
package com.wire.android.feature.aiassistant.download

import com.wire.android.feature.ai_assistant.BuildConfig
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import dev.zacsweers.metro.Inject

data class HuggingFaceDownloadAuthorization(
    val token: String,
    val downloadUrl: String
)

interface HuggingFaceTokenProvider {
    suspend fun getDownloadAuthorization(descriptor: AiModelDescriptor): HuggingFaceDownloadAuthorization?
}

class BuildConfigHuggingFaceTokenProvider(
    private val token: String?,
    private val baseUrl: String
) : HuggingFaceTokenProvider {

    @Inject
    constructor() : this(
        token = BuildConfig.HUGGING_FACE_TOKEN,
        baseUrl = BuildConfig.HUGGING_FACE_BASE_URL
    )

    override suspend fun getDownloadAuthorization(descriptor: AiModelDescriptor): HuggingFaceDownloadAuthorization? {
        val nonBlankToken = token?.takeIf { it.isNotBlank() } ?: return null
        return HuggingFaceDownloadAuthorization(
            token = nonBlankToken,
            downloadUrl = descriptor.huggingFaceDownloadUrl()
        )
    }

    private fun AiModelDescriptor.huggingFaceDownloadUrl(): String =
        "${baseUrl.trimEnd('/')}/$repositoryId/resolve/$revision/$artifactPath"
}
