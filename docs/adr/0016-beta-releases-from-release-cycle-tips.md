# 16. Beta releases from release-cycle branch tips

Date: 2026-10-14

## Status

Accepted

## Context

Release-cycle branches stabilize a selected Wire Android release while develop
continues independently. Requiring beta commits to be ancestors of develop
blocks stabilization fixes created or cherry-picked onto a release-cycle branch.
Cherry-picking the same changes back produces different commits and does not
satisfy that requirement.

Allowing every historical release-cycle commit would enable a newly numbered
beta to omit fixes already selected for the release. Restricting sources to the
latest tip avoids that mistake, but reduces retry flexibility.

## Decision

Beta releases originate only from live remote tips of `release/cycle-*` branches.
Pushed tags must match at least one such tip; manual releases must match the
selected release-cycle branch's tip. Develop ancestry is not required.

Both workflows use one source-validation command through Make, following ADR
0012. Existing tag format, app-version matching, numbering, checks, and
distribution remain unchanged. Release/candidate retains its separate RC flow.

Each executed source validation applies the tip rule anew. Successful validation
pins the tagged commit for downstream jobs; no historical eligibility record is
introduced.

## Consequences

- Release-cycle fixes can reach successive betas independently of develop.
- Older commits fail fresh validation after their branch advances or is deleted.
- Failed downstream jobs can reuse successful validation through ordinary
  failed-job reruns; rerunning validation may require a new beta at the latest tip.
- A branch moving before validation can cause rejection; moving afterward does
  not change the commit being built.
- This policy does not enforce monotonically newer deployments across retries.
- Existing release-cycle branches need the workflows and validation command
  before they can use this process.
