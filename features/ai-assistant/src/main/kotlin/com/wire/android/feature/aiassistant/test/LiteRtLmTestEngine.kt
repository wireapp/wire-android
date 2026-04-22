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
package com.wire.android.feature.aiassistant.test

import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiteRtLmTestEngine @Inject constructor(
    private val descriptor: AiModelDescriptor,
    private val inferenceFactory: LiteRtLmInferenceFactory
) : AiModelTestEngine {
    override suspend fun runHealthCheck(modelPath: String): AiModelHealthCheckResult =
        withContext(Dispatchers.IO) {
            if (!File(modelPath).exists()) {
                return@withContext AiModelHealthCheckResult.MissingModel
            }
            if (!descriptor.isSupportedByLiteRtLm()) {
                return@withContext AiModelHealthCheckResult.UnsupportedModel
            }

            runCatching {
                inferenceFactory.create(modelPath).use { inference ->
                    inference.generateResponse(HEALTH_CHECK_PROMPT)
                }
            }.fold(
                onSuccess = { response ->
                    if (response.isBlank()) {
                        AiModelHealthCheckResult.EmptyResponse
                    } else {
                        AiModelHealthCheckResult.Healthy
                    }
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) throw throwable
                    AiModelHealthCheckResult.InferenceFailed(throwable.message ?: throwable::class.java.simpleName)
                }
            )
        }

    private companion object {
        const val HEALTH_CHECK_PROMPT = "Reply with OK if you can read this."

        private fun AiModelDescriptor.isSupportedByLiteRtLm(): Boolean =
            localFileName.endsWith(".litertlm", ignoreCase = true)
    }
}
