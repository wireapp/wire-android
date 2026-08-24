# ADR 0089: Conversation system-message mapper ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `259330e1acebdcd87dafa2150123a4754b094ab4`

## Context

`SystemMessageContentMapper` remained in `:app` after the conversation feature took
ownership of the shared UI-message model and `MessageResourceProvider`. Its
non-resource dependencies were already available through the existing conversation
feature and core edges. The only remaining app-owned inputs were the localized labels
for enabled and disabled conversation receipt mode.

Keeping the mapper in app would leave one branch of message-content mapping behind an
app implementation boundary and would prevent the remaining mapper closure from being
reviewed as a sequence of small, package-preserving moves.

## Decision

Move `SystemMessageContentMapper` package-preserving from `:app` to
`:features:conversation`. Retain its public FQN, `@Inject` constructor, constructor
parameters, `SelfNameType`, mapping branches, date and locale behavior, and nullability
contract. The only production-source change is selecting
`com.wire.android.feature.conversation.R` instead of app `R`.

Move, without copying, these string IDs to the feature's matching `strings.xml` files:

- `label_system_message_receipt_mode_on`;
- `label_system_message_receipt_mode_off`.

Both IDs retain their exact 20 definitions across `values`, `values-de`, `values-es`,
`values-et`, `values-hu`, `values-it`, `values-pl`, `values-pt`, `values-ru`, and
`values-si`. Extend the existing resource-ownership gate so the complete 15-ID message
resource set has exactly 187 definitions and canonical SHA-256 fingerprint
`e1c55eecb2f465d2c0bf8710648454d6d0d59c3b8a9138eb05e22a94a19e157b`.

Move the focused mapper test to the feature. Replace only its app-test-local
`TestMessage` and `assertIs` dependencies with a local `Message.System` fixture and
JUnit `assertInstanceOf`; retain the existing assertions and add explicit checks that
receipt-mode mapping selects the two feature resource IDs.

## Ownership boundary

`RegularMessageMapper`, `RegularMessageContentMapper`, `MessageContentMapper`,
`MessagePreviewContentMapper`, and the final `MessageMapper` remain app-owned in this
atom. App consumers keep resolving the unchanged mapper FQN. The app-owned
`AppModule.provideMessageResourceProvider()` provider and Metro composition remain
unchanged.

The package, constructor, and generated factory identity do not change, so existing
baseline and startup profile descriptors remain valid. No Gradle/settings edge,
Navigation 3 source or identity, Metro binding group, profile, stability descriptor,
canonical Mermaid diagram, KMP source set, or iOS glue changes.

## Consequences

App production Kotlin decreases from 1004 to **1003**, while conversation feature
production increases from 107 to **108**. App unit-test files decrease from 291 to
**290**, while conversation feature unit-test files increase from 38 to **39**. The
app mapper production/test inventories each decrease from 8 to **7**.

Strict app conversation production/tests remain **173/54**. Strict app conversation
resource consumers, distinct resource references, and `BuildConfig` consumers remain
**77/397/3**. The feature retains 25 Crowdin-tracked resource files and qualifier
directories and now contains **299** string definitions.

## Verification and rollback

Run the moved mapper test, message-resource ownership test, and conversation boundary
test in the feature. Run the app-owned content, preview, regular, and final mapper tests.
Compile the conversation feature plus app dev and fdroid variants sequentially with
Java 21. Verify exact resource absence in app, qualifier coverage, definition count and
fingerprint, source/test rename detection, inventory counts, and `git diff --check`.

Rollback the source/test moves, all 20 resource definitions, ownership assertions, and
documentation as one atom if mapping behavior, resource merging, Metro construction,
profile identity, or app compilation changes. Stop instead of widening the atom if it
requires a feature-to-app dependency, another mapper or provider implementation,
package/FQN changes, duplicated resources, Navigation 3 work, or KMP/iOS work.
