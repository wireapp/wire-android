# ADR 0095: Move link-preview message-body visibility to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `88c3606089b6e2ee203d38ebe144f608fe062b11`

## Context

`MessageBody.shouldHideStandalonePreviewedUrl` decides whether a standalone text
URL is hidden when the same URL has a link preview. Its production dependencies
are the feature-owned `MessageBody`, neutral `UIText`, and Kalium
`MessageLinkPreview`. The only production consumer is the app-owned
`MessageContentAndStatus` renderer.

The existing app test exercises six visibility cases. The conversation feature
already exposes its facade to app and has the required Kalium, UI-common, and
Jupiter test dependencies.

## Decision

Move the production source and focused test to `:features:conversation`, retaining
their packages and FQNs. Change only the production extension visibility from
`internal` to public: the app renderer remains in a different Gradle module and
must continue to call the same extension through the existing facade edge.

The focused test preserves all cases and changes only JUnit 4 imports to the
feature's existing Jupiter API. The conversation boundary test records the exact
three-import budget, public visibility, package, and absence of the legacy app
source.

## Consequences

The post-move inventory is app production/tests **995/285** and conversation
feature production/tests **115/44**. No resources, Gradle/settings edge, Metro,
Navigation 3, profile, stability, canonical Mermaid, KMP/iOS source, or app
consumer import changes with this atom.

## Verification and rollback

Run the focused feature test, feature compilation, and app dev/fdroid Kotlin
compilation. Confirm the production normalized comparison differs only by the
visibility modifier, all six test cases remain, rename similarity is retained,
the boundary guard passes, and `git diff --check` is clean.

Rollback the two moves, ownership guard, ADR, and factual architecture updates
together if the unchanged app consumer cannot compile through the facade.
