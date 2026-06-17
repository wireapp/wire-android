# Semantic Search Prototype Plan

## Goal

Build a local semantic message search prototype using:

- Gecko text embedder
- App SQLDelight database storage
- Exact cosine similarity in Kotlin

The prototype should avoid a vector database extension initially. It should prove whether local embeddings plus exact search are good enough before introducing a dedicated ANN/vector-search dependency.

```text
query text
 -> Gecko embedding
 -> load stored message embeddings from SQLDelight
 -> exact cosine similarity in Kotlin
 -> top-k message IDs
 -> hydrate existing Message.Standalone results
```

Keep the feature behind a flag and reuse the existing Kalium message-search architecture.

## 1. Storage

Add a derived embedding table near:

`kalium/data/persistence/src/commonMain/db_user/com/wire/kalium/persistence/Messages.sq`

```sql
CREATE TABLE MessageEmbedding (
    message_id TEXT NOT NULL,
    conversation_id TEXT AS QualifiedIDEntity NOT NULL,
    embedding_model TEXT NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    embedding BLOB NOT NULL,
    source_text_hash TEXT NOT NULL,
    created_at INTEGER NOT NULL,

    FOREIGN KEY (message_id, conversation_id)
        REFERENCES Message(id, conversation_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    PRIMARY KEY (message_id, conversation_id, embedding_model)
);

CREATE INDEX message_embedding_conversation_index
ON MessageEmbedding(conversation_id, embedding_model);
```

For the prototype, store normalized `FloatArray` values as little-endian `ByteArray`.

Later optimization: switch to `Int8` quantized vectors if storage or read latency becomes a problem.

## 2. Persistence API

Add a small DAO beside:

`kalium/data/persistence/src/commonMain/kotlin/com/wire/kalium/persistence/dao/message/MessageDAO.kt`

Suggested API:

```kotlin
interface MessageEmbeddingDAO {
    suspend fun upsert(...)
    suspend fun delete(...)
    suspend fun getConversationEmbeddings(conversationId, model): List<MessageEmbeddingEntity>
    suspend fun getGlobalEmbeddings(model, limit: Int? = null): List<MessageEmbeddingEntity>
    suspend fun getMessagesNeedingEmbedding(model, limit: Int): List<MessageTextForEmbeddingEntity>
}
```

Do not expose this DAO to the UI. Embeddings are derived persistence data.

## 3. Embedder Boundary

Create a logic-level abstraction:

```kotlin
interface TextEmbeddingModel {
    val modelId: String
    val dimension: Int
    suspend fun embed(text: String): FloatArray
}
```

Android implementation wraps Gecko plus SentencePiece tokenizer.

For unit tests, use a fake deterministic embedder.

Prototype model inputs:

```text
gecko.tflite
sentencepiece.model
```

Open question for implementation: decide whether the model files are bundled app assets, downloaded model files, or debug-only manually pushed files.

## 4. Indexing

Add `MessageSemanticIndexer` in the Kalium logic/data layer:

```kotlin
interface MessageSemanticIndexer {
    suspend fun indexNextBatch(limit: Int = 100): IndexingResult
    suspend fun indexMessage(messageId: String, conversationId: ConversationId)
    suspend fun clearModelIndex(modelId: String)
}
```

Initial indexing rules:

- Index only visible text messages.
- Skip deleted messages.
- Skip expired ephemeral messages.
- Re-index when `source_text_hash` changes.
- Let database cascade deletes remove embeddings when message rows are deleted.
- Run on an IO/background dispatcher.
- Batch work and keep it cancelable.

## 5. Search

Mirror the existing text-search use cases:

- `SearchMessagesInConversationUseCase`
- `SearchMessagesGloballyUseCase`

Add prototype semantic equivalents:

```kotlin
SearchMessagesSemanticallyInConversationUseCase
SearchMessagesSemanticallyGloballyUseCase
```

Algorithm:

```text
embed query
load candidate embeddings
score = dot(query, messageEmbedding)
sort descending
take top-k
hydrate messages through existing MessageDAO / repository mapper
```

Vectors should be normalized at write time, so cosine similarity is just dot product.

For the first version, exact cosine runs in Kotlin. SQLDelight stores and filters; Kotlin ranks.

## 6. UI Integration

Prototype path:

- Add a debug/dev-only semantic-search toggle to existing conversation message search UI.
- Keep current text search as fallback.
- Reuse normal message result rows.
- Avoid new result UI unless needed.
- Add simple indexing status only in debug/dev if needed.

## 7. Tests

Minimum useful tests:

- `FloatArray <-> ByteArray` adapter round trip.
- Cosine scorer ordering.
- DAO upsert/delete/query behavior.
- Indexer skips non-text, deleted, expired, and unchanged messages.
- Semantic search returns hydrated messages in score order.
- Regression test for edited text changing `source_text_hash`.

Run changed Kalium module tests before finishing implementation.

## Recommended Prototype Scope

Start with conversation-only semantic search.

Global semantic search should follow after measuring memory and latency. Exact global search over large histories may be too slow without candidate limits, quantization, or a later vector-search backend such as `sqlite-vec` or HNSW.

## Success Criteria

The prototype is good enough if:

- It indexes 1k-10k local text messages without UI jank.
- A query returns top 20 results in about 300-700 ms on a modern Android device.
- Re-running indexing is incremental.
- Deleting or editing messages does not leave stale searchable content.
- No message text or embeddings leave the device.

