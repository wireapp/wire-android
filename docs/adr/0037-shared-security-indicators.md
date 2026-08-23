# ADR 0037: Own shared security indicators in core UI common

**Status:** Accepted
**Baseline:** `efec11412`, `chore/android-modularization`

## Decision

Move the package-preserving, Kalium-free Compose primitives
`LegalHoldIndicator`, `ProteusVerifiedIcon`, and `MLSVerifiedIcon` from `:app`
to `:core:ui-common`. Their five required vector variants move with them:
the legal-hold indicator and the day/night Proteus and MLS valid-certificate
icons. The default `label_client_verified` string and its complete existing
ten-qualifier localization coverage also move to `:core:ui-common` without
changing text or attributes.

The app retains its Legal Hold preview and the Kalium-aware or app-specific
renderers: `ConversationVerificationIcons`, `MLSVerificationIcon`,
`MLSRevokedIcon`, and `MLSNotVerifiedIcon`. No Kalium types or functions are
introduced into the shared ownership boundary.

The moved resources also had direct app resource-ID consumers. Those consumers
now reference `core:ui-common`'s generated `R` namespace. This closes resource
ownership without duplicating values or introducing a new module edge; `:app`
and `:features:conversation` already depend on `:core:ui-common`.

## Consequences

The feature participant renderer can use its package-stable shared indicators
without depending on `:app`. Existing app call sites retain their Kotlin API and
only direct resource-ID call sites switch to `commonR`. Visual output, content
descriptions, theme variants, previews, and app-specific verification behavior
are unchanged.

Core source filenames intentionally differ from the remaining app adapter and
preview filenames. This prevents duplicate `LegalHoldIndicatorKt` and
`VerifiedIconsKt` JVM facades while preserving the Kotlin package and API.

An app source/resource ownership test protects the split: it asserts the exact
three shared declarations and five drawable files are core-owned, validates the
full localized verified-label coverage, checks direct resource consumers use
`commonR`, and rejects Kalium imports in the new shared sources.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :core:ui-common:compileDebugKotlin
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.common.SecurityIndicatorOwnershipSourceTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a shared indicator requires a Kalium
type, an app-only API, or a dependency edge outside `:core:ui-common`.
