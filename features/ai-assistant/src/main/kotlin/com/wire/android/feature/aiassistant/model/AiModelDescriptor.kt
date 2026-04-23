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

data class AiModelDescriptor(
    val displayName: String,
    val repositoryId: String,
    val artifactPath: String,
    val localDirectoryName: String,
    val localFileName: String,
    val promptCapability: AiPromptCapability = AiPromptCapability.Weak,
    val revision: String = DEFAULT_REVISION,
    val expectedByteSize: Long? = null,
    val sha256: String? = null
) {
    init {
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(repositoryId.isNotBlank()) { "repositoryId must not be blank" }
        require(artifactPath.isNotBlank()) { "artifactPath must not be blank" }
        require(localDirectoryName.isNotBlank()) { "localDirectoryName must not be blank" }
        require(localFileName.isNotBlank()) { "localFileName must not be blank" }
        require(revision.isNotBlank()) { "revision must not be blank" }
        require(expectedByteSize == null || expectedByteSize > 0L) { "expectedByteSize must be positive" }
        require(sha256 == null || SHA_256_REGEX.matches(sha256)) { "sha256 must be a 64-character hex string" }
    }

    companion object {
        const val DEFAULT_REVISION = "main"

        private val SHA_256_REGEX = Regex("[a-fA-F0-9]{64}")
    }
}

enum class AiPromptCapability {
    Weak,
    Capable
}
