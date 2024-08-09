# 1. Record architecture decisions

Date: 2024-08-05

## Status

Accepted

## Context

We agreed in the past to use ADR's, but we lost track of it as we were using confluence to keep
them. This concern was raised in the last collective, and we need to decide how to proceed.

## Decision

We will use Architecture Decision Records in the code and as part of the review process.
We will use the [Lightway ADR template](0000-template-lightway-adr.md) to keep the ADRs simple and
easy to maintain.

## Consequences

- We need to add a new folder to the repository, `docs/adr`, to keep the architecture decision
  records.
- Whenever a new refactoring or library is introduced, a new ADR should be created.
- You can always request in the Pull request review process to add a new ADR, if you think it's
  necessary.
