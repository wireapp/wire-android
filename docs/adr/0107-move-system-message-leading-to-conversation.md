# ADR 0107: Move system-message leading presentation to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `14216e6fcf6ffe67ed69a4ea9240d502386aaa78`

## Context

`SystemMessageItemLeading` only renders the icon described by `SystemMessageContent`, but that
stable presentation contract was nested in the app-owned system-message factory/rendering file.
The factory still needs app resources, while the contract itself uses only neutral annotations,
Compose types, color/dimensions, and the core-owned support-page enum.

## Decision

Move `SystemMessageItemLeading` byte-for-byte to `:features:conversation` and extract the existing
`SystemMessageContent` declaration into its own feature source with the same package/FQN, fields,
annotations, and defaults. Keep `SystemMessageItem.kt` and its resource-backed factory app-owned.

## Consequences

App production/tests become **981/283** and the conversation feature becomes **130/49**.
Strict app conversation production/tests become **158/52**. No resource, dependency, Metro,
Navigation 3, profile, stability, KMP/iOS, or host-runtime change occurs.

## Verification and rollback

Run `ConversationModuleBoundaryTest` and compile feature/app dev. Verify that the leading source
is a 100% rename and the extracted contract is textually identical to its former declaration.
Revert this commit to re-inline the contract and restore app ownership.
