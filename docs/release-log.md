# Android release log

The release log lists each Android build and regression run for a release on one Confluence page. QA and the release captain read the page instead of collecting run links by hand. The log shows results. It never blocks a build, test, signing step, or deployment.

[ADR 0020](adr/0020-android-release-log.md) records the design.

## How it works

1. The Beta release and Production workflows each export two small files, `evidence.json`, as artifacts. One holds the release context. The other holds the digest of each signed APK. Both are kept for 90 days. A separate job exports the context, so a failed build still leaves it.
2. When the workflow ends, even if it failed or was cancelled, the `Release log` workflow starts. It runs `develop` code only.
3. It reads the run metadata and the two files from GitHub, and checks them against each other.
4. It sends one HTTPS POST with flat text fields to a Confluence automation webhook.
5. A Confluence automation rule appends the entry to the page for that release version, and creates the page for the first entry.

Production runs are logged the same way. The report lists only the Prod pipeline: `validate-release`, the Prod build, and one row for each deployment target (`s3`, `google-play`, `github-release`). It leaves out the F-Droid pipeline and the `deployment-summary` job, because a summary is not proof of any one target. F-Droid and other flavors export no evidence and get no entry. A rerun that only deploys reports its own attempt and jobs, and says the build evidence belongs to an earlier attempt.

Regression runs join the log in a later change.

## What an entry holds

| Field | Content |
| --- | --- |
| `releaseVersion` | Bare version, for example `4.38.0`. |
| `kind` | `Beta N`, `Production`, `Regression`, or `Deflake`. |
| `result` | `passed`, `failed`, `cancelled`, or `partial`. |
| `runUrl` | The run and attempt on GitHub. |
| `commit` | Short application SHA. |
| `apkName`, `apkSha256` | The final signed APK, after signing lineage. |
| `buildRunUrl` | The matched build run, for a regression only. |
| `deploy` | The result of each release job. |
| `tests` | Test counts, selector, and report link, for a regression only. A test that failed first and passed on a rerun shows as `N passed on rerun`. |
| `gaps` | `none`, or each missing or unverified item. |
| `eventId` | The event identity, `<kind>:<repository>:<run id>:<attempt>`. A replay carries the same value. |

`eventId` is the twelfth field. Add it to the webhook rule's field list and show it in the entry, so a replay is easy to spot on the page. The rule needs this change before the reporter sends the field.

A missing result is a gap and never a pass. A run with no executed test, or with more selected tests than counted tests, is never a pass either. A run whose evidence file is missing still gets an entry that says so.

## Manual and branch runs of a release workflow are skipped

Only a run that a tag push started (Beta) or a `release` event started (Production) is a release run. The reporter checks the event and the run's `head_branch`. A manual run, or a run from a branch, is not reported, even if the branch is named like a tag. This stops a user with write access from forging an entry with a modified workflow. A skipped run writes no payload, logs `Release log: run <id> is not a release run, nothing to report`, and ends green.

## The Confluence side

The rule lives in the Confluence automation settings. This repository documents none of it. The field table above is the contract between the reporter and the rule, so a change to a field name needs the same change in the rule. The rule's owner keeps the setup notes.

People edit the QA assessment and release captain sections of a page. The rule only appends below them. Do not rename or move a release page, because the rule finds it by its title and location.

## Set it up

Create the environment before the first run. If the workflow names an environment that does not exist, GitHub creates it with no branch limit.

1. Create the environment `release-log` and limit its deployment branches to `develop`.
2. Add two environment secrets from the webhook trigger card in the rule:
   - `RELEASE_LOG_WEBHOOK_URL`
   - `RELEASE_LOG_WEBHOOK_TOKEN`, sent as the `X-Automation-Webhook-Token` header.
3. Add the repository variable `RELEASE_LOG_PUBLICATION_ENABLED`. Set it to `false`.

The job runs only on the default branch, so a manual run from another branch does nothing.

Publication stays off until the variable is exactly `true`. While it is off, the workflow still builds each entry and shows it in the job summary. It sends nothing. Never paste the URL or token in a ticket, chat, or log.

## Run it by hand

Open the `Release log` workflow, choose `workflow_dispatch` on `develop`, and enter the run ID and attempt of a completed run. Tick `dry_run` to see the entry without sending it.

To see the fields for a payload file on your machine, with no network call:

```sh
make release-log/publish payload=payload.json summary=/dev/stdout dry_run=1
```

## When something goes wrong

| Symptom | Meaning and action |
| --- | --- |
| The reporter run fails and the summary shows a `partial` entry | A successful build had no export. The entry was sent with the gap stated. Read the collect step. |
| The reporter run fails with "version cannot be established" | The run came from a tag push or a release, but it has no valid release context and its tag no longer points at the run's commit. Nothing was sent, and a rerun repeats the result. Add the entry to the page by hand. |
| The reporter run fails with an HTTP status | Confluence did not accept the request. Check the rule's audit log, then run the workflow again by hand. A replay can add a duplicate entry. |
| The reporter succeeds and the page has no entry | HTTP success means Confluence accepted the request. Read the rule's audit log. |
| A page starts with a later build | The first entry never arrived, so the rule created the page for the first event it received. A hand run of the earlier build appends below it. |
| A release has a second page with a number in its title | The rule did not find the existing page. Someone renamed or moved it, or another page has the same title. Move the entries to one page, restore the title, and tell the rule's owner. |

## Limits

- Two first entries for one new version within seconds can create a second page with a numbered title. Releases produce entries minutes or days apart.
- A branch cut before this change has no export steps. Its runs show a gap and no APK digest.
