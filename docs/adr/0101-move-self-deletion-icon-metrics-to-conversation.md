# ADR 0101: Move self-deletion icon metrics to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `eef2161db0aad9ce07c71467f8878aa93e402ab4`

## Context

`SelfDeletionTimerHelper.kt` contains the pure deletion-icon metric calculation and the
extension from feature-owned timer state. Its only runtime caller is the app-owned expiration
renderer, while all seven calculation tests were app-owned.

## Decision

Move the metric source and its seven tests to `:features:conversation`, preserving package,
FQN, values, quantization behavior, and the public `iconMetrics` transition seam. Make the raw
calculation internal because no app caller uses it, and convert the moved tests from JUnit 4 to
the feature's Jupiter convention. Keep the renderer-only start angle and stroke fraction as two
private app constants with their unchanged numeric values; they are canvas policy, not metric
model state.

## Consequences

App production/tests become **988/283** and the conversation feature becomes **122/49**.
Strict app conversation production/tests become **164/52**. Resources, dependency edges,
Metro, Navigation 3, profiles, stability, KMP/iOS, and runtime behavior do not change.

## Verification and rollback

Run `DeletionIconMetricsTest` and `ConversationModuleBoundaryTest`, then compile feature and
app dev. Revert this commit to restore the helper, tests, and renderer constant imports.
