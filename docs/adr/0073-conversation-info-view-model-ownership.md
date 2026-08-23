# ADR 0073: Conversation info ViewModel ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `1ff2ab4ad`

## Context

`ConversationInfoViewState` already belongs to `:features:conversation`, while
`ConversationInfoViewModel` and its focused tests remained in `:app`. The ViewModel
contained conversation presentation behavior and Kalium use-case orchestration, but
its assisted argument was the app-owned, parcelized `ConversationNavArgs` and its
empty-name fallback selected an app string resource. It was also bound through the
broad app-owned `ConversationCoreManualViewModelFactoryGroup`.

The `CurrentAccount` Metro qualifier used by the ViewModel had the stable FQN
`com.wire.android.di.CurrentAccount`, but its declaration was physically embedded in
the app `CoreLogicModule`. That made an otherwise valid feature dependency look
app-owned.

## Decision

Move `ConversationInfoViewModel`, its test, and its arrangement to
`:features:conversation` while preserving their packages and the ViewModel's public
FQN, `conversationId`, mutable Compose state, `observeConversationDetails()`, and
`mentionedUserData(String)` behavior.

The feature owns `ConversationInfoViewModelArgs`, containing only a Kalium
`ConversationId` and an injected `UIText` for the deleted-account label. The ViewModel
continues to select the shared `username_unavailable_label` directly from
`:core:ui-common`; it uses the injected text only for the deleted-account fallback.
No localized `member_name_deleted_label` resource moves because app has three other
consumers and remains its proven owner.

App keeps a thin `conversationInfoViewModel(ConversationNavArgs)` composable adapter.
It maps the existing route argument to the feature argument and injects
`UIText.StringResource(R.string.member_name_deleted_label)`. The Navigation 3 call,
route type, screen API, and result behavior remain unchanged.

The feature owns a dedicated `ConversationInfoManualViewModelFactoryGroup` and the
generated `ConversationInfoManualViewModelFactoryMetroBindings`. App removes the info
gateway from `ConversationCoreViewModelGraph` and installs the dedicated binding
exactly once in `AppSessionViewModelGraph`. `ConversationModule` does not provide or
bind this ViewModel.

Move the `CurrentAccount` qualifier declaration to `:core:di` without changing its
package, annotation retention, qualifier semantics, providers, or consumers. App and
conversation already depend on `:core:di`, so this adds no Gradle edge.

The focused tests move with the ViewModel. They use local one-to-one and group detail
builders backed by neutral `TestConversation` and `TestUser` fixtures, not app
message-composer fixtures. They assert the exact injected deleted-account `UIText`,
the exact common unavailable-resource ID, and the Proteus verification status field.

## Dependency and stop conditions

This slice adds no Gradle dependency, resource move, route change, screen API change,
or feature-to-feature edge. Stop rather than broaden the extraction if Metro requires
an app binding/provider, the feature needs an app import, the localized label must
move, Navigation 3 identity changes, or a new module edge becomes necessary.

## Consequences

Conversation info presentation behavior, arguments, tests, and Metro factory
generation now have a feature owner. App retains only route adaptation, localized
resource selection, and session composition. The broad conversation-core factory no
longer owns this ViewModel, and the qualifier's physical owner now matches its neutral
DI purpose while preserving every consumer FQN.

The canonical module graph is unchanged because all required Gradle edges already
existed. Source counts and the verified baseline are refreshed in the architecture
graph document.

## Verification

Use Java 21 to run the moved `ConversationInfoViewModelTest`, the conversation module
boundary test, the app assembly-ownership test, `EntryOwnedViewModelGatewaySourceTest`,
and `ConversationNavigation3SourceTest`. Compile `:features:conversation`, then compile
app dev and fdroid variants sequentially.

Inspect generated KSP output to prove that the dedicated conversation-info factory
and bindings exist, the old core factory no longer contains an info method, and the
dedicated bindings are installed once. Finish with `git diff --check`, old-path and
app-import audits, resource-owner checks, and rename-similarity inspection.
