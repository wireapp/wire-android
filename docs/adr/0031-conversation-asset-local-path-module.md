# ADR 0031: Move asset local-path ViewModels to the conversation feature

**Status:** Accepted

## Context

At baseline `c8d840a80` on `chore/android-modularization`, the asset local-path
ViewModels were app-owned despite being a cohesive conversation leaf. Their
runtime dependencies are already available to `:features:conversation`; however,
the assisted implementation participates in the app's Metro factory and its
scoped-preview KSP aggregate.

## Decision

Move these files while preserving their Kotlin packages and imports:

- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/ConversationAssetPathsViewModel.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/ConversationAssetPathsViewModel.kt`
- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/AssetLocalPathViewModel.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/AssetLocalPathViewModel.kt`
- `app/src/test/kotlin/com/wire/android/ui/home/conversations/messages/item/ConversationAssetPathsViewModelTest.kt`
  to `features/conversation/src/test/kotlin/com/wire/android/ui/home/conversations/messages/item/ConversationAssetPathsViewModelTest.kt`
- `app/src/test/kotlin/com/wire/android/ui/home/conversations/messages/item/AssetLocalPathViewModelTest.kt`
  to `features/conversation/src/test/kotlin/com/wire/android/ui/home/conversations/messages/item/AssetLocalPathViewModelTest.kt`

The only production-source exception to byte identity is making
`AssetLocalPathViewModelImpl` public. Its app-owned Metro factory contract
references that implementation across the module boundary. This is a visibility
change only; no behavior, package, import, or constructor change is made.

`ScopedMessageViewModelGraph` receives a `PreviewProvider` parameter in its two
private scoped helper overloads, preserving the app aggregate as the default.
Only the two asset-local-path branches explicitly use the existing feature-owned
`ConversationViewModelScopedPreviews` aggregate. Feature KSP already owns the
processor and aggregate. No Metro binding, Navigation, resource, manifest, host
configuration, Kalium, or consumer-package change is required; the one classpath
dependency required by compilation is recorded below.

The move exposes `okio.Path` through the `GetMessageAssetUseCase` call path.
Kalium Logic keeps Okio as an implementation dependency, so
`:features:conversation` declares the existing catalog alias
`implementation(libs.okio.core)`. This is the sole authorized compile-blocker
exception and adds neither an app nor a feature dependency.

## Consequences

The app keeps its existing Metro bindings and factory references because all
public class names and packages remain stable. The feature aggregate contains
typing and asset-local-path previews, while normal runtime resolution still uses
the same manual Metro factory. `ConversationModuleBoundaryTest` records both
moved paths and the KSP aggregate handoff; the app source test permits the new
feature location of the scoped arguments type.

This decision does not authorize Cells, attachment model, resource, or further
conversation extraction work.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelTest \
  --tests com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModelTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :app:testDevDebugUnitTest \
  --tests com.wire.android.di.metro.ScopedMessageManualViewModelFactoryTest \
  --tests com.wire.android.ui.EntryOwnedViewModelGatewaySourceTest \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
rg -n 'AssetLocalPathViewModelPreview' \
  features/conversation/build/generated/ksp/debug/kotlin/com/wire/android/di/ConversationViewModelScopedPreviews.kt
```

Stop rather than widening the slice if the feature KSP output does not expose
the assisted factory or asset preview to app, if Metro runtime resolution changes,
or if any extra app/feature edge is required.
