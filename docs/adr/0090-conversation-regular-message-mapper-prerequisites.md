# ADR 0090: Close the regular-message mapper prerequisites

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `de830886e2e5297b22234a4088ded89454e6f098`

## Context

`RegularMessageMapper` remained in `:app` after the shared UI-message models,
resource provider, and system mapper moved to `:features:conversation`. Its remaining
source-level app dependencies were the package-preserved `ISOFormatter` and two
message-presentation string IDs. `ImageAsset` and `Accent` already keep their legacy
packages while being physically owned by `:core:ui-common`.

Moving the regular mapper together with these prerequisites would mix dependency
closure, localized resources, production mapping, and a broad test-fixture adaptation
in one review. Keeping the prerequisites separate makes the following mapper move a
source-and-test ownership atom.

## Decision

Move `ISOFormatter` package-preserving from `:app` to `:features:conversation`.
Retain its public FQN, `@Inject` constructor, method signature, Java date formatting,
default locale and time-zone behavior, and generated Metro factory identity.

Move, without copying, these message-presentation IDs to the feature's matching
`strings.xml` files:

- `sent_a_message_with_unknown_content` with 11 definitions;
- `label_quote_original_message_date` with 13 definitions.

The exact 24 definitions retain their values, attributes, and qualifier coverage.
Extend the existing message-resource ownership gate so all 17 feature-owned IDs have
exactly 211 definitions and canonical SHA-256 fingerprint
`dfb6b3a5b81f7db207ea6121129c90bd858f78b16404021dc6b326d21aae216a`.

Switch the three existing consumers to the feature resource namespace:

- `RegularMessageContentMapper` for unknown content and quoted-message date labels;
- `PreviewMessageTypes` for the quoted-message preview;
- `MessageDraftViewModelTest` for its quoted-message fixture.

No resource fallback, formatting argument, or consumer behavior changes.

## Ownership boundary

`RegularMessageMapper`, its focused test, `MessageContentMapper`,
`MessagePreviewContentMapper`, and the final `MessageMapper` remain app-owned in this
atom. `ISOFormatter` is intentionally feature-owned because its only production
consumers are the regular and final message mappers that are moving through the same
closure; there is no independent neutral-core consumer.

The formatter package and constructor remain unchanged, so its baseline/startup
profile descriptors and app graph references remain valid while the feature KSP
processor generates the same factory FQN. No Gradle/settings edge, Metro binding
container, Navigation 3 source or identity, profile, stability descriptor, canonical
Mermaid diagram, KMP source set, or iOS glue changes.

## Consequences

App production Kotlin decreases from 1003 to **1002**, while conversation feature
production increases from 108 to **109**. Unit-test inventories remain **290** in app
and **39** in the feature. Strict app conversation production/tests/resource
consumers/distinct references/`BuildConfig` consumers remain **173/54/77/397/3**.

The feature retains 25 Crowdin-tracked resource files and qualifier directories and
now contains **323** string definitions. `RegularMessageContentMapper` no longer
imports app `R`; its remaining imports resolve from the conversation feature, neutral
core, Kalium, Kotlin, or existing third-party dependencies.

## Verification and rollback

Run the conversation resource-ownership and module-boundary tests. Run the focused
regular mapper and message-draft tests, then compile the feature and app dev/fdroid
variants sequentially with Java 21. Verify source byte identity, exact resource
absence in app, 24 unchanged definitions, qualifier coverage and fingerprint, the
three feature-namespace consumers, inventory counts, unchanged profile descriptors,
and `git diff --check`.

Rollback the formatter move, all 24 resource definitions, three consumer namespace
changes, tests, and documentation as one atom if formatting, resource merging, Metro
construction, profile identity, or app compilation changes. Stop instead of widening
the atom if it requires a feature-to-app dependency, another formatter, copied
resources, mapper logic changes, Navigation 3 work, or KMP/iOS work.
