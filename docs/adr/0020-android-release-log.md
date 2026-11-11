# 20. Android release log

Date: 2026-11-09

## Status

Accepted

## Context

Beta, production, and regression results sit in separate workflow runs. QA and the release captain collect the links by hand. They need one page per release that shows what was built, what was tested, and what is missing. The page must not become a build, test, signing, or deployment gate.

Three identities stay separate. The application build is a run and attempt. The final signed APK is a SHA-256 digest. A QA execution has its own run and attempt, and it tests an APK that an earlier run built. Run names, test commits, and the value `latest` do not identify that APK.

## Decision

Release workflows export evidence. A separate reporter workflow sends it to Confluence. Confluence automation writes the page.

- The Beta and Prod build runs and the manual production regression run each export evidence records as `evidence.json` files. A build run exports two, a context record and a build record. The export steps are nonblocking. The artifacts are kept for 90 days. Binary artifact retention does not change.
- F-Droid builds, scheduled QA runs, and unrelated workflows are not reported. A production regression run is identified by its source workflow, `qa-android-prod-tests.yml`, and not by its `TAGS` value.
- The reporter runs on `workflow_run` completion. It uses trusted default-branch code, read-only permissions, and the `release-log` environment. It checks each export against GitHub run metadata. It treats downloaded files as data and never executes them.
- The reporter matches a regression to a production build by the SHA-256 of the tested APK. A missing, ambiguous, or expired match appears as a gap and never as a pass.
- The reporter sends one HTTPS POST to a Confluence automation incoming webhook. The request carries the `X-Automation-Webhook-Token` header and flat text fields: version, kind, result, links, commit, APK name and digest, deploy outcome, test counts, and gaps. It uses a 20-second timeout, follows no redirect, never retries on its own, and never logs the webhook URL or token.
- A Confluence automation rule keeps one page per release version. It appends each event to the page and creates the page for the first event. The rule lives in the Confluence automation settings. The repository documents none of it.
- The repository holds no Jira task, no Jira credentials, and no Confluence API token. The webhook URL and token are the only secrets. They live in the `release-log` environment.
- Publication is off unless the repository variable `RELEASE_LOG_PUBLICATION_ENABLED` is `true`. When it is off, the reporter writes a preview and sends nothing.

ADR 0016 source policy and ADR 0012 Make entrypoints do not change. The reporting commands run through Make.

## Consequences

- Reporting cannot change a build, test, signing, or deployment result. A reporter failure shows in its own run.
- HTTP success means Confluence accepted the request. The page content is checked in Confluence.
- The page grows by appended entries. Editors keep the QA assessment and release captain sections, which the rule never rewrites.
- Two first events for the same new version within seconds can create a second page with a numbered title. Events for one release arrive minutes or days apart, so the pilot accepts the risk.
- The rule creates the page for whichever event arrives first. If Beta 1 never arrives, Beta 2 creates the page, and a hand run of Beta 1 later appends below it. Each entry names its run, so the order on the page is the order of arrival.
- The rule finds the page by its title and location. Renaming or moving the page, or a page with the same title elsewhere, makes the next event create a numbered page. Editors leave the title and location alone.
- A cancelled run can leave its export missing. The reporter then sends the evidence it can verify and states the gap.
- A release-cycle branch without the export steps gets partial evidence until it receives them.
- The rule and the reporter are two parts with one field contract. A change to a field name needs a change in both.
