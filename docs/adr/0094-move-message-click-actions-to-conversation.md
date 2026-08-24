# ADR 0094: Move message click actions to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `d804aeb623f8aa7f48e7d062b02ce250edd758a6`

## Context

`MessageClickActions` is the sealed, package-preserved interaction contract passed
from existing app timeline, preview, composer, media, search, and settings UI into
message item rendering. Its only production imports are the already feature-owned
`UIMessage`/`MessageSenderId` model closure and `ConversationId`/`UserId` from the
existing Kalium Logic feature API. It has no Android runtime, app resource, audio,
Cells, renderer, Metro, navigation, or host implementation dependency.

The baseline has 12 unchanged app consumer files and no focused test that references
this contract. The app already has its sole inbound `:features:conversation` edge,
so preserving the package and FQN requires no caller or Gradle change.

## Decision

Move `MessageClickActions.kt` from `:app` to `:features:conversation` as a
byte-identical production source move. Preserve the package, sealed-class identity,
public and internal types, property signatures, defaults, imports, and `FullItem` /
`Content` data-class identities. Do not move consumers, message item containers,
renderers, Cells, Android/audio code, resources, or any host implementation.

The conversation boundary test registers the destination source, asserts its legacy
package and exact four-import dependency budget, and rejects the old app path. No
focused source test moves because none exists.

## Consequences

The post-move inventory is app production/tests **996/286** and conversation feature
production/tests **114/43**. The strict app conversations directory contains
**172** production files. The feature now owns the package-preserved interaction
contract; all 12 app consumers continue to compile through the unchanged facade edge.

No Gradle/settings edge, resource, Navigation 3, Metro binding/group, profile or
stability descriptor, canonical Mermaid source, KMP/iOS source, or consumer import
changes with this atom.

## Verification and rollback

Compare the destination source byte-for-byte with baseline `d804aeb62`; inspect its
imports and all consumers; run the conversation boundary test, feature compilation,
and app dev/fdroid Kotlin compilation. Confirm R100 rename classification, the
post-move inventories, permitted changed paths only, and `git diff --check`.

Rollback the source move, ownership guard, ADR, and factual documentation together
if package/FQN compatibility or app compilation changes. Stop rather than adding a
module edge or following consumers into app.
