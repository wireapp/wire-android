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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.content.external

import com.wire.content.asset.PreparedAsset
import kotlin.jvm.JvmInline

/**
 * Opaque handle owned by the platform edge. Common code can pass the token back to a platform
 * capability, but must not interpret it as a URI, URL, authority, or file-system path.
 */
@JvmInline
value class ExternalContentReference(val token: String) {
    init {
        require(token.isNotBlank()) { "An external content reference cannot be blank" }
    }
}

data class ExternalContentImportRequest(
    val reference: ExternalContentReference,
    val saveToDeviceIfInvalid: Boolean = false,
    val mimeType: String? = null,
    val audioWavesMask: List<Int>? = null,
)

sealed interface ExternalContentImportResult {
    data class Success(val asset: PreparedAsset) : ExternalContentImportResult
    data class TooLarge(
        val asset: PreparedAsset,
        val maximumSizeBytes: Long,
    ) : ExternalContentImportResult

    data object Cancelled : ExternalContentImportResult
    data object Unsupported : ExternalContentImportResult
    data object Failure : ExternalContentImportResult
}

sealed interface PlatformResult<out T> {
    data class Success<T>(val value: T) : PlatformResult<T>
    data object Cancelled : PlatformResult<Nothing>
    data object Unsupported : PlatformResult<Nothing>
    data class Failure(val reason: String? = null) : PlatformResult<Nothing>
}
