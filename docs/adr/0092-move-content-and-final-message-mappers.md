# ADR 0092: Move content and final message mappers to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `58334e842a1da959020dfd608937017f6b91fd66`

## Context

`MessageContentMapper` selects regular/system presentation mapping from a Kalium
message, while `MessageMapper` creates the final existing `UIMessage` hierarchy.
Their complete production dependency closure is already feature-owned or neutral:
the message models, regular/system mappers, user lookup, avatars, membership,
accent, and ISO formatting. Both retain the package-preserved contracts consumed by
app timeline and message use cases.

## Decision

Move `MessageContentMapper.kt` and `MessageMapper.kt` from `:app` to
`:features:conversation` with byte-identical production source. Preserve their
packages/FQNs, visibility, `@Inject` constructors, constructor parameter order,
mapping branches, and helper extensions. Move the two focused tests with them.

The tests retain their existing cases and assertions. Only app-local `TestMessage`
is replaced by private, minimal Kalium `Message.Regular` fixtures; `TestUser` and
`CoroutineTestExtension` continue to come from `:core:ui-common` test fixtures.
This fixture-only substitution avoids a feature-to-app test dependency.

The conversation boundary test registers both production sources, requires feature
source/test ownership, validates the package/FQN and injected constructor contracts,
rejects the legacy app paths, and rejects app-local message fixtures. The existing
feature KSP processor therefore generates the same
`MessageContentMapper$MetroFactory` and `MessageMapper$MetroFactory` identities;
profile descriptors and Metro configuration do not change.

## Ownership boundary

`MessagePreviewContentMapper` remains app-owned for the next atom. App also retains
downstream timeline rendering, Compose/message-list rendering, Markdown
parsing/rendering, and use cases, which consume unchanged mapper FQNs through the
existing app-to-conversation edge.

No Gradle/settings dependency, resource, Navigation 3, Metro binding/group,
consumer import, profile/stability descriptor, canonical Mermaid, KMP, or iOS change
is part of this atom.

## Consequences

The post-move inventory is app production/tests **999/287** and conversation feature
production/tests **112/42**. Top-level app mapper production/test inventories are
**4/4**; conversation mapper production/test inventories are **8/7**. Production
behavior, FQNs, injection construction, generated Metro factory identities, and app
consumer imports stay unchanged.

## Verification and rollback

Run the moved mapper and conversation boundary tests in the feature; run the three
unchanged app consumer tests; compile the feature and app dev/fdroid variants with
Java 21. Inspect source identity, factory/profile descriptors, inventories, rename
classification, working-tree scope, and `git diff --check`.

Rollback the two source/test moves, private test fixtures, boundary registration, and
documentation together if mapping behavior, constructor/FQN identity, Metro factory
identity, profiles, or app compilation changes. Stop rather than widening scope if a
production source needs app implementation/resources, a package or constructor
change, a new Gradle edge, or a Metro/profile change.
