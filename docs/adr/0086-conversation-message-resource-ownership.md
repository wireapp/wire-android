# ADR 0086: Conversation message resource ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `8185620f2cf39027a650c95a4c0de11441284d07`

## Context

The single existing `UIMessage` model is the shared presentation contract for the
conversation timeline, quotes, drafts, search, paging, and conversation-list
previews. After moving its message-presentation primitives, its remaining app-owned
dependencies were nine string resources and
`MarkdownConstants.NON_BREAKING_SPACE`.

Duplicating the resources or introducing a feature-specific message model would keep
the ownership split and make the later model move harder to review. The nine resource
IDs already describe conversation message presentation and have a closed localization
set in app resources.

## Decision

Move these resource IDs from `:app` to `:features:conversation`:

- `label_message_edit_sent_failure`;
- `label_message_sent_failure`;
- `label_message_edit_sent_remotely_failure`;
- `label_message_sent_remotely_failure`;
- `label_message_decryption_failure_message_with_error_code`;
- `label_message_decryption_failure_message`;
- `deleted_message_text`;
- `label_message_status_edited_with_date`;
- `url_maps_location_coordinates_fallback`.

Move all **120** existing definitions byte-for-byte across their exact **25**
`values*` qualifiers. Existing app production and test consumers import
`com.wire.android.feature.conversation.R as conversationR` and change only the
nine affected references. Other app resource references continue to use app `R`.

Add a feature ownership test that locks:

- absence of the nine IDs from app resources;
- exact qualifier coverage and definition count;
- a SHA-256 fingerprint of qualifier, ID, attributes, and value;
- the exact app consumer set and use of the feature resource namespace.

## Ownership boundary

This is a resource prerequisite, not the `UIMessage` move. `UIMessage`,
`MessageMapper`, Markdown parsing/rendering, Compose message rendering, Navigation
3 runtime, and Android composition remain in their current owners. The remaining
source blocker for moving `UIMessage` is the narrow non-breaking-space default;
it must be resolved without pulling the app Markdown parser into the feature.

No Gradle/settings edge, Metro binding, Navigation 3 source, baseline/startup
profile, stability descriptor, KMP source, or canonical Mermaid diagram changes.

## Consequences

Strict app conversation production/tests remain **175/54**, conversation feature
production remains **104**, and feature tests become **36**. The feature now owns
**25** `strings.xml` files in **25** qualifier directories with **232** string
definitions. App resource files lose exactly 120 definitions without changing any
translation, formatting placeholder, or `translatable` attribute.

The next reviewable slice can move the existing `UIMessage` package-preserving
after isolating its non-breaking-space default. It must not introduce a parallel
message, quote, draft, search, or paging model.

## Verification and rollback

Run `ConversationMessageResourceOwnershipTest` and
`ConversationModuleBoundaryTest` in the feature, then focused mapper/draft tests
in app. Compile feature, app dev, and app fdroid variants sequentially with Java 21.
Verify exact resource counts, qualifier coverage, Crowdin mapping, app-resource
absence, consumer imports, `git diff --check`, and unchanged production source
counts.

Rollback the resource definitions, nine consumer aliases, ownership test, and
documentation as one unit if resource merging, localization coverage, IDs, values,
attributes, or caller compilation changes. Stop instead of widening the slice if
the move requires a new message model, app implementation dependency, feature-to-
feature edge, or Markdown parser dependency.
