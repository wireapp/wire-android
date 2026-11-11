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

A manual production regression run joins it through the `QA Android Prod Regression RC` trigger.

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

A regression run of `qa-android-prod-tests.yml` is a release run only when a person dispatched it (`workflow_dispatch`) from the default branch or from a `release/*` branch. A scheduled run, a push, or a dispatch from any other branch is skipped the same way, before the reporter reads any artifact. The reporter reads the default branch from one repository call. A deflake source run must pass the same check. A production build record counts only when its own run was a `release` event on a version tag.

## Production regression evidence

Only a manual run of `qa-android-prod-tests.yml` with the `production` flavor writes a regression record. Scheduled runs, the other QA workflows, and runs of other flavors write none, including a deflake of a run of another flavor. The `TAGS` value does not identify a production regression, because scheduled runs use `@RC` too. A deflake run of a production regression writes a record as well.

The QA execution job adds two steps after the test summary and before cleanup: `Export release log QA record` and `Upload release log QA record`. Both continue on error and run after failed tests. A cancelled run writes nothing, and the page shows the evidence as unavailable. The normal job has exactly these two steps for the release log. The deflake job adds a third, `Upload release log skip marker`, described below. The artifact is `release-log-qa-<run id>-<attempt>`, kept for 90 days. For a manual run, `<run id>` is the run of `qa-android-prod-tests.yml`, because it calls the shared regression workflow.

The record holds:

- The SHA-256 of the downloaded APK files, under the APK names from the download step. A run that cannot read an APK records no digest and appears as unmatched.
- The suite, the `TAGS` selector, the mode (`full` or `deflake`), the retry count, the report link, and the test counts.
- The tests commit. For a deflake run, this is the restored test revision.
- A fingerprint of the deflake bundle this run wrote, and for a deflake run, the fingerprint of the bundle it used.

Test counts follow `total = passed + failed + skipped + missing`. A deflake run also needs `selected = total`. Counts that contradict each other are dropped and the entry shows no results. The recovered count, `passed on rerun`, comes from the merged results and not from the Allure summary. It counts only a test that failed in the first attempt and has a `passed` result in the last, as the merge step marks it. A test that was skipped or missing on the retry does not count. When the merged results are missing or unreadable, the count is unavailable and the entry omits it. A missing result, a run that executed no test, and an empty full run are gaps and never a pass.

Normal runs of the production workflow use two retries. A deflake run uses no retries unless the dispatch sets a count from 0 to 3. A test pins both defaults.

To write the record by hand from the QA step environment, run `make release-log/qa-export output=evidence.json`.

## How the reporter attributes a regression

The reporter reads the QA record of the run and checks it against the GitHub run: repository, run ID, attempt, and workflow. It does not require the tests commit to equal the run commit. It ignores artifacts of other attempts. It treats the record as data and drops a report link that is not on the repository's GitHub Pages site.

It then looks for the production build that made the tested APK. It lists the production runs from the 90 days before the regression and reads each attempt's build record. It needs exactly one build with the same APK file name and SHA-256. The digest proves the bytes are the same, so the version label never refuses a match; the label can differ from the release tag and its build number is not the version code. Zero matches, several matches, an expired or invalid record, and a listing that cannot finish are all gaps. The word `latest` and the tests commit never identify the APK.

A regression that matches no build is still reported. It goes to the page for the APK's version label. When the APK has no readable label, it goes to one shared page for unmatched regressions. The `gaps` field then says the regression is not attached to a build page, and the entry has no `buildRunUrl`.

A deflake entry must trace back to a full run. The reporter reads the source run's record and compares two fingerprints: the one in the deflake record, and the one the source run wrote for its bundle. It also checks the tests commit, the suite, and the selector. It follows the chain of source runs until it reaches a full run. A fingerprint that differs, a bundle with no fingerprint, a source with no record, and a chain that repeats all fail the attribution. The entry then appears with the gap and without a build link. The deflake entry keeps its own results and retry count, and the `tests` field says `not a full regression`.

A manual run of another flavor writes no record. A separate job in `qa-android-prod-tests.yml`, `release-log-skip-marker`, handles a normal run. The job has no `needs`, runs when the run mode is not a deflake and the `flavor` input is not `production`, continues on error, and has read-only repository access. It runs `make release-log/qa-skip` and uploads a small marker artifact, `release-log-qa-skip-<run id>-<attempt>`, kept for 90 days. The marker holds only the repository, run ID, and attempt. Because the job starts first and needs nothing, a run that is cancelled or fails before the export step still has its marker. A deflake reuses the flavor of its source run, so the `flavor` input says nothing about it. The deflake job decides instead. Its export step reads the flavor from the source bundle, and when that flavor is not `production` it writes the same marker (`RELEASE_LOG_SKIP_OUTPUT`). A step after it uploads the marker under the same name. The job `release-log-skip-marker` never runs for a deflake, and the normal job never uploads a marker, so only one upload uses that name in a run. A test checks this for a normal and a deflake run, with a production and a non-production flavor. When the record of the collected attempt is missing and a marker is present, the reporter ends with no entry and no red run, and the collect step logs that the run is not a release run. The marker may come from any attempt up to the collected one, because "Re-run failed jobs" does not run the marker job again and the run's inputs do not change. Each marker must match its own attempt and come from an unexpired artifact. A marker of a later attempt does not count. A record always wins over a marker. A missing record with no marker is still missing evidence. The job condition also skips scheduled QA runs, and the reporter skips them again.

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
| A regression sits on the shared page for unmatched regressions | The tested APK matched no production build and had no readable version label. Read the `gaps` field. Move the entry by hand if the release is known. |
| A regression shows `partial` with a gap about the source | The deflake source did not verify. Read the `gaps` field. The deflake tests still ran. |
| A page starts with a later build | The first entry never arrived, so the rule created the page for the first event it received. A hand run of the earlier build appends below it. |
| A release has a second page with a number in its title | The rule did not find the existing page. Someone renamed or moved it, or another page has the same title. Move the entries to one page, restore the title, and tell the rule's owner. |

## Limits

- Two first entries for one new version within seconds can create a second page with a numbered title. Releases produce entries minutes or days apart.
- A branch cut before this change has no export steps. Its runs show a gap and no APK digest.
