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
package com.wire.android.vectorsearch

import android.content.Context
import com.wire.android.di.ApplicationContext
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.MessageEmbeddingSearchResult
import com.wire.kalium.logic.feature.message.MessageEmbeddingVectorChunk
import com.wire.kalium.logic.feature.message.MessageEmbeddingVectorIndex
import dev.zacsweers.metro.Inject
import java.io.File

class ObjectBoxMessageEmbeddingVectorIndexFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(userId: UserId): MessageEmbeddingVectorIndex {
        val safeAccountId = userId.toString().replace(unsafePathCharsRegex, "_")
        val directory = File(context.filesDir, "message-vector-index/$safeAccountId")
        val store = ObjectBoxMessageEmbeddingVectorStore.create(context, directory)
        return ObjectBoxMessageEmbeddingVectorIndex(store)
    }

    private companion object {
        val unsafePathCharsRegex = Regex("[^A-Za-z0-9._-]")
    }
}

internal class ObjectBoxMessageEmbeddingVectorIndex(
    private val store: ObjectBoxMessageEmbeddingVectorStore
) : MessageEmbeddingVectorIndex {

    override suspend fun replaceMessageEmbeddings(
        messageId: String,
        conversationId: ConversationId,
        embeddingModel: String,
        chunks: List<MessageEmbeddingVectorChunk>
    ): Either<StorageFailure, Unit> = wrapObjectBoxRequest {
        store.replace(messageId, conversationId.value, conversationId.domain, embeddingModel, chunks)
    }

    override suspend fun clearModelIndex(embeddingModel: String): Either<StorageFailure, Unit> = wrapObjectBoxRequest {
        store.clearModel(embeddingModel)
    }

    override suspend fun searchConversation(
        conversationId: ConversationId,
        embeddingModel: String,
        queryVector: FloatArray,
        limit: Int
    ): Either<StorageFailure, List<MessageEmbeddingSearchResult>> = wrapObjectBoxRequest {
        search(
            embeddingModel = embeddingModel,
            queryVector = queryVector,
            limit = limit,
            conversationId = conversationId
        )
    }

    override suspend fun searchGlobal(
        embeddingModel: String,
        queryVector: FloatArray,
        limit: Int
    ): Either<StorageFailure, List<MessageEmbeddingSearchResult>> = wrapObjectBoxRequest {
        search(
            embeddingModel = embeddingModel,
            queryVector = queryVector,
            limit = limit,
            conversationId = null
        )
    }

    private fun search(
        embeddingModel: String,
        queryVector: FloatArray,
        limit: Int,
        conversationId: ConversationId?
    ): List<MessageEmbeddingSearchResult> {
        return store.search(
            embeddingModel,
            queryVector,
            limit,
            conversationId?.value,
            conversationId?.domain
        )
            .asSequence()
            .distinct()
            .take(limit)
            .toList()
    }

    private inline fun <T> wrapObjectBoxRequest(block: () -> T): Either<StorageFailure, T> =
        runCatching(block)
            .fold(
                onSuccess = { it.right() },
                onFailure = { StorageFailure.Generic(it).left() }
            )

    private companion object {
        const val CHUNK_OVERFETCH_FACTOR = 4
    }
}
