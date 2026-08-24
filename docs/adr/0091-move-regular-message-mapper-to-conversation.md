# ADR 0091: Move regular-message mapping to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `dd49999b9483310bf49e2a9579fe58ccb368efb5`

## Context

`RegularMessageMapper` converts Kalium regular-message content into the existing
conversation presentation models. Its production dependency closure is now owned by
`:features:conversation` or neutral core: UI-message models, image assets, participant
lookup, visual-media parameters, accent mapping, `MessageResourceProvider`,
`ISOFormatter`, and all referenced localized resources.

Leaving the mapper in `:app` would keep a feature implementation behind the
composition root after its complete dependency closure had moved. The package and
constructor can remain unchanged, so app consumers do not require import or graph
changes.

## Decision

Move `RegularMessageContentMapper.kt` package-preserving from `:app` to
`:features:conversation`. Preserve the public `RegularMessageMapper` FQN, its
`@Inject` constructor, `AssetMessageContentMetadata`, mapping branches, helper
functions, visibility, and all resource/formatting behavior byte-for-byte.

Move `RegularMessageContentMapperTest.kt` to the feature. Replace only its dependency
on the broad app-only `TestMessage` fixture with private, minimal Kalium message and
asset fixtures. Remove the obsolete mocked asset-download use case and file-system
setup: the mapper has no such constructor dependency and never invoked it. Retain the
five production mapping assertions.

Extend the conversation boundary gate to enforce source/test ownership, the legacy
package and constructor contract, local test fixtures, absence of the legacy app
paths, and the no-app-import rule. Remove the mapper from the expected app resource
consumer set because it now consumes the same feature resources inside their owner
module.

## Ownership boundary

`MessageContentMapper`, `MessagePreviewContentMapper`, and final `MessageMapper`
remain app-owned in this atom. They consume the unchanged `RegularMessageMapper` FQN
through the existing app-to-conversation dependency. The feature's existing KSP
processor generates the same `RegularMessageMapper$MetroFactory` identity, so the
existing baseline/startup profile descriptors remain valid and unchanged.

No Gradle/settings edge, Metro binding container, Navigation 3 source or identity,
localized resource definition, profile, stability descriptor, canonical Mermaid
diagram, KMP source set, or iOS glue changes.

## Consequences

App production Kotlin decreases from 1002 to **1001**, while conversation feature
production increases from 109 to **110**. App unit tests decrease from 290 to
**289**, while feature unit tests increase from 39 to **40**. The top-level app
mapper production/test inventories become **6/6**; the feature mapper inventories
become **6/5**.

The feature retains **323** string definitions, including the exact 17-ID,
211-definition message-presentation closure. Strict app conversation
production/tests/resource consumers/distinct references/`BuildConfig` consumers
remain **173/54/77/397/3**.

## Verification and rollback

Run the moved mapper test, message-resource ownership test, and conversation boundary
test in the feature. Run the downstream app `MessageContentMapperTest` and
`MessageMapperTest`, then compile the feature and app dev/fdroid variants sequentially
with Java 21. Verify the production source is byte-identical, its package/FQN and
constructor are unchanged, the test retains five test cases, generated Metro/profile
identity is unchanged, inventories match, and `git diff --check` passes.

Rollback the source/test moves, local fixture adaptation, boundary/resource-consumer
expectations, and documentation as one atom if mapping, Metro construction, profiles,
or app compilation changes. Stop instead of widening if the move requires an app
implementation dependency, another mapper abstraction, resource duplication,
Navigation 3 changes, or KMP/iOS work.
