# ADR 0078: Conversation image-asset paging ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `a560794cf`

## Context

The conversation image gallery still obtained its paged image messages through
`ObserveImageAssetMessagesFromConversationUseCase` in `:app`. The use case and its
focused test formed a closed presentation leaf with `UIAssetMapper` and
`TimeZoneProvider`: none of the three production classes owns an activity, screen,
navigation entry, permission, picker, file import, or other Android runtime side
effect.

The leaf is behavior-sensitive. It configures the Kalium paging source, maps every
asset field into `UIAssetMessage`, resolves the unavailable-username fallback, and
inserts localized month/year labels using one system time-zone lookup for each emitted
`PagingData`.

## Decision

Move `ObserveImageAssetMessagesFromConversationUseCase`, `UIAssetMapper`,
`TimeZoneProvider`, and the focused use-case test to `:features:conversation`. Preserve
their packages, public FQNs, injected constructor signatures, and source bodies.

Declare `api(libs.androidx.paging3)` because the feature-owned public use case exposes
`Flow<PagingData<UIImageAssetPagingItem>>`. Declare
`testImplementation(libs.androidx.paging.testing)` for the moved snapshot test. Do not
add Paging Compose and do not remove the app's existing Paging dependencies while its
screens and ViewModel remain app-owned.

No repository-module edge, internal conversation module, resource, navigation entry,
Metro group, profile entry, UI-message abstraction, or KMP source set changes in this
slice. The canonical Mermaid module graph is unchanged because Paging 3 is recorded as
a direct third-party library dependency rather than a repository module.

## Behavioral contract

- Paging keeps page size 20, initial load size 20, and prefetch distance 30.
- Starting offset remains `max(0, initialOffset - 30).toLong()`.
- Each emitted `PagingData` performs exactly one system-default time-zone lookup.
- A label precedes the first asset and every month or year transition, and no other
  separator is inserted.
- Labels continue to use `monthYearHeader`.
- `UIAssetMapper` preserves asset ID, time, username or unavailable fallback,
  conversation ID, message ID, asset path, and self-asset flag.
- The flow continues to execute on `dispatchers.io()`.

## Ownership boundary

The facade owns the image-only paging use case, its presentation mapper, time-zone
provider, and focused test. `:app` continues to own
`ConversationAssetMessagesViewModel`, media screens and Navigation 3 entries, the
file-asset pipeline, platform pickers, permissions, activity results, and runtime
composition.

Stop rather than broaden this move if the leaf requires an app implementation import,
Paging Compose, a changed public FQN or Metro factory descriptor, a UIMessage or
MessageMapper dependency, a file-asset concern, or a new repository-module edge.

## Consequences

The image gallery's data-to-presentation paging seam has one reusable Android feature
owner while host runtime work remains in the application. Review stays dominated by
four exact file moves, two dependency declarations, ownership gates, and documentation.

## Verification

Run the moved paging test with `ConversationModuleBoundaryTest`, then the app module
dependency boundary test. Compile `:features:conversation`, app dev, and app fdroid
sequentially with Java 21.

Inspect generated Metro factories to prove one feature output and no app source-compile
output for each moved injected class, while constructor descriptors remain unchanged.
Confirm the old paths are absent, the documented counts match, app baseline/startup
profiles are unchanged, no moved source imports app implementation, and finish with
`git diff --check` and rename-similarity inspection.
