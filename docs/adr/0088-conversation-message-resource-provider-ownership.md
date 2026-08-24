# ADR 0088: Conversation message resource-provider ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `6efea27deea4d34c2e93e98394fdd9955b3c37eb`

## Context

`MessageResourceProvider` is the constructor contract shared by the app-owned
regular and system message mappers. After `UIMessage` and `UIQuotedMessage` moved
to `:features:conversation`, the provider remained in `:app` only because its four
default resource IDs were app-owned. Keeping it there would make every subsequent
subordinate-mapper move depend on an app implementation type.

The four IDs already describe conversation member names and fallback message
content. Their existing localization set is closed: 47 definitions across 12
`values*` qualifiers. Copying them or introducing another provider would create two
sources of truth and make the mapper closure harder to review.

## Decision

Move `MessageResourceProvider` package-preserving to
`:features:conversation`, retaining its public FQN, data-class shape, property
order, annotations, constructor defaults, and the app-owned
`AppModule.provideMessageResourceProvider()` binding.

Move, without copying, these IDs from app resources to the matching feature
`strings.xml` files:

- `member_name_deleted_label`;
- `member_name_you_label_lowercase`;
- `member_name_you_label_titlecase`;
- `sent_a_message_with_content`.

The exact 47 existing definitions and XML attributes remain unchanged. App
consumers select the moved IDs through
`com.wire.android.feature.conversation.R as conversationR`; unrelated app resource
references remain in the app namespace.

Extend the existing message-resource ownership gate so all 13 feature-owned IDs
have exactly 167 definitions, the exact qualifier coverage, and the canonical
SHA-256 fingerprint
`db7575c9aab257e848b4ba7e0c40e81d2b900dc78512abd76a87875b500675af`.
Add a focused provider-default test and include the moved source in the conversation
module boundary manifest.

## Ownership boundary

`RegularMessageMapper`, `SystemMessageContentMapper`, `MessageContentMapper`,
`MessageMapper`, and `MessagePreviewContentMapper` remain app-owned in this atom.
The app Metro module continues constructing the same provider FQN, so generated
factory names and baseline/startup profile identities do not change.

No Gradle/settings edge, Navigation 3 contract, Metro binding, profile, stability
descriptor, canonical Mermaid diagram, KMP source set, or iOS glue changes.

## Consequences

Overall app production Kotlin decreases from 1005 to **1004**, while conversation
feature production increases from 106 to **107** and feature unit-test files from
37 to **38**. Strict app conversation production/tests remain **173/54**. Strict
conversation app-`R` consumers decrease from 78 to **77**; the normalized distinct
resource-reference count remains **397**, and strict `BuildConfig` consumers remain
**3**.

The feature still has 25 Crowdin-tracked resource files and qualifier directories,
now containing **279** string definitions. The next mapper slice can relocate the
additional resources required by `SystemMessageContentMapper` and then move that
mapper without introducing an app dependency.

## Verification and rollback

Run the provider-default, resource-ownership, and conversation-boundary tests in
the feature. Run the regular, system, preview, and conversation-info assembly tests
in app. Compile the feature and app dev/fdroid variants sequentially with Java 21.
Verify resource absence in app, exact definition counts/fingerprint/qualifiers,
consumer namespaces, rename detection, and `git diff --check`.

Rollback the provider move, all 47 resource definitions, app namespace changes,
tests, and documentation as one atom if resource merging, localization coverage,
constructor defaults, Metro assembly, or app compilation changes. Stop instead of
widening the atom if it requires a feature-to-app dependency, a new provider,
package/FQN changes, duplicated resources, or KMP/iOS work.
