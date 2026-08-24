# ADR 0109: Move the reusable settings switch to UI common

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `045566f28f6c781961909fa2e011795806b8cb56`

## Context

`SettingsOptionSwitch` and `SwitchState` are used by app settings, new-conversation,
conversation-details, and Cells UI. Keeping this neutral presentation primitive in `:app`
prevents those consumers from moving independently and would force the conversation feature to
depend on the application module.

## Decision

Move the implementation package-preserving to `:core:ui-common`. Move `label_on` and
`content_description_toggle_setting_label` with their exact 16 and 7 localized definitions to
the same owner, remove the three duplicate Cells definitions, and update the two direct resource
callers to use UI-common `R`. Add a focused source test for ownership and localization parity.

## Consequences

App production/tests become **978/284**. Strict app conversation and conversation-feature counts
remain **156/53** and **133/49**. UI common gains one production and one unit-test file plus the
exact **23** resource definitions. Packages, public names, Compose behavior, Gradle edges,
Navigation 3 identities, profiles, stability configuration, and KMP/iOS sources do not change.
This neutral prerequisite lets conversation-details UI move without a feature-to-app edge.

## Verification and rollback

Run `SettingsOptionSwitchResourceOwnershipSourceTest`, compile UI common, Cells, and app dev, and
verify that neither app nor Cells retains either transferred resource ID. Revert this commit to
restore app ownership.
