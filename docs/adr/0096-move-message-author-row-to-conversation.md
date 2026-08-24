# ADR 0096: Move message author chrome to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `a1b8c5529dd4550bac9b4a4ab45fa108cfa9fe13`

## Context

`MessageAuthorRow` and `MessageSmallLabel` render reusable author and timestamp
chrome for existing message items. Their dependencies are already conversation-owned
models or neutral UI-common/theme contracts. They do not use app resources, Android
runtime effects, Paging, audio, Cells, Metro, or navigation.

## Decision

Move `MessageAuthorRow.kt` from `:app` to `:features:conversation` as a byte-identical,
package-preserved source relocation. Keep both public composable FQNs and all existing
app consumers unchanged. Register feature ownership and reject the legacy app path in
the conversation boundary test.

## Consequences

App production/tests become **994/285** and the conversation feature becomes
**116/44**. The strict app conversation production inventory becomes **170**. No
resource, Gradle/settings, Metro, Navigation 3, profile, stability, KMP/iOS, canonical
Mermaid, or consumer change accompanies this atom.

## Verification and rollback

Compare the source byte-for-byte with baseline, run the conversation boundary test,
compile the feature and app, inspect the R100 rename and inventories, and run
`git diff --check`. Roll back the source move, guard, ADR, and factual documentation
together if any unchanged consumer fails to compile.
