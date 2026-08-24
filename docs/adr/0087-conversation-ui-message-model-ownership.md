# ADR 0087: Conversation UI message model ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `272ae8e1dcb7d777d0dbcbd71c407919741c4a2c`

## Context

The existing `UIMessage` hierarchy is the single presentation model consumed by
the conversation timeline, quoted messages, drafts, search, image paging, message
details, and conversation-list previews. Its message-presentation primitives and
nine resource IDs already belong to `:features:conversation`, but `UIMessage` and
`UIQuotedMessage` still physically belonged to `:app`.

Moving only `UIMessage` is not a closed change because `MessageBody` exposes
`UIQuotedMessage`. Moving both files together closes that model boundary. Their
only remaining source-level app concern was the two default references to
`MarkdownConstants.NON_BREAKING_SPACE`; the rest of their imports are already
feature-owned, neutral-core-owned, Android APIs, Kalium APIs, or declared library
dependencies.

## Decision

Move `UIMessage.kt` and `UIQuotedMessage.kt` package-preserving from `:app` to
`:features:conversation`. Preserve all public declarations, FQNs, serializers,
defaults, quote mapping behavior, and existing consumers.

Do not move or depend on the app Markdown parser. Replace only the two last-message
preview defaults with a feature-private constant whose exact value remains the HTML
entity `"&nbsp;"`. This is not a Unicode non-breaking space or an ordinary space;
existing app parsing and rendering deliberately normalize the HTML entity.

Move the existing `UIMessage.Regular.mapToQuotedContent` extension together with
`UIQuotedMessage`. Its `appLogger` import is physically owned by
`:core:ui-common`, despite the legacy package name, and the remaining imports are
Kalium or existing serialization contracts. No mapper split or new dependency is
required.

## Ownership boundary

`:features:conversation` now owns the one canonical UI message and quoted-message
model closure. App keeps `MessageMapper`, its subordinate content/preview mappers,
Markdown parsing/rendering, Compose message rendering, Navigation 3 runtime, and
Android composition until their own dependency closures are ready.

There is no parallel message, quote, draft, search, paging, or conversation-list
model. Package and FQN preservation means all production and test consumers keep
their existing imports. No Gradle/settings edge, resource definition, Metro
binding, Navigation source, profile, stability descriptor, Mermaid edge, or KMP
source changes with this ownership move.

## Consequences

Strict app conversation production sources become **173**, total app production
Kotlin sources become **1005**, conversation feature production sources become
**106**, and feature tests become **37**. App conversation tests remain **54**.
The strict app conversation tree still has **78** app-resource consumers,
**397** distinct `R.type.name` references, and **3** `BuildConfig` consumers.

Feature tests lock the exact `"&nbsp;"` defaults and their serialization round trip.
The module boundary test locks feature ownership, package preservation, old-path
absence, the permitted import budget, and the absence of the app Markdown constant.
The message-resource ownership manifest no longer treats the moved `UIMessage` as
an app consumer.

The remaining message mapper closure is intentionally separate. It first needs its
app resource set and remaining helper dependencies isolated, then subordinate
content/preview mappers can move before the final `MessageMapper` adoption.

## Verification and rollback

Run the feature `UIMessageTest`, `ConversationModuleBoundaryTest`, and
`ConversationMessageResourceOwnershipTest`; run the focused app message mapper,
quote, multipart-quote, and draft tests. Compile `:features:conversation`, app dev,
and app fdroid sequentially with Java 21. Verify two source renames, unchanged
packages/FQNs and consumers, exact source counts, no Gradle/resource/profile/
stability changes, and `git diff --check`.

Rollback the two source moves, private constant, tests, and documentation as one
unit if serialization, default separators, quote mapping, resource resolution, or
consumer compilation changes. Stop instead of widening the slice if it requires
commonmark/parser code, an app or feature-to-feature edge, consumer renames, or a
second UI message model.
