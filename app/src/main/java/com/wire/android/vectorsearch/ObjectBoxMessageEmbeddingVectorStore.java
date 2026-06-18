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
package com.wire.android.vectorsearch;

import android.content.Context;
import com.wire.kalium.logic.data.id.QualifiedID;
import com.wire.kalium.logic.feature.message.MessageEmbeddingSearchResult;
import com.wire.kalium.logic.feature.message.MessageEmbeddingVectorChunk;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.ObjectWithScore;
import io.objectbox.query.QueryCondition;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ObjectBoxMessageEmbeddingVectorStore {

    private static final String KEY_SEPARATOR = "\u001F";
    private static final Map<String, ObjectBoxMessageEmbeddingVectorStore> OPEN_STORES = new HashMap<>();

    private final BoxStore store;
    private final Box<MessageEmbeddingVectorEntity> box;

    private ObjectBoxMessageEmbeddingVectorStore(BoxStore store) {
        this.store = store;
        this.box = store.boxFor(MessageEmbeddingVectorEntity.class);
    }

    public static synchronized ObjectBoxMessageEmbeddingVectorStore create(Context context, File directory) {
        String directoryPath = directory.getAbsolutePath();
        ObjectBoxMessageEmbeddingVectorStore existingStore = OPEN_STORES.get(directoryPath);
        if (existingStore != null) {
            return existingStore;
        }

        BoxStore store = MyObjectBox.builder()
                .androidContext(context.getApplicationContext())
                .directory(directory)
                .build();
        ObjectBoxMessageEmbeddingVectorStore vectorStore = new ObjectBoxMessageEmbeddingVectorStore(store);
        OPEN_STORES.put(directoryPath, vectorStore);
        return vectorStore;
    }

    static synchronized void closeAllForTests() {
        for (ObjectBoxMessageEmbeddingVectorStore store : OPEN_STORES.values()) {
            store.store.close();
        }
        OPEN_STORES.clear();
    }

    public void replace(
            String messageId,
            String conversationIdValue,
            String conversationIdDomain,
            String embeddingModel,
            List<MessageEmbeddingVectorChunk> chunks
    ) {
        store.runInTx(() -> {
            long[] existingIds = box.query(
                    MessageEmbeddingVectorEntity_.messageId.equal(messageId)
                            .and(MessageEmbeddingVectorEntity_.conversationIdValue.equal(conversationIdValue))
                            .and(MessageEmbeddingVectorEntity_.conversationIdDomain.equal(conversationIdDomain))
                            .and(MessageEmbeddingVectorEntity_.embeddingModel.equal(embeddingModel))
            ).build().findIds();
            if (existingIds.length > 0) {
                box.remove(existingIds);
            }
            List<MessageEmbeddingVectorEntity> rows = new ArrayList<>(chunks.size());
            for (MessageEmbeddingVectorChunk chunk : chunks) {
                MessageEmbeddingVectorEntity row = new MessageEmbeddingVectorEntity();
                row.key = key(messageId, conversationIdValue, conversationIdDomain, embeddingModel, chunk.getChunkIndex());
                row.messageId = messageId;
                row.conversationIdValue = conversationIdValue;
                row.conversationIdDomain = conversationIdDomain;
                row.embeddingModel = embeddingModel;
                row.chunkIndex = chunk.getChunkIndex();
                row.chunkCount = chunk.getChunkCount();
                row.sourceTextHash = chunk.getSourceTextHash();
                row.createdAt = chunk.getCreatedAt();
                row.embedding = chunk.getVector();
                rows.add(row);
            }
            box.put(rows);
        });
    }

    public void clearModel(String embeddingModel) {
        long[] ids = box.query(MessageEmbeddingVectorEntity_.embeddingModel.equal(embeddingModel))
                .build()
                .findIds();
        if (ids.length > 0) {
            box.remove(ids);
        }
    }

    public List<MessageEmbeddingSearchResult> search(
            String embeddingModel,
            float[] queryVector,
            int limit,
            String conversationIdValue,
            String conversationIdDomain
    ) {
        int overFetchLimit = Math.max(limit, limit * 4);
        QueryCondition<MessageEmbeddingVectorEntity> condition =
                MessageEmbeddingVectorEntity_.embedding.nearestNeighbors(queryVector, overFetchLimit)
                        .and(MessageEmbeddingVectorEntity_.embeddingModel.equal(embeddingModel));
        if (conversationIdValue != null && conversationIdDomain != null) {
            condition = condition
                    .and(MessageEmbeddingVectorEntity_.conversationIdValue.equal(conversationIdValue))
                    .and(MessageEmbeddingVectorEntity_.conversationIdDomain.equal(conversationIdDomain));
        }

        List<ObjectWithScore<MessageEmbeddingVectorEntity>> rows = box.query(condition)
                .build()
                .findWithScores();
        List<MessageEmbeddingSearchResult> results = new ArrayList<>(Math.min(rows.size(), limit));
        Set<String> seen = new HashSet<>();
        for (ObjectWithScore<MessageEmbeddingVectorEntity> rowWithScore : rows) {
            MessageEmbeddingVectorEntity row = rowWithScore.get();
            String resultKey = row.conversationIdDomain + KEY_SEPARATOR + row.conversationIdValue + KEY_SEPARATOR + row.messageId;
            if (seen.add(resultKey)) {
                results.add(new MessageEmbeddingSearchResult(
                        new QualifiedID(row.conversationIdValue, row.conversationIdDomain),
                        row.messageId
                ));
                if (results.size() == limit) {
                    break;
                }
            }
        }
        return results;
    }

    private static String key(
            String messageId,
            String conversationIdValue,
            String conversationIdDomain,
            String embeddingModel,
            int chunkIndex
    ) {
        return conversationIdDomain + KEY_SEPARATOR
                + conversationIdValue + KEY_SEPARATOR
                + messageId + KEY_SEPARATOR
                + embeddingModel + KEY_SEPARATOR
                + chunkIndex;
    }
}
