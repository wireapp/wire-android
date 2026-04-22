# Use LiteRT-LM for Gemma-3n Model Inference

## Status

Accepted

## Context

The AI assistant feature uses `com.google.mediapipe:tasks-genai` (`LlmInference`) to run on-device
language model inference for the health-check and (future) chat flows.

The default model descriptor targets **Gemma 3n E2B IT** in the `.litertlm` format
(`google/gemma-3n-E2B-it-litert-lm`). When this model is loaded through MediaPipe's
`LlmInferenceEngine_CreateEngine`, the process terminates with:

```
INVALID_ARGUMENT: Unknown model type: tf_lite_audio_adapter
```

The Gemma-3n `.litertlm` bundle embeds an audio adapter sub-model whose type identifier
(`tf_lite_audio_adapter`) is not recognized by the version of MediaPipe currently available on
Maven Central (`0.10.27`, the latest). Gemma-3n support in MediaPipe is incomplete on Android —
`LlmInferenceEngine_Session_AddAudio()` returns `Not implemented` and model loading crashes.

Google AI Edge Gallery runs Gemma-3n successfully because it bundles **LiteRT-LM**
(`com.google.ai.edge.litertlm:litertlm-android`), Google's dedicated successor to the MediaPipe
LLM Inference API, which understands the Gemma-3n model format.

## Decision

Add `com.google.ai.edge.litertlm:litertlm-android` as a dependency of the `features:ai-assistant`
module. Implement a `LiteRtLmTestEngine` (analogous to the existing `MediaPipeTestEngine`) that
runs the model health-check via LiteRT-LM. Bind `LiteRtLmTestEngine` as the `AiModelTestEngine`
in the Hilt DI module.

`MediaPipeTestEngine` and its `MediaPipeLlmInferenceFactory` abstraction are retained for
potential future use with non-Gemma-3n models that MediaPipe supports; the existing guard
(`isSupportedByMediaPipeLlmInference`) continues to prevent MediaPipe from being called with
Gemma-3n model files.

## Consequences

**Easier:**
- Gemma-3n E2B IT model health-check works on supported devices (Pixel 8a and others with
  sufficient RAM).
- The inference backend is hidden behind `LiteRtLmInference`/`LiteRtLmInferenceFactory`
  interfaces, keeping `LiteRtLmTestEngine` fully unit-testable without an Android runtime.
- LiteRT-LM is Google's recommended path for on-device LLM inference going forward, so future
  model additions are likely to be `.litertlm`-format only.

**More difficult / trade-offs:**
- A second on-device inference library is now in the dependency graph alongside MediaPipe. APK
  size will increase by the LiteRT-LM native `.so` payload.
- LiteRT-LM's Kotlin API is early-stage; if the API changes between releases a migration will be
  needed.
- Audio inference (Gemma-3n multimodal) is not yet supported by LiteRT-LM's public Kotlin API
  either; only text inference is enabled at this stage.
