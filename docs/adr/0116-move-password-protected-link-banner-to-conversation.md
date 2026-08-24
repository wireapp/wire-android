# ADR 0116: Move the password-protected link banner to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `f9163e05b`

## Context

`PasswordProtectedLinkBanner` is a guest-access renderer whose presentation dependencies are
already neutral UI-common contracts. It remained app-owned only because its two dedicated labels
were still in app resources.

## Decision

Move the renderer package-preserving to `:features:conversation`. Transfer
`password_protected_link_banner_title`, `password_protected_link_banner_description`, and all 15
existing localized definitions. Change only the resource namespace and add focused source,
qualifier, and ownership coverage.

## Consequences

App production/tests become **969/285**. Strict app conversation production/tests become
**147/54** and the conversation feature becomes **142/55**. App conversation sources importing
app `R` become **69**, with **345** distinct resource-alias tokens. The feature owns **933** string
definitions. Packages, public API, rendering behavior, Metro, Navigation 3, profiles, stability,
Gradle edges, and KMP/iOS sources do not change.

## Verification and rollback

Run `PasswordProtectedLinkBannerOwnershipTest` and app dev compilation. Revert this commit to
restore app ownership.
