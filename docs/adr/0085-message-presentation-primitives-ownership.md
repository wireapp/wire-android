# ADR 0085: Message-presentation primitive ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `c505335c693c9789eb6f2bf4b1fb585a15907385`

## Context

The app-owned `UIMessage` model is a shared blocker for quote, search, asset-paging,
draft, and timeline presentation ownership. Moving those consumers independently by
introducing capability-specific copies would preserve the source cycle and create
competing message models instead of advancing the conversation feature boundary.

Three dependencies of `UIMessage` remained app-owned despite having conversation-only
responsibilities: date grouping, the copyable-message contract, and the immutable
Markdown node/preview model. Their app renderers and consumers already access them by
stable packages, and their dependency closure is available in `:features:conversation`
without a new Gradle edge.

## Decision

Move `MessageDateGroupingMapper.kt`, `Copyable.kt`, and `MarkdownNode.kt` to
`:features:conversation` while preserving their packages, public FQNs, declarations,
and file contents. Move `MessageDateGroupingMapperTest` with its owner unchanged.

The feature owns:

- `MessageDateTimeGroup`, `groupedUIMessageDateTime`, and the date-divider predicate;
- the `Copyable` contract used by copyable message content;
- `MarkdownNode` and `MarkdownPreview`, which represent message Markdown data.

App keeps Markdown parsing and Compose rendering, message-list rendering, resources,
and the still-app-owned `UIMessage`, `UIQuotedMessage`, and mapping pipeline. Existing
callers retain the same imports because every package and FQN is unchanged.

## Ownership boundary

This slice deliberately does not introduce a parallel quote/search message model and
does not move `UIMessage` before its remaining app resource and Markdown-constant
dependencies are resolved. It removes three proven app implementation edges from the
future message-model closure while leaving runtime behavior byte-identical.

No Gradle/settings dependency, resource/Crowdin definition, Metro binding, Navigation
3 source, baseline/startup profile, stability descriptor, KMP source, canonical
Mermaid diagram, or caller changes in this slice.

## Consequences

The strict app conversation production/test counts remain **175/54**. Conversation
feature production/tests become **104/35**, and overall app main Kotlin sources become
**1007**. The nine date-grouping tests move to the feature owner without modification.

`UIMessage` still requires a later reviewable prerequisite for app string resources
and `MarkdownConstants.NON_BREAKING_SPACE`; this slice narrows that later diff instead
of hiding it behind a new abstraction.

## Verification and rollback

Run the moved date-grouping test and conversation boundary test with Java 21. Compile
the feature and app dev/fdroid variants sequentially. Verify R100 rename evidence,
old-path absence, exact import budgets, source counts, unchanged profiles/stability,
unchanged resources and Mermaid diagrams, and `git diff --check`.

Rollback the four moves and boundary documentation as one unit if packages, FQNs,
date grouping, copy behavior, Markdown model declarations, or caller compilation
changes. Stop instead of widening the move if it requires commonmark dependencies,
resource movement, parser/renderer movement, a new message model, or changes to
`UIMessage`, `MessageMapper`, or their consumers.
