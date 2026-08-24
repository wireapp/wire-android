# ADR 0097: Move reaction presentation to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `d9160ebd4a3eda4632803922bff3cb7f07939c64`

## Context

`MessageReactionsItem` and `ReactionPill` render the reaction row used by existing
app message items. Their code depends only on conversation-owned message models and
interaction helpers plus neutral UI-common/theme contracts. The existing neutral
public `MultipleThemePreviews` annotation in `:core:ui-common` lets the moved preview
functions retain equivalent light/dark preview coverage. The two accessibility
string IDs are consumed only by `ReactionPill` and have six definitions across the
default, German, and Russian qualifiers.

## Decision

Move both composables to `:features:conversation` with preserved packages and FQNs.
Keep the runtime composable behavior unchanged; change `ReactionPill` only from app
`R` to feature `R`, and point both preview imports to the existing public neutral core
annotation. Move, rather than copy, all six definitions of both accessibility
IDs. A focused ownership test verifies source ownership, qualifier coverage, values,
attributes, and absence from app. The aggregate boundary test registers both sources.

## Consequences

App production/tests become **992/285** and the conversation feature becomes
**118/45**. Strict app conversation production becomes **168**. Feature strings grow
from 609 to **615** definitions; the separate 59-ID/608-definition message-mapping
resource guard is unchanged. Existing app callers retain the same FQNs.

No Gradle/settings, Metro, Navigation 3, profile, stability, KMP/iOS, canonical
Mermaid, or host/runtime change accompanies this atom.

## Verification and rollback

Run the focused reaction ownership test and aggregate conversation boundary test,
compile feature and app, validate exact XML transfer and zero app duplicates, inspect
rename similarity and inventories, and run `git diff --check`. Roll back sources,
resources, tests, guards, ADR, and factual docs together if any invariant fails.
