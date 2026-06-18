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

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.HnswIndex;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Unique;
import io.objectbox.annotation.VectorDistanceType;

@Entity
public class MessageEmbeddingVectorEntity {

    static final int GECKO_EMBEDDING_DIMENSION = 768;

    @Id
    public long id;
    @Unique
    public String key;
    @Index
    public String messageId;
    @Index
    public String conversationIdValue;
    @Index
    public String conversationIdDomain;
    @Index
    public String embeddingModel;
    public int chunkIndex;
    public int chunkCount;
    public String sourceTextHash;
    public long createdAt;
    @HnswIndex(dimensions = GECKO_EMBEDDING_DIMENSION, distanceType = VectorDistanceType.COSINE)
    public float[] embedding;

    public MessageEmbeddingVectorEntity() {
        key = "";
        messageId = "";
        conversationIdValue = "";
        conversationIdDomain = "";
        embeddingModel = "";
        sourceTextHash = "";
        embedding = new float[GECKO_EMBEDDING_DIMENSION];
    }
}
