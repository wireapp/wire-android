# Use Local Gecko Embeddings for Semantic Message Search

## Status

Accepted

## Context

Semantic message indexing in Kalium is built around the `TextEmbeddingModel` boundary. The
Android app currently binds `CreateEmbeddingsForExistingMessagesUseCase` to
`DeterministicLocalTextEmbeddingModel`, which is useful for prototype tests but does not produce
real semantic vectors.

The AI assistant debug flow already downloads the Gecko 110M English embedding artifacts from
Hugging Face: `Gecko_256_quant.tflite` and `sentencepiece.model`. Google documents Gecko as an
on-device embedder for Android through the AI Edge local RAG SDK, using both the `.tflite` model
and the SentencePiece tokenizer.

## Decision

Add `com.google.ai.edge.localagents:localagents-rag` as a dependency of the
`features:ai-assistant` module and use its `GeckoEmbeddingModel` to implement Kalium's
`TextEmbeddingModel` interface on Android.

The app-provided model uses the downloaded Gecko bundle from private app storage, reports model id
`gecko-110m-en-256-quant-v1`, dimension `768`, and max token count `256`, and defaults to CPU
inference until device profiling justifies enabling GPU.

Because the implementation lives in `features:ai-assistant` and implements a Kalium interface, the
module now depends directly on `com.wire.kalium:kalium-logic`. The account-scoped message binding
injects this app-scoped embedding model instead of the deterministic debug model.

## Consequences

**Easier:**
- Existing semantic indexing now creates real Gecko embeddings without changing Kalium's indexing
  or search APIs.
- The model and tokenizer use the same downloaded artifact bundle that the debug screen already
  manages.
- The wrapper remains unit-testable because Gecko construction is hidden behind a small factory
  seam.

**More difficult / trade-offs:**
- `features:ai-assistant` has a new dependency on Kalium logic and Google AI Edge RAG.
- APK size increases because the RAG SDK ships native libraries for Gecko embedding and vector
  store support.
- Google AI Edge RAG is an early SDK; future updates may require adapting the wrapper.
